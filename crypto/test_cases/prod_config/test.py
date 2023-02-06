import os

import yatest

from crypta.lib.python import time_utils
from crypta.lib.python.rtmr.test_framework import runner
from crypta.graph.rtmr.test import common

FILE_ROOT = yatest.common.source_path("crypta/graph/rtmr/test/data/resource_service/invalid_resources/resources")


def test_prod_config(tmpdir, resource_service):
    prod_config_path = yatest.common.source_path("rtmapreduce/config/user_tasks/rtcrypta_graph.py")
    with open(prod_config_path) as f:
        prod_config = f.read()

    manifest = common.create_manifest(
        resource_service.url_prefix,
        template_path=yatest.common.test_source_path("prod_config.cfg"),
        extra_vars={"prod_config": prod_config},
    )

    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1500000000"

    return runner.run(tmpdir, common.INPUT_FORMATTERS, common.OUTPUT_FORMATTERS, common.get_input_dir(), common.TASK, manifest, no_strict_check_output=True)
