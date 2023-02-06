from __future__ import print_function

from ads.bsyeti.big_rt.py_test_lib import (
    make_namedtuple,
    make_json_file,
    BulliedProcess,
    create_yt_queue,
    execute_cli,
    waiting_iterable,
    launch_bullied_processes_reading_queue,
)

from extsearch.images.robot.rt.protos.state_pb2 import TAnnDataRT

from quality.user_sessions.rt.protos.profile_pb2 import TProfileMessage

from quality.user_sessions.rt.protos.queue_pb2 import TQueueValueMessage

from library.python.sanitizers import asan_is_on

import collections
import contextlib
import itertools
import jinja2
import logging
import os
import pytest
import random
import re
import sys
import yatest.common
import time
import yt.wrapper


@contextlib.contextmanager
def create_state_table(yt_client, path, schema):
    logging.info("Creating state table %s", path)
    yt_client.create_table(path, recursive=True, attributes={"dynamic": True, "schema": schema})
    logging.info("Mounting state table %s", path)
    yt_client.mount_table(path, sync=True)
    logging.info("Mounted state table %s", path)
    yield path
    logging.info("Unmounting state table %s", path)
    yt_client.unmount_table(path, sync=True)
    logging.info("Removing state table %s", path)
    yt_client.remove(path)
    logging.info("Removed state table %s", path)


@pytest.fixture()
def stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    if not asan_is_on():
        input_shards_count = 10
        data_part_length = 100
        restart_max_seconds = 40
    else:
        input_shards_count = 3
        data_part_length = 40
        restart_max_seconds = 100

    test_id = re.sub(r'[^\w\d]', '_', request.node.name)
    input_queue_path = "//tmp/foxxmary/input_queue_" + test_id
    consuming_system_path = "//tmp/foxxmary/test_consuming_system_" + test_id
    stateful_state_table_path = "//tmp/states/foxxmary/State_" + test_id
    state_schema = [
        {"name": "DocumentId", "type": "string", "sort_order": "ascending"},
        {"name": "AnnDataRT", "type": "string"},
        {"name": "Timestamp", "type": "uint64"},
    ]
    stateful_read_only_state_table_path = "//tmp/foxxmary/states/ReadOnlyState_" + test_id
    read_only_schema = [
        {"name": "user_id", "type": "string", "sort_order": "ascending"},
        {"name": "reqid", "type": "string", "sort_order": "ascending"},
        {"name": "query", "type": "string"},
        {"name": "relev", "type": "string"},
        {"name": "rearr", "type": "string"},
        {"name": "documents", "type": "string"},
        {"name": "next_delayed_event_index", "type": "uint64"},
    ]
    queue_output = "//tmp/foxxmary/output_queue_" + test_id
    queue_output_schema = [
        {"name": "DocumentId", "type": "string"},
        {"name": "TextId", "type": "string"},
        {"name": "AnnData", "type": "string"},
    ]

    input_yt_queue = create_yt_queue(standalone_yt_cluster.get_yt_client(), input_queue_path, input_shards_count)

    queue_consumer = "stateful_cow"
    execute_cli(["consumer", "create", input_yt_queue["path"], queue_consumer, "--ignore-in-trimming", "0"])

    yt_client = standalone_yt_cluster.get_yt_client()

    with create_state_table(yt_client, stateful_state_table_path, state_schema), create_state_table(
        yt_client, stateful_read_only_state_table_path, read_only_schema
    ), create_state_table(yt_client, queue_output, queue_output_schema):
        yield make_namedtuple("FactordbStatefulTestStand", **locals())


def gen_shard_testing_schemas(length):
    for j in range(1, 1000):
        cnt = int(2.5 ** j)
        yield [i % cnt for i in range(length)]
        yield [random.randint(0, cnt - 1) for i in range(length)]


def gen_testing_shard_ids(shards_count, length):
    schemas = itertools.islice(gen_shard_testing_schemas(length), shards_count)

    data = {shard: ["%d" % ((u + 1) * shards_count + shard) for u in schema] for shard, schema in enumerate(schemas)}
    test_ids = collections.Counter(uid for ids in data.values() for uid in ids)

    flag_ids = ["%d" % int(shards_count * 1e9 + shard) for shard in range(shards_count)]
    for i, flag_uid in enumerate(flag_ids):
        data[i].append(flag_uid)
    return data, dict(test_ids), set(flag_ids)


def make_stateful_config(stand, worker_minor_name="", workers=1):
    shards_count = stand.input_yt_queue["shards"]
    with open(yatest.common.source_path("extsearch/images/robot/rt/tests/config.json")) as f:
        conf_s = jinja2.Template(f.read()).render(
            shards_count=shards_count,
            max_shards=int((shards_count + workers - 1) / workers),
            port=stand.port_manager.get_port(),
            consuming_system_main_path=stand.consuming_system_path,
            consumer=stand.queue_consumer,
            input_queue=stand.input_yt_queue["path"],
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(yatest.common.output_path(), "global_{}.log".format(worker_minor_name)),
            worker_minor_name=worker_minor_name,
            state_table_path=stand.stateful_state_table_path,
            read_only_state_table_path=stand.stateful_read_only_state_table_path,
            queue_output=stand.queue_output,
            max_inflight_bytes=str(int(200 * shards_count / workers)),
        )
    return make_namedtuple(
        "StatefulConfig", path=make_json_file(conf_s, name_template="sharding_config_{json_hash}.json")
    )


def decode_proto_state(state, state_type):
    state_proto = state_type()
    state_proto.ParseFromString(state)
    return state_proto


class StatefulProcess(BulliedProcess):
    def __init__(self, config_path):
        super(StatefulProcess, self).__init__(
            launch_cmd=[
                yatest.common.binary_path("extsearch/images/robot/rt/rt_usersessions/rt_usersessions"),
                '-c',
                config_path,
            ]
        )


def stateful_launch_k_process(stand, data, k, stable=True):
    configs = [make_stateful_config(stand, worker_minor_name=str(worker), workers=k) for worker in range(k)]
    statefuls = [StatefulProcess(config.path) for config in configs]
    restart_randmax = None if stable else stand.restart_max_seconds
    launch_bullied_processes_reading_queue(
        statefuls, stand.input_yt_queue, stand.queue_consumer, data, restart_randmax=restart_randmax, timeout=600
    )


def stateful_launch_one_process_stable(**args):
    stateful_launch_k_process(k=1, **args)


def stateful_launch_one_process_unstable(**args):
    stateful_launch_k_process(k=1, stable=False, **args)


def stateful_launch_two_process_stable(**args):
    stateful_launch_k_process(k=2, **args)


def stateful_launch_two_process_unstable(**args):
    stateful_launch_k_process(k=2, stable=False, **args)


@pytest.mark.parametrize(
    "stateful_launcher",
    [
        stateful_launch_one_process_stable,
        stateful_launch_one_process_unstable,
        stateful_launch_two_process_stable,
        stateful_launch_two_process_unstable,
    ],
)
def test_stateful(stand, stateful_launcher):
    print(">>> OUTPUT DIR: " + yatest.common.output_path(), file=sys.stderr)

    # generate and upload test data to yt
    shard_ids, test_ids, flag_ids = gen_testing_shard_ids(
        shards_count=stand.input_yt_queue["shards"], length=stand.data_part_length
    )

    needed = {}

    data = {}
    for shard, ids in shard_ids.items():
        data[shard] = []
        for identifier in ids:
            Url = "https://yandex.ru/search/" + str(shard)

            queueValueMessage = TQueueValueMessage()
            queueValueMessage.UserID = str(identifier)
            queueValueMessage.RequestID = str(identifier)
            queueValueMessage.EventType = queueValueMessage.EEventType.Value('CLICK')
            queueValueMessage.Url = Url
            queueValueMessage.Timestamp = int(time.time())
            data[shard].append(queueValueMessage.SerializeToString())

            queueValueMessage = TQueueValueMessage()
            queueValueMessage.UserID = str(identifier)
            queueValueMessage.RequestID = str(identifier)
            queueValueMessage.EventType = queueValueMessage.EEventType.Value('RESULT_SHOWS_BATCH')
            queueValueMessage.Timestamp = int(time.time())
            for i in range(3):
                shows = queueValueMessage.ResultShows.add()
                shows.Url = Url
            data[shard].append(queueValueMessage.SerializeToString())

            if needed.get(Url) is None:
                needed[Url] = {}
            if needed[Url].get("query " + str(identifier)) is None:
                needed[Url]["query " + str(identifier)] = [0, 0]
            needed[Url]["query " + str(identifier)][0] += 1
            needed[Url]["query " + str(identifier)][1] += 3

    stand.input_yt_queue["queue"].write(data)

    read_only_states = {}
    for ids in shard_ids.itervalues():
        for identifier in ids:
            message = TProfileMessage()
            message.UserID = identifier
            message.RequestID = identifier
            message.Query = "query " + str(identifier)
            read_only_states[identifier] = message
    stand.yt_client.insert_rows(
        stand.stateful_read_only_state_table_path,
        [
            dict(
                user_id=state.UserID,
                reqid=state.RequestID,
                query=state.Query,
            )
            for identifier, state in read_only_states.iteritems()
        ],
        format='yson',
    )

    # launch and wait stateful
    stateful_launcher(stand=stand, data=data)

    logging.info("Stateful processor read all messages and was finished")

    def get_rows(query):
        for waiting_state in waiting_iterable(timeout=60, period=8):
            with waiting_state.suppress_exceptions_if_not_last:
                rows = stand.yt_client.select_rows(query, yt.wrapper.SYNC_LAST_COMMITED_TIMESTAMP)
                return rows

    rows = get_rows(
        query="DocumentId, AnnDataRT, Timestamp from [{table}]".format(table=stand.stateful_state_table_path)
    )

    got = {}
    for row in rows:
        AnnDataRT = TAnnDataRT()
        AnnDataRT.ParseFromString(row['AnnDataRT'])
        for textid in AnnDataRT.TextIdToAnnDataMapping:
            for timestamp in AnnDataRT.TextIdToAnnDataMapping[textid].TimestampToAnnDataMapping:
                annData = AnnDataRT.TextIdToAnnDataMapping[textid].TimestampToAnnDataMapping[timestamp]
                if got.get(row["DocumentId"]) is None:
                    got[row["DocumentId"]] = {}
                if got[row["DocumentId"]].get(annData.Text) is None:
                    got[row["DocumentId"]][annData.Text] = [0, 0]
                for regionData in annData.IndexAnnData:
                    got[row["DocumentId"]][annData.Text][0] += regionData.ShowsClicksData.Clicks
                    got[row["DocumentId"]][annData.Text][1] += regionData.ShowsClicksData.Shows
    assert needed == got
