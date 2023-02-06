import yatest.common
import yt.wrapper as yt

from crypta.audience.lib.watchman import (
    watchman,
)
from crypta.audience.lib.tasks import (
    audience,
    lookalike,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def test_watchman(yt_stuff, conf, mock_solomon_server, frozen_time, mock_zk):
    client = yt_stuff.get_yt_client()

    for size, directory in enumerate((
        conf.paths.audience.input,
        conf.paths.audience.output,
        conf.paths.storage.for_full,
        conf.paths.storage.queue,
        conf.paths.lookalike.input,
        conf.paths.lookalike.output,
    ), start=1):
        for i in range(size):
            client.create("table", yt.ypath_join(directory, str(i)), recursive=True)

    for i in range(1, 4):
        path = yt.ypath_join(conf.paths.audience.batches, str(i))
        client.create("table", path, recursive=True)
        client.set_attribute(path, audience.tables.InputBatch.Meta.META, {audience.tables.InputBatch.Meta.SEGMENTS: [None] * i})

        path = yt.ypath_join(conf.paths.lookalike.segments.batches, str(i))
        client.create("table", path, recursive=True)
        client.set_attribute(path, lookalike.paths.InputBatch.BATCH_SIZE, i)

    tests.yt_test_func(
        yt_client=client,
        data_path=yatest.common.test_source_path("data"),
        func=lambda: watchman.main(conf.proto),
        input_tables=(
            (tables.YsonTable(
                "user_data.yson",
                conf.paths.lab.data.userdata,
                on_write=tables.OnWrite(attributes={"_last_update_date": "2020-09-01"})
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "regular_segments_state_storage.yson",
                conf.paths.audience.dynamic.states.regular,
                audience.tables.RegularSegmentStateStorage(client).schema,
                dynamic=True,
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "regular_segments_state_storage.yson",
                conf.paths.audience.dynamic.states.lookalike,
                audience.tables.LookalikeSegmentStateStorage(client).schema,
                dynamic=True,
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "lookalike_stats_storage.yson",
                conf.paths.audience.dynamic.lookalike_stats,
                audience.tables.LookalikeStatsStorage(client).schema,
                dynamic=True,
            ), tests.TableIsNotChanged()),
        ) + tuple(
            (tables.YsonTable("user_data.yson", yt.ypath_join(conf.paths.lookalike.segments.waiting, str(i), "Meta")), tests.TableIsNotChanged())
            for i in range(3)
        ),
    )
    return mock_solomon_server.dump_push_requests()
