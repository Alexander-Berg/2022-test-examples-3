import itertools
import os

from google.protobuf import json_format
import yatest.common
import yt.wrapper as yt
from yt import yson

from crypta.audience.lib.tasks import (
    audience,
    lookalike,
)
from crypta.audience.lib.tasks.lookalike import (
    paths,
    prediction,
)
from crypta.audience.test.fat import identifiers
import crypta.lab.lib.tables as lab_tables
from crypta.lib.proto.user_data import user_data_stats_pb2
from crypta.lib.python.bt import test_helpers
import crypta.lib.python.bt.conf.conf as conf
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def attributes(segments):
    return dict({
        "schema": schema_utils.yt_schema_from_dict({
            "weight": "double",
            "permanent_id": "int64",
            "yandexuid": "string",
            "ts": "int64",
            "volatile_id": "string",
            "yuid": "string",
        }),
        "batch_size": len(segments),
        "priority": 0.0,
        "segments_meta": {
            volatile_id: {
                "crypta_related_goals": [
                    "50000000"
                ],
                "crypta_segment_info": {
                    "something": "something"
                },
                "crypta_segment_type": "audience",
                "filter_capacity": size,
                "num_output_buckets": 3,
                "options": {
                    "enforce_device_and_platform": True,
                    "enforce_region": True,
                    "include_input": False,
                    "max_coverage": 4000000
                },
                "permanent_id": permanent_id,
                "priority": 0.0,
                "ts": 1590000000,
                "volatile_id": volatile_id
            }
            for volatile_id, (permanent_id, size) in segments.iteritems()
        }
    })


def get_volatile_id(id_):
    return "uuid-{}".format(id_)


def create_row(permanent_id, yuid):
    return {
        "weight": 1.,
        "permanent_id": permanent_id,
        "yandexuid": yuid,
        "ts": 1590000000,
        "volatile_id": get_volatile_id(permanent_id),
        "yuid": yuid,
    }


class InputGenerator(object):
    def __init__(self):
        self.id_generator = itertools.count()
        self.permanent_id_generator = itertools.count()
        self.directory = yatest.common.test_output_path("batches")
        os.makedirs(self.directory)

    def table(self, segments):
        permanent_ids = list(itertools.islice(self.permanent_id_generator, len(segments)))
        batch = [
            create_row(permanent_id, yuid)
            for permanent_id, segment in zip(permanent_ids, segments)
            for yuid in segment
        ]

        id_ = next(self.id_generator)
        src = os.path.join(self.directory, str(id_))
        with open(src, "w") as stream:
            yson.dump(batch, stream, yson_type="list_fragment", yson_format="pretty")

        return (
            tables.YsonTable(
                src,
                yt.ypath_join(conf.paths.lookalike.segments.batches, str(id_)),
                on_write=tables.OnWrite(attributes=attributes({
                    get_volatile_id(permanent_id): (permanent_id, len(segment))
                    for permanent_id, segment in zip(permanent_ids, segments)
                })),
            ),
            tests.IsAbsent(),
        )


def waiting_on_read():
    def convert_meta(row):
        if lab_tables.UserDataStats.Fields.DISTRIBUTIONS in row:
            row = json_format.MessageToDict(audience._prepare_stats(row, with_filter=True))
            row[lab_tables.UserDataStats.Fields.DISTRIBUTIONS]["Main"]["Mean"]["Data"] = len(row[lab_tables.UserDataStats.Fields.DISTRIBUTIONS]["Main"]["Mean"]["Data"])

        return row

    return tables.OnRead(row_transformer=convert_meta)


def meta_user_attrs_transformer(attrs):
    if paths.Meta.OPTIONS in attrs:
        attrs[paths.Meta.OPTIONS] = {
            segment_id: json_format.MessageToDict(options)
            for segment_id, options in prediction.extract_segments_options(attrs[paths.Meta.OPTIONS]).iteritems()
        }

    if paths.Meta.STATS_OPTIONS in attrs:
        stats_options = user_data_stats_pb2.TUserDataStatsOptions()
        stats_options.ParseFromString(attrs[paths.Meta.STATS_OPTIONS])
        attrs[paths.Meta.STATS_OPTIONS] = json_format.MessageToDict(stats_options)

    return attrs


def test_prepare_enqueued_segments(prepared_local_yt, frozen_time):
    gen = InputGenerator()
    yt_client = prepared_local_yt.get_yt_client()

    return tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: test_helpers.execute(lookalike.interaction.PrepareEnqueuedSegments()),
        input_tables=[
            gen.table([
                identifiers.YANDEXUIDS[:2],
                identifiers.YANDEXUIDS[2:5],
            ]),
            gen.table([
                [],
                identifiers.JUNK_YANDEXUIDS[:4],
            ]),
        ],
        output_tables=[
            (
                cypress.CypressNode(conf.paths.lookalike.segments.waiting),
                tests.TestNodesInMapNodeChildren([tests.Diff(transformer=meta_user_attrs_transformer)], tag="waiting", on_read=waiting_on_read()),
            ),
            (
                cypress.CypressNode(conf.paths.lookalike.output),
                tests.TestNodesInMapNodeChildren([tests.Diff()], tag="output"),
            ),
            (
                tables.get_yson_table_with_schema(
                    "lookalike_segment_state_storage.yson",
                    conf.paths.audience.dynamic.states.lookalike,
                    audience.tables.LookalikeSegmentStateStorage(yt_client).schema,
                    dynamic=True,
                ),
                tests.Diff(),
            ),
        ],
    )
