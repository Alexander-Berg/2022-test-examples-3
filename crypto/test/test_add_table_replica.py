import collections
import os

from transfer_manager.python.recipe.interface import TransferManagerTest
import yt.transfer_manager.client as tm_client
import yt.wrapper as yt

from crypta.lib.python.yt import (
    tm_utils,
    yt_helpers,
)
from crypta.lib.python.yt.dyntables import (
    kv_schema,
    kv_setup,
)
from crypta.lib.python.yt.dyntable_utils import (
    add_table_replica,
    replica_utils,
)


TEMP_PREFIX = "//tmp"
YT_POOL = "default"


def setup_single_replica(master_client, master_path, replica_client, replica_path):
    return kv_setup.kv_setup(
        master_client,
        [(yt_helpers.get_cluster_name(replica_client), replica_client)],
        master_path,
        replica_path,
        kv_schema.get(),
        kv_schema.create_pivot_keys(1),
        sync=True
    )


def cleanup_id(replica):
    result = dict(vars(replica))
    del result["id"]
    return result


def canonify_replicas(replicas):
    return sorted([cleanup_id(replica) for replica in replicas], key=lambda item: (item["cluster"] + ":" + item["path"]))


DyntablesSetup = collections.namedtuple("DyntablesSetup", [
    "master_client",
    "master_path",
    "replica_1_client",
    "replica_2_client",
    "replica_path",
])


class TestAddTableReplicaWithTm(TransferManagerTest):
    def prepare(self, prefix):
        master_client = self.first_cluster_client
        replica_1_client = self.first_cluster_client
        replica_2_client = self.second_cluster_client

        master_path = yt.ypath_join(prefix, "master")
        replica_path = yt.ypath_join(prefix, "replica")

        for client in [master_client, replica_1_client, replica_2_client]:
            client.mkdir(prefix, recursive=True)

        setup_single_replica(master_client, master_path, replica_1_client, replica_path)
        assert 1 == len(replica_utils.list_replicas(master_client, master_path))

        os.environ[tm_client.YT_TRANSFER_MANAGER_URL_ENV] = self.tm_client.backend_url

        return DyntablesSetup(
            master_client=master_client,
            master_path=master_path,
            replica_1_client=replica_1_client,
            replica_2_client=replica_2_client,
            replica_path=replica_path,
        )

    def test_add_table_replica_same_cluster(self):
        setup = self.prepare("//same_cluster")

        replica_client = setup.replica_1_client

        add_table_replica.add_table_replica(
            setup.master_client,
            setup.master_path,
            replica_client,
            setup.replica_path,
            replica_client,
            "{}-2".format(setup.replica_path),
            TEMP_PREFIX,
            YT_POOL,
            force=False,
            tm_client=tm_utils.get_client("FAKE_YT_TOKEN"),
            replica_attrs={"enable_dynamic_store_read": True},
            replication_attrs={"enable_replicated_table_tracker": False},
        )
        yt_helpers.wait_for_replicas_enabled(setup.master_client, setup.master_path)

        assert replica_client.get_attribute(setup.replica_path, "enable_dynamic_store_read")
        return canonify_replicas(replica_utils.list_replicas(setup.master_client, setup.master_path))

    def test_add_table_replica(self):
        setup = self.prepare("//with_transfer_manager")

        add_table_replica.add_table_replica(
            setup.master_client,
            setup.master_path,
            setup.replica_1_client,
            setup.replica_path,
            setup.replica_2_client,
            setup.replica_path,
            TEMP_PREFIX,
            YT_POOL,
            force=False,
            tm_client=tm_utils.get_client("FAKE_YT_TOKEN"),
            replica_attrs={"enable_dynamic_store_read": True},
            replication_attrs={"enable_replicated_table_tracker": False},
        )
        yt_helpers.wait_for_replicas_enabled(setup.master_client, setup.master_path)

        assert setup.replica_2_client.get_attribute(setup.replica_path, "enable_dynamic_store_read")
        return canonify_replicas(replica_utils.list_replicas(setup.master_client, setup.master_path))
