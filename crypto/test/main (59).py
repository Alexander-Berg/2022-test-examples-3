import json

from library.python.protobuf.json import proto2json

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.ltp.viewer.proto import (
    command_pb2,
    index_pb2,
)
from crypta.ltp.viewer.services.api.proto import api_pb2


def get_result(response, logbroker_client):
    return {
        "response": json.loads(proto2json.proto2json(response, proto2json.Proto2JsonConfig(map_as_object=True))),
        "logbroker": consumer_utils.read_all(logbroker_client.create_consumer(), timeout=10),
    }


def test_get_history(ltp_viewer_api_client, yuid, history, logbroker_client):
    command = api_pb2.TGetHistoryRequest()
    command.Id.Type = yuid.id_type
    command.Id.Value = yuid.id
    command.Owner = "owner"

    response = ltp_viewer_api_client.GetHistory(command)

    return get_result(response, logbroker_client)


def test_get_history_dated(ltp_viewer_api_client, yuid, history, logbroker_client):
    command = api_pb2.TGetHistoryRequest()
    command.Id.Type = yuid.id_type
    command.Id.Value = yuid.id
    command.Owner = "owner"
    command.FromDate = "2020-09-10"
    command.ToDate = "2020-09-20"

    response = ltp_viewer_api_client.GetHistory(command)

    return get_result(response, logbroker_client)


def test_preload_history(ltp_viewer_api_client, logbroker_client):
    command = command_pb2.TPreloadHistoryCommand()
    command.Id.Type = "yandexuid"
    command.Id.Value = "12345"
    command.Owner = "owner"

    response = ltp_viewer_api_client.PreloadHistory(command)

    return get_result(response, logbroker_client)


def test_preload_history_chunk(ltp_viewer_api_client, logbroker_client):
    command = command_pb2.TPreloadHistoryChunkCommand()
    command.Id.Type = "yandexuid"
    command.Id.Value = "12345"
    command.Log = index_pb2.LtpWatch
    command.Date = "2020-01-01"

    response = ltp_viewer_api_client.PreloadHistoryChunk(command for _ in range(2))

    return get_result(response, logbroker_client)


def test_expire(ltp_viewer_api_client, logbroker_client):
    command = command_pb2.TExpireCommand()
    command.TTLSeconds = 30

    response = ltp_viewer_api_client.Expire(command)

    return get_result(response, logbroker_client)


def test_drop_history(ltp_viewer_api_client, logbroker_client):
    command = command_pb2.TDropHistoryCommand()
    command.HistoryId = "id"

    response = ltp_viewer_api_client.DropHistory(command for _ in range(2))

    return get_result(response, logbroker_client)


def test_get_user_queries(ltp_viewer_api_client, yuid, yuid2, ydb_client, logbroker_client):
    owner = "new_owner"
    ydb_client.save_query(owner, yuid, "", "")
    ydb_client.save_query(owner, yuid2, "2021-01-20", "2021-01-23")

    command = api_pb2.TGetUserQueriesRequest()
    command.Owner = owner

    response = ltp_viewer_api_client.GetUserQueries(command)

    return get_result(response, logbroker_client)


def test_get_progress_missing(ltp_viewer_api_client, history, logbroker_client):
    command = api_pb2.TGetProgressRequest()
    command.Id.Type = "yandexuid"
    command.Id.Value = "12345"

    response = ltp_viewer_api_client.GetProgress(command)

    return get_result(response, logbroker_client)


def test_get_progress(ltp_viewer_api_client, yuid, history, logbroker_client):
    command = api_pb2.TGetProgressRequest()
    command.Id.Type = yuid.id_type
    command.Id.Value = yuid.id

    response = ltp_viewer_api_client.GetProgress(command)

    return get_result(response, logbroker_client)
