import os
import json
import logging
import jsonschema
import yatest.common


def test_configs():
    schema_path = yatest.common.source_path('search/geo/tools/task_manager/configs/tests/schema.json')
    schema = json.load(open(schema_path))
    validator = jsonschema.Draft3Validator(schema)
    for config in os.listdir(yatest.common.source_path('search/geo/tools/task_manager/configs')):
        if config.endswith('.json'):
            config_path = os.path.join(yatest.common.source_path('search/geo/tools/task_manager/configs'), config)
            config = json.load(open(config_path))
            for name, params in config.iteritems():
                if not validator.is_valid(params):
                    logging.error('Failed to validate config for {name} in {f_name}'.format(name=name, f_name=os.path.basename(config_path)))
                    errors = [str(err) for err in validator.iter_errors(params)]
                    for err in errors:
                        logging.error(err)
                    raise Exception('Failed to validate config file')
                else:
                    logging.info('Config for {name} in {f_name} is valid'.format(name=name, f_name=os.path.basename(config_path)))
