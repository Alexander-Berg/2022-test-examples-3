import os

import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.s2s.lib.proto.config_pb2 import TConfig


def render_config_file(
    yt_proxy,
    provider=None,
    state_ttl_days=1,
    postback_url="",
    postback_retries=0,
    conversion_name_to_goal_ids=None,
    static_goal_id=None,
    column_names=None
):
    config_file_path = yatest.common.test_output_path("config.yaml")

    context = {
        "environment": "qa",
        "client": "aliexpress",
        "yt_proxy": yt_proxy,
        "provider": provider or {"type": "empty_provider"},
        "state_ttl_days": state_ttl_days,
        "postback_url": postback_url,
        "max_retries": postback_retries,
        "column_names": column_names or {},
    }

    if static_goal_id is not None:
        context["static_goal_id"] = static_goal_id
    else:
        context["conversion_name_to_goal_ids"] = conversion_name_to_goal_ids or {}

    templater.render_file(
        yatest.common.source_path("crypta/s2s/config/config.yaml"),
        config_file_path,
        context,
        strict=True,
    )

    return config_file_path


def read_config(config_file):
    os.environ["POSTBACK_PASS_PHRASE"] = "__POSTBACK_PASS_PHRASE__"
    return yaml_config.parse_config(TConfig, config_file)
