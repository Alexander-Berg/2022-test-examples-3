import os

import yatest.common
from yt.wrapper import http_helpers

from crypta.lib.python import (
    templater,
    test_utils,
)


class CryptaStyxMutator(test_utils.TestBinaryContextManager):
    bin_path = "crypta/styx/services/mutator/bin/crypta-styx-mutator"
    app_config_template_path = "crypta/styx/services/mutator/bundle/templates/config.yaml"

    def __init__(self, working_dir, master, change_log_lb_config, min_delete_interval_sec, frozen_time, template_args):
        super(CryptaStyxMutator, self).__init__("Styx Mutator", frozen_time=frozen_time)

        self.working_dir = working_dir
        self.master = master
        self.min_delete_interval_sec = min_delete_interval_sec
        self.change_log_lb_config = change_log_lb_config
        self.frozen_time = frozen_time
        self.template_args = template_args

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

            "change_log_server": "localhost",
            "change_log_port": self.change_log_lb_config.port,
            "change_log_topic": self.change_log_lb_config.topic,
            "client_id": self.change_log_lb_config.consumer,
            "client_tvm_id": "1",

            "master_table_path": self.master.path,
            "yt_proxy_url": http_helpers.get_proxy_url(client=self.master.yt_client),
            "proxy_role": "default",

            "min_delete_interval_sec": self.min_delete_interval_sec,

            "log_dir": self.working_dir,
            "use_secure_tvm": False,
            "log_level": "trace",
        }, **kwargs)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)
