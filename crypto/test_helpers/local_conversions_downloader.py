import os

import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)
from crypta.s2s.services.conversions_downloader.lib import config_pb2


class LocalConversionsDownloader(test_utils.TestBinaryContextManager):
    bin_path = "crypta/s2s/services/conversions_downloader/bin/crypta-s2s-conversions-downloader"
    config_template_path = "crypta/s2s/services/conversions_downloader/docker/templates/config.template.yaml"

    def __init__(self, working_dir, grut_address, yt_proxy, download_log_logbroker_config, process_log_logbroker_config, solomon_url):
        super(LocalConversionsDownloader, self).__init__("Conversions Downloader")

        self.working_dir = working_dir
        self.grut_address = grut_address
        self.yt_proxy = yt_proxy
        self.download_log_logbroker_config = download_log_logbroker_config
        self.process_log_logbroker_config = process_log_logbroker_config
        self.solomon_url = solomon_url

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
            "grut_address": self.grut_address,
            "yt_proxy": self.yt_proxy,
            "tvm_id": 0,
            "logbroker_server": self.download_log_logbroker_config.host,
            "logbroker_port": self.download_log_logbroker_config.port,
            "download_log_topic": self.download_log_logbroker_config.topic,
            "consumer": self.download_log_logbroker_config.consumer,
            "process_log_topic": self.process_log_logbroker_config.topic,
            "solomon_url": self.solomon_url
        }

        templater.render_file(
            yatest.common.source_path(self.config_template_path),
            config_path,
            params,
        )
