import os
import requests

import retry
import yatest.common

from yt.wrapper import http_helpers

from crypta.styx.services.common.test_utils.crypta_styx_api_client import CryptaStyxApiClient
from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class CryptaStyxApi(test_utils.TestBinaryContextManager):
    bin_path = "crypta/styx/services/api/bin/crypta-styx-api"
    app_config_template_path = "crypta/styx/services/api/bundle/templates/config.yaml"

    def __init__(self,
                 min_delete_interval_sec,
                 yt_kv,
                 working_dir,
                 port,
                 access_log_lb_config,
                 change_log_lb_config,
                 self_tvm_id,
                 clients_config_path,
                 juggler_url_prefix,
                 **template_args):
        super(CryptaStyxApi, self).__init__("Styx API")

        self.ping_id = "puid:ping"
        self.min_delete_interval_sec = min_delete_interval_sec
        self.yt = yt_kv.yt
        self.replica = yt_kv.replica
        self.working_dir = working_dir
        self.port = port
        self.access_log_lb_config = access_log_lb_config
        self.change_log_lb_config = change_log_lb_config
        self.template_args = template_args
        self.self_tvm_id = self_tvm_id
        self.clients_config_path = clients_config_path
        self.juggler_url_prefix = juggler_url_prefix

    def _prepare_start(self):
        self.app_config_path = os.path.join(self.working_dir, "config.yaml")

        self.logger.info("yt.config = %s", repr(self.yt.config))
        self._render_app_config(logs_dir=self.working_dir, **self.template_args)
        self.config = yaml_config.load(self.app_config_path)

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.app_config_path,
            "--clients", yatest.common.source_path(self.clients_config_path),
        ]

    def _render_app_config(self, **template_args):
        params = dict({
            "port": self.port,
            "logs_dir": self.working_dir,

            "environment": "qa",
            "dc": "fake",
            "min_delete_interval_sec": self.min_delete_interval_sec,

            "ping_id": self.ping_id,

            "proxy_role": "default",
            "replica_table_path": self.replica.path,
            "replica_yt_proxy": http_helpers.get_proxy_url(self.replica.yt_client),

            "fqdn": "localhost",

            "access_log_topic": self.access_log_lb_config.topic,
            "access_log_server": self.access_log_lb_config.host,
            "access_log_port": self.access_log_lb_config.port,

            "change_log_topic": self.change_log_lb_config.topic,
            "change_log_server": self.change_log_lb_config.host,
            "change_log_port": self.change_log_lb_config.port,

            "self_tvm_id": self.self_tvm_id,

            "juggler_url_prefix": self.juggler_url_prefix,
        }, **template_args)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)

    def create_client(self):
        return CryptaStyxApiClient("localhost", self.config['http']['port'])

    def _wait_until_up(self):
        client = self.create_client()

        @retry.retry(tries=300, delay=0.1)
        def check_is_up():
            assert client.version().status_code == requests.codes.ok, "Failed to start service"

        check_is_up()

        # TODO(r-andrey): fix that
        # try:
        #     helpers.upload_and_identify(client, self.ping_id, [TMatchedId(ping_constants.PING_RESPONSE_ID, 0, 0, {})])
        # except AssertionError as e:
        #     if "Service has run out of quota" not in e.message:
        #         raise
