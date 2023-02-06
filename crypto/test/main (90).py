import pytest
import yatest
from yt import yson

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.siberia.bin.common.data.proto.user_set_pb2 import TUserSet


NOT_READY_USER_SET = TUserSet(Status="not_ready", Id="1", ExpirationTime=111111111, Title="Title", Type="type")
READY_USER_SET = TUserSet(Status="ready", Id="1", ExpirationTime=111111111, Title="Title", Type="type")


def get_src_table_schema():
    schema = [
        {"name": "user_yandexuid", "type": "uint64", "required": True},
        {"name": "list_field", "type": "any"},
        {"name": "test_response_code", "type": "uint64"},
    ]
    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = False
    return schema


@pytest.mark.parametrize("execution_error,src_table,user_set", [
    pytest.param(False, "no_errors.yson", NOT_READY_USER_SET, id="no_errors"),
    pytest.param(False, "no_errors.yson", READY_USER_SET, id="user_set_is_ready"),
    pytest.param(True, "with_errors_400.yson", NOT_READY_USER_SET, id="with_errors_400"),
    pytest.param(False, "with_errors_404.yson", NOT_READY_USER_SET, id="with_errors_404"),
    pytest.param(True, "with_errors_500.yson", NOT_READY_USER_SET, id="with_errors_500"),
])
def test_siberia_users_uploader(yt_stuff, execution_error, src_table, mock_siberia_core_server, source_table_path, config_path, tvm_api, tvm_src_id):
    tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/users_uploader/bin/crypta-siberia-upload-users"),
        args=[
            "--config", config_path
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(src_table, source_table_path, get_src_table_schema()), tests.Exists()),
        ],
        env={
            "TVM_SECRET": tvm_api.get_secret(tvm_src_id),
        },
        must_be_execution_error=execution_error
    )

    def key(x):
        method = x["method"]
        yandexuid = x["body"]["Users"][0]["Attributes"]["@yandexuid"]["Values"] if method == "/users/add" else None
        return (method, yandexuid)

    return sorted(mock_siberia_core_server.commands, key=key)
