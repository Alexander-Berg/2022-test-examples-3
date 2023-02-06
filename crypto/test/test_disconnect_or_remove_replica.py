import pytest
import retry

import transfer_manager.python.recipe.lib as tm_lib

from crypta.cm.tools.bin.dyn_tools.bin.test import helpers
from crypta.lib.python.yt.dyntable_utils import replica_utils


def call_method_by_name(obj, name, *args):
    return getattr(obj, name)(*args)


class TestDisconnectOrRemoveReplica(helpers.DynToolsTest):
    @pytest.mark.parametrize("operation_name", ["disconnect_replica", "remove_replica"])
    def test_disconnect_or_remove_replica(self, yt_subdir, operation_name):
        cm_tools, setup = self.prepare(yt_subdir)

        call_method_by_name(cm_tools, operation_name, [
            "--replica-cluster", self.second_cluster,
        ])

        ref_replica_statuses = set([
            replica_utils.ReplicaStatus(helpers.EMPTY, tm_lib.CLUSTER_A, setup.replica_path, helpers.EMPTY, helpers.ENABLED, "0", 'true'),
        ])
        ref_modes = [helpers.SYNC]

        @retry.retry(tries=5, delay=1, exceptions=AssertionError)
        def check_replicas():
            result = helpers.parse_list_replicas(cm_tools.list_replicas().std_out)
            assert ref_replica_statuses == result.statuses
            assert ref_modes == result.modes

        check_replicas()
