import os

import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
)


class DelayLine(test_utils.TestBinaryContextManager):
    bin_path = "crypta/ext_fp/delay_line/bin/crypta-ext-fp-delay-line"
    app_config_template_path = "crypta/ext_fp/delay_line/bundle/templates/config.yaml"

    def __init__(self, working_dir, artificial_delay_sec, ext_fp_event_delayed_log_lb_config, ext_fp_event_log_lb_config, frozen_time):
        super(DelayLine, self).__init__("Delay Line", frozen_time=frozen_time)

        self.working_dir = working_dir
        self.artificial_delay_sec = artificial_delay_sec
        self.ext_fp_event_delayed_log_lb_config = ext_fp_event_delayed_log_lb_config
        self.ext_fp_event_log_lb_config = ext_fp_event_log_lb_config

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")
        self._render_app_config()

        return [
            yatest.common.binary_path(self.bin_path),
            "--config-file", self.app_config_path,
        ]

    def _render_app_config(self, **kwargs):
        params = dict({
            "environment": "qa",
            "input_log_server": "localhost",
            "input_log_port": self.ext_fp_event_delayed_log_lb_config.port,
            "input_log_topic": self.ext_fp_event_delayed_log_lb_config.topic,
            "output_log_server": "localhost",
            "output_log_topic": self.ext_fp_event_log_lb_config.topic,
            "output_log_port": self.ext_fp_event_log_lb_config.port,
            "artificial_delay_sec": self.artificial_delay_sec,
            "ext_fp_max_events_per_batch": 1,
            "log_dir": self.working_dir,
            "log_level": "trace",
            "use_secure_tvm": False,
            "client_id": self.ext_fp_event_delayed_log_lb_config.consumer,
            "client_tvm_id": "1",
            "fqdn": "localhost",
        }, **kwargs)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)
