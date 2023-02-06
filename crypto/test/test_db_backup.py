import datetime
import os

import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    templater,
    time_utils,
)
from crypta.lib.python.yt.test_helpers import (
    replicated_tables,
    tables,
    tests,
)
from crypta.lib.python.yt.tm_utils.test_with_tm import TestWithTm


CONFIG_TEMPLATE = "crypta/cm/services/db_backup/bundle/config.yaml"
FROZEN_TIME = str(1600000000)
BACKUP_TTL_DAYS = 7


def render_config_file(template_path, parameters, test_subdir=None, file_name="config.yaml"):
    template_path = yatest.common.source_path(template_path)

    working_dir = yatest.common.test_output_path(test_subdir)
    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, file_name)
    templater.render_file(template_path, config_path, parameters)

    return config_path


def get_config_path(src_cluster, master_table, src_table, src_tmp_dir, dst_cluster, dst_dir):
    return render_config_file(
        CONFIG_TEMPLATE,
        {
            "master_cluster": src_cluster,
            "master_table": master_table,
            "src_cluster": src_cluster,
            "src_table": src_table,
            "src_tmp_dir": src_tmp_dir,
            "src_pool": "pool",
            "dst_cluster": dst_cluster,
            "dst_dir": dst_dir,
            "backup_ttl_days": BACKUP_TTL_DAYS,
        },
    )


class TestDbBackup(TestWithTm):
    def test_db_backup(self):
        src_dir = self.get_subdir_path("src")
        master_table_path = yt.ypath_join(src_dir, "master")
        src_table_path = yt.ypath_join(src_dir, "replica")
        src_tmp_dir = self.get_subdir_path("tmp")
        dst_dir = self.get_subdir_path("dst")

        config_path = get_config_path(self.first_cluster, master_table_path, src_table_path, src_tmp_dir, self.second_cluster, dst_dir)

        env = self.get_tm_env()
        env[time_utils.CRYPTA_FROZEN_TIME_ENV] = FROZEN_TIME

        return tests.yt_test(
            self.first_cluster_client,
            binary=yatest.common.binary_path("crypta/cm/services/db_backup/bin/crypta-cm-db-backup"),
            args=["--config", config_path],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (
                    replicated_tables.ReplicatedDynamicYsonTable(
                        "input.yson",
                        self.first_cluster_client,
                        master_table_path,
                        self.first_cluster_client,
                        src_table_path,
                    ),
                    tests.TableIsNotChanged()
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable("output.yson", yt.ypath_join(dst_dir, FROZEN_TIME), yson_format="pretty"),
                    (
                        tests.Diff(yt_client=self.second_cluster_client),
                        tests.ExpirationTime(ttl=datetime.timedelta(days=BACKUP_TTL_DAYS), yt_client=self.second_cluster_client),
                    )
                )
            ],
            env=env,
        )
