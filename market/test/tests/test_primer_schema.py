import importlib

tests = importlib.import_module('market.sre.conf.kraken.common.tests.primer_config_tests')

test_primer_schema = tests.test_primer_schema
test_unique_params = tests.test_unique_params
