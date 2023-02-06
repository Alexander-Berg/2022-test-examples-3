import os

import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)
from crypta.s2s.services.conversions_processor.lib.processor import config_pb2


class LocalConversionsProcessor(test_utils.TestBinaryContextManager):
    bin_path = "crypta/s2s/services/conversions_processor/bin/crypta-s2s-conversions-processor"
    config_template_path = "crypta/s2s/services/conversions_processor/docker/templates/config.template.yaml"

    def __init__(
        self,
        working_dir,
        grut_address,
        cdp_api_address,
        yt_proxy,
        process_log_logbroker_config,
        tvm_id,
        cdp_api_tvm_id,
        max_order_age_days,
        max_backup_size,
        stats_port,
        env,
    ):
        super(LocalConversionsProcessor, self).__init__("Conversions Processor", env=env)

        self.working_dir = working_dir
        self.grut_address = grut_address
        self.cdp_api_address = cdp_api_address
        self.yt_proxy = yt_proxy
        self.process_log_logbroker_config = process_log_logbroker_config
        self.tvm_id = tvm_id
        self.cdp_api_tvm_id = cdp_api_tvm_id
        self.max_order_age_days = max_order_age_days
        self.max_backup_size = max_backup_size
        self.stats_port = stats_port

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
            "cdp_api_address": self.cdp_api_address,
            "yt_proxy": self.yt_proxy,
            "tvm_id": self.tvm_id,
            "cdp_api_tvm_id": self.cdp_api_tvm_id,
            "logbroker_server": self.process_log_logbroker_config.host,
            "logbroker_port": self.process_log_logbroker_config.port,
            "process_log_topic": self.process_log_logbroker_config.topic,
            "consumer": self.process_log_logbroker_config.consumer,
            "lines_per_file": 1,
            "max_order_age_days": self.max_order_age_days,
            "max_backup_size": self.max_backup_size,
            "max_worker_count": 2,
            "stats_host": "localhost",
            "stats_port": self.stats_port,
        }

        templater.render_file(
            yatest.common.source_path(self.config_template_path),
            config_path,
            params,
        )
