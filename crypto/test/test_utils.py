import json
import os

import yatest


def arcadia_local_source_path(relative_path):
    return os.path.join(
        yatest.common.source_path('crypta/graph/matching/human/test'),
        relative_path
    )


def read_arcadia_local_json_dump(filename):
    # assumes that recs are in json array
    with open(arcadia_local_source_path(filename)) as f:
        for rec in json.load(f):
            yield rec
