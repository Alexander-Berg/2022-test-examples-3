import os

import pytest
import yaml
import yatest.common

from crypta.lib.python import templater


@pytest.mark.parametrize("environment", [
    "testing",
    "stable",
])
def test_index_and_config(environment):
    index_template_path = yatest.common.source_path("crypta/cm/offline/config/index.yaml")
    config_template_path = yatest.common.source_path("crypta/cm/offline/config/config.yaml")

    index_path = yatest.common.test_output_path("index.yaml")
    templater.render_file(index_template_path, index_path, vars=dict(environment=environment), strict=True)

    with open(index_path) as f:
        index = yaml.safe_load(f)

    parse_cm_access_log_config_path = yatest.common.test_output_path("parse_cm_access_log_config.yaml")
    templater.render_file(config_template_path, parse_cm_access_log_config_path, vars=dict(environment=environment, task="CryptaOfflineCmParseCmAccessLogTask"), strict=True)

    split_by_tag_config_path = yatest.common.test_output_path("split_by_tag_config.yaml")
    templater.render_file(config_template_path, split_by_tag_config_path, vars=dict(environment=environment, task="CryptaOfflineCmSplitByTagTask", tags=[item["tag"] for item in index]), strict=True)

    canonical_files = [
        index_path,
        parse_cm_access_log_config_path,
        split_by_tag_config_path,
    ]

    for item in index:
        tag = item["tag"]
        ttl = item["ttl"]

        update_state_config_path = yatest.common.test_output_path("{}_update_state_config.yaml".format(tag))
        templater.render_file(config_template_path, update_state_config_path, vars=dict(environment=environment, task="CryptaOfflineCmUpdateStateTask", tag=tag, ttl=ttl), strict=True)
        canonical_files.append(update_state_config_path)

    return {
        os.path.basename(path): yatest.common.canonical_file(path, local=True)
        for path in canonical_files
    }
