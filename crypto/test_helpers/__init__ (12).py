import os

import yatest

from crypta.lib.python import templater
from crypta.lib.python.yql import test_helpers
from crypta.siberia.bin.user_data_uploader.lib import config_fields


def get_config_path(
    yt_proxy,
    user_data_yt_table,
    custom_user_data_yt_dir,
    ydb_endpoint,
    ydb_database,
    crypta_sampler_udf_url,
    denominator,
    rest,
):
    working_dir = yatest.common.test_output_path("user_data_uploader")

    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, "config.yaml")
    context = {
        "environment": "qa",
        "yt_tmp_dir": "//tmp",
        "yt_proxy": yt_proxy,
        "user_data_yt_table": user_data_yt_table,
        "custom_user_data_yt_dir": custom_user_data_yt_dir,
        "ydb_endpoint": ydb_endpoint,
        "ydb_database": ydb_database,
        "crypta_sampler_udf_url": crypta_sampler_udf_url,
        "denominator": denominator,
        "rest": rest,
    }
    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/user_data_uploader/bundle/config.yaml"),
        config_path,
        context,
        strict=True
    )
    return config_path


def add_ydb_token_to_yql(config, ydb_token):
    test_helpers.add_ydb_token_to_yql(config[config_fields.YDB_TOKEN_NAME], ydb_token)
