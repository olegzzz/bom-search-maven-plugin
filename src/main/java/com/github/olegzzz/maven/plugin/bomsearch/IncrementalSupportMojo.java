package com.github.olegzzz.maven.plugin.bomsearch;

import static java.util.Arrays.stream;

import java.io.File;
import java.io.IOException;
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
  public static final String ARTIFACT_DELIMITER = ",";
  public static final String GROUP_DELIMITER = ":";

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

  @Parameter(defaultValue = "true", property = "useIncrementalBuild")
  protected boolean useIncrementalBuild = true;

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
          .append(GROUP_DELIMITER)
          .append(String.join(ARTIFACT_DELIMITER, b.getValue()))
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
    List<String> list;
    try {
      list = FileUtils.loadFile(filename);
    } catch (IOException e) {
      String msg = "Unable to read build status.";
      getLog().warn(msg);
      getLog().debug(msg, e);
      throw e;
    }
    Map<String, List<String>> res = new HashMap<>();
    for (String coordinates : list) {
      String[] groupArtifactList = coordinates.split(GROUP_DELIMITER);
      String group = groupArtifactList[0];
      res.putIfAbsent(group, new LinkedList<>());

      List<String> artifacts = stream(groupArtifactList[1].split(ARTIFACT_DELIMITER))
          .collect(Collectors.toList());
      res.computeIfPresent(group, (key, values) -> {
        values.addAll(artifacts);
        return values;
      });
    }
    return res;
  }

  @Override
  public void execute() throws MojoExecutionException {
    if (useIncrementalBuild) {
      getLog().debug("Incremental build enabled.");
      incBuildHelper = new IncrementalBuildHelper(mojoExecution, session);
    } else {
      getLog().debug("Incremental build disabled.");
    }
  }
}
