import os

import yatest.common
from yt.wrapper import http_helpers

from crypta.lib.python import (
    templater,
    test_utils,
)


class Mutator(test_utils.TestBinaryContextManager):
    bin_path = "crypta/cm/services/mutator/bin/crypta-cm-mutator"
    app_config_template_path = "crypta/cm/services/mutator/bundle/templates/config.yaml"

    def __init__(self, working_dir, master, change_log_lb_config, evacuate_log_lb_config, frozen_time, template_args=None):
        super(Mutator, self).__init__("CM Mutator", frozen_time=frozen_time)

        self.master = master
        self.change_log_lb_config = change_log_lb_config
        self.evacuate_log_lb_config = evacuate_log_lb_config
        self.ping_ns = "ping"
        self.template_args = template_args or {}
        self.working_dir = working_dir

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")
        self._render_app_config(**self.template_args)

        return [
            yatest.common.binary_path(self.bin_path),
            "--config-file", self.app_config_path,
        ]

    def _render_app_config(self, **kwargs):
        params = dict({
            "environment": "qa",
            "dc": "qa",

            "ping_ns": self.ping_ns,

            "change_log_server": "localhost",
            "change_log_port": self.change_log_lb_config.port,
            "change_log_topic": self.change_log_lb_config.topic,
            "evacuate_log_server": "localhost",
            "evacuate_log_port": self.evacuate_log_lb_config.port,
            "evacuate_log_topic": self.evacuate_log_lb_config.topic,
            "client_id": self.change_log_lb_config.consumer,
            "client_tvm_id": "1",

            "fqdn": "localhost",

            "master_table_path": self.master.path,
            "yt_proxy_url": http_helpers.get_proxy_url(client=self.master.yt_client),
            "proxy_role": "default",

            "log_dir": self.working_dir,
            "use_secure_tvm": False,
            "log_level": "trace",
            "tracked_back_references": ["ext_ns"],
        }, **kwargs)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)
