import os

import yatest
import yatest.common.network

from crypta.lib.python import (
    templater,
    test_utils,
)


class RtDuidUploader(test_utils.TestBinaryContextManager):
    def __init__(self, working_dir, logbroker_config, sample_percent, **kwargs):
        super(RtDuidUploader, self).__init__("Duid Uploader", **kwargs)

        self.working_dir = working_dir
        self.logbroker_config = logbroker_config
        self.sample_percent = sample_percent

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")
        self.factory_config_path = os.path.join(self.working_dir, "factory_config.yaml")

        self._render_app_config()
        self._render_factory_config()

        return [
            yatest.common.binary_path(self.bin_path),
            "--config-file", self.app_config_path,
            "--factory-config-file", self.factory_config_path,
        ]

    def _render_app_config(self):
        params = {
            "environment": "qa",
            "dc": "qa",
            "logbroker_server": "localhost",
            "logbroker_port": self.logbroker_config.port,
            "topic": self.logbroker_config.topic,
            "client_id": self.logbroker_config.consumer,
            "client_tvm_id": "1",
            "log_dir": self.working_dir,
            "use_secure_tvm": False,
            "log_level": "trace",
            "sample_percent": self.sample_percent,
        }

        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)

    def _render_factory_config(self):
        raise NotImplementedError
