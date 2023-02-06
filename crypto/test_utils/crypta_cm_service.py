import os
import requests

import retry
import yatest.common

from yt.wrapper import http_helpers

from crypta.cm.services.common.data.python.id import TId
from crypta.cm.services.common.data.python.matched_id import TMatchedId
from crypta.cm.services.common.ping.python import constants as ping_constants
from crypta.cm.services.common.serializers.id.string.python import id_string_serializer
from crypta.cm.services.common.test_utils import helpers
from crypta.cm.services.common.test_utils.crypta_cm_client import CryptaCmClient
from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class CryptaCmService(test_utils.TestBinaryContextManager):
    bin_path = "crypta/cm/services/api/bin/crypta-cm-api"
    app_config_template_path = "crypta/cm/services/api/bundle/templates/config.yaml"
    turbo_decryptor_secret_path = "crypta/cm/services/common/test_utils/data/turbo-decryptor-secret"

    def __init__(self,
                 yt_kv,
                 working_dir,
                 access_log_lb_config,
                 change_log_lb_config,
                 self_tvm_id,
                 clients_config_path,
                 juggler_url_prefix,
                 quoter_host_port,
                 **template_args):
        super(CryptaCmService, self).__init__("CM")

        self.ping_id = TId("ping", "ping")
        self.yt = yt_kv.yt
        self.replica = yt_kv.replica
        self.working_dir = working_dir
        self.access_log_lb_config = access_log_lb_config
        self.change_log_lb_config = change_log_lb_config
        self.template_args = template_args
        self.self_tvm_id = self_tvm_id
        self.clients_config_path = clients_config_path
        self.juggler_url_prefix = juggler_url_prefix
        self.quoter_enabled = quoter_host_port is not None
        self.quoter_host_port = quoter_host_port if quoter_host_port is not None else ""

    def _prepare_start(self):
        self.app_config_path = os.path.join(self.working_dir, "config.yaml")

        self.logger.info("yt.config = %s", repr(self.yt.config))
        self._render_app_config(logs_dir=self.working_dir, **self.template_args)
        self.config = yaml_config.load(self.app_config_path)

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.app_config_path,
            "--clients", yatest.common.source_path(self.clients_config_path),
            "--turbo-decryptor-secret", yatest.common.source_path(self.turbo_decryptor_secret_path),
        ]

    def _render_app_config(self, **template_args):
        params = dict({
            "environment": "qa",
            "dc": "fake",

            "ping_id": id_string_serializer.ToString(self.ping_id),

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

            "quoter_enabled": self.quoter_enabled,
            "quoter_host_port": self.quoter_host_port,
        }, **template_args)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)

    def create_client(self):
        return CryptaCmClient("localhost", self.config['http']['port'], self)

    def _wait_until_up(self):
        client = self.create_client()

        @retry.retry(tries=300, delay=0.1)
        def check_is_up():
            assert client.version().status_code == requests.codes.ok, "Failed to start service"

        check_is_up()

        try:
            helpers.upload_and_identify(client, self.ping_id, [TMatchedId(ping_constants.PING_RESPONSE_ID, 0, 0, {})])
        except AssertionError as e:
            if "Service has run out of quota" not in str(e):
                raise


def create(**template_args):
    def decorator(f):
        def wrapper(yt_kv, access_log_logbroker_config, mutator, tvm_ids, clients_config_path, session_mock_juggler_server, mock_quoter_server):
            with yatest.common.network.PortManager() as port_manager:
                working_dir = os.path.join(yatest.common.test_output_path(), f.__name__)

                if not os.path.isdir(working_dir):
                    os.mkdir(working_dir)

                with CryptaCmService(
                        yt_kv=yt_kv,
                        working_dir=working_dir,
                        port=port_manager.get_port(),
                        access_log_lb_config=access_log_logbroker_config,
                        change_log_lb_config=mutator.change_log_lb_config,
                        self_tvm_id=tvm_ids.api,
                        clients_config_path=clients_config_path,
                        juggler_url_prefix=session_mock_juggler_server.url_prefix,
                        quoter_host_port="localhost:{}".format(mock_quoter_server.port) if mock_quoter_server.is_enabled else None,
                        **template_args
                ) as service:
                    yield service.create_client()

        return wrapper
    return decorator
