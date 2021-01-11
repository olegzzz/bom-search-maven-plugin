package com.github.olegzzz.maven.plugin.bomsearch;

import org.jsoup.nodes.Document;

interface DocumentLoader {
  Document loadGroupByUri(String uri);
}
