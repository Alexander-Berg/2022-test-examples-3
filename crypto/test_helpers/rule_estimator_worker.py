import os

import yatest
import yatest.common.network

from crypta.lab.rule_estimator.services.worker.proto import config_pb2
from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class RuleEstimatorWorker(test_utils.TestBinaryContextManager):
    bin_path = "crypta/lab/rule_estimator/services/worker/bin/crypta-rule-estimator-worker"
    config_template_path = "crypta/lab/rule_estimator/services/worker/docker/templates/config.template.yaml"

    def __init__(self, rule_estimator_api, working_dir, logbroker_config, yt_proxy, yt_operation_owners, siberia_host, siberia_port, api_url, stats_port, stats_host, env):
        super(RuleEstimatorWorker, self).__init__("Rule Estimator Worker", env=env)

        self.working_dir = working_dir
        self.logbroker_config = logbroker_config
        self.rule_estimator_api = rule_estimator_api
        self.yt_proxy = yt_proxy
        self.yt_operation_owners = yt_operation_owners
        self.siberia_host = siberia_host
        self.siberia_port = siberia_port
        self.api_url = api_url
        self.stats_port = stats_port
        self.stats_host = stats_host

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
            "consumer": self.logbroker_config.consumer,
            "tvm_id": 0,
            "rule_estimator_api_endpoint": self.rule_estimator_api.endpoint,
            "yt_proxy": self.yt_proxy,
            "yt_operation_owners": self.yt_operation_owners,
            "siberia_host": self.siberia_host,
            "siberia_port": self.siberia_port,
            "api_url": self.api_url,
            "stats_port": self.stats_port,
            "stats_host": self.stats_host,
            "dc": "qa",
        }

        templater.render_file(
            yatest.common.source_path(self.config_template_path),
            config_path,
            params,
        )
