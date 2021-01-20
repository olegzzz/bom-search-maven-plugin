package com.github.olegzzz.maven.plugin.bomsearch;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import org.jsoup.nodes.Document;

public class DocumentParser {

  private final DocumentLoader loader;

  public DocumentParser(DocumentLoader documentLoader) {
    this.loader = documentLoader;
  }

  /**
   * Converts a {@link Document} into a list of artifact ids.
   *
   * @param uri full uri to a group on a maven repo, e.g. `https://repo.maven.apache.org/maven2/org/springframework/`
   * @return list of artifactIds of null
   */
  public List<String> parseArtifactsIds(String uri) {
    final Document document = loader.loadGroupByUri(uri);
    if (document != null) {
      return document
          .select(SearchMojo.TAG_A)
          .stream()
          .filter(SearchMojo.TITLE_BOM)
          .map(SearchMojo.GET_HREF)
          .map(SearchMojo.REMOVE_SLASH)
          .collect(toList());
    } else {
      return Collections.emptyList();
    }
  }
}
