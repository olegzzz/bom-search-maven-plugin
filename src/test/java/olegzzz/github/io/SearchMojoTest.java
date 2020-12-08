package olegzzz.github.io;

import static junit.framework.Assert.assertTrue;
import static olegzzz.github.io.SearchMojo.GET_HREF;
import static olegzzz.github.io.SearchMojo.TITLE_BOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Search mojo")
public class SearchMojoTest {

  @Nested
  @DisplayName("TITLE_BOM")
  public class TitleBom {

    public static final String TITLE = "title";

    @ParameterizedTest
    @NullAndEmptySource
    public void returnsFalseWhenNoTitleAttribute(String title) {
      assertFalse(TITLE_BOM.test(elementWithTitle(title)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"some-dependency-bom", "another-bom"})
    public void returnsTrueWhenTitleContainsBom(String title) {
      assertTrue(TITLE_BOM.test(elementWithTitle(title)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"notbom", "another-dependency"})
    public void returnsFalseWhenTitleDoesNotContainBom(String title) {
      assertFalse(TITLE_BOM.test(elementWithTitle(title)));
    }

    private Element elementWithTitle(String title) {
      Element el = new Element("a");
      el.attr(TITLE, title);
      return el;
    }
  }

  @Nested
  @DisplayName("PACKAGING_POM")
  public class PackagingPom {

    @Test
    public void returnTrueIfPackagingPom() {
      Dependency d = new Dependency();
      d.setType("pom");
      assertTrue(SearchMojo.PACKAGING_POM.test(d));
    }

    @Test
    public void returnFalseIfPackagingNotPom() {
      Dependency d = new Dependency();
      d.setType("jar");
      assertFalse(SearchMojo.PACKAGING_POM.test(d));
    }

  }

  @Nested
  @DisplayName("SCOPE_IMPORT")
  public class ScopeImport {

    @Test
    public void returnTrueIfScopeImport() {
      Dependency d = new Dependency();
      d.setScope("import");
      assertTrue(SearchMojo.SCOPE_IMPORT.test(d));
    }

    @Test
    public void returnsFalseIfScopeNotImport() {
      Dependency d = new Dependency();
      d.setScope("provided");
      assertFalse(SearchMojo.SCOPE_IMPORT.test(d));
    }

  }

  @Nested
  @DisplayName("REMOVE_SLASH")
  public class RemoveSlash {

    @ParameterizedTest
    @ValueSource(strings = {"foobar", "foo/", "foo/bar", "/", "//", "/bar", "/foo/"})
    public void returnsStringWithNoSlashes(String string) {
      assertFalse(SearchMojo.REMOVE_SLASH.apply(string).contains("/"));
    }

  }

  @Nested
  @DisplayName("GET_HREF")
  public class GetHref {

    @Test
    public void returnAttributeWhenPresent() {
      Element el = new Element("a");
      el.attr("href", "foobar");
      assertEquals("foobar", GET_HREF.apply(el));
    }

    @Test
    public void returnsEmptyStringWhenNotPresent() {
      Element el = new Element("a");
      el.attr("href", null);
      assertTrue(GET_HREF.apply(el).isEmpty());
    }

  }

  @Nested
  @DisplayName("MIN_COUNT_PREDICATE")
  public class MinCountPredicate {

    @Test
    public void returnsTrueIfValueGreaterThanOrEqualElseReturnFalse() {
      Map<String, Integer> map = new HashMap<>();
      map.put("foo", 15);
      map.put("bar", 5);
      map.put("baz", 1);
      Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
      assertTrue(SearchMojo.MIN_COUNT_PREDICATE.apply(2).test(iterator.next()));
      assertTrue(SearchMojo.MIN_COUNT_PREDICATE.apply(5).test(iterator.next()));
      assertFalse(SearchMojo.MIN_COUNT_PREDICATE.apply(2).test(iterator.next()));
    }

  }

  public abstract class Base {

    protected SearchMojo mojo;

    @BeforeEach
    public void setUp() {
      mojo = new SearchMojo();
    }

    protected Dependency createDependency(String groupId, String packaging, String scope) {
      Dependency d = new Dependency();
      d.setType(packaging);
      d.setScope(scope);
      d.setGroupId(groupId);
      return d;
    }

  }

  @Nested
  public class GetProjectBoms extends Base {

    @Test
    public void returnsEmptySetIfNoDependencyManagementSection() {
      MavenProject project = new MavenProject();
      assertTrue(mojo.getProjectBoms(project).isEmpty());
    }

    @Test
    public void returnsBomDependenciesIfAny() {
      MavenProject project = new MavenProject();
      Model model = new Model();
      DependencyManagement management = new DependencyManagement();
      List<Dependency> deps = Arrays.asList(
          createDependency("org.foobar", "pom", "import"),
          createDependency("net.foo", "jar", "provided"),
          createDependency("net.bar", "jar", "system")
      );
      management.setDependencies(deps);
      model.setDependencyManagement(management);
      project.setModel(model);
      Set<String> projectBoms = mojo.getProjectBoms(project);

      assertEquals(1, projectBoms.size());
      assertTrue(projectBoms.contains("org.foobar"));
    }

  }

  @Nested
  public class SelectDependencies extends Base {

    @Test
    public void returnsNonPomAndNonImportDependencies() {

      MavenProject project = new MavenProject();
      List<Dependency> deps = Arrays.asList(
          createDependency("org.foobar", "jar", "test"),
          createDependency("org.pompackaging", "pom", "test"),
          createDependency("org.importscope", "jar", "import")
      );
      project.setDependencies(deps);

      List<String> selected = mojo.selectDependencies(deps, Collections.emptySet());
      assertEquals(1, selected.size());
      assertTrue(selected.contains("org.foobar"));

    }

    @Test
    public void returnsDependenciesAbsentInTheFilter() {
      MavenProject project = new MavenProject();
      List<Dependency> deps = Arrays.asList(
          createDependency("org.foobar", "jar", "test"),
          createDependency("org.baz", "jar", "test"),
          createDependency("org.pompackaging", "pom", "test"),
          createDependency("org.importscope", "jar", "import")
      );
      project.setDependencies(deps);
      List<String> selected =
          mojo.selectDependencies(deps, new HashSet<>(Collections.singletonList("org.baz")));
      assertEquals(1, selected.size());
      assertTrue(selected.contains("org.foobar"));
    }

  }
}