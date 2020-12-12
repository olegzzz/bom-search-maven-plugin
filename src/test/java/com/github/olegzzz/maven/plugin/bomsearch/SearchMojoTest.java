package com.github.olegzzz.maven.plugin.bomsearch;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static com.github.olegzzz.maven.plugin.bomsearch.SearchMojo.GET_HREF;
import static com.github.olegzzz.maven.plugin.bomsearch.SearchMojo.TITLE_BOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collection;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.AdditionalAnswers;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Search mojo")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SearchMojoTest {

  @Nested
  @DisplayName("TITLE_BOM predicate")
  public class TitleBomPredicate {

    public static final String TITLE = "title";

    @ParameterizedTest
    @NullAndEmptySource
    public void returns_False_if_value_is_empty(String title) {
      assertFalse(TITLE_BOM.test(elementWithTitle(title)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"some-dependency-bom", "another-bom"})
    public void returns_True_if_value_contains_bom(String title) {
      assertTrue(TITLE_BOM.test(elementWithTitle(title)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"notbom", "another-dependency"})
    public void returns_False_if_value_contains_no_bom(String title) {
      assertFalse(TITLE_BOM.test(elementWithTitle(title)));
    }

    private Element elementWithTitle(String title) {
      Element el = new Element("a");
      el.attr(TITLE, title);
      return el;
    }
  }

  @Nested
  @DisplayName("PACKAGING_POM predicate")
  public class PackagingPomPredicate {

    @Test
    public void returns_True_If_value_is_pom() {
      Dependency d = new Dependency();
      d.setType("pom");
      assertTrue(SearchMojo.PACKAGING_POM.test(d));
    }

    @Test
    public void returns_False_If_value_is_not_pom() {
      Dependency d = new Dependency();
      d.setType("jar");
      assertFalse(SearchMojo.PACKAGING_POM.test(d));
    }

  }

  @Nested
  @DisplayName("SCOPE_IMPORT predicate")
  public class ScopeImportPredicate {

    @Test
    public void returns_True_If_value_is_import() {
      Dependency d = new Dependency();
      d.setScope("import");
      assertTrue(SearchMojo.SCOPE_IMPORT.test(d));
    }

    @Test
    public void returns_False_If_value_is_not_import() {
      Dependency d = new Dependency();
      d.setScope("provided");
      assertFalse(SearchMojo.SCOPE_IMPORT.test(d));
    }

  }

  @Nested
  @DisplayName("REMOVE_SLASH function")
  public class RemoveSlashFunction {

    @ParameterizedTest(name = "in string \"{0}\"")
    @ValueSource(strings = {"foobar", "foo/", "foo/bar", "/", "//", "/bar", "/foo/"})
    public void replaces_slash(String string) {
      assertFalse(SearchMojo.REMOVE_SLASH.apply(string).contains("/"));
    }

  }

  @Nested
  @DisplayName("GET_HREF function")
  public class GetHrefFunction {

    @Test
    public void returns_value_when_attribute_present() {
      Element el = new Element("a");
      el.attr("href", "foobar");
      assertEquals("foobar", GET_HREF.apply(el));
    }

    @Test
    public void returns_empty_when_attribute_absent() {
      Element el = new Element("a");
      el.attr("href", null);
      assertTrue(GET_HREF.apply(el).isEmpty());
    }

  }

  @Nested
  @DisplayName("MIN_COUNT_PREDICATE")
  public class MinCountPredicate {

    @Test
    public void returns_True_If_value_greater_than_or_equal_to_argument() {
      Map<String, Integer> map = new HashMap<>();
      map.put("foo", 15);
      map.put("bar", 5);
      Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
      assertTrue(SearchMojo.MIN_COUNT_PREDICATE.apply(2).test(iterator.next()));
      assertTrue(SearchMojo.MIN_COUNT_PREDICATE.apply(5).test(iterator.next()));

    }

    @Test
    public void returns_False_if_value_is_less_than_argument() {
      Map<String, Integer> map = new HashMap<>();
      map.put("baz", 1);
      Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
      assertFalse(SearchMojo.MIN_COUNT_PREDICATE.apply(2).test(iterator.next()));
    }

  }

  public static abstract class Base {

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
    public void returns_empty_set_If_no_DependencyManagement_section() {
      MavenProject project = new MavenProject();
      assertTrue(mojo.getProjectBoms(project).isEmpty());
    }

    @Test
    public void returns_dependencies_with_BOM_packaging_If_any() {
      MavenProject project = new MavenProject();
      Model model = new Model();
      DependencyManagement management = new DependencyManagement();
      List<Dependency> deps = asList(
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
    public void returns_dependencies_excluding_pom_packaging_and_import_scope() {

      MavenProject project = new MavenProject();
      List<Dependency> deps = asList(
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
    public void returns_collections_disjunction() {
      MavenProject project = new MavenProject();
      List<Dependency> deps = asList(
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

  @Nested
  public class FilterDependencies extends Base {

    @Test
    public void returns_map_with_cardinality_greater_than_or_equal_to_given() {
      String group1 = "org.group1";
      Collection<String> groups = asList(group1, group1, group1, "org.group2");
      Collection<String> filtered = mojo.filterDependencies(groups, 3);
      assertEquals(1, filtered.size());
      assertTrue(filtered.contains(group1));
    }

  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  public class SearchForBoms {

    @Spy
    private SearchMojo mojo;

    @Test
    public void returns_boms_If_available() {
      doAnswer(AdditionalAnswers.returnsFirstArg()).when(mojo).groupUri(anyString());
      Document document = mock(Document.class);
      doReturn(document).when(mojo).loadGroup(anyString());
      List<String> bomsList = singletonList("bom");
      doReturn(emptyList())
          .doReturn(bomsList)
          .doReturn(emptyList())
          .when(mojo).parseBomArtifactIds(eq(document));
      Map<String, List<String>> boms = mojo.searchForBoms(asList("group1", "group2", "group3"));
      assertEquals(1, boms.size());
      assertTrue(boms.get("group2").contains("bom"));

    }

    @Test
    public void returns_empty_list_If_no_boms_available() {
      doAnswer(AdditionalAnswers.returnsFirstArg()).when(mojo).groupUri(anyString());
      Document document = mock(Document.class);
      doReturn(document).when(mojo).loadGroup(anyString());
      doReturn(emptyList()).when(mojo).parseBomArtifactIds(eq(document));
      Map<String, List<String>> boms = mojo.searchForBoms(singletonList("group1"));
      assertTrue(boms.isEmpty());
    }

  }


}