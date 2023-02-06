import json
import os
import yatest


def read_json_test_data(filename, path_prefix="search/metrics/nirvana_macro/tests/data/"):
    path = yatest.common.source_path(os.path.join(path_prefix, filename))
    with open(path) as f:
        return json.load(f)
