import itertools

import mock
import pytest
import yatest.common
import yt.wrapper as yt

from crypta.audience.lib.tasks import (
    audience,
    lookalike,
)
from crypta.lib.python.bt import test_helpers
import crypta.lib.python.bt.conf.conf as conf
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def attributes(id_, **kwargs):
    return dict({
        "crypta_maintain_geo_distribution": "true",
        "segment_priority": "0",
        "crypta_create_time": 1610000000000,
        "crypta_segment_info": {
            "something": "something",
        },
        "crypta_status": "new",
        "crypta_related_goals": [
            "50000000"
        ],
        "crypta_maintain_device_distribution": "true",
        "crypta_lookalike_precision": 3,
        "segment_id": id_,
    }, **kwargs)


@pytest.fixture
def id_generator():
    return itertools.count()


class InputGenerator(object):
    def __init__(self, id_generator, directory):
        self.id_generator = id_generator
        self.directory = directory

    def table(self, src="input.yson", removed=True, **attrs):
        id_ = next(self.id_generator)
        return (
            tables.YsonTable(
                src,
                yt.ypath_join(self.directory, "uuid-{}".format(id_)),
                on_write=tables.OnWrite(attributes=attributes(id_, **attrs))
            ),
            tests.IsAbsent() if removed else tests.TableIsNotChanged(),
        )

    def tables(self, count, **kwargs):
        return [
            self.table(**kwargs)
            for _ in range(count)
        ]


@pytest.fixture
def audience_gen(id_generator):
    return InputGenerator(id_generator, yt.ypath_join(conf.paths.lookalike.input, "audience"))


@pytest.fixture
def lal_internal_gen(id_generator):
    return InputGenerator(id_generator, yt.ypath_join(conf.paths.lookalike.input, "lal_internal"))


def run_test(clean_local_yt, inputs):
    batch_ids_gen = itertools.count()
    yt_client = clean_local_yt.get_yt_client()

    def generate_new_batch_id(_):
        return "batch-{}".format(next(batch_ids_gen))

    conf.proto.Options.Lookalike.InputBatch = 2
    conf.proto.Options.Lookalike.InputBatchCount = 2
    conf.proto.Options.Lookalike.InputIncompleteBatchMinAgeSec = 0

    with mock.patch("crypta.audience.lib.tasks.lookalike.paths.Paths.generate_new_batch_id", generate_new_batch_id),\
            mock.patch("crypta.audience.lib.tasks.lookalike.interaction.random.shuffle", lambda x: x):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: test_helpers.execute(lookalike.interaction.EnqueueAllSegments()),
            data_path=yatest.common.test_source_path("data/enqueue_all_segments"),
            input_tables=inputs,
            output_tables=[
                # In Siberia stats some cookies will be skipped because of skip filter
                (
                    cypress.CypressNode(conf.paths.lookalike.segments.batches),
                    tests.TestNodesInMapNode([tests.Diff()], tag="batch"),
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


def test_simple(prepared_local_yt, frozen_time, audience_gen, lal_internal_gen):
    return run_test(prepared_local_yt, audience_gen.tables(2) + lal_internal_gen.tables(2))


def test_errors(prepared_local_yt, frozen_time, audience_gen, lal_internal_gen):
    return run_test(prepared_local_yt, [
        audience_gen.table(),
        audience_gen.table(crypta_lookalike_precision=0),
        lal_internal_gen.table(_max_coverage=0),
        lal_internal_gen.table(src="empty.yson"),
    ])


def test_too_many_batches(prepared_local_yt, frozen_time, audience_gen, lal_internal_gen):
    return run_test(prepared_local_yt, audience_gen.tables(4) + lal_internal_gen.tables(4, removed=False))
