import datetime
import os

import mapreduce.yt.python.yt_stuff as vanilla
import pytest
import yatest
from yt import yson

from crypta.lib.python.spine.yt import yt_replicated_table
from crypta.lib.python.yt.dyntables import kv_setup


MASTER = "master"
REPLICA_1 = "replica-1"
REPLICA_2 = "replica-2"


@pytest.fixture
def table_path():
    return "//table"


@pytest.fixture
def schema():
    schema = yson.YsonList([
        dict(name="xxx", type="int64", required=True, sort_order="ascending"),
        dict(name="yyy", type="int64", required=True),
    ])
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = True
    return schema


def get_yt_config(yt_id, cell_tag):
    return vanilla.YtConfig(
        wait_tablet_cell_initialization=True,
        node_config={
            "tablet_node": {
                "resource_limits": {
                    "tablet_dynamic_memory": 100 * 1024 * 1024,
                    "tablet_static_memory": 100 * 1024 * 1024,
                }
            }
        },
        yt_id=yt_id,
        yt_work_dir=yatest.common.test_output_path(yt_id),
        cell_tag=cell_tag,
    )


@pytest.fixture(scope="session")
def clusters():
    names = [MASTER, REPLICA_1, REPLICA_2]
    result = {}

    for cell_tag, name in enumerate(names):
        config = get_yt_config(name, cell_tag)

        os.makedirs(config.yt_work_dir)

        yt = vanilla.YtStuff(config)
        yt.start_local_yt()

        yt.get_yt_client().set("//sys/@cluster_name", name)

        result[name] = yt

    cluster_config = {yt.yt_id: yt.get_cluster_config() for yt in result.values()}

    for yt in result.values():
        yt.get_yt_client().set("//sys/clusters", cluster_config)

    yield result

    for yt in result.values():
        yt.stop_local_yt()


@pytest.fixture(scope="function")
def juggler_client():
    class MockJugglerClient(object):
        def __init__(self):
            self.requests = []

        def send_ok(self, host, service, description):
            self.send_event("OK", host, service, description)

        def send_warn(self, host, service, description):
            self.send_event("WARN", host, service, description)

        def send_crit(self, host, service, description):
            self.send_event("CRIT", host, service, description)

        def send_event(self, status, host, service, description):
            self.requests.append({
                "host": host,
                "service": service,
                "status": status,
                "description": description
            })

        def dump_events_requests(self):
            return self.requests

    return MockJugglerClient()


@pytest.fixture(scope="function")
def replicated_table(clusters, table_path):
    return yt_replicated_table.ReplicatedTable(
        master=yt_replicated_table.Master(
            name=MASTER,
            proxy=clusters[MASTER].get_server(),
            path=table_path,
            expected_attributes={"tablet_cell_bundle": "default"},
            replication_lag_threshold=datetime.timedelta(seconds=60),
            sync_count=2,
        ),
        replicas=[
            yt_replicated_table.Replica(
                name=REPLICA_1,
                proxy=clusters[REPLICA_1].get_server(),
                path=table_path,
                expected_replication_attributes={"replicated_table_tracker_enabled": True},
            ),
            yt_replicated_table.Replica(
                name=REPLICA_2,
                proxy=clusters[REPLICA_2].get_server(),
                path=table_path,
                expected_replication_attributes={"replicated_table_tracker_enabled": True},
            ),
        ],
    )


@pytest.fixture(scope="function")
def unavailable_replicated_table(table_path):
    return yt_replicated_table.ReplicatedTable(
        yt_replicated_table.Master(
            name=MASTER,
            proxy="unavailable.master",
            path=table_path,
            replication_lag_threshold=datetime.timedelta(seconds=60),
            sync_count=2,
        ),
        replicas=[
            yt_replicated_table.Replica(
                name="replica",
                proxy="unavailable.proxy",
                path=table_path,
            ),
        ],
    )


@pytest.fixture(scope="function")
def db_setup(clusters, schema, table_path):
    return kv_setup.kv_setup(
        clusters[MASTER].get_yt_client(),
        [
            (REPLICA_1, clusters[REPLICA_1].get_yt_client()),
            (REPLICA_2, clusters[REPLICA_2].get_yt_client()),
        ],
        table_path,
        table_path,
        schema,
        [[]],
        sync=True
    )
