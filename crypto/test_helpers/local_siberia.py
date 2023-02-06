import os

import retry
import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
)
from crypta.siberia.bin.common.siberia_client import SiberiaClient


class LocalSiberia(test_utils.TestBinaryContextManager):
    config_template_path = "crypta/siberia/bin/core/bundle/templates/config.yaml"
    binary_path = "crypta/siberia/bin/core/bin/crypta-siberia"

    def __init__(
            self,
            working_dir,
            port,
            ydb_endpoint,
            ydb_database,
            access_log_logbroker_config,
            change_log_logbroker_config,
            describe_log_logbroker_config,
            describe_slow_log_logbroker_config,
            segmentate_log_logbroker_config,
            self_tvm_id,
            clients_config_path,
            juggler_url_prefix,
            frozen_time=None,
    ):
        super(LocalSiberia, self).__init__("Siberia", frozen_time=frozen_time)
        self.environment = "qa"
        self.working_dir = working_dir
        self.config_path = os.path.join(self.working_dir, "config.yaml")
        self.port = port
        self.ydb_endpoint = ydb_endpoint
        self.ydb_database = ydb_database
        self.access_log_logbroker_config = access_log_logbroker_config
        self.change_log_logbroker_config = change_log_logbroker_config
        self.describe_log_logbroker_config = describe_log_logbroker_config
        self.describe_slow_log_logbroker_config = describe_slow_log_logbroker_config
        self.segmentate_log_logbroker_config = segmentate_log_logbroker_config
        self.tvm_id = self_tvm_id
        self.clients_config_path = clients_config_path
        self.juggler_url_prefix = juggler_url_prefix

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self._render_config()

        return [
            yatest.common.binary_path(self.binary_path),
            "--config", self.config_path,
            "--clients", yatest.common.source_path(self.clients_config_path),
        ]

    def _render_config(self):
        context = dict(
            environment=self.environment,
            dc="qa_dc",
            self_tvm_id=self.tvm_id,

            ydb_endpoint=self.ydb_endpoint,
            ydb_database=self.ydb_database,
            port=self.port,
            logs_dir=self.working_dir,
            use_secure_tvm=False,

            access_log_logbroker_server=self.access_log_logbroker_config.host,
            access_log_logbroker_port=self.access_log_logbroker_config.port,
            access_log_topic=self.access_log_logbroker_config.topic,

            change_log_logbroker_server=self.change_log_logbroker_config.host,
            change_log_logbroker_port=self.change_log_logbroker_config.port,
            change_log_topic=self.change_log_logbroker_config.topic,
            change_log_partitions_count=1,

            describe_log_logbroker_server=self.describe_log_logbroker_config.host,
            describe_log_logbroker_port=self.describe_log_logbroker_config.port,
            describe_log_topic=self.describe_log_logbroker_config.topic,
            describe_log_partitions_count=1,
            describing_max_ids_count=10,

            describe_slow_log_logbroker_server=self.describe_slow_log_logbroker_config.host,
            describe_slow_log_logbroker_port=self.describe_slow_log_logbroker_config.port,
            describe_slow_log_topic=self.describe_slow_log_logbroker_config.topic,
            describe_slow_log_partitions_count=1,

            segmentate_log_logbroker_server=self.segmentate_log_logbroker_config.host,
            segmentate_log_logbroker_port=self.segmentate_log_logbroker_config.port,
            segmentate_log_topic=self.segmentate_log_logbroker_config.topic,
            segmentate_log_partitions_count=1,

            juggler_url_prefix=self.juggler_url_prefix,

            fqdn="fqdn",
        )
        templater.render_file(yatest.common.source_path(self.config_template_path), self.config_path, context, strict=True)

    def get_client(self):
        return SiberiaClient("localhost", self.port)

    def _wait_until_up(self):
        client = self.get_client()

        @retry.retry(tries=250, delay=0.2)
        def check_is_up():
            client.version()

        check_is_up()
