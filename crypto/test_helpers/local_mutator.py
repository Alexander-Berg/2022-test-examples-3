import os

import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
)


class LocalMutator(test_utils.TestBinaryContextManager):
    config_template_path = "crypta/siberia/bin/mutator/bundle/templates/config.yaml"
    binary_path = "crypta/siberia/bin/mutator/bin/crypta-siberia-mutator"

    def __init__(self, working_dir, ydb_endpoint, ydb_database, logbroker_config):
        super(LocalMutator, self).__init__("Siberia Mutator")

        self.working_dir = working_dir
        self.ydb_endpoint = ydb_endpoint
        self.ydb_database = ydb_database
        self.logbroker_config = logbroker_config

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        config_path = os.path.join(self.working_dir, "config.yaml")
        self._render_config(config_path)

        return [
            yatest.common.binary_path(self.binary_path),
            "--config-file", config_path,
        ]

    def _render_config(self, config_path):
        context = dict(
            environment="qa",
            dc="qa",
            logbroker_server="localhost",
            logbroker_port=self.logbroker_config.port,
            topic=self.logbroker_config.topic,
            client_id=self.logbroker_config.consumer,
            ydb_endpoint=self.ydb_endpoint,
            ydb_database=self.ydb_database,
            use_secure_tvm=False,
            log_dir=self.working_dir,
        )
        templater.render_file(yatest.common.source_path(self.config_template_path), config_path, context, strict=True)
