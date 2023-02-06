from yt.yson import yson_types

from crypta.cm.services.common.serializers.id.string.python import id_string_serializer
from crypta.cm.services.common.serializers.match.record.python import match_record_serializer
from crypta.lib.native.database.python.record import TRecord


def read_match(yt_kv, ext_id):
    rows = list(yt_kv.yt_client.lookup_rows(yt_kv.replica.path, [{"key": id_string_serializer.ToString(ext_id)}]))

    if not rows:
        return None

    assert len(rows) == 1
    row = rows[0]

    return match_record_serializer.FromRecord(TRecord.Create(yson_types.get_bytes(row["key"]), yson_types.get_bytes(row["value"])))
