import base64
import json

from ads.bsyeti.libs.log_protos import crypta_profile_pb2
from library.python.protobuf.json import proto2json
import yatest.common

from crypta.graph.bochka.lib import tasks
from crypta.graph.rt.events.proto.soup_pb2 import TSoupEvent
from crypta.graph.soup.config.proto.bigb_pb2 import TLinks
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import tables, tests


def crypta_log_row_transformer(row):
    proto = crypta_profile_pb2.TCryptaLog()
    proto.ParseFromString(row["value"])
    row["value"] = json.loads(proto2json.proto2json(proto))
    return row


def test_yt_to_bb_push_task(yt_stuff, conf, frozen_time, logbroker_client):
    source_path = "//source_path"

    yt_results = tests.yt_test_func(
        yt_client=yt_stuff.get_yt_client(),
        func=lambda: test_helpers.execute(tasks.Yt2BBPushTask(source_path=source_path)),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(tables.YsonTable("source.yson", source_path), tests.TableIsNotChanged())],
        output_tables=[
            (
                tables.YsonTable(
                    "collector.yson",
                    "//collector/fresh/prefix_1500000000",
                    on_read=tables.OnRead(row_transformer=crypta_log_row_transformer),
                    yson_format="pretty",
                ),
                tests.Diff(),
            )
        ],
    )
    assert [] == consumer_utils.read_all(logbroker_client.create_consumer(), timeout=30)
    return {"yt": yt_results}


def test_yt_soup_to_lb_push_task(yt_stuff, conf, frozen_time, logbroker_client):
    source_path = "//source_path"

    tests.yt_test_func(
        yt_client=yt_stuff.get_yt_client(),
        func=lambda: test_helpers.execute(tasks.YtSoup2LBPushTask(source_path=source_path)),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(tables.YsonTable("soup_yandexuid.yson", source_path), tests.TableIsNotChanged())],
    )

    def deserialize(value):
        proto = TSoupEvent()
        proto.ParseFromString(base64.b64decode(value))
        return json.loads(proto2json.proto2json(proto))

    return {
        "lb": [deserialize(row) for row in consumer_utils.read_all(logbroker_client.create_consumer(), timeout=30)]
    }


def test_yt_soup_to_vulture_push_task(yt_stuff, conf, frozen_time, logbroker_client):
    source_path = "//source_path"

    tests.yt_test_func(
        yt_client=yt_stuff.get_yt_client(),
        func=lambda: test_helpers.execute(tasks.YtSoup2VultureTask(source_path=source_path)),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(tables.YsonTable("soup_yandexuid.yson", source_path), tests.TableIsNotChanged())],
    )

    def deserialize(value):
        proto = TLinks()
        proto.ParseFromString(value)
        return json.loads(proto2json.proto2json(proto))

    def splitlines(rows):
        fst, scd = rows.split(b"\x05\n")
        return [fst + b"\x05", scd]

    return {
        "lb": [
            deserialize(row)
            for row in consumer_utils.read_all(
                logbroker_client.create_consumer(), timeout=30, chunk_splitter=splitlines
            )
        ]
    }


def test_yt_to_bb_push_value_task(yt_stuff, conf, frozen_time, logbroker_client):
    source_path = "//source_path"

    yt_results = tests.yt_test_func(
        yt_client=yt_stuff.get_yt_client(),
        func=lambda: test_helpers.execute(tasks.Yt2BBPushValueTask(source_path=source_path)),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(tables.YsonTable("source_value.yson", source_path), tests.TableIsNotChanged())],
        output_tables=[
            (
                tables.YsonTable(
                    "collector.yson",
                    "//collector/fresh/prefix_1500000000",
                    on_read=tables.OnRead(row_transformer=crypta_log_row_transformer),
                    yson_format="pretty",
                ),
                tests.Diff(),
            )
        ],
    )
    assert [] == consumer_utils.read_all(logbroker_client.create_consumer(), timeout=30)
    return {"yt": yt_results}
