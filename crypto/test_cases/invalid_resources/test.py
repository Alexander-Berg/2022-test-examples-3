import yatest.common

from crypta.graph.rtmr.test import common
from crypta.lib.python.rtmr.test_framework import runner

FILE_ROOT = yatest.common.source_path("crypta/graph/rtmr/test/data/resource_service/invalid_resources/resources")


def test_graph(tmpdir, resource_service):
    manifest = common.create_manifest(resource_service.url_prefix)

    return runner.run(tmpdir, common.INPUT_FORMATTERS, common.OUTPUT_FORMATTERS, common.get_input_dir(), common.TASK, manifest, no_strict_check_output=True)
