import collections
import os
import subprocess

import retry
import yatest

from crypta.cm.tools.bin.dyn_tools.lib import helpers as lib_helpers
from crypta.lib.python import templater
from crypta.lib.python import yaml_config
from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.dyntable_utils import replica_utils
from crypta.lib.python.yt.dyntables import (
    kv_schema,
    kv_setup,
)
from crypta.lib.python.yt.tm_utils.test_with_tm import TestWithTm


MASTER = "master"
REPLICA_1 = "replica-1"
REPLICA_2 = "replica-2"
EMPTY = ""
ASYNC = "async"
SYNC = "sync"
ENABLED = "enabled"


class DynTools(object):
    bin_path = "crypta/cm/tools/bin/dyn_tools/bin/cm-dyn-tools"
    app_config_template_path = "crypta/cm/tools/bin/dyn_tools/bin/test/data/config.yaml"

    def __init__(self, master_server, replica_servers, yt_home_path):
        self._run_timeout = 120
        self._app_config_path = os.path.join(yatest.common.test_output_path(), "config.yaml")

        templater.render_file(
            yatest.common.source_path(self.app_config_template_path),
            self._app_config_path,
            {
                "master_server": master_server,
                "replica_servers": {replica: {} for replica in replica_servers},
                "home_path": yt_home_path,
            },
        )
        self._config = yaml_config.load(self._app_config_path)

    @property
    def config(self):
        return self._config

    def list_replicas(self):
        return self.run(["list-replicas"])

    def add_replica(self, args, env=dict()):
        return self.run(["add-replica"] + args,  env)

    def remove_replica(self, args):
        return self.run(["remove-replica"] + args)

    def disconnect_replica(self, args):
        return self.run(["disconnect-replica"] + args)

    def run(self, args, env=dict()):
        command_line = [yatest.common.binary_path(self.bin_path)] + list(args) + [
            "--config",
            self._app_config_path,
        ]

        run_env = os.environ.copy()
        run_env.update(env)

        return yatest.common.execute(
            command_line,
            env=run_env,
            timeout=self._run_timeout,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )


DyntablesSetup = collections.namedtuple("DyntablesSetup", [
    "master_client",
    "master_server",
    "master_path",
    "replica_1_client",
    "replica_1_server",
    "replica_2_client",
    "replica_2_server",
    "replica_path",
])


class DynToolsTest(TestWithTm):
    def prepare(self, yt_subdir):
        cm_tools = DynTools(
            master_server=self.first_cluster,
            replica_servers=[
                self.first_cluster,
                self.second_cluster,
            ],
            yt_home_path=self.get_subdir_path(yt_subdir),
        )

        setup = self._get_dyntables_setup(
            lib_helpers.get_master_path(cm_tools.config),
            lib_helpers.get_replica_path(cm_tools.config),
        )

        setup_replicated_table(
            setup.master_client,
            setup.master_path,
            [
                setup.replica_1_client,
                setup.replica_2_client,
            ],
            setup.replica_path,
        )

        return cm_tools, setup

    def _get_dyntables_setup(self, master_path, replica_path):
        return DyntablesSetup(
            master_client=self.first_cluster_client,
            master_server=self.first_cluster,
            master_path=master_path,

            replica_1_client=self.first_cluster_client,
            replica_1_server=self.first_cluster,

            replica_2_client=self.second_cluster_client,
            replica_2_server=self.second_cluster,

            replica_path=replica_path,
        )


def setup_replicated_table(master_client, master_path, replica_clients, replica_path):
    sync_replica_count = 1

    kv_setup.kv_setup(
        master_client,
        [
            (yt_helpers.get_cluster_name(replica_client), replica_client) for replica_client in replica_clients
        ],
        master_path,
        replica_path,
        kv_schema.get(),
        kv_schema.create_pivot_keys(1),
        sync_replica_count=sync_replica_count,
        enable_replicated_table_tracker=True,
    )

    def has_enough_sync_replicas():
        replicas = replica_utils.list_replicas(master_client, master_path)
        modes = [replica.mode for replica in replicas]
        assert sync_replica_count == modes.count(SYNC)

    retry.retry_call(has_enough_sync_replicas, tries=60, delay=1, exceptions=AssertionError)


def parse_list_replicas(stdout):
    def check_header_line(header_line):
        ref_fields = list(replica_utils.ReplicaStatus._fields)
        assert ref_fields == header_line.split()

    def parse_status_line(replica_status_line):
        return replica_utils.ReplicaStatus(*replica_status_line.split())

    def cleanup_status(replica_status):
        return replica_utils.ReplicaStatus(
            id="",
            mode="",
            cluster=replica_status.cluster,
            path=replica_status.path,
            state=replica_status.state,
            replication_lag_time=replica_status.replication_lag_time,
            replicated_table_tracker_enabled=replica_status.replicated_table_tracker_enabled,
        )

    output_lines = stdout.rstrip().split("\n")
    assert len(output_lines) >= 1, output_lines

    header_line = output_lines[0]
    check_header_line(header_line)

    replica_lines = output_lines[1:]
    raw_statuses = set(parse_status_line(line) for line in replica_lines)

    statuses = set(cleanup_status(status) for status in raw_statuses)
    modes = sorted([status.mode for status in raw_statuses])

    Result = collections.namedtuple("Result", ["statuses", "modes"])
    return Result(
        statuses,
        modes,
    )
