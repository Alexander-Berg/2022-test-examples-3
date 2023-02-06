import pytest
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.siberia.bin.common import yt_schemas
from crypta.siberia.bin.common.proto.crypta_id_user_data_pb2 import TCryptaIdUserData
from crypta.siberia.bin.make_crypta_id_user_data.lib import test_helpers


YANDEXUID_USER_DATA_TABLE = "//yandexuid_user_data"
YANDEXUID_TO_CRYPTA_ID_TABLE = "//yandexuid_to_crypta_id"
DST_TABLE = "//crypta_id_user_data"


@pytest.fixture(scope="function")
def config_path(yt_stuff):
    return test_helpers.get_config_path(
        yt_proxy=yt_stuff.get_server(),
        yandexuid_user_data_table_path=YANDEXUID_USER_DATA_TABLE,
        yandexuid_to_crypta_id_table_path=YANDEXUID_TO_CRYPTA_ID_TABLE,
        crypta_id_user_data_table_path=DST_TABLE,
    )


def test_basic(yt_stuff, config_path):
    output_row_transformer = row_transformers.yson_to_proto_dict(TCryptaIdUserData)
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/make_crypta_id_user_data/bin/crypta-make-crypta-id-user-data"),
        args=[
            "--config", config_path,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("yandexuid_user_data.yson", YANDEXUID_USER_DATA_TABLE, test_helpers.get_yandexuid_user_data_schema()), tests.TableIsNotChanged()),
            (
                tables.get_yson_table_with_schema("yandexuid_to_crypta_id.yson", YANDEXUID_TO_CRYPTA_ID_TABLE, test_helpers.get_yandexuid_to_crypta_id_schema()),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable("crypta_id_user_data.yson", DST_TABLE, yson_format="pretty", on_read=tables.OnRead(row_transformer=output_row_transformer)),
                [tests.Diff(), tests.SchemaEquals(yt_schemas.get_crypta_id_user_data_schema())],
            ),
        ],
        env={"LOCAL_YT_SERVER": yt_stuff.get_server()},
    )
