package com.github.olegzzz.maven.plugin.bomsearch;

import static com.github.olegzzz.maven.plugin.bomsearch.SearchMojo.GET_HREF;
import static com.github.olegzzz.maven.plugin.bomsearch.SearchMojo.TITLE_BOM;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class SearchMojoTest {

  public static class TitleBomPredicate {

    public static final String TITLE = "title";

    @Test
    public void returns_False_if_value_is_empty() {
      assertFalse(TITLE_BOM.test(elementWithTitle("")));
    }

    @Test
    public void returns_False_if_value_is_null() {
      assertFalse(TITLE_BOM.test(elementWithTitle(null)));
    }

    @Test
    public void returns_True_if_value_contains_bom() {
      assertTrue(TITLE_BOM.test(elementWithTitle("some-dependency-bom")));
      assertTrue(TITLE_BOM.test(elementWithTitle("another-bom")));
    }

    @Test
    public void returns_False_if_value_contains_no_bom() {
      assertFalse(TITLE_BOM.test(elementWithTitle("notbom")));
      assertFalse(TITLE_BOM.test(elementWithTitle("another-dependency")));
    }

    private Element elementWithTitle(String title) {
      Element el = new Element("a");
      el.attr(TITLE, title);
      return el;
    }
  }

  public static class PackagingPomPredicate {

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

  public static class ScopeImportPredicate {

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

  public static class RemoveSlashFunction {

    @Test
    public void replaces_slash() {
      assertFalse(SearchMojo.REMOVE_SLASH.apply("foobar").contains("/"));
      assertFalse(SearchMojo.REMOVE_SLASH.apply("foo/").contains("/"));
      assertFalse(SearchMojo.REMOVE_SLASH.apply("foo/bar").contains("/"));
      assertFalse(SearchMojo.REMOVE_SLASH.apply("/").contains("/"));
      assertFalse(SearchMojo.REMOVE_SLASH.apply("//").contains("/"));
      assertFalse(SearchMojo.REMOVE_SLASH.apply("/bar").contains("/"));
      assertFalse(SearchMojo.REMOVE_SLASH.apply("/foo/").contains("/"));
    }

  }

  public static class GetHrefFunction {

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

  public static class MinCountPredicate {

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

    @Before
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

  public static class GetProjectBoms extends Base {

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
      Set<Dependency> projectBoms = mojo.getProjectBoms(project);

      assertEquals(1, projectBoms.size());
      assertTrue(projectBoms.stream().anyMatch(d -> "org.foobar".equals(d.getGroupId())));
    }

  }

  public static class SelectDependencies extends Base {

    @Test
    public void returns_dependencies_excluding_pom_packaging_and_import_scope() {

      MavenProject project = new MavenProject();
      List<Dependency> deps = asList(
          createDependency("org.foobar", "jar", "test"),
          createDependency("org.pompackaging", "pom", "test"),
          createDependency("org.importscope", "jar", "import")
      );
      project.setDependencies(deps);

      List<ArtifactGroup> selected = mojo.selectGroups(deps, Collections.emptySet());

      assertEquals(1, selected.size());
      assertTrue(selected.contains(new ArtifactGroup("org.foobar")));

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

      Dependency dep1 = new Dependency();
      dep1.setGroupId("org.baz");

      List<ArtifactGroup> selected =
          mojo.selectGroups(deps, new HashSet<>(Collections.singletonList(dep1)));
      assertEquals(1, selected.size());
      assertTrue(selected.contains(new ArtifactGroup("org.foobar")));
    }

  }

  public static class FilterDependencies extends Base {

    @Test
    public void returns_map_with_cardinality_greater_than_or_equal_to_given() {
      ArtifactGroup group1 = new ArtifactGroup("org.group1");
      ArtifactGroup group2 = new ArtifactGroup("org.group2");

      Collection<ArtifactGroup> groups = asList(group1, group1, group1, group2);
      List<ArtifactGroup> filtered = mojo.filterGroups(groups, 3);
      assertEquals(1, filtered.size());
      assertTrue(filtered.contains(group1));
    }

  }
}