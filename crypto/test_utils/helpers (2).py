import yatest.common

from crypta.lib.python import templater


def render_config_file(template_file, local_yt, mock_sandbox_server_with_identifiers_udf):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path(template_file),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "crypta_identifier_udf_url": mock_sandbox_server_with_identifiers_udf.get_udf_url(),
            "matching_ids": ["puid"],
        },
    )
    return config_file_path
