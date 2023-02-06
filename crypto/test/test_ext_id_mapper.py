import pytest

from crypta.cm.services.common.ext_id_mapper.lib import ext_id_mapper


HANDLE = "expire"
SUBCLIENT = "expirator"


def get_mapper():
    return ext_id_mapper.ExtIdMapper(
        max_retries=0,
        tvm_src_id="1",
        tvm_dst_id="2",
        hosts=["example.com"],
        url=ext_id_mapper.get_url_template(HANDLE, SUBCLIENT),
        timeout=1,
        threads=1,
        rps_limit=1,
        max_fails=0,
        failed_request_queue_size=0,
    )


def get_query_string(mapper, type_, value):
    return mapper.get_request_wo_headers([
        {
            ext_id_mapper.TYPE: type_,
            ext_id_mapper.VALUE: value,
        },
    ]).next()[1]


@pytest.mark.parametrize("type_,value,ref_query_string", [
    ["type", "value", "/expire?subclient=expirator&type=type&value=value"],
    ["type:", "value:", "/expire?subclient=expirator&type=type%3A&value=value%3A"],
    ["type%", "value%", "/expire?subclient=expirator&type=type%25&value=value%25"],
])
def test_get_request_wo_headers(type_, value, ref_query_string):
    mapper = get_mapper()
    assert ref_query_string == get_query_string(mapper, type_, value)
