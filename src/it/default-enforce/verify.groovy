def log = new File(basedir,'build.log')

assert 1 == log.text.count('[INFO] Changes detected. Searching for available BOM dependencies.') : 'Should log when searching'
assert 1 == log.text.count('[INFO] No suitable BOMs found.') : 'Should not find any missing BOMs'
assert 1 == log.text.count('[INFO] BUILD SUCCESS'): 'Build succeeded.'