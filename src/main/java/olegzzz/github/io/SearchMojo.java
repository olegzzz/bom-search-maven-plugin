package olegzzz.github.io;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//todo: handle maven settings / passwords and repos urls

/**
 * Search mojo.
 */
@Mojo(name = "search", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SearchMojo extends AbstractMojo {

  public static final String ATTR_TITLE = "title";
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchMojo.class);
  public static final String PACKAGING_TYPE_POM = "pom";
  public static final String SCOPE_IMPORT = "import";

  /**
   * Main project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * Does something
   *
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Set<Dependency> deps = getDependencies();
    Map<Dependency, List<String>> probablyBOMs = getProbablyBOMs(deps);
    probablyBOMs.forEach((dep, list) -> LOGGER
        .info("Found following BOMs that might suite '{}:{}:{}': {}", dep.getGroupId(),
            dep.getArtifactId(), dep.getVersion(), list));
  }


  //todo: remove
  public void foo() {
    //https://repo.maven.apache.org/maven2
    try {
      String repoUrl = "https://repo.maven.apache.org/maven2";
      String groupId = "io.dropwizard";
      String version = "1.3.5";
      String url = repoUrl + "/" + artifactCoordinates(groupId);
      LOGGER.info("URL {}", url);
      Document doc = Jsoup.connect(url).get();
      Elements aTags = doc.select("a");
      if (aTags.size() > 0) {
        List<String> probablyBoms = aTags.stream()
            .filter(e -> e.attr("title").contains("-bom"))
            .map(e -> String
                .format("%s:%s:%s", groupId, e.attr("href").replaceAll("/", ""), version))
            .collect(Collectors.toList());
        LOGGER.info("Probably BOMs for {} {}", "io.dropwizard", probablyBoms);
      }

    } catch (IOException e) {
      LOGGER.error("Jsoup error ", e);
    }

  }

  private String artifactCoordinates(String groupId) {
    return groupId.replaceAll("\\.", "/");
  }

  private Map<Dependency, List<String>> getProbablyBOMs(Collection<Dependency> deps) {
    String repoUrl = "https://repo.maven.apache.org/maven2";

    Map<Dependency, List<String>> res = new HashMap<>();
    for (Dependency dep : deps) {
      String uri = repoUrl + "/" + artifactCoordinates(dep.getGroupId());
      try {
        Document doc = Jsoup.connect(uri).get();
        Elements aTags = doc.select("a");
        if (aTags.size() > 0) {
          List<String> probablyBOMs = aTags.stream()
              .filter(e -> e.attr(ATTR_TITLE).contains("-bom"))
              .map(e -> e.attr("href").replaceAll("/", ""))
              .map(e -> String.format("%s:%s:%s", dep.getGroupId(), e, dep.getVersion()))
              .collect(Collectors.toList());
          if (probablyBOMs.size() > 0) {
            res.put(dep, probablyBOMs);
          }
        }
      } catch (IOException e) {
        LOGGER.error("Jsoup error ", e);
      }
    }
    return res;
  }

  private Set<Dependency> getDependencies() {
    Set<String> existingBOMs = Optional.ofNullable(project.getDependencyManagement())
        .orElse(new DependencyManagement())
        .getDependencies()
        .stream()
        .filter(d -> PACKAGING_TYPE_POM.equals(d.getType()))
        .filter(d -> SCOPE_IMPORT.equals(d.getScope()))
        .map(Dependency::getGroupId).collect(Collectors.toSet());

    return project.getDependencies().stream()
        .filter(d -> !existingBOMs.contains(d.getGroupId()))
        .collect(Collectors.toSet());
  }

}
