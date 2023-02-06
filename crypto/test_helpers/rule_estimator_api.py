import os

from google.protobuf.empty_pb2 import Empty
import grpc
import retry
import yatest
import yatest.common.network

from crypta.lab.rule_estimator.services.api.proto import (
    api_pb2_grpc,
    config_pb2,
)
from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class RuleEstimatorApi(test_utils.TestBinaryContextManager):
    bin_path = "crypta/lab/rule_estimator/services/api/bin/crypta-rule-estimator-api"
    config_template_path = "crypta/lab/rule_estimator/services/api/docker/templates/config.template.yaml"

    def __init__(self, port, working_dir, logbroker_config, yt_proxy, **kwargs):
        super(RuleEstimatorApi, self).__init__("Rule Estimator API", **kwargs)

        self.working_dir = working_dir
        self.logbroker_config = logbroker_config
        self.port = port
        self.endpoint = "[::]:{}".format(port)
        self.yt_proxy = yt_proxy

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
            "logbroker_server": "localhost",
            "logbroker_port": self.logbroker_config.port,
            "topic": self.logbroker_config.topic,
            "tvm_id": 0,
            "port": self.port,
            "yt_proxy": self.yt_proxy,
        }

        templater.render_file(
            yatest.common.source_path(self.config_template_path),
            config_path,
            params,
        )

    def create_client(self):
        channel = grpc.insecure_channel(self.endpoint)
        stub = api_pb2_grpc.RuleEstimatorStub(channel)
        return stub

    def _wait_until_up(self):
        client = self.create_client()
        retry.retry_call(lambda: client.Ping(Empty()), tries=300, delay=0.1)
