import os

import grpc
import retry
import yatest.common

from crypta.cm.services.common import quoter_clients
from crypta.lib.python import (
    templater,
    test_utils,
)


class Quoter(test_utils.TestBinaryContextManager):
    bin_path = "crypta/cm/services/quoter/bin/quoter/bin/crypta-cm-quoter"
    app_config_template_path = "crypta/cm/services/quoter/bundle/templates/config.yaml"

    def __init__(self, working_dir, solomon_port):
        super(Quoter, self).__init__("CM Quoter")

        self.working_dir = working_dir
        self.solomon_port = solomon_port
        self.env.setdefault("SOLOMON_TOKEN", "FAKE_SOLOMON_TOKEN")
        self.port_manager = yatest.common.network.PortManager()
        self.host = "localhost"

    def _prepare_start(self):
        self.port = self.port_manager.get_port()

        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")
        self._render_app_config()

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.app_config_path,
        ]

    def _on_exit(self):
        self.port_manager.release()
        self.port = None

    def _render_app_config(self):
        template_params = dict({
            "environment_type": "qa",
            "port": self.port,
            "solomon_schema": "http",
            "solomon_host": "localhost",
            "solomon_port": self.solomon_port,
            "logs_dir": self.working_dir,
        })
        self.logger.info("App config template parameters = %s", template_params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, template_params)

    def _wait_until_up(self):
        client = quoter_clients.QuoterClient(self.host, self.port)

        @retry.retry(tries=100, delay=0.1)
        def check_is_up():
            self.logger.info("check_is_up")
            try:
                state = client.get_quota_state("qa_crypta")
                assert state.Description != "Haven't received actual state yet"
            except grpc.RpcError as e:
                if e.code() != grpc.StatusCode.NOT_FOUND:
                    raise
                raise Exception(e.details())

        check_is_up()
