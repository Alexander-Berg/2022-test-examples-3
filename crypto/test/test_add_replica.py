import transfer_manager.python.recipe.lib as tm_lib

from crypta.cm.tools.bin.dyn_tools.bin.test import helpers
from crypta.lib.python.yt.dyntable_utils import replica_utils


class TestAddReplicaWithTm(helpers.DynToolsTest):
    def test_add_replica_same_cluster(self, yt_subdir):
        cm_tools, setup = self.prepare(yt_subdir)

        replica_server = setup.replica_1_server
        replica_copy_path = "{}-copy".format(setup.replica_path)

        cm_tools.add_replica(
            [
                "--src-cluster", replica_server,
                "--dst-cluster", replica_server,
                "--dst-table", replica_copy_path,
            ],
            env=self.get_tm_env(),
        )

        ref_replica_statuses = set([
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_A, setup.replica_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_A, replica_copy_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_B, setup.replica_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
        ])
        ref_modes = sorted([helpers.SYNC, helpers.ASYNC, helpers.ASYNC])

        result = helpers.parse_list_replicas(cm_tools.list_replicas().std_out)
        assert ref_replica_statuses == result.statuses
        assert ref_modes == result.modes

    def test_add_replica_with_tm(self, yt_subdir):
        cm_tools, setup = self.prepare(yt_subdir)

        replica_copy_path = "{}-copy".format(setup.replica_path)

        init_replica_statuses = set([
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_A, setup.replica_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_B, setup.replica_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
        ])
        init_replica_modes = sorted([helpers.SYNC, helpers.ASYNC])

        init_statuses = helpers.parse_list_replicas(cm_tools.list_replicas().std_out)
        assert init_replica_statuses == init_statuses.statuses
        assert init_replica_modes == init_statuses.modes

        cm_tools.add_replica(
            [
                "--src-cluster", setup.replica_1_server,
                "--dst-cluster", setup.replica_2_server,
                "--dst-table", replica_copy_path,
            ],
            env=self.get_tm_env(),
        )

        ref_replica_statuses = init_replica_statuses.union({
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_B, replica_copy_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
        })
        ref_replica_modes = sorted(init_replica_modes + [helpers.ASYNC])

        result = helpers.parse_list_replicas(cm_tools.list_replicas().std_out)
        assert ref_replica_statuses == result.statuses
        assert ref_replica_modes == result.modes
