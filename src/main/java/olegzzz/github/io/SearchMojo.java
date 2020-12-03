package olegzzz.github.io;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.CollectionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//todo: handle maven settings / passwords and repos urls

/**
 * Mojo searches mave repo for available BOM artifacts for project dependencies.
 */
@Mojo(name = "search", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SearchMojo extends AbstractMojo {

  private static final Logger LOGGER = LoggerFactory.getLogger("bom-search-maven-plugin");

  public static final String ATTR_TITLE = "title";
  public static final String ATTR_HREF = "href";
  public static final String TAG_A = "a";

  public static final Predicate<Element> TITLE_BOM =
      el -> el.attr(ATTR_TITLE).contains("-bom");
  public static final Predicate<Dependency> PACKAGING_POM =
      d -> "pom".equals(d.getType());
  public static final Predicate<Dependency> SCOPE_IMPORT =
      d -> "import".equals(d.getScope());
  public static final Function<String, String> REMOVE_SLASH =
      s -> s.replaceAll("/", "");

  public static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2";

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @SuppressWarnings("unused")
  @Parameter(property = "minOccurrence", defaultValue = "2")
  private int minOccurrence;

  @SuppressWarnings("unused")
  @Parameter(property = "mavenRepoUrl", defaultValue = MAVEN_CENTRAL_URL)
  private String mavenRepoUrl;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Set<String> bomGroupIds = getProjectBoms(project);
    Collection<String> depGroupIds = selectDepsWithoutBoms(project.getDependencies(), bomGroupIds);
    Collection<String> dedupGroupIds = dedupDeps(depGroupIds, minOccurrence);
    Map<String, List<String>> boms = searchForBoms(dedupGroupIds);
    printResults(boms);
  }

  private void printResults(Map<String, List<String>> boms) {
    if (boms.isEmpty()) {
      LOGGER.info("No suitable BOMs found.");
    } else {
      LOGGER.info("Following BOMs can be used {}", flatten(boms));
    }
  }

  private List<String> flatten(Map<String, List<String>> boms) {
    return boms
        .entrySet()
        .stream()
        .map(e -> e.getValue()
            .stream()
            .map(artifactId -> String.format("%s:%s", e.getKey(), artifactId))
            .collect(Collectors.toList()))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private Collection<String> dedupDeps(Collection<String> groupIds, int minOccurrence) {
    return CollectionUtils.getCardinalityMap(groupIds)
        .entrySet()
        .stream()
        .filter(e -> e.getValue() >= minOccurrence)
        .map(Map.Entry::getKey)
        .collect(toList());
  }

  private String groupIdToUri(String groupId) {
    return groupId.replaceAll("\\.", "/");
  }

  private Map<String, List<String>> searchForBoms(Collection<String> depGroups) {
    Map<String, List<String>> res = new HashMap<>();

    for (String group : depGroups) {
      String uri = groupUri(group);
      Document doc = loadGroup(uri);
      List<String> boms = findBomArtifactIds(doc);
      if (!boms.isEmpty()) {
        res.put(group, boms);
      }
    }
    return res;
  }

  private Document loadGroup(String uri) {
    try {
      return Jsoup.connect(uri).get();
    } catch (IOException e) {
      LOGGER.error("Unable to fetch uri '{}'", uri);
      throw new RuntimeException(e);
    }
  }

  private List<String> findBomArtifactIds(Document doc) {
    return doc
        .select(TAG_A)
        .stream()
        .filter(TITLE_BOM)
        .map(el -> el.attr(ATTR_HREF))
        .map(REMOVE_SLASH)
        .collect(toList());
  }

  private String groupUri(String group) {
    return String.format("%s/%s", mavenRepoUrl, groupIdToUri(group));
  }

  private List<String> selectDepsWithoutBoms(List<Dependency> deps, Set<String> excludes) {
    return deps
        .stream()
        .filter(PACKAGING_POM.negate())
        .filter(SCOPE_IMPORT.negate())
        .map(Dependency::getGroupId)
        .filter(groupId -> !excludes.contains(groupId))
        .collect(toList());
  }

  private Set<String> getProjectBoms(MavenProject project) {
    return Optional.ofNullable(project.getDependencyManagement())
        .orElse(new DependencyManagement())
        .getDependencies()
        .stream()
        .filter(PACKAGING_POM)
        .filter(SCOPE_IMPORT)
        .map(Dependency::getGroupId)
        .collect(Collectors.toSet());
  }

}
