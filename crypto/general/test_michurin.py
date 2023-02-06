from __future__ import print_function

import collections
import itertools
import json
import logging
import os
import random
import re

from ads.bsyeti.libs.py_test_yt import helpers as bigrt_helpers
from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    execute_cli,
    launch_bullied_processes_reading_queue,
    make_json_file,
    make_namedtuple,
)
import jinja2
from library.python.codecs import loads as codecs_loads
import pytest
import yatest.common
import yt.wrapper

pytest.register_assert_rewrite("crypta.graph.rt.sklejka.michurin.ft.helpers")

from crypta.graph.rt.sklejka.michurin.ft import helpers
from crypta.graph.rt.sklejka.michurin.proto.state_pb2 import TMichurinState


logger = logging.getLogger(__name__)


MATCHER_SCHEMA = [
    {"name": "Id", "type": "uint64", "sort_order": "ascending"},
    {"name": "State", "type": "string"},
    {"name": "Codec", "type": "string"},
]
CID_SCHEMA = [
    {
        "name": "Hash",
        "type": "uint64",
        "expression": "farm_hash(Id)",
        "sort_order": "ascending",
    },
    {"name": "Id", "type": "string", "sort_order": "ascending"},
    {"name": "CryptaId", "type": "string"},
]


def create_state_table(yt_client, path, schema):
    logging.info("Creating state table %s", path)
    yt_client.create(
        "table",
        path,
        recursive=True,
        attributes={
            "dynamic": True,
            "schema": schema,
        },
    )
    yt_client.mount_table(path, sync=True)
    logging.info("Mounted table %s", path)


@pytest.fixture(scope="session")
def yt_rpc_proxy_conf(standalone_yt_cluster):
    logging.info("Preparing YT RPC config")
    yt_config_path = os.path.join(yatest.common.output_path(), "yt_rpc_proxy.cfg")
    bigrt_helpers.prepare_rpc_config([standalone_yt_cluster], yt_config_path)
    os.environ["YTRPC_CLUSTERS_CONFIG"] = yt_config_path
    logging.info("Ready YT RPC config")
    return yt_config_path


@pytest.fixture
def stand(
    request,
    standalone_yt_cluster,
    standalone_yt_ready_env,
    yt_rpc_proxy_conf,
    port_manager,
    config_test_default_enabled,
):
    logger.info("OUTPUT DIR: " + yatest.common.test_output_path())
    input_shards_count = 4
    data_part_length = 100

    edge_limit = 100
    # TODO: test hysteresis here
    enforce_edge_limit_after = 100

    old_edges_count = 10
    reset_after_count = 0
    merged_state_TTL = 1209600

    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    michurin_queue_path = "//tmp/michurin_queue_" + test_id
    rewind_queue_path = "//tmp/rewind_queue_" + test_id
    cryptaid_queue_path = "//tmp/cryptaid_queue_" + test_id
    vulture_queue_path = "//tmp/vulture_queue_" + test_id
    michurin_consuming_system_path = "//tmp/test_michurin_consuming_system_" + test_id
    cryptaid_consuming_system_path = "//tmp/test_cryptaid_consuming_system_" + test_id
    michurin_state_table_path = "//tmp/states/michurin_state_" + test_id
    cryptaid_state_table_path = "//tmp/states/cid_state_" + test_id

    input_yt_queue = create_yt_queue(
        standalone_yt_cluster.get_yt_client(), michurin_queue_path, input_shards_count
    )
    michurin_consumer = "michurin"
    execute_cli(
        [
            "consumer",
            "create",
            input_yt_queue["path"],
            michurin_consumer,
            "--ignore-in-trimming",
            "0",
        ]
    )

    rewind_consumer = "michurin_rewind"
    rewind_yt_queue = create_yt_queue(
        standalone_yt_cluster.get_yt_client(), rewind_queue_path, input_shards_count
    )
    execute_cli(
        [
            "consumer",
            "create",
            rewind_yt_queue["path"],
            rewind_consumer,
            "--ignore-in-trimming",
            "0",
        ]
    )

    cryptaid_yt_queue = create_yt_queue(
        standalone_yt_cluster.get_yt_client(), cryptaid_queue_path, input_shards_count
    )
    cryptaid_consumer = "cryptaid_michurin"
    execute_cli(
        [
            "consumer",
            "create",
            cryptaid_yt_queue["path"],
            cryptaid_consumer,
            "--ignore-in-trimming",
            "0",
        ]
    )

    vulture_yt_queue = create_yt_queue(
        standalone_yt_cluster.get_yt_client(), vulture_queue_path, input_shards_count
    )
    vulture_consumer = "brusilov"
    execute_cli(
        [
            "consumer",
            "create",
            vulture_yt_queue["path"],
            vulture_consumer,
            "--ignore-in-trimming",
            "0",
        ]
    )

    yt_client = standalone_yt_cluster.get_yt_client()

    create_state_table(yt_client, michurin_state_table_path, MATCHER_SCHEMA)
    create_state_table(yt_client, cryptaid_state_table_path, CID_SCHEMA)
    return make_namedtuple("SimpleShardingTestStand", **locals())


def gen_shard_testing_schemas(length):
    for j in range(1, 1000):
        cnt = int(2.5 ** j)
        yield [i % cnt for i in range(length)]
        yield [random.randint(0, cnt - 1) for i in range(length)]


def gen_testing_shard_ids(shards_count, length):
    schemas = itertools.islice(gen_shard_testing_schemas(length), shards_count)

    data = {
        shard: ["%d" % ((u + 1) * shards_count + shard) for u in schema]
        for shard, schema in enumerate(schemas)
    }
    test_ids = collections.Counter(uid for ids in data.values() for uid in ids)

    flag_ids = ["%d" % int(shards_count * 1e9 + shard) for shard in range(shards_count)]
    for i, flag_uid in enumerate(flag_ids):
        data[i].append(flag_uid)
    return data, dict(test_ids), set(flag_ids)


def make_stateful_config(stand, worker_minor_name="", workers=1):
    shards_count = stand.input_yt_queue["shards"]
    with open(
        yatest.common.source_path("crypta/graph/rt/sklejka/michurin/ft/config.json.j2")
    ) as f:
        max_shards = int((shards_count + workers - 1) / workers)
        conf_s = jinja2.Template(f.read(), undefined=jinja2.StrictUndefined).render(
            shards_count=shards_count,
            michurin_max_shards=max_shards,
            cryptaid_max_shards=max_shards,
            vulture_max_shards=max_shards,
            port=stand.port_manager.get_port(),
            michurin_consuming_system_main_path=stand.michurin_consuming_system_path,
            michurin_consumer=stand.michurin_consumer,
            cryptaid_consuming_system_main_path=stand.cryptaid_consuming_system_path,
            cryptaid_consumer=stand.cryptaid_consumer,
            michurin_input_queue=stand.input_yt_queue["path"],
            cryptaid_input_queue=stand.cryptaid_yt_queue["path"],
            vulture_input_queue=stand.vulture_yt_queue["path"],
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(
                yatest.common.test_output_path(),
                "global_{}.log".format(worker_minor_name),
            ),
            michurin_log=os.path.join(yatest.common.test_output_path(), "michurin.log"),
            michurin_worker_minor_name="michurin_" + worker_minor_name,
            cryptaid_worker_minor_name="cryptaid_" + worker_minor_name,
            michurin_state_table_path=stand.michurin_state_table_path,
            cryptaid_state_table_path=stand.cryptaid_state_table_path,
            edge_limit=stand.edge_limit,
            enforce_edge_limit_after=stand.enforce_edge_limit_after,
            max_inflight_bytes=str(int(200 * shards_count / workers)),
            rewind_queue_path=stand.rewind_queue_path,
            merged_state_TTL=stand.merged_state_TTL,
            force_edges_strong_for_split=json.dumps(True),
        )
    return make_namedtuple(
        "StatefulConfig",
        path=make_json_file(conf_s, name_template="sharding_config_{json_hash}.json"),
    )


def make_stateful_config_resharder(stand, worker_minor_name="", workers=1):
    shards_count = stand.input_yt_queue["shards"]
    with open(
        yatest.common.source_path(
            "crypta/graph/rt/sklejka/michurin/ft/config_resharder.json.j2"
        )
    ) as f:
        conf_s = jinja2.Template(f.read(), undefined=jinja2.StrictUndefined).render(
            shards_count=shards_count,
            port=stand.port_manager.get_port(),
            michurin_consuming_system_main_path=stand.michurin_consuming_system_path,
            michurin_input_queue=stand.input_yt_queue["path"],
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(
                yatest.common.test_output_path(),
                "global_{}.log".format(worker_minor_name),
            ),
            rewind_worker_minor_name="rewind_" + worker_minor_name,
            cryptaid_state_table_path=stand.cryptaid_state_table_path,
            rewind_queue_path=stand.rewind_queue_path,
            attempts_count=5,
            timeout_ms=800,
            max_cache_size=1000,
            max_cache_duration_ms=3000,
            update_local_cache_period_ms=300,
            update_yt_cache_period_ms=600,
            max_memory_usage=10000000,
        )
    return make_namedtuple(
        "StatefulConfig",
        path=make_json_file(conf_s, name_template="resharder_config_{json_hash}.json"),
    )


class StatefulProcess(BulliedProcess):
    def __init__(self, config_path):
        super(StatefulProcess, self).__init__(
            launch_cmd=[
                yatest.common.binary_path(
                    "crypta/graph/rt/sklejka/michurin/bin/michurin"
                ),
                "--config-json",
                config_path,
            ]
        )


class StetefulProcessResharder(BulliedProcess):
    def __init__(self, config_path):
        super(StetefulProcessResharder, self).__init__(
            launch_cmd=[
                yatest.common.binary_path(
                    "crypta/graph/rt/sklejka/resharder/bin/resharder"
                ),
                "--config-json",
                config_path,
            ]
        )


def check_queue_is_read(queue, consumer, expected_offsets):
    try:
        consumer_offsets = queue["queue"].get_consumer_offsets(consumer)
    except Exception:
        logger.exception("Can't get consumer offsets or actual table size")
        return False

    actual_offsets = 0
    for shard in range(queue["shards"]):
        consumer_offset = consumer_offsets[shard]
        if consumer_offset == queue["queue"].unreachable_offset:
            continue
        actual_offsets += consumer_offset + 1

    logger.info(
        "expected_offsets: {}, offsets: {}".format(expected_offsets, actual_offsets)
    )
    return actual_offsets >= expected_offsets


def check_queue_is_write(queue, consumer, expected_offsets):
    try:
        infos = queue["queue"].get_shard_infos()
    except Exception:
        logger.exception("Can't get shards info of qyt queue")
        return False

    actual_offsets = sum(shard["total_row_count"] for shard in infos)

    logger.info(
        "expected_offsets: {}, offsets: {}".format(expected_offsets, actual_offsets)
    )
    return actual_offsets >= expected_offsets


def stateful_launch_k_process(stand, check_func, k):
    configs = [
        make_stateful_config(stand, worker_minor_name=str(worker), workers=k)
        for worker in range(k)
    ]
    statefuls = [StatefulProcess(config.path) for config in configs]
    launch_bullied_processes_reading_queue(
        statefuls,
        data_or_check_func=check_func,
        timeout=200,
    )


def stateful_launch_k_process_resharder(stand, check_func, k):
    configs = [
        make_stateful_config_resharder(stand, worker_minor_name=str(worker), workers=k)
        for worker in range(k)
    ]
    statefuls = [StetefulProcessResharder(config.path) for config in configs]
    launch_bullied_processes_reading_queue(
        statefuls,
        data_or_check_func=check_func,
        timeout=250,
    )


def stateful_launcher(**args):
    stateful_launch_k_process(k=2, **args)


def stateful_launcher_resharder(**args):
    stateful_launch_k_process_resharder(k=2, **args)


def select_yt_rows(yt_client, query):
    _format = yt.wrapper.format.YsonFormat(encoding=None)
    return yt_client.select_rows(
        query, yt.wrapper.SYNC_LAST_COMMITED_TIMESTAMP, format=_format
    )


def test_enforcing_edge_limit_reset_dropped_cids(stand):
    (
        shard_data,
        expected_cid_map,
        vertex_data_len,
    ) = helpers.generate_shards_data_sunflower(
        stand.input_yt_queue["shards"], stand.edge_limit, stand.old_edges_count
    )

    data = helpers.pack_shard_data(shard_data)
    stand.input_yt_queue["queue"].write(data)

    logger.info("Launching Stateful processor")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.cryptaid_yt_queue, stand.cryptaid_consumer, vertex_data_len
        ),
    )
    logger.info("Stateful processor finished")

    graphs = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.michurin_state_table_path)
        )
    )
    cids = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.cryptaid_state_table_path)
        )
    )
    vults = list(
        select_yt_rows(
            stand.yt_client, "* from [{}/queue]".format(stand.vulture_queue_path)
        )
    )
    logger.info("Recieved states from YT")

    expected_michurin_states_count = len(shard_data)
    assert expected_michurin_states_count == len(graphs)

    graph_states = {}
    for row in graphs:
        state = row[b"State"]
        if row[b"Codec"]:
            state = codecs_loads(row[b"Codec"], state)

        state_proto = TMichurinState()
        state_proto.ParseFromString(state)
        graph_states[row[b"Id"]] = state_proto
    logger.info("Parsed graph states")

    for cid, state in graph_states.items():
        assert len(state.Graph.Edges) == stand.edge_limit
        for edge in state.Graph.Edges:
            assert edge.TimeStamp == 1000

    helpers.assert_cid_map(expected_cid_map, cids)
    helpers.assert_vults(None, vults)


def test_merged_to(stand):
    (
        shard_data,
        expected_cid_map,
        vertex_data_len,
    ) = helpers.generate_shards_data_merged_to(
        stand.input_yt_queue["shards"],
        reset_after_count=stand.reset_after_count,
    )

    data = helpers.pack_shard_data(shard_data)
    stand.input_yt_queue["queue"].write(data)

    michurin_states = {}
    for state_id in shard_data.keys():
        # avoid zero cryptaids
        state_id += 100
        state = TMichurinState()
        state.MergedToCryptaId = state_id * 10
        state.Graph.Id = state_id

        michurin_states[state_id] = state.SerializeToString()

    stand.yt_client.insert_rows(
        stand.michurin_state_table_path,
        [
            {"Id": identifier, "State": state}
            for identifier, state in michurin_states.items()
        ],
        format="yson",
    )

    logger.info("Launching Stateful processor")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.cryptaid_yt_queue, stand.cryptaid_consumer, vertex_data_len
        ),
    )
    logger.info("Stateful processor finished")

    graphs = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.michurin_state_table_path)
        )
    )
    cids = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.cryptaid_state_table_path)
        )
    )
    vults = list(
        select_yt_rows(
            stand.yt_client, "* from [{}/queue]".format(stand.vulture_queue_path)
        )
    )
    logger.info("Recieved states from YT")

    expected_michurin_states_count = len(shard_data)
    assert expected_michurin_states_count == len(graphs)

    for row in graphs:
        raw_state = row[b"State"]
        if row[b"Codec"]:
            raw_state = codecs_loads(row[b"Codec"], raw_state)

        state = TMichurinState()
        state.ParseFromString(raw_state)
        assert row[b"Id"] == state.Graph.Id
        assert state.MergedToCryptaId == state.Graph.Id * 10
        assert len(state.Graph.Edges) == 0

    helpers.assert_cid_map(expected_cid_map, cids)
    assert vults == [], "No updates to vulture should be made during this test"


def test_merge(primary_yt_cluster, stand):
    msg_count = 10
    (
        shard_data,
        expected_cid_map,
        merge_shard_data,
        expected_cid_update_requests,
        merge_expected_cid_update_requests,
        merge_expected_cid_map,
    ) = helpers.generate_shards_for_merge(
        stand.input_yt_queue["shards"],
        msg_count,
    )

    data = helpers.pack_shard_data(shard_data)
    stand.input_yt_queue["queue"].write(data)

    logger.info("Launching Stateful processor")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.cryptaid_yt_queue,
            stand.cryptaid_consumer,
            expected_cid_update_requests,
        ),
    )
    logger.info("Stateful processor finished")

    graphs = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.michurin_state_table_path)
        )
    )
    cids = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.cryptaid_state_table_path)
        )
    )
    logger.info("Recieved states from YT")

    expected_michurin_states_count = len(shard_data)
    assert expected_michurin_states_count == len(graphs)

    for row in graphs:
        raw_state = row[b"State"]
        if row[b"Codec"]:
            raw_state = codecs_loads(row[b"Codec"], raw_state)

        state = TMichurinState()
        state.ParseFromString(raw_state)
        assert row[b"Id"] == state.Graph.Id
        assert state.MergedToCryptaId == 0
        assert len(state.Graph.Edges) == msg_count

    helpers.assert_cid_map(expected_cid_map, cids)

    merge_data = helpers.pack_shard_data(merge_shard_data)
    stand.input_yt_queue["queue"].write(merge_data)

    for key, rows in merge_data.items():
        data[key].extend(rows)
    logger.info("Launching Stateful processor for merge events")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.cryptaid_yt_queue,
            stand.cryptaid_consumer,
            expected_cid_update_requests + merge_expected_cid_update_requests,
        ),
    )
    logger.info("Stateful processor finished")

    merged_cids = [cid for cid in expected_cid_map.keys() if not cid % 2]

    logger.info("Launching resharder merge")
    stateful_launcher_resharder(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.rewind_yt_queue,
            stand.rewind_consumer,
            (msg_count + 1) * len(merged_cids),
        ),
    )
    logger.info("Resharder finished")

    logger.info("Launching Stateful processor after resharding")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_write(
            stand.vulture_yt_queue, stand.vulture_consumer, 16
        ),
    )
    logger.info("Stateful processor finished")

    graphs = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.michurin_state_table_path)
        )
    )
    cids = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.cryptaid_state_table_path)
        )
    )
    vults = list(
        select_yt_rows(
            stand.yt_client, "* from [{}/queue]".format(stand.vulture_queue_path)
        )
    )
    logger.info("Recieved states from YT")

    helpers.assert_cid_map(merge_expected_cid_map, cids)
    helpers.assert_vults(None, vults)

    for row in graphs:
        raw_state = row[b"State"]
        if row[b"Codec"]:
            raw_state = codecs_loads(row[b"Codec"], raw_state)

        state = TMichurinState()
        state.ParseFromString(raw_state)
        if row[b"Id"] in merged_cids:
            assert state.MergedToCryptaId == row[b"Id"] + 1
            assert len(state.Graph.Edges) == 0
        else:
            assert state.MergedToCryptaId == 0
            # all edges from two graphs and one edge between
            assert len(state.Graph.Edges) == msg_count * 2 + 1


def test_cid_update(primary_yt_cluster, stand):
    (
        shard_data,
        expected_cid_map,
        michurin_states,
        expected_offsets,
    ) = helpers.generate_data_for_cid_update(stand.input_yt_queue["shards"])

    stand.yt_client.insert_rows(
        stand.michurin_state_table_path,
        [
            {"Id": identifier, "State": state}
            for identifier, state in michurin_states.items()
        ],
        format="yson",
    )

    data = helpers.pack_shard_data(shard_data)
    stand.input_yt_queue["queue"].write(data)

    logger.info("Launching Stateful processor")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.cryptaid_yt_queue, stand.cryptaid_consumer, expected_offsets
        ),
    )
    logger.info("Stateful processor finished")

    graphs = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.michurin_state_table_path)
        )
    )
    cids = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.cryptaid_state_table_path)
        )
    )
    vults = list(
        select_yt_rows(
            stand.yt_client, "* from [{}/queue]".format(stand.vulture_queue_path)
        )
    )
    logger.info("Recieved states from YT")

    expected_michurin_states_count = len(shard_data)
    # because for each graph we make bookkeeping::CID_UPDATE

    assert expected_michurin_states_count == len(graphs)

    helpers.assert_cid_map(expected_cid_map, cids)

    for row in graphs:
        raw_state = row[b"State"]
        if row[b"Codec"]:
            raw_state = codecs_loads(row[b"Codec"], raw_state)

        state = TMichurinState()
        state.ParseFromString(raw_state)
        assert state.BookkeepingCIDUpdatedAt != 0
        # that means we update BookkeepingCIDUpdatedAt
    assert vults == []
    # we didn't write anything to vulcher


def test_tombstone_delete(primary_yt_cluster, stand):
    (
        shard_data,
        michurin_states,
        graphs_number,
        graphs_to_delete_number,
    ) = helpers.generate_data_for_tombstone_delete(
        stand.input_yt_queue["shards"], merged_state_TTL=stand.merged_state_TTL
    )

    stand.yt_client.insert_rows(
        stand.michurin_state_table_path,
        [
            {"Id": identifier, "State": state}
            for identifier, state in michurin_states.items()
        ],
        format="yson",
    )

    data = helpers.pack_shard_data(shard_data)
    stand.input_yt_queue["queue"].write(data)

    logger.info("Launching Stateful processor tombstone_delete")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.input_yt_queue, stand.michurin_consumer, 4
        ),
    )
    logger.info("Stateful processor finished")

    graphs = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.michurin_state_table_path)
        )
    )
    cids = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.cryptaid_state_table_path)
        )
    )
    vults = list(
        select_yt_rows(
            stand.yt_client, "* from [{}/queue]".format(stand.vulture_queue_path)
        )
    )
    logger.info("Recieved states from YT")

    assert len(graphs) == graphs_number - graphs_to_delete_number

    for row in graphs:
        id = row[b"Id"]
        assert str(id)[-1] != "0"
        # id, which ended with 0 should be deleted

    assert vults == []
    assert cids == []
    # we didn't write anything to vulcher and cid_states


def test_split(primary_yt_cluster, stand):
    (
        shard_data,
        michurin_states,
        graphs_number,
        expected_graphs_number,
        expected_vertices_number,
    ) = helpers.generate_data_for_split(stand.input_yt_queue["shards"])

    stand.yt_client.insert_rows(
        stand.michurin_state_table_path,
        [
            {"Id": identifier, "State": state}
            for identifier, state in michurin_states.items()
        ],
        format="yson",
    )

    data = helpers.pack_shard_data(shard_data)
    stand.input_yt_queue["queue"].write(data)

    logger.info("Launching Stateful processor split")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.cryptaid_yt_queue, stand.cryptaid_consumer, 8
        ),
    )
    logger.info("Stateful processor finished")

    logger.info("Launching resharder split")
    stateful_launcher_resharder(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.rewind_yt_queue, stand.rewind_consumer, 4
        ),
    )
    logger.info("resharder finished")

    logger.info("Launching Stateful processor split")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_read(
            stand.input_yt_queue, stand.michurin_consumer, len(data)
        ),
    )
    logger.info("Stateful processor finished %s", len(data))

    logger.info("Launching Stateful processor split 2")
    stateful_launcher(
        stand=stand,
        check_func=lambda: check_queue_is_write(
            stand.vulture_yt_queue, stand.vulture_consumer, 3
        ),
    )
    logger.info("Stateful processor finished")

    graphs = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.michurin_state_table_path)
        )
    )
    cids = list(
        select_yt_rows(
            stand.yt_client, "* from [{}]".format(stand.cryptaid_state_table_path)
        )
    )
    vults = list(
        select_yt_rows(
            stand.yt_client, "* from [{}/queue]".format(stand.vulture_queue_path)
        )
    )
    logger.info("Recieved states from YT")

    # TODO check if exactly the biggest component get old cid

    # check if number of all vertices are constant
    global_vertices_number = 0
    for row in graphs:
        raw_state = row[b"State"]
        if row[b"Codec"]:
            raw_state = codecs_loads(row[b"Codec"], raw_state)

        state = TMichurinState()
        state.ParseFromString(raw_state)
        global_vertices_number += len(state.Graph.Vertices)
    assert global_vertices_number == expected_vertices_number

    # check if graphs were splited
    assert len(graphs) == expected_graphs_number

    # check if old cids are not change
    ids = set()
    ids_to_check = []
    for shard_id in range(stand.input_yt_queue["shards"]):
        for i in range(2):
            ids_to_check.append(str(100 + shard_id * 10 + i))
    for row in graphs:
        id = row[b"Id"]
        ids.add(str(id))
    for id in ids_to_check:
        assert id in ids

    # check if cid_states were updated
    assert len(cids) == 8

    # new states should be reflected in the vulcher
    assert vults != []
