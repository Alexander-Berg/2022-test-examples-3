import transfer_manager.python.recipe.lib as tm_lib

from crypta.cm.tools.bin.dyn_tools.bin.test import helpers
from crypta.lib.python.yt.dyntable_utils import replica_utils


class TestListReplicas(helpers.DynToolsTest):
    def test_list_replicas(self, yt_subdir):
        cm_tools, setup = self.prepare(yt_subdir)

        ref_replica_statuses = set([
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_A, setup.replica_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_B, setup.replica_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
        ])
        ref_modes = sorted([helpers.SYNC, helpers.ASYNC])

        result = helpers.parse_list_replicas(cm_tools.list_replicas().std_out)
        assert ref_replica_statuses == result.statuses
        assert ref_modes == result.modes
