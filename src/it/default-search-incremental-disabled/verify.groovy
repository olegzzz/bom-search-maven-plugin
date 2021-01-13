def bomList = new File( basedir, "target/maven-status/bom-search-maven-plugin/search/default-cli/bomList.lst" )
assert !bomList.exists()

def log = new File(basedir,'build.log')
assert 1 == log.text.count('[DEBUG] Incremental build disabled.') : 'Should indicate incremental build disabled'
assert 1 == log.text.count('[INFO] Following BOMs found for module: [org.springframework:spring-framework-bom].') : 'Should list found BOMs'
