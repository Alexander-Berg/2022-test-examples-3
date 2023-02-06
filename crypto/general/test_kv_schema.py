from yt import yson

from crypta.lib.python.yt import test_helpers
from crypta.lib.python.yt.dyntables import kv_schema


def test_kv_schema():
    return test_helpers.get_schema_for_canonization(yson.yson_to_json(kv_schema.get()))
