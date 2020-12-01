package olegzzz.github.io;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//todo: handle maven settings / passwords and repos urls

/**
 * Search mojo.
 */
@Mojo(name = "search", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SearchMojo extends AbstractMojo {

  public static final String ATTR_TITLE = "title";
  public static final String PACKAGING_TYPE_POM = "pom";
  public static final String SCOPE_IMPORT = "import";
  public static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2";

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchMojo.class);
  public static final String ATTR_HREF = "href";
  public static final String TAG_A = "a";

  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;
  @Parameter(defaultValue = "2")
  private int minRepeatGroups;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Set<String> bomGroups = getProjectBoms(project);
    Collection<Dependency> dependencies = filterDependencies(project.getDependencies(), bomGroups);
    Collection<Dependency> dedupDependency = dedupDependencies(dependencies, minRepeatGroups);
    Map<String, List<String>> boms = searchForBoms(dedupDependency);
    boms.forEach((groupId, bomList) -> LOGGER
        .info("Found following BOMs for groupId '{}': {}", groupId, bomList));
  }

  private Collection<Dependency> dedupDependencies(Collection<Dependency> deps,
                                                   int minRepeatGroups) {
    return deps
        .stream()
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .filter(e -> e.getValue() >= minRepeatGroups)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  private String groupIdToUri(String groupId) {
    return groupId.replaceAll("\\.", "/");
  }

  private Map<String, List<String>> searchForBoms(Collection<Dependency> deps) {
    // group -> [group:artifact:version]
    Map<String, List<String>> res = new HashMap<>();

    for (Dependency dep : deps) {
      String uri = buildGroupUri(dep);
      try {
        Document doc = loadGroup(uri);
        List<String> boms = selectBomReferences(doc, dep);
        if (!boms.isEmpty()) {
          res.put(dep.getGroupId(), boms);
        }
      } catch (IOException e) {
        LOGGER.error("Jsoup error ", e);
      }
    }
    return res;
  }

  private Document loadGroup(String uri) throws IOException {
    return Jsoup.connect(uri).get();
  }

  private List<String> selectBomReferences(Document doc, Dependency dep) {
    return doc
        .select(TAG_A)
        .stream()
        .filter(el -> el.attr(ATTR_TITLE).contains("-bom"))
        .map(el -> el.attr(ATTR_HREF))
        .map(href -> href.replaceAll("/", ""))
        .map(href -> String.format("%s:%s:%s", dep.getGroupId(), href, dep.getVersion()))
        .collect(Collectors.toList());
  }


  private String buildGroupUri(Dependency dep) {
    return MAVEN_CENTRAL_URL + "/" + groupIdToUri(dep.getGroupId());
  }

  private Collection<Dependency> filterDependencies(List<Dependency> dependencies,
                                                    Set<String> excludeGroups) {
    return dependencies
        .stream()
        .filter(d -> !excludeGroups.contains(d.getGroupId()))
        .collect(Collectors.toList());
  }

  private Set<String> getProjectBoms(MavenProject project) {
    Set<String> existingBOMs;

    if (project.getDependencyManagement() != null) {
      existingBOMs = project.getDependencies()
          .stream()
          .filter(d -> PACKAGING_TYPE_POM.equals(d.getType()))
          .filter(d -> SCOPE_IMPORT.equals(d.getScope()))
          .map(Dependency::getGroupId)
          .collect(Collectors.toSet());
    } else {
      existingBOMs = Collections.emptySet();
    }

    return existingBOMs;
  }

}
