package com.github.olegzzz.maven.plugin.bomsearch;

import java.io.IOException;
import javax.annotation.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JsoupDocumentLoader implements DocumentLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsoupDocumentLoader.class);

  @Nullable
  public Document loadGroupByUri(String uri) {
    try {
      return Jsoup.connect(uri).get();
    } catch (IOException e) {
      LOGGER.warn(String.format("Unable to fetch dependencies for uri '%s' due to '%s'.", uri, e));
      return null;
    }
  }
}
