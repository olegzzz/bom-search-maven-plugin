package com.github.olegzzz.maven.plugin.bomsearch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.incremental.IncrementalBuildHelper;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class SearchMojoTestCase extends AbstractMojoTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  public void test_config_params_override() throws Exception {
    SearchMojo mojo = getMojo("search-config-override");

    assertEquals(3, getVariableValueFromObject(mojo, "minOccurrence"));
    assertEquals("https://foobar", getVariableValueFromObject(mojo, "mavenRepoUrl"));
  }

  public void test_empty_search() throws Exception {
    SearchMojo mojo = getMojo("search-basic");

    Log log = mock(Log.class);
    mojo.setLog(log);

    mojo.execute();

    verify(log).debug(startsWith("Incremental build disabled."));
    verify(log).info(startsWith("No suitable BOMs found."));
  }

  public void test_basic_search() throws Exception {
    SearchMojo mojo = getMojo("search-basic");

    Log log = mock(Log.class);
    mojo.setLog(log);
    MavenProject project = (MavenProject) getVariableValueFromObject(mojo, "project");

    Dependency dep1 = new Dependency();
    dep1.setGroupId("org.springframework");
    dep1.setArtifactId("spring-context");

    Dependency dep2 = new Dependency();
    dep2.setGroupId("org.springframework");
    dep2.setArtifactId("spring-core");

    project.setDependencies(Arrays.asList(dep1, dep2));

    DocumentParser docParserMock = mock(DocumentParser.class);
    when(docParserMock.parseArtifactsIds(anyString()))
        .thenReturn(Collections.singletonList(new ArtifactId("spring-bom")));
    setVariableValueToObject(mojo, "docParser", docParserMock);

    mojo.execute();

    verify(log).debug(startsWith("Incremental build disabled."));
    verify(log).info(
        startsWith("Following BOMs found for module: [org.springframework:spring-framework-bom]."));

  }

  public void test_incremental_with_changes() throws Exception {
    SearchMojo mojo = getMojo("search-incremental");

    Log log = mock(Log.class);
    mojo.setLog(log);
    MavenProject project = (MavenProject) getVariableValueFromObject(mojo, "project");

    Dependency dep1 = new Dependency();
    dep1.setGroupId("org.springframework");
    dep1.setArtifactId("spring-context");

    Dependency dep2 = new Dependency();
    dep2.setGroupId("org.springframework");
    dep2.setArtifactId("spring-core");

    project.setDependencies(Arrays.asList(dep1, dep2));

    DocumentParser docParserMock = mock(DocumentParser.class);
    when(docParserMock.parseArtifactsIds(anyString()))
        .thenReturn(Collections.singletonList(new ArtifactId("spring-bom")));
    setVariableValueToObject(mojo, "docParser", docParserMock);
    IncrementalBuildHelper buildHelperMock = mock(IncrementalBuildHelper.class);
    when(buildHelperMock.inputFileTreeChanged(any(DirectoryScanner.class))).thenReturn(true);
    DirectoryScanner dirScannerMock = mock(DirectoryScanner.class);
    when(buildHelperMock.getDirectoryScanner()).thenReturn(dirScannerMock);
    setVariableValueToObject(mojo, "incBuildHelper", buildHelperMock);

    mojo.execute();

    verify(log).debug(startsWith("Incremental build enabled."));
    verify(log).info(
        startsWith("Following BOMs found for module: [org.springframework:spring-framework-bom]."));
    reset(log);

    mojo.execute();

    verify(log).debug(startsWith("Incremental build enabled."));
    verify(log).info(startsWith("Changes detected. Searching for available BOM dependencies."));
    verify(log).info(
        startsWith("Following BOMs found for module: [org.springframework:spring-framework-bom]."));
  }

  public void test_incremental_no_changes() throws Exception {
    SearchMojo mojo = getMojo("search-incremental");

    Log log = mock(Log.class);
    mojo.setLog(log);
    MavenProject project = (MavenProject) getVariableValueFromObject(mojo, "project");

    Dependency dep1 = new Dependency();
    dep1.setGroupId("org.springframework");
    dep1.setArtifactId("spring-context");

    Dependency dep2 = new Dependency();
    dep2.setGroupId("org.springframework");
    dep2.setArtifactId("spring-core");

    project.setDependencies(Arrays.asList(dep1, dep2));

    DocumentParser docParserMock = mock(DocumentParser.class);
    when(docParserMock.parseArtifactsIds(anyString()))
        .thenReturn(Collections.singletonList(new ArtifactId("spring-bom")));
    setVariableValueToObject(mojo, "docParser", docParserMock);
    IncrementalBuildHelper buildHelperMock = mock(IncrementalBuildHelper.class);
    when(buildHelperMock.inputFileTreeChanged(any(DirectoryScanner.class))).thenReturn(false);
    DirectoryScanner dirScannerMock = mock(DirectoryScanner.class);
    when(buildHelperMock.getDirectoryScanner()).thenReturn(dirScannerMock);
    setVariableValueToObject(mojo, "incBuildHelper", buildHelperMock);

    mojo.execute();

    verify(log).debug(startsWith("Incremental build enabled."));
    verify(log).info(
        startsWith("Following BOMs found for module: [org.springframework:spring-framework-bom]."));
    reset(log);

    mojo.execute();

    verify(log).debug(startsWith("Incremental build enabled."));
    verify(log).info(startsWith("No changes detected."));
    verify(log).info(
        startsWith("Following BOMs found for module: [org.springframework:spring-framework-bom]."));


  }

  private SearchMojo getMojo(String location) throws Exception {
    File pom = new File(String.format("target/test-classes/unit/%s/plugin-config.xml", location));
    assertTrue(pom.exists());

    SearchMojo mojo = (SearchMojo) lookupMojo("search", pom);
    assertNotNull(mojo);

    setVariableValueToObject(mojo, "project", getMockMavenProject());
    setVariableValueToObject(mojo, "session", getMockMavenSession());
    setVariableValueToObject(mojo, "mojoExecution", getMockMojoExecution());


    return mojo;
  }

  private MavenSession getMockMavenSession() {
    MavenSession session = mock(MavenSession.class);
    when(session.getCurrentProject()).thenReturn(getMockMavenProject());
    return session;
  }

  private MavenProject getMockMavenProject() {
    MavenProject mp = new MavenProject();
    mp.getBuild().setDirectory("target");
    mp.getBuild().setOutputDirectory("target/classes");
    mp.getBuild().setSourceDirectory("src/main/java");
    mp.getBuild().setTestOutputDirectory("target/test-classes");
    return mp;
  }

  private MojoExecution getMockMojoExecution() {
    MojoDescriptor md = new MojoDescriptor();
    md.setGoal("search");

    MojoExecution me = new MojoExecution(md);

    PluginDescriptor pd = new PluginDescriptor();
    pd.setArtifactId("bom-search-maven-plugin");
    md.setPluginDescriptor(pd);

    return me;
  }

}