import os
import json

import yatest.common


def source_path(path):
    try:
        source = os.path.join('extsearch/video/ugc/sqs_moderation/mod2_tests/handlers/meta_notify', path)
        source = yatest.common.source_path(source)
        return source
    except (AttributeError, NotImplementedError):
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], path)


def load_data(file_path, schema):

    with open(source_path(file_path), r'r') as f:
        data = json.load(f)
    data, err = schema.load(data)
    if err:
        raise ValueError(f'Schema {schema} changed, test data must be changed!')
    return data
