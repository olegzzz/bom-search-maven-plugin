def log = new File(basedir,'build.log')

assert 1 == log.text.count('[INFO] Changes detected. Searching for available BOM dependencies.') : 'Should log when searching'
assert (1 == log.text.count('[WARNING] Following BOMs available but not used: [org.springframework:spring-framework-bom, io.dropwizard:dropwizard-bom]')
        || 1 == log.text.count('[WARNING] Following BOMs available but not used: [io.dropwizard:dropwizard-bom, org.springframework:spring-framework-bom]'))
assert 1 == log.text.count('[INFO] BUILD FAILURE'): 'Build failed.'