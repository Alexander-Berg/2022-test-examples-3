import json
import os

import yatest.common

from crypta.dmp.adobe.bin.transfer_meta_to_yt.lib import transfer
from crypta.dmp.common.data.python import meta
from crypta.lib.python import time_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


META_YT_PATH = "//adobe/meta"
DESTINATION_ID = 777
TARIFF = 1
HIERARCHY = []
ACL = ["xxx"]


class AdobeApiClientMock(object):
    def __init__(self, data):
        self.data = data

    def get_destination_mappings(self, destination_id):
        assert destination_id == DESTINATION_ID
        return self.data


def test_positive(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1500000000"

    with open(yatest.common.test_source_path("data/input.json")) as stream:
        data = json.load(stream)

    return tests.yt_test_func(
        yt_client,
        lambda: transfer.run(AdobeApiClientMock(data), yt_client, DESTINATION_ID, META_YT_PATH, HIERARCHY, TARIFF, ACL),
        output_tables=[
            (tables.YsonTable("output.yson", META_YT_PATH, yson_format="pretty"), (tests.SchemaEquals(meta.get_schema()), tests.Diff()))
        ]
    )
