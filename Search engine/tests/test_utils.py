import json
import os

import yatest
from row import Serp

SCALE = 'test'


def create_serp(components=None, query=None, config_id=None, table_index=None):
    query = query or {}
    parser_result = {}
    if components is not None:
        parser_result['components'] = components
    return Serp.from_row({'query': query, 'parser-result': parser_result, 'id': '1', 'ConfigId': config_id, 'tableIndex': table_index})


def create_failed_serp(error='Test error message'):
    return Serp.from_row({'query': {}, 'id': '1', 'error': error})


def create_component(label=None, url=None, component_type=None, wizard_type=None, alignment=None, json_slice=[]):
    result = {}
    if label is not None:
        result[SCALE] = {'name': label}
    if url is not None:
        result['componentUrl'] = {'pageUrl': url}

    result['json.slices'] = json_slice
    if component_type or wizard_type:
        info = {}
        result['componentInfo'] = info
        if component_type:
            info['type'] = component_type
        if wizard_type:
            info['wizardType'] = wizard_type
        if alignment:
            info['alignment'] = alignment

    return result


def read_json_test_data(filename, path_prefix='search/metrics/monitoring/tests/data/'):
    path = yatest.common.source_path(os.path.join(path_prefix, filename))
    with open(path) as f:
        return json.load(f)


def read_json_lines_test_data(filename, path_prefix='search/metrics/monitoring/tests/data/'):
    path = yatest.common.source_path(os.path.join(path_prefix, filename))
    with open(path) as f:
        for line in f:
            yield json.loads(line)


def read_text_test_data(filename, path_prefix='search/metrics/monitoring/tests/data/'):
    path = yatest.common.source_path(os.path.join(path_prefix, filename))
    with open(path) as f:
        return f.read()
