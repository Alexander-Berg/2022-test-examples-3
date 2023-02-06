import json
import os
import yatest
from functools import reduce


def read_json_test_data(filename, path_prefix="search/metrics/converter/tests/data/"):
    path = yatest.common.source_path(os.path.join(path_prefix, filename))
    with open(path) as f:
        return json.load(f)


def check_fields_diff(components, expected_component, list_fields):
    for fields in list_fields:
        if not isinstance(fields, list):
            fields = [fields]
        _diff_params(components, expected_component, _diff_field_by_path(fields))


def _diff_params(actual, expected, f):
    if f(expected):
        assert f(actual) == f(expected)


def _diff_field_by_path(fields):
    def res(value):
        return reduce(lambda v, k: v.get(k, {}), fields, value)
    return res
