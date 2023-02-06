import contextlib
import os

import pytest
import yaml
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.s2s.lib.proto.config_pb2 import TConfig


@pytest.mark.parametrize("environment", [
    "stable"
])
def test_index_and_config(environment):
    os.environ["POSTBACK_PASS_PHRASE"] = "__POSTBACK_PASS_PHRASE__"

    index_path = yatest.common.test_output_path("index.yaml")
    templater.render_file(yatest.common.source_path("crypta/s2s/config/index.yaml"), index_path, vars={"environment": environment}, strict=True)

    calc_stats_config_path = yatest.common.test_output_path("calc_stats_config.yaml")
    templater.render_file(yatest.common.source_path("crypta/s2s/config/calc_stats_config.yaml"), calc_stats_config_path, vars={"environment": environment}, strict=True)

    canonical_files = {
        os.path.basename(path): yatest.common.canonical_file(path, local=True)
        for path in [index_path, calc_stats_config_path]
    }

    config_template_path = yatest.common.source_path("crypta/s2s/config/config.yaml")

    with open(index_path) as f:
        index = yaml.safe_load(f)

    for context in index:
        context["environment"] = environment

        config_path = yatest.common.test_output_path("{}_config.yaml".format(context["client"]))
        templater.render_file(config_template_path, config_path, vars=context, strict=True)

        with managed_secrets(context["provider"]["type"]):
            yaml_config.parse_config(TConfig, config_path)

        canonical_files[os.path.basename(config_path)] = yatest.common.canonical_file(config_path, local=True)

    return canonical_files


@contextlib.contextmanager
def managed_secrets(provider_type):
    provider_type_to_secrets = {
        "google_sheets_provider": {"GOOGLE_SERVICE_ACCOUNT_KEY": "XXX"},
        "sftp_provider": {"CRYPTA_SFTP_PASSWORD": "XXX"},
    }
    secrets = provider_type_to_secrets.get(provider_type, {})

    for k, v in secrets.items():
        os.putenv(k, v)

    yield secrets

    for k in secrets:
        os.unsetenv(k)
