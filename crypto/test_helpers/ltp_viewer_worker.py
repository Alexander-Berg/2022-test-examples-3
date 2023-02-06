import os

import yatest
import yatest.common.network

from crypta.ltp.viewer.services.worker.proto import config_pb2
from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class LtpViewerWorker(test_utils.TestBinaryContextManager):
    bin_path = "crypta/ltp/viewer/services/worker/bin/crypta-ltp-viewer-worker"
    config_template_path = "crypta/ltp/viewer/services/worker/docker/templates/config.template.yaml"

    def __init__(self, ltp_viewer_api, working_dir, logbroker_config, yt_proxy, stats_port, stats_host, local_ydb, pages, categories, regions, env):
        super(LtpViewerWorker, self).__init__("LTP Viewer Worker", env=env)

        self.working_dir = working_dir
        self.logbroker_config = logbroker_config
        self.ltp_viewer_api = ltp_viewer_api
        self.yt_proxy = yt_proxy
        self.stats_port = stats_port
        self.stats_host = stats_host
        self.local_ydb = local_ydb
        self.pages = pages
        self.categories = categories
        self.regions = regions

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.config_path = os.path.join(self.working_dir, "config.yaml")

        self._render_config(self.config_path)
        self.config = yaml_config.parse_config(config_pb2.TConfig, self.config_path)

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.config_path,
        ]

    def _render_config(self, config_path):
        params = {
            "environment": "qa",
            "logbroker_server": "localhost",
            "logbroker_port": self.logbroker_config.port,
            "topic": self.logbroker_config.topic,
            "consumer": self.logbroker_config.consumer,
            "tvm_id": 0,
            "ltp_viewer_api_endpoint": self.ltp_viewer_api.endpoint,
            "yt_proxy": self.yt_proxy,
            "dynamic_yt_proxy": self.yt_proxy,
            "ydb_endpoint": self.local_ydb.endpoint,
            "ydb_database": self.local_ydb.database,
            "stats_port": self.stats_port,
            "stats_host": self.stats_host,
            "tasks_per_history": 2,
            "pages_path": self.pages,
            "categories_path": self.categories,
            "regions_path": self.regions,
        }

        templater.render_file(
            yatest.common.source_path(self.config_template_path),
            config_path,
            params,
        )
