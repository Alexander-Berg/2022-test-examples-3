import itertools
import json
import os

from google.protobuf import json_format
import yatest.common
import yt.wrapper as yt
from yt import yson

from crypta.audience.lib.tasks import constants
from crypta.audience.lib.tasks.audience import (
    audience,
    tables as audience_tables,
)
from crypta.audience.test.fat import (
    fixtures,
    identifiers,
)
from crypta.lib.proto.user_data.user_data_stats_pb2 import TUserDataStats
from crypta.lib.python.bt import test_helpers
import crypta.lib.python.bt.conf.conf as conf
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def make_attributes(segments, matching_table, target_type):
    result = {
        "crypta_meta": {
            "matching_table": matching_table,
            "segments": {
                volatile_id: {
                    "audience_segment_id": audience_id,
                    "crypta_segment_info": {
                        "segment_type": "geo" if is_geo else "does not matter",
                        "content_type": id_type,
                        "source_id": source_id,
                        "segment_id": audience_id,
                    },
                    "dev_matching_type": "FAKE",
                    "id_type": id_type,
                    "related_goals": related_goals,
                }
                for volatile_id, (audience_id, source_id, id_type, related_goals, is_geo) in segments.iteritems()
            },
        },
        "priority": 2,
    }
    if target_type:
        result["crypta_meta"]["target_type"] = target_type
    return result


def create_row(id_value, volatile_id):
    return {
        "id_value": id_value,
        "segment_id": volatile_id,
    }


class Segment(object):
    def __init__(self, id_type, ids, related_goals=None, is_geo=False, use_crypta_id_with_source_id=False):
        self.id_type = audience_tables.Matching.YUID if use_crypta_id_with_source_id else id_type
        self.source_id = constants.CRYPTAID_SOURCEID[0] if use_crypta_id_with_source_id else 0
        self.ids = ids
        self.related_goals = related_goals or []
        self.is_geo = is_geo


class BatchGenerator(object):
    def __init__(self):
        self.batch_id_generator = itertools.count()
        self.permanent_id_generator = itertools.count()
        self.directory = yatest.common.test_output_path("batches")
        os.makedirs(self.directory)

    def generate_batch(self, segments, matching_table, target_type):
        permanent_ids = list(itertools.islice(self.permanent_id_generator, len(segments)))
        volatile_ids = ["{}-{}".format(segment.id_type, permanent_id) for segment, permanent_id in zip(segments, permanent_ids)]

        batch = [
            create_row(id_value, volatile_id)
            for volatile_id, segment in zip(volatile_ids, segments)
            for id_value in segment.ids
        ]

        batch_name = "batch-{}".format(next(self.batch_id_generator))
        src = os.path.join(self.directory, batch_name)
        with open(src, "w") as stream:
            yson.dump(batch, stream, yson_type="list_fragment", yson_format="pretty")

        return (
            tables.YsonTable(
                src,
                yt.ypath_join(conf.paths.audience.batches, batch_name),
                on_write=tables.OnWrite(attributes=make_attributes({
                    volatile_id: (permanent_id, segment.source_id, segment.id_type, segment.related_goals, segment.is_geo)
                    for volatile_id, permanent_id, segment in zip(volatile_ids, permanent_ids, segments)
                }, matching_table, target_type)),
            ),
            tests.IsAbsent(),
        )


def generate_crypta_id_filter():
    crypta_id_filter = yatest.common.test_output_path("batches/crypta_id_filter.yson")
    crypta_ids = (
        {"crypta_id": crypta_id} for crypta_id in identifiers.YANDEXUIDS[:5]
    )

    with open(crypta_id_filter, "w") as stream:
        yson.dump(crypta_ids, stream, yson_type="list_fragment", yson_format="pretty")

    return (
        tables.get_yson_table_with_schema(
            crypta_id_filter,
            conf.paths.audience.matching.cryptaids,
            schema=schema_utils.yt_schema_from_dict({"crypta_id": "string"}),
        ),
        tests.TableIsNotChanged(),
    )


def stats_row_transformer(row):
    stats = TUserDataStats()
    stats.ParseFromString(row["Stats"])
    row["Stats"] = json.loads(json_format.MessageToJson(stats))
    mean = row["Stats"]["Distributions"]["Main"].setdefault("Mean", {})
    mean["Data"] = len(mean.get("Data", []))
    return row


def test_enqueue_batches_correct(prepared_local_yt, frozen_time, matching_table, user_data_stats):
    return run_test(prepared_local_yt, [
        ([
            Segment(audience_tables.Matching.YUID, identifiers.YANDEXUIDS[:2]),
            Segment(audience_tables.Matching.YUID, identifiers.JUNK_YANDEXUIDS[:4]),
        ], None, audience_tables.Matching.YUID),
        ([
            Segment(audience_tables.Matching.CRYPTA_ID, identifiers.YANDEXUIDS[:2], use_crypta_id_with_source_id=True),
            Segment(audience_tables.Matching.CRYPTA_ID, identifiers.YANDEXUIDS[2:5], use_crypta_id_with_source_id=True),
        ], None, None),
        ([
            Segment(audience_tables.Matching.PHONE, identifiers.PHONES[:2]),
            Segment(audience_tables.Matching.EMAIL, identifiers.EMAILS[:2]),
            Segment(audience_tables.Matching.IDFA_GAID, identifiers.IDFAS_GAIDS[:2]),
        ], conf.paths.audience.matching.by_id_value, None),
        ([
            Segment(audience_tables.Matching.PUID, identifiers.PUIDS[:2]),
            Segment(audience_tables.Matching.PUID, identifiers.PUIDS[2:5]),
        ], None, audience_tables.Matching.PUID),
        ([
            Segment(audience_tables.Matching.CRYPTA_ID, identifiers.YANDEXUIDS[:2]),
            Segment(audience_tables.Matching.CRYPTA_ID, identifiers.YANDEXUIDS[2:5] + identifiers.JUNK_YANDEXUIDS[:4]),
        ], None, audience_tables.Matching.CRYPTA_ID),
    ])


def test_enqueue_batches_errors(prepared_local_yt, frozen_time, user_data_stats):
    return run_test(prepared_local_yt, [
        ([
            Segment(audience_tables.Matching.YUID, []),
        ], None, None),
    ])


def test_enqueue_batches_geo(prepared_local_yt, frozen_time, user_data_stats):
    return run_test(prepared_local_yt, [
        ([Segment(audience_tables.Matching.YUID, identifiers.YANDEXUIDS[:2], is_geo=True)], None, None),
    ])


def test_enqueue_batches_related_goals(prepared_local_yt, frozen_time, user_data_stats):
    related_goals = identifiers.random_related_goals()
    fixtures.create_related_goals_tables(related_goals)

    return run_test(prepared_local_yt, [
        ([Segment(audience_tables.Matching.YUID, identifiers.YANDEXUIDS[:2], related_goals=related_goals)], None, None),
    ])


def run_test(clean_local_yt, inputs):
    gen = BatchGenerator()

    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(audience.EnqueueBatches()),
        input_tables=[
            gen.generate_batch(segments, matching_table, target_type)
            for segments, matching_table, target_type in inputs
        ] + [
            generate_crypta_id_filter()
        ],
        output_tables=[
            (
                cypress.CypressNode(conf.paths.audience.output),
                tests.TestNodesInMapNode([tests.Diff()], tag="output"),
            ),
            (
                cypress.CypressNode(conf.paths.storage.email_phone_queue),
                tests.TestNodesInMapNode([tests.Diff()], tag="email_phone"),
            ),
            (
                cypress.CypressNode(conf.paths.storage.device_queue),
                tests.TestNodesInMapNode([tests.Diff()], tag="device"),
            ),
            (
                cypress.CypressNode(conf.paths.storage.queue_segments_info),
                tests.TestNodesInMapNode([tests.Diff()], tag="queue_segments_info"),
            ),
            (
                cypress.CypressNode(conf.paths.storage.for_full),
                tests.TestNodesInMapNode([tests.Diff()], tag="for_full"),
            ),
            (
                cypress.CypressNode(conf.paths.storage.puid_queue),
                tests.TestNodesInMapNode([tests.Diff()], tag="puid"),
            ),
            (
                cypress.CypressNode(conf.paths.storage.crypta_id_queue),
                tests.TestNodesInMapNode([tests.Diff()], tag="crypta_id"),
            ),
            (
                tables.DynamicYsonTable(
                    "regular_segment_state_storage.yson",
                    conf.paths.audience.dynamic.states.regular,
                    yson_format="pretty",
                ),
                tests.Diff(),
            ),
            (
                tables.DynamicYsonTable(
                    "segment_properties_storage.yson",
                    conf.paths.audience.dynamic.properties,
                    yson_format="pretty",
                ),
                tests.Diff(),
            ),
            (
                tables.DynamicYsonTable(
                    "stats_storage.yson",
                    conf.paths.audience.dynamic.stats,
                    yson_format="pretty",
                    on_read=tables.OnRead(row_transformer=stats_row_transformer),
                ),
                tests.Diff(),
            ),
            (
                tables.DynamicYsonTable(
                    "segment_goals_relation_storage.yson",
                    conf.paths.audience.dynamic.goals_relation,
                    yson_format="pretty",
                    on_read=tables.OnRead(row_transformer=tests.float_to_str),
                ),
                tests.Diff(),
            ),
        ],
    )
