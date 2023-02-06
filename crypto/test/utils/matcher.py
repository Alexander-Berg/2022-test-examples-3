import os

import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
)


class Matcher(test_utils.TestBinaryContextManager):
    bin_path = "crypta/ext_fp/matcher/bin/crypta-ext-fp-matcher"
    app_config_template_path = "crypta/ext_fp/matcher/bundle/templates/config.yaml"

    def __init__(self, working_dir, fp_event_log_lb_config, ext_fp_match_log_lb_config, beeline_api_url, ertelecom_api_url, intentai_api_url, mts_api_url, rostelecom_api_url):
        super(Matcher, self).__init__("Ext FP Matcher")

        self.working_dir = working_dir
        self.fp_event_log_lb_config = fp_event_log_lb_config
        self.ext_fp_match_log_lb_config = ext_fp_match_log_lb_config
        self.beeline_api_url = beeline_api_url
        self.ertelecom_api_url = ertelecom_api_url
        self.intentai_api_url = intentai_api_url
        self.mts_api_url = mts_api_url
        self.rostelecom_api_url = rostelecom_api_url

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
            "input_log_port": self.fp_event_log_lb_config.port,
            "fp_event_log_topic": self.fp_event_log_lb_config.topic,
            "beeline_api_url": self.beeline_api_url,
            "beeline_api_use_authentication": False,
            "ertelecom_api_url": self.ertelecom_api_url,
            "ertelecom_api_use_authentication": False,
            "ext_fp_match_log_server": "localhost",
            "ext_fp_match_log_topic": self.ext_fp_match_log_lb_config.topic,
            "ext_fp_match_log_port": self.ext_fp_match_log_lb_config.port,
            "ext_fp_max_events_per_batch": 1,
            "intentai_api_url": self.intentai_api_url,
            "intentai_api_use_authentication": False,
            "mts_api_url": self.mts_api_url,
            "mts_use_authentication": False,
            "rostelecom_api_url": self.rostelecom_api_url,
            "prefiltered_source_ids": [
                "beeline",
                "ertelecom",
                "intentai",
                "mts",
                "rostelecom",
            ],
            "log_dir": self.working_dir,
            "log_level": "trace",
            "use_secure_tvm": False,
            "client_id": self.fp_event_log_lb_config.consumer,
            "client_tvm_id": "1",
            "fqdn": "localhost",
        }, **kwargs)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)
