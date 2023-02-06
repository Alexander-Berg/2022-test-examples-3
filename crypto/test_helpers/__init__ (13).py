import os

import yatest

from crypta.lib.python import templater


def get_config_path(yt_proxy, source_table_path, siberia_host, siberia_port, user_set_id, tvm_src_id, tvm_dst_id, fields_id_types):
    working_dir = yatest.common.test_output_path("users_uploader")

    if not os.path.isdir(working_dir):
        os.makedirs(working_dir)

    config_path = os.path.join(working_dir, "config.yaml")
    context = {
        "yt_proxy": yt_proxy,
        "environment": "qa",
        "src_table": source_table_path,
        "yt_tmp_dir": "//tmp",
        "siberia_host": siberia_host,
        "siberia_port": siberia_port,
        "max_rps_per_job": 50,
        "max_rps": 500,
        "tvm_src_id": tvm_src_id,
        "tvm_dst_id": tvm_dst_id,
        "login": "test_login",
        "user_set_id": user_set_id,
        "fields_id_types": fields_id_types,
        "max_concurrent_jobs": 1,
        "batch_size": 2,
        "max_failed_jobs_count": 1,
    }
    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/users_uploader/bundle/config.yaml"),
        config_path,
        context,
        strict=True,
    )
    return config_path
