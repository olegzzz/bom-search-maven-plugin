package com.github.olegzzz.maven.plugin.bomsearch;

import static com.github.olegzzz.maven.plugin.bomsearch.SearchMojo.GET_HREF;
import static com.github.olegzzz.maven.plugin.bomsearch.SearchMojo.TITLE_BOM;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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
      Set<String> projectBoms = mojo.getProjectBoms(project);

      assertEquals(1, projectBoms.size());
      assertTrue(projectBoms.contains("org.foobar"));
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

  public static class FilterDependencies extends Base {

    @Test
    public void returns_map_with_cardinality_greater_than_or_equal_to_given() {
      String group1 = "org.group1";
      Collection<String> groups = asList(group1, group1, group1, "org.group2");
      Collection<String> filtered = mojo.filterDependencies(groups, 3);
      assertEquals(1, filtered.size());
      assertTrue(filtered.contains(group1));
    }

  }

  public static class SearchForBoms {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Spy
    private SearchMojo mojo;

    @Test
    public void returns_boms_If_available() {
      doAnswer(returnsFirstArg()).when(mojo).groupUri(anyString());
      Document group1Doc = mock(Document.class);
      Document group2Doc = mock(Document.class);
      doReturn(group1Doc)
          .doReturn(group2Doc)
          .doReturn(null) // artifact for private group not found in central repo
          .when(mojo).loadGroup(anyString(), any(SearchMojo.DocumentLoader.class));
      List<String> bomsList = singletonList("bom");
      doReturn(emptyList())
          .doReturn(bomsList)
          .doReturn(emptyList())
          .when(mojo).parseBomArtifactIds(any(Document.class));
      Map<String, List<String>> boms =
          mojo.searchForBoms(asList("group1", "group2", "privateGroup"));
      assertEquals(1, boms.size());
      assertTrue(boms.get("group2").contains("bom"));

    }

    @Test
    public void returns_empty_list_If_no_boms_available() {
      doAnswer(returnsFirstArg()).when(mojo).groupUri(anyString());
      Document document = mock(Document.class);
      doReturn(document).when(mojo).loadGroup(anyString(), any(SearchMojo.DocumentLoader.class));
      doReturn(emptyList()).when(mojo).parseBomArtifactIds(eq(document));
      Map<String, List<String>> boms = mojo.searchForBoms(singletonList("group1"));
      assertTrue(boms.isEmpty());
    }


  }

  public static class LoadGroup {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Spy
    private SearchMojo mojo;

    @Mock
    private Log log;

    @Before
    public void beforeEach() {
      mojo.setLog(log);
    }

    @Test
    public void returns_null_if_unable_to_load() throws IOException {
      SearchMojo.DocumentLoader loader = mock(SearchMojo.DocumentLoader.class);
      String uri = "https://foobar.com";
      when(loader.load(anyString()))
          .thenThrow(new HttpStatusException("Expected exception. Not found", 404, uri));
      assertNull(mojo.loadGroup(uri, loader));
      verify(log).warn(startsWith("Unable to fetch dependencies for uri 'https://foobar.com'"));
    }

    @Test
    public void returns_document_if_can_load() throws IOException {
      SearchMojo.DocumentLoader loader = mock(SearchMojo.DocumentLoader.class);
      String uri = "https://foobar.com";
      Document doc = mock(Document.class);
      when(loader.load(anyString())).thenReturn(doc);
      assertSame(mojo.loadGroup(uri, loader), doc);
    }

  }


}