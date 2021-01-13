def log = new File(basedir,'build.log')

assert 1 == log.text.count('[INFO] Changes detected. Searching for available BOM dependencies.') : 'Should log when searching'
assert 1 == log.text.count('[WARNING] There are following BOMs available but not used: [org.springframework:spring-framework-bom]') : 'Should warn about found BOMs'
assert 1 == log.text.count('[INFO] BUILD SUCCESS'): 'Build succeeded.'