import os

import requests
import retry
import yatest.common

from crypta.cm.services.common import quoter_clients
from crypta.lib.python import (
    templater,
    test_utils,
)


class QuoterHttpProxy(test_utils.TestBinaryContextManager):
    bin_path = "crypta/cm/services/quoter/bin/http_proxy/bin/crypta-cm-quoter-http-proxy"
    app_config_template_path = "crypta/cm/services/quoter/bundle/templates/http_proxy_config.yaml"

    def __init__(self, working_dir, quoter_port):
        super(QuoterHttpProxy, self).__init__("CM Quoter Http Proxy")

        self.working_dir = working_dir
        self.quoter_port = quoter_port
        self.port_manager = yatest.common.network.PortManager()
        self.host = "localhost"

    def _prepare_start(self):
        self.port = self.port_manager.get_port()

        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "http_proxy_config.yaml")
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
            "quoter_port": self.quoter_port,
        })
        self.logger.info("App config template parameters = %s", template_params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, template_params)

    def _wait_until_up(self):
        client = quoter_clients.QuoterHttpClient(self.host, self.port)

        @retry.retry(tries=100, delay=0.1)
        def check_is_up():
            self.logger.info("check_is_up")
            try:
                state = client.get_quota_state("qa_crypta")
                assert state["Description"] != "Haven't received actual state yet"
            except quoter_clients.QuoterHttpClient.Exception as e:
                if e.status_code != requests.codes.not_found:
                    raise
                raise Exception("{} {}".format(e.status_code, e.message))

        check_is_up()
