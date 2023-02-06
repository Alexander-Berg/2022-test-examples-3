import os

import requests
import retry
import yatest.common
from yt.wrapper import http_helpers

from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class CryptaResourceService(test_utils.TestBinaryContextManager):
    bin_path = "crypta/utils/rtmr_resource_service/bin/server/bin/crypta-rs-server"
    app_config_template_path = "crypta/utils/rtmr_resource_service/config/config.template.yaml"
    DC = "DC"

    def __init__(
        self,
        yt_kv,
        working_dir,
        file_root,
        port,
        resources,
        solomon_port,
    ):
        super(CryptaResourceService, self).__init__("Resource Service", env={"QLOUD_DATACENTER": self.DC, "DEPLOY_NODE_DC": self.DC})

        if not os.path.isdir(working_dir):
            os.mkdir(working_dir)

        self.yt_kv = yt_kv
        self.working_dir = working_dir
        self.file_root = file_root
        self.port = port
        self.resources = resources
        self.solomon_port = solomon_port

    def _prepare_start(self):
        self.app_config_path = os.path.join(self.working_dir, "app-config")

        self._render_app_config()
        self.config = yaml_config.load(self.app_config_path)

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.app_config_path,
        ]

    def _render_app_config(self):
        params = {
            "environment": "qa",
            "dcs": {self.DC: http_helpers.get_proxy_url(self.yt_kv.replica.yt_client)},
            "master_path": self.yt_kv.master.path,
            "replica_path": self.yt_kv.replica.path,
            "master_proxy": http_helpers.get_proxy_url(self.yt_kv.master.yt_client),
            "port": self.port,
            "file_root": self.file_root,
            "resources": self.resources,
            "solomon_port": self.solomon_port,
        }
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)

    def _wait_until_up(self):
        @retry.retry(tries=300, delay=0.1)
        def check_is_up():
            requests.get(self.url_prefix + "/ping").raise_for_status()
            requests.get(self.solomon_url_prefix + "/ping").raise_for_status()

        check_is_up()

    @property
    def url_prefix(self):
        return "http://[::]:{}".format(self.port)

    @property
    def solomon_url_prefix(self):
        return "http://[::]:{}".format(self.solomon_port)

    def get_sensors(self):
        return requests.get(self.solomon_url_prefix + "/sensors").json()
