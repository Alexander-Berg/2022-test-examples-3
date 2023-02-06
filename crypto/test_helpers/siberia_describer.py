import os

import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class SiberiaDescriber(test_utils.TestBinaryContextManager):
    bin_path = "crypta/siberia/bin/describer/bin/crypta-siberia-describer"
    app_config_template_path = "crypta/siberia/bin/describer/bundle/templates/config.yaml"

    def __init__(self, working_dir, logbroker_config, ydb_endpoint, ydb_database, describing_batch_size, stats_update_threshold, frozen_time, template_args=None):
        super(SiberiaDescriber, self).__init__("Siberia Describer", frozen_time=frozen_time)

        self.logbroker_config = logbroker_config
        self.ydb_endpoint = ydb_endpoint
        self.ydb_database = ydb_database
        self.describing_batch_size = describing_batch_size
        self.stats_update_threshold = stats_update_threshold
        self.working_dir = working_dir
        self.template_args = template_args or {}

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")

        self._render_app_config(**self.template_args)
        self.config = yaml_config.load(self.app_config_path)

        return [
            yatest.common.binary_path(self.bin_path),
            "--config-file", self.app_config_path,
        ]

    def _render_app_config(self, **kwargs):
        params = dict({
            "environment": "qa",
            "dc": "qa",
            "logbroker_server": "localhost",
            "logbroker_port": self.logbroker_config.port,
            "topic": self.logbroker_config.topic,
            "client_id": self.logbroker_config.consumer,
            "client_tvm_id": "1",
            "log_dir": self.working_dir,
            "use_secure_tvm": False,
            "ydb_endpoint": self.ydb_endpoint,
            "ydb_database": self.ydb_database,
            "describing_batch_size": self.describing_batch_size,
            "stats_update_threshold": self.stats_update_threshold,
        }, **kwargs)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)
