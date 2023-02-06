import os

import yatest

from crypta.lib.python import templater


def get_config_path(ydb_endpoint, ydb_database, siberia_host, siberia_port, tvm_src_id, tvm_dst_id, tvm_api_port):
    working_dir = yatest.common.test_output_path("expirator")

    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, "config.yaml")
    context = {
        "environment": "qa",
        "siberia_host": siberia_host,
        "siberia_port": siberia_port,
        "tvm_src_id": tvm_src_id,
        "tvm_dst_id": tvm_dst_id,
        "tvm_test_port": tvm_api_port,
        "ydb_endpoint": ydb_endpoint,
        "ydb_database": ydb_database,
    }
    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/expirator/bundle/config.yaml"),
        config_path,
        context,
        strict=True,
    )
    return config_path
