package com.github.olegzzz.maven.plugin.bomsearch;

import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.CollectionUtils;
import org.jsoup.nodes.Element;

//todo: handle maven settings / passwords and repos urls

/**
 * Searches for available BOM dependencies for current project.
 */
@Mojo(name = "search", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SearchMojo extends IncrementalSupportMojo {

  static final String TAG_A = "a";

  static final Predicate<Element> TITLE_BOM =
      el -> el.attr("title").contains("-bom");
  static final Predicate<Dependency> PACKAGING_POM =
      d -> "pom".equals(d.getType());
  static final Predicate<Dependency> SCOPE_IMPORT =
      d -> "import".equals(d.getScope());
  static final Function<String, String> REMOVE_SLASH =
      s -> s.replaceAll("/", "");
  static final Function<Element, String> GET_HREF =
      el -> el.attr("href");
  static final Function<Integer, Predicate<Map.Entry<?, Integer>>> MIN_COUNT_PREDICATE =
      min -> e -> e.getValue() >= min;

  static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2";

  @SuppressWarnings("unused")
  @Parameter(property = "bomsearch.minOccurrence", defaultValue = "2")
  private int minOccurrence;

  @SuppressWarnings("unused")
  @Parameter(property = "bomsearch.mavenRepoUrl", defaultValue = MAVEN_CENTRAL)
  private String mavenRepoUrl;

  private DocumentParser docParser;

  @Override
  public void execute() throws MojoExecutionException {
    super.execute();

    if (skip) {
      return;
    }

    docParser = new DocumentParser(new JsoupDocumentLoader());

    Collection<DependencyModel> boms;
    if (super.incremental) {
      if (isPomFilesChanged()) {
        getLog().info("Changes detected. Searching for available BOM dependencies.");
        boms = doSearch();
      } else {
        getLog().info("No changes detected.");
        boms = readBomList();
        if (boms == null) {
          boms = doSearch();
        }
      }
    } else {
      boms = doSearch();
    }
    writeBomList(boms);
    printResults(boms);
  }

  private Collection<DependencyModel> doSearch() {
    Set<Dependency> bomDependencies = getProjectBoms(project);
    List<String> groups = selectGroups(project.getDependencies(), bomDependencies);
    List<String> dedupGroups = filterGroups(groups, minOccurrence);
    return searchForBoms(dedupGroups);
  }

  private void printResults(Collection<DependencyModel> boms) {
    if (boms.isEmpty()) {
      getLog().info("No suitable BOMs found.");
    } else {
      getLog().info(String.format("Following BOMs found for module: %s.", boms));
    }
  }

  @VisibleForTesting
  protected List<String> filterGroups(Collection<String> groups, int minOccurrence) {
    return CollectionUtils
        .getCardinalityMap(groups)
        .entrySet()
        .stream()
        .filter(MIN_COUNT_PREDICATE.apply(minOccurrence))
        .map(Map.Entry::getKey)
        .collect(toList());
  }

  private String groupIdToUri(String groupId) {
    return groupId.replaceAll("\\.", "/");
  }

  @VisibleForTesting
  protected Collection<DependencyModel> searchForBoms(List<String> groups) {
    Collection<DependencyModel> res = new LinkedList<>();

    for (String group : groups) {
      String uri = groupUri(group);
      List<String> artifactIds = docParser.parseArtifactsIds(uri);
      if (!artifactIds.isEmpty()) {
        res.add(new DependencyModel(group, artifactIds.get(0)));
        if (artifactIds.size() > 1) {
          getLog().warn(String.format("More than one BOM artifact found four group %s.", group));
        }
      }
    }
    return res;
  }

  @VisibleForTesting
  String groupUri(String group) {
    return String.format("%s/%s", mavenRepoUrl, groupIdToUri(group));
  }

  @VisibleForTesting
  List<String> selectGroups(List<Dependency> deps, Set<Dependency> excludes) {
    Set<String> groupIds =
        excludes.stream().map(Dependency::getGroupId).collect(Collectors.toSet());
    return deps
        .stream()
        .filter(PACKAGING_POM.negate())
        .filter(SCOPE_IMPORT.negate())
        .filter(d -> !groupIds.contains(d.getGroupId()))
        .map(Dependency::getGroupId)
        .collect(toList());
  }

  @VisibleForTesting
  Set<Dependency> getProjectBoms(MavenProject project) {
    return Optional.ofNullable(project.getOriginalModel().getDependencyManagement())
        .orElse(new DependencyManagement())
        .getDependencies()
        .stream()
        .filter(PACKAGING_POM)
        .filter(SCOPE_IMPORT)
        .collect(Collectors.toSet());
  }

}
