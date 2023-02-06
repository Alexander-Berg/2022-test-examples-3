import time

from library.python.protobuf.json import proto2json
import requests
import yatest.common
import yt.wrapper as yt

from crypta.lib.proto.identifiers import id_pb2
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.ltp.viewer.lib import ltp_logs
from crypta.ltp.viewer.lib.test_helpers import transformers
from crypta.ltp.viewer.proto import (
    command_pb2,
    index_pb2,
)
from crypta.ltp.viewer.services.api.proto import (
    api_pb2,
)


def get_index_schema():
    return schema_utils.yt_schema_from_dict({
        column: "string"
        for column in ("id", "id_type", "sources")
    })


def get_ltp_browser_url_title_schema():
    return schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "ActionTimestamp": "int64",
        "EventName": "string",
        "Title": "string",
        "Yasoft": "string",
        "APIKey": "int64",
    }, sort_by=["id", "id_type", "ActionTimestamp"])


def get_ltp_visit_goals_v2_schema():
    return schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "ActionTimestamp": "int64",
        "CounterName": "string",
        "GoalName": "string",
        "GoalPatternTypes": "any",
        "GoalPatternUrls": "any",
        "GoalType": "string",
        "ReachedCounter": "uint64",
        "ReachedGoal": "uint64",
        "Autobudget": "boolean",
    }, sort_by=["id", "id_type", "ActionTimestamp"])


def get_ltp_rsya_shows_schema():
    return schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "ActionTimestamp": "uint64",
        "BannerText": "string",
        "BannerTitle": "string",
        "SecondTitle": "string",
        "SelectType": "int64",
        "RegionID": "int64",
        "BannerURL": "string",
        "URLClusterID": "uint64",
        "ProductType": "string",
        "BannerID": "int64",
        "PageID": "int64",
        "Referer": "string",
        "BMCategory1ID": "int64",
        "BMCategory2ID": "int64",
        "BMCategory3ID": "int64",
        "BMCategoryID": "int64",
    }, sort_by=["id", "id_type", "ActionTimestamp"])


def id_to_crypta_id_schema():
    return schema_utils.yt_dyntable_schema_from_dict(
        {
            "IdType": "string",
            "IdValue": "string",
            "CryptaId": "uint64",
        },
        ["IdType", "IdValue"],
        "farm_hash(IdValue) % 768",
    )


def crypta_id_to_graph_schema():
    return schema_utils.yt_dyntable_schema_from_dict(
        {
            "CryptaId": "uint64",
            "Graph": "string",
        },
        ["CryptaId"],
        "farm_hash(CryptaId) % 768",
    )


def canonize_stats(stats):
    for stat in stats["sensors"]:
        if stat["kind"] == "HIST_RATE":
            stat["sum"] = sum(stat["hist"]["buckets"])
            del stat["hist"]
    stats["sensors"].sort(key=lambda x: (x["labels"]["cmd_type"], x["labels"]["sensor"]))
    return stats


def test_worker(ltp_viewer_api_client, ltp_viewer_worker_config, local_yt, expired_history, expire_ttl, local_ydb, ydb_client):
    index_on_write = tables.OnWrite(
        attributes={"schema": get_index_schema()},
        sort_by=["id", "id_type"],
        row_transformer=transformers.index_row_transformer,
    )
    crypta_id_to_graph_on_write = tables.OnWrite(
        attributes={"schema": crypta_id_to_graph_schema()},
        row_transformer=transformers.crypta_id_to_graph_row_transformer,
    )

    date = "2021-10-20"
    yuid = id_pb2.TId(
        Type="yandexuid",
        Value="111",
    )
    crypta_id = id_pb2.TId(
        Type="crypta_id",
        Value="3000",
    )
    glueless_yuid = id_pb2.TId(
        Type="yandexuid",
        Value="555",
    )
    ids = [yuid, crypta_id, glueless_yuid]

    def test_func():
        for id_ in ids:
            request = command_pb2.TPreloadHistoryCommand(Id=id_, Owner="owner")
            ltp_viewer_api_client.PreloadHistory(request)

        ltp_viewer_api_client.Expire(command_pb2.TExpireCommand(TTLSeconds=int(expire_ttl.total_seconds())))

        time.sleep(30)
        result = canonize_stats(requests.get("http://{}:{}/metrics".format(ltp_viewer_worker_config.StatsHost, ltp_viewer_worker_config.StatsPort)).json())
        return {
            "history": {
                proto2json.proto2json(id_): proto2json.proto2json(ltp_viewer_api_client.GetHistory(api_pb2.TGetHistoryRequest(
                    Id=id_,
                    Owner="owner",
                )))
                for id_ in ids
            },
            "stats": result,
            "id-to-history-id": local_ydb.dump_table(ydb_client.path("id-to-history-id")),
            "files": sorted(item.name for item in local_ydb.client.list_directory(""))
        }

    return tests.yt_test_func(
        local_yt.get_yt_client(),
        test_func,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    "id_to_crypta_id.yson",
                    ltp_viewer_worker_config.Paths.IdToCryptaIdTable,
                    schema=id_to_crypta_id_schema(),
                    dynamic=True,
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.DynamicYsonTable(
                    "crypta_id_to_graph.yson",
                    ltp_viewer_worker_config.Paths.CryptaIdToGraphTable,
                    on_write=crypta_id_to_graph_on_write,
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    "index.yson",
                    ltp_viewer_worker_config.Paths.IndexPath,
                    on_write=index_on_write,
                ),
                tests.TableIsNotChanged()
            ),

            (
                tables.get_yson_table_with_schema(
                    "ltp_browser_url_title.yson",
                    yt.ypath_join(ltp_logs.LOGS_DICT[index_pb2.LtpBrowserUrlTitle].path, date),
                    schema=get_ltp_browser_url_title_schema(),
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.get_yson_table_with_schema(
                    "ltp_visit_goals_v2.yson",
                    yt.ypath_join(ltp_logs.LOGS_DICT[index_pb2.LtpVisitGoalsV2].path, date),
                    schema=get_ltp_visit_goals_v2_schema(),
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.get_yson_table_with_schema(
                    "ltp_rsya_shows.yson",
                    yt.ypath_join(ltp_logs.LOGS_DICT[index_pb2.LtpRsyaShows].path, date),
                    schema=get_ltp_rsya_shows_schema(),
                ),
                tests.TableIsNotChanged()
            ),
        ],
        return_result=True,
    )
