package com.github.olegzzz.maven.plugin.bomsearch;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.shared.incremental.IncrementalBuildHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(Enclosed.class)
public class IncrementalSupportMojoTest {

  public static class ReadStatusFile {

    @Rule
    public final MojoRule mojoRule = new MojoRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IncrementalBuildHelper buildHelper;

    private IncrementalSupportMojo mojo;

    @Before
    public void beforeEach() throws IllegalAccessException {
      mojo = new IncrementalSupportMojo();
      mojoRule.setVariableValueToObject(mojo, "incBuildHelper", buildHelper);
    }

    @Test
    public void returns_null_when_unable_to_read_status_dir() throws MojoExecutionException {
      when(buildHelper.getMojoStatusDirectory()).thenThrow(new MojoExecutionException("Expected"));
      assertNull(mojo.readStatusFile("foobar"));
    }

    @Test
    public void returns_empty_list_if_file_does_not_exist() throws MojoExecutionException {
      File fakeFile = new File("");
      when(buildHelper.getMojoStatusDirectory()).thenReturn(fakeFile);
      assertNull(mojo.readStatusFile("foobar"));
    }

  }

}