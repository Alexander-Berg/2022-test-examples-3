import os

import yatest.common
import yt.wrapper as yt

from crypta.lib.python import templater
from crypta.lib.python.yt.test_helpers import (
    files,
    replicated_tables,
    tables,
    tests,
)
from crypta.lib.python.yt.tm_utils.test_with_tm import TestWithTm


CONFIG_TEMPLATE = "crypta/styx/services/history_publisher/bundle/config.yaml"


def render_config_file(template_path, parameters, test_subdir=None, file_name="config.yaml"):
    template_path = yatest.common.source_path(template_path)

    working_dir = yatest.common.test_output_path(test_subdir)
    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, file_name)
    templater.render_file(template_path, config_path, parameters)

    return config_path


def get_config_path(src_cluster, master_table, src_table, dst_cluster, dst_dir, yt_tmp_dir):
    return render_config_file(
        CONFIG_TEMPLATE,
        {
            "master_cluster": src_cluster,
            "master_table": master_table,
            "src_cluster": src_cluster,
            "src_pool": "pool",
            "src_table": src_table,
            "dst_cluster": dst_cluster,
            "dst_dir": dst_dir,
            "yt_tmp_dir": yt_tmp_dir,
            "additional_dst_clusters": [src_cluster],  # transfer manager recipe allows for only 2 clusters, so reusing master
        },
    )


class TestHistoryPublisher(TestWithTm):
    def test_history_publisher(self):
        src_dir = self.get_subdir_path("src")
        master_table_path = yt.ypath_join(src_dir, "master")
        src_table_path = yt.ypath_join(src_dir, "replica")
        dst_dir = self.get_subdir_path("dst")
        dst_table = yt.ypath_join(dst_dir, "as-table")
        dst_file = yt.ypath_join(dst_dir, "as-file")
        yt_tmp_dir = self.get_subdir_path("tmp")

        config_path = get_config_path(self.first_cluster, master_table_path, src_table_path, self.second_cluster, dst_dir, yt_tmp_dir)

        env = self.get_tm_env()

        return tests.yt_test(
            self.first_cluster_client,
            binary=yatest.common.binary_path("crypta/styx/services/history_publisher/bin/crypta-styx-history-publisher"),
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
                    tables.YsonTable("as-table.yson", dst_table, yson_format="pretty"),
                    tests.Diff(yt_client=self.second_cluster_client)
                ),
                (
                    files.YtFile("as-file", dst_file),
                    tests.Diff(yt_client=self.second_cluster_client)
                ),
                (
                    tables.YsonTable("additional-as-table.yson", dst_table, yson_format="pretty"),
                    tests.Diff(yt_client=self.first_cluster_client)
                ),
                (
                    files.YtFile("additional-as-file", dst_file),
                    tests.Diff(yt_client=self.first_cluster_client)
                ),
            ],
            env=env,
        )
