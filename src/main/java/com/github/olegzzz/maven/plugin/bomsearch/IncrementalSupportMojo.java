package com.github.olegzzz.maven.plugin.bomsearch;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
   * Set this to 'true' to disable plugin.
   */
  @Parameter(property = "bomsearch.skip", defaultValue = "false")
  protected boolean skip;

  /**
   * The current build session instance.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(defaultValue = "true", property = "bomsearch.incremental")
  protected boolean incremental = true;

  private IncrementalBuildHelper incBuildHelper;

  protected boolean isPomFilesChanged() throws MojoExecutionException {
    DirectoryScanner directoryScanner = incBuildHelper.getDirectoryScanner();
    directoryScanner.setBasedir(project.getBasedir());
    directoryScanner.setIncludes("**/pom.xml");

    return incBuildHelper.inputFileTreeChanged(directoryScanner);
  }

  /**
   * Writes data represented as an iterable into file with name <code>filename</code> using
   * <code>toString</code>.
   *
   * @param fileName name of the status file
   * @param data     data to be written
   */
  protected void writeStatusFile(String fileName, String data) {
    File filename;
    try {
      filename = new File(incBuildHelper.getMojoStatusDirectory(), fileName);
    } catch (Exception e) {
      String msg = "Unable to read status directory.";
      getLog().debug(msg, e);
      getLog().warn(msg);
      return;
    }

    try {
      FileUtils.fileWrite(filename.getAbsolutePath(), data);
    } catch (Exception e) {
      String msg = String.format("Unable to write status file '%s'.", fileName);
      getLog().debug(msg, e);
      getLog().warn(msg);
    }
  }

  @Nullable
  protected List<String> readStatusFile(String fileName) {
    File file;
    try {
      file = new File(incBuildHelper.getMojoStatusDirectory(), fileName);
    } catch (MojoExecutionException e) {
      String msg = "Unable to read status directory.";
      getLog().debug(msg, e);
      getLog().warn(msg);
      return null;
    }
    try {
      List<String> data = FileUtils.loadFile(file);
      if (data.isEmpty()) {
        return null;
      } else {
        return data;
      }
    } catch (IOException e) {
      String msg = String.format("Unable to read build status file '%s'.", fileName);
      getLog().warn(msg);
      getLog().debug(msg, e);
      return null;
    }
  }

  // TODO: replace with SearchMojo.flatten
  protected void writeBomList(Map<ArtifactGroup, List<ArtifactId>> boms) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<ArtifactGroup, List<ArtifactId>> entry : boms.entrySet()) {
      sb.append(entry.getKey())
          .append(GROUP_DELIMITER)
          .append(entry.getValue().stream().map(ArtifactId::getValue)
              .collect(joining(ARTIFACT_DELIMITER)))
          .append(System.lineSeparator());
    }
    writeStatusFile(BOM_LIST_FILENAME, sb.toString());
  }

  @Nullable
  protected Map<ArtifactGroup, List<ArtifactId>> readBomList() {
    List<String> list = readStatusFile(BOM_LIST_FILENAME);
    if (list == null) {
      return null;
    }

    Map<ArtifactGroup, List<ArtifactId>> res = new HashMap<>();
    for (String coordinates : list) {
      String[] groupArtifactList = coordinates.split(GROUP_DELIMITER);
      String groupId = groupArtifactList[0];
      final ArtifactGroup artifactGroup = new ArtifactGroup(groupId);
      res.putIfAbsent(artifactGroup, new LinkedList<>());

      List<ArtifactId> artifacts = stream(groupArtifactList[1].split(ARTIFACT_DELIMITER))
          .map(ArtifactId::new)
          .collect(Collectors.toList());
      res.computeIfPresent(artifactGroup, (key, values) -> {
        values.addAll(artifacts);
        return values;
      });
    }
    return res;
  }

  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      return;
    }
    if (incremental) {
      getLog().debug("Incremental build enabled.");
      if (incBuildHelper == null) {
        incBuildHelper = new IncrementalBuildHelper(mojoExecution, session);
      }
    } else {
      getLog().debug("Incremental build disabled.");
    }
  }

}
