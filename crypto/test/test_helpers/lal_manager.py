import os
import time

import yatest.common

from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)


class LalManager(test_utils.TestBinaryContextManager):
    bin_path = "crypta/lookalike/services/lal_manager/bin/crypta-lookalike-lal-manager"
    app_config_template_path = "crypta/lookalike/services/lal_manager/bundle/templates/config.yaml"

    def __init__(
        self,
        working_dir,
        logbroker_config,
        frozen_time,
        tvm_api,
        self_tvm_id,
        siberia_tvm_id,
        local_siberia,
        local_ca,
        yt_stuff,
        yt_working_dir,
        audience_segments_table_path,
        cdp_segments_table_path,
        describing_mode,
        template_args=None
    ):
        super(LalManager, self).__init__("Lookalike Lal Manager", frozen_time=frozen_time)

        self.logbroker_config = logbroker_config
        self.working_dir = working_dir
        self.tvm_api = tvm_api
        self.yt_working_dir = yt_working_dir
        self.audience_segments_table_path = audience_segments_table_path
        self.cdp_segments_table_path = cdp_segments_table_path
        self.local_siberia = local_siberia
        self.local_ca = local_ca
        self.yt_stuff = yt_stuff
        self.self_tvm_id = self_tvm_id
        self.siberia_tvm_id = siberia_tvm_id
        self.env["TVM_SECRET"] = tvm_api.secrets[str(self_tvm_id)]
        self.env["YT_TOKEN"] = "unused"
        self.describing_mode = describing_mode

        self.template_args = template_args or {}

    def _prepare_start(self):
        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")

        self._render_app_config(**self.template_args)
        self.config = yaml_config.load(self.app_config_path)

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.app_config_path,
        ]

    def _render_app_config(self, **kwargs):
        params = dict({
            "environment": "qa",
            "dc": "qa",
            "logbroker_server": self.logbroker_config.host,
            "logbroker_port": self.logbroker_config.port,
            "topic": self.logbroker_config.topic,
            "client_id": self.logbroker_config.consumer,
            "use_secure_tvm": False,
            "log_dir": self.working_dir,

            "self_tvm_id": self.self_tvm_id,

            "lal_database_cluster_url": self.yt_stuff.get_server(),
            "static_yt_proxy": self.yt_stuff.get_server(),

            "yt_working_dir": self.yt_working_dir,
            "audience_segments_table_path": self.audience_segments_table_path,
            "cdp_segments_table_path": self.cdp_segments_table_path,
            "stats_check_interval_seconds": 1,

            "siberia_host": self.local_siberia.host,
            "siberia_port": self.local_siberia.port,
            "siberia_tvm_id": self.siberia_tvm_id,

            "custom_audience_host": self.local_ca.host,
            "custom_audience_port": self.local_ca.port,

            "describing_mode": self.describing_mode,
        }, **kwargs)
        self.logger.info("App config template parameters = %s", params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, params)

    def _wait_until_up(self):
        time.sleep(5)
