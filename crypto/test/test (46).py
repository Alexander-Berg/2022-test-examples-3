import json

from library.python.protobuf.json import proto2json

from crypta.siberia.bin.common.siberia_client import SiberiaClient
from crypta.siberia.bin.common import sample_stats_getter


def test_basic(mock_siberia_core_server, yt_stuff, user_sets):
    path = "//samples"

    yt_stuff.yt_client.write_table(path, ({"user_set_id": user_set_id} for user_set_id in user_sets))

    siberia_client = SiberiaClient(mock_siberia_core_server.host, mock_siberia_core_server.port)
    all_stats = sample_stats_getter.get_stats(yt_stuff.yt_client, siberia_client, "", path, retry_timeout=0.1)
    return {
        "stats": {user_set_id: json.loads(proto2json.proto2json(stats)) for user_set_id, stats in all_stats.iteritems()},
        "server": sorted(mock_siberia_core_server.commands),
    }
