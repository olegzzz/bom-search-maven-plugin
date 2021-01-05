package com.github.olegzzz.maven.plugin.bomsearch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.incremental.IncrementalBuildHelper;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.apache.maven.shared.utils.io.FileUtils;

public class IncrementalSupportMojo extends AbstractMojo {

  public static final String BOM_LIST_FILENAME = "bomList.lst";
  @SuppressWarnings("unused")
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  /**
   * Needed for storing the status for the incremental build support.
   */
  @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
  private MojoExecution mojoExecution;

  /**
   * The current build session instance.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  private IncrementalBuildHelper incBuildHelper;

  protected boolean isPomFileChanged() throws MojoExecutionException {
    DirectoryScanner directoryScanner = incBuildHelper.getDirectoryScanner();
    directoryScanner.setBasedir(project.getBasedir());
    directoryScanner.setIncludes("**/pom.xml");

    return incBuildHelper.inputFileTreeChanged(directoryScanner);
  }

  protected void writeBomList(Map<String, List<String>> boms) throws MojoExecutionException {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, List<String>> b : boms.entrySet()) {
      sb.append(b.getKey())
          .append(":")
          .append(String.join(",", b.getValue()))
          .append(System.lineSeparator());
    }

    File filename = new File(incBuildHelper.getMojoStatusDirectory(), BOM_LIST_FILENAME);
    try {
      FileUtils.fileWrite(filename.getAbsolutePath(), sb.toString());
    } catch (IOException e) {
      String msg = "Unable to write build status.";
      getLog().warn(msg);
      getLog().debug(msg, e);
    }
  }

  protected Map<String, List<String>> readBomList() throws MojoExecutionException, IOException {
    File filename = new File(incBuildHelper.getMojoStatusDirectory(), BOM_LIST_FILENAME);
    List<String> list = FileUtils.loadFile(filename);
    Map<String, List<String>> res = new HashMap<>();
    for (String coordinates : list) {
      String[] groupArtifacts = coordinates.split(":");
      String group = groupArtifacts[0];
      res.putIfAbsent(group, new LinkedList<>());

      List<String> artifacts =
          Arrays.stream(groupArtifacts[1].split(",")).collect(Collectors.toList());
      res.computeIfPresent(group, (key, values) -> {
        values.addAll(artifacts);
        return values;
      });
    }
    return res;
  }

  @Override
  public void execute() throws MojoExecutionException {
    incBuildHelper = new IncrementalBuildHelper(mojoExecution, session);
  }
}
