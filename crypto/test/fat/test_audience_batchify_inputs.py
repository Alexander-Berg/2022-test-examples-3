import itertools
import os

import mock
import yatest.common
import yt.wrapper as yt
from yt import yson

from crypta.audience.lib.tasks import constants
from crypta.audience.lib.tasks.audience import (
    audience,
    tables as audience_tables,
)
from crypta.lib.python import time_utils
from crypta.lib.python.bt import test_helpers
import crypta.lib.python.bt.conf.conf as conf
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def make_attributes(segment_id, segment_group):
    return {
        audience_tables.Input.Attributes.MATCHING_TYPE: "exact",
        audience_tables.Input.Attributes.ID_TYPE: segment_group.id_type,
        audience_tables.Input.Attributes.STATUS: segment_group.status,
        audience_tables.Input.Attributes.DEVICE_MATCHING_TYPE: "FAKE",
        audience_tables.Input.Attributes.SEGMENT_ID: segment_id,
        audience_tables.Input.Attributes.CRYPTA_SEGMENT_INFO: {
            "source_id": str(segment_group.source_id),
            "rest": "something",
            "segment_type": segment_group.segment_type,
        },
        audience_tables.Input.Attributes.SEGMENT_PRIORITY: 0,
        audience_tables.Input.Attributes.CRYPTA_RELATED_GOALS: [],
    }


class InputTablesGenerator(object):
    def __init__(self):
        self.directory = yatest.common.test_output_path("inputs")
        self.segment_id_generator = itertools.count(1)

    @staticmethod
    def create_row(id_value):
        return {"id_value": str(id_value)}

    def generate_table(self, segment_group):
        segment_id = next(segment_group.segment_id_generator or self.segment_id_generator)

        segment = [
            self.create_row(user_id_value)
            for user_id_value in range(segment_group.size)
        ]
        segment_file_path = os.path.join(self.directory, str(segment_id))
        if not os.path.isdir(self.directory):
            os.makedirs(self.directory)

        with open(segment_file_path, "w") as stream:
            yson.dump(segment, stream, yson_type="list_fragment", yson_format="pretty")

        return (
            tables.YsonTable(
                segment_file_path,
                yt.ypath_join(conf.paths.audience.input, "{}-{}-{}".format(segment_group.segment_type, segment_group.id_type, segment_id)),
                on_write=tables.OnWrite(attributes=make_attributes(segment_id, segment_group))
            ),
            tests.IsAbsent() if segment_group.removed else tests.TableIsNotChanged(),
        )

    def generate_tables(self, segment_group):
        return [
            self.generate_table(segment_group)
            for _ in range(segment_group.count)
        ]


class SegmentGroup(object):
    def __init__(self, segment_type, id_type, count, segment_id_generator=None, size=3, removed=True, status=audience_tables.Output.Statuses.NEW, use_crypta_id_with_source_id=False):
        self.id_type = audience_tables.Matching.YUID if use_crypta_id_with_source_id else id_type
        self.source_id = constants.CRYPTAID_SOURCEID[0] if use_crypta_id_with_source_id else 0
        self.segment_type = segment_type
        self.count = count
        self.segment_id_generator = segment_id_generator
        self.size = size
        self.removed = removed
        self.status = status


def get_batch_filename(yt_client, dir_path, name):
    segments = yt_client.get_attribute(yt.ypath_join(dir_path, name), audience_tables.InputBatch.Meta.META)[audience_tables.InputBatch.Meta.SEGMENTS].values()
    return "-".join(sorted(str(x[audience_tables.InputBatch.Meta.AUDIENCE_SEGMENT_ID]) for x in segments))


def run_test(clean_local_yt, frozen_time, segment_groups):
    gen = InputTablesGenerator()

    original_find_inputs = audience.BatchifyInputsWithSegmentType._find_inputs

    def find_inputs(self, path):
        result = list(original_find_inputs(self, path))
        for path in result:
            path.attributes[audience_tables.Attributes.MODIFICATION_TIME] = time_utils.get_current_moscow_datetime().isoformat()
        return result

    with mock.patch.object(audience.BatchifyInputsWithSegmentType, "_find_inputs", find_inputs):
        results = tests.yt_test_func(
            yt_client=clean_local_yt.get_yt_client(),
            func=lambda: test_helpers.execute(audience.BatchifyInputs()),
            input_tables=sum((
                gen.generate_tables(segment_group)
                for segment_group in segment_groups
            ), []),
            output_tables=[
                (
                    cypress.CypressNode(conf.paths.audience.batches),
                    tests.TestNodesInMapNode([tests.Diff()], tag="batches", filename_func=get_batch_filename),
                ),
                (
                    cypress.CypressNode(conf.paths.audience.output),
                    tests.TestNodesInMapNode([tests.Diff()], tag="output"),
                ),
                (
                    tables.get_yson_table_with_schema(
                        "segment_state_storage.yson",
                        conf.paths.audience.dynamic.states.regular,
                        audience_tables.RegularSegmentStateStorage(clean_local_yt.get_yt_client()).schema,
                        dynamic=True,
                    ),
                    tests.Diff(),
                ),
            ],
        )
        return {os.path.basename(x.get("file", x).get("uri")): x for x in results}


def test_batchify_inputs_correct(prepared_local_yt, frozen_time):
    return run_test(prepared_local_yt, frozen_time, [
        SegmentGroup("metrika", audience_tables.Matching.YUID, 1),
        SegmentGroup("pixel", audience_tables.Matching.YUID, 1),
        SegmentGroup("uploading", audience_tables.Matching.YUID, 2),
        SegmentGroup("uploading", audience_tables.Matching.PHONE, 2),
        SegmentGroup("uploading", audience_tables.Matching.CRM, 2),
        SegmentGroup("uploading", audience_tables.Matching.CRYPTA_ID, 2),
        SegmentGroup("uploading", audience_tables.Matching.PUID, 2),
        SegmentGroup("uploading", audience_tables.Matching.CRYPTA_ID, 2, use_crypta_id_with_source_id=True),
    ])


def test_batchify_inputs_errors(prepared_local_yt, frozen_time):
    return run_test(prepared_local_yt, frozen_time, [
        SegmentGroup("uploading", audience_tables.Matching.YUID, 1, size=0),
        SegmentGroup("uploading", "whatever", 1),
        SegmentGroup("uploading", audience_tables.Matching.YUID, 1, status="old", removed=False),
        SegmentGroup("uploading", audience_tables.Matching.YUID, 1, removed=False, segment_id_generator=itertools.repeat(0)),
        SegmentGroup("uploading", audience_tables.Matching.YUID, 2),
    ])
