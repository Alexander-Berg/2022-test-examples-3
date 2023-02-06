import pytest
import yatest
import yatest.common.network

from crypta.cm.services.common.test_utils.rt_cm_duid_uploader import RtCmDuidUploader

pytest_plugins = [
    "crypta.cm.services.common.test_utils.fixtures",
    "crypta.lib.python.logbroker.test_helpers.fixtures",
]


@pytest.fixture
def rt_cm_duid_uploader(cm_client, logbroker_config, tvm_ids, tvm_api):
    url = cm_client.host.replace("http", "post")
    with RtCmDuidUploader(
        url=url + "/upload?subclient=qa",
        src_tvm_id=tvm_ids.upload_only,
        dst_tvm_id=tvm_ids.api,
        tvm_api=tvm_api,
        working_dir=yatest.common.test_output_path("rt_cm_duid_uploader"),
        logbroker_config=logbroker_config,
        sample_percent=100,
    ) as mock:
        yield mock
