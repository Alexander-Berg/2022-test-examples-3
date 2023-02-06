import time

from google.protobuf import json_format
from google.protobuf.empty_pb2 import Empty
import yatest.common

from crypta.lib.python.yt.test_helpers import utils
from crypta.siberia.bin.custom_audience.suggester.grpc import suggester_service_pb2

SUGGEST_REQUEST = suggester_service_pb2.TSuggestRequest(
    Text="diapers"
)
SUGGEST_REQUEST.Flags.AppsEnabled = True


def test_ping(suggester):
    response = suggester.client.Ping(Empty())
    assert "OK" == response.Message


def test_ready(suggester):
    response = suggester.client.Ready(Empty())
    assert "OK" == response.Message


def test_suggest(suggester):
    response = suggester.client.Suggest(SUGGEST_REQUEST)
    return json_format.MessageToDict(response)


def test_get_hosts(suggester):
    response = suggester.client.GetHosts(suggester_service_pb2.TGetHostsRequest(
        HostIds=[19000000000, 19000000001, 19000000002]
    ))
    return json_format.MessageToDict(response)


def test_get_apps(suggester):
    response = suggester.client.GetApps(suggester_service_pb2.TGetAppsRequest(
        AppIds=[19900000000, 19900000001, 19900000002]
    ))
    return json_format.MessageToDict(response)


def test_update_db(suggester, hosts_table, segments_table, apps_table, module_yt_stuff, update_period_seconds):
    first_response = suggester.client.Suggest(SUGGEST_REQUEST)

    utils.write_yson_table_from_file(
        module_yt_stuff.get_yt_client(),
        yatest.common.test_source_path("data/hosts_updated.yson"),
        hosts_table,
    )
    utils.write_yson_table_from_file(
        module_yt_stuff.get_yt_client(),
        yatest.common.test_source_path("data/segments_updated.yson"),
        segments_table,
    )
    utils.write_yson_table_from_file(
        module_yt_stuff.get_yt_client(),
        yatest.common.test_source_path("data/apps_updated.yson"),
        apps_table,
    )

    time.sleep(update_period_seconds * 3)

    second_response = suggester.client.Suggest(SUGGEST_REQUEST)

    return {
        "first_response": json_format.MessageToDict(first_response),
        "second_response": json_format.MessageToDict(second_response),
    }
