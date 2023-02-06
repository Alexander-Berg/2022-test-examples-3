import os

import pytest
import yaml
import yatest.common

from crypta.lib.python import templater


@pytest.mark.parametrize("environment", [
    "testing",
    "stable"
])
def test_index_and_config(environment):
    index_publisher_path = yatest.common.test_output_path("index_publisher.yaml")
    templater.render_file(yatest.common.source_path("crypta/dmp/yandex/config/index_publisher.yaml"), index_publisher_path, vars={"environment": environment}, strict=True)

    mac_hash_yuid_maker_path = yatest.common.test_output_path("mac_hash_yuid_maker.yaml")
    templater.render_file(yatest.common.source_path("crypta/dmp/yandex/config/mac_hash_yuid_maker.yaml"), mac_hash_yuid_maker_path, vars={"environment": environment}, strict=True)

    index_path = yatest.common.test_output_path("index.yaml")
    templater.render_file(yatest.common.source_path("crypta/dmp/yandex/config/index.yaml"), index_path, vars={"environment": environment}, strict=True)

    canonical_files = {
        os.path.basename(path): yatest.common.canonical_file(path, local=True)
        for path in (index_publisher_path, mac_hash_yuid_maker_path, index_path)
    }

    config_template_path = yatest.common.source_path("crypta/dmp/yandex/config/config.yaml")

    with open(index_path) as f:
        index = yaml.safe_load(f)

    for context in index:
        if context.get("internal"):
            continue

        context["errors_emails"] = yaml.dump(context["errors_emails"], default_flow_style=None).strip("\n")
        context["stats_emails"] = yaml.dump(context["stats_emails"], default_flow_style=None).strip("\n")
        context["environment"] = environment

        config_path = yatest.common.test_output_path("{}_config.yaml".format(context["dmp_login"]))
        templater.render_file(config_template_path, config_path, vars=context, strict=True)

        with open(config_path) as f:
            yaml.safe_load(f)

        canonical_files[os.path.basename(config_path)] = yatest.common.canonical_file(config_path, local=True)

    return canonical_files
