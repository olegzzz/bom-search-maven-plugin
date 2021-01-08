def bomList = new File( basedir, "target/maven-status/bom-search-maven-plugin/search/default-cli/bomList.lst" );
def lines = bomList.readLines();
assert lines[0] == 'org.springframework:spring-framework-bom'

def log = new File(basedir,'build.log')
assert 1 == log.text.count('[INFO] Changes detected. Searching for available BOM dependencies.') : 'Should log when searching'
assert 1 == log.text.count('[INFO] Following BOMs found for module: [org.springframework:spring-framework-bom].') : 'Should list found BOMs'
