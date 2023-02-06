import yatest.common
import yt.wrapper as yt
import yt.yson as yson

from crypta.dmp.common.data.python import (
    bindings,
    meta,
)
from crypta.dmp.common.upload_to_audience import upload
from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

FROZEN_TIME = "1111111111"


def get_tariff_prices_schema():
    schema = [
        {"name": "dmp_id", "type": "uint64", "required": True, "sort_order": "ascending"},
        {"name": "tariff_id", "type": "uint64", "required": True},
        {"name": "price_rub", "type": "double", "required": True}
    ]
    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    return schema


def test_upload_to_audience(tvm_api, local_yt, local_yt_and_yql_env, mock_audience_server, config):
    env = {
        upload.UPLOAD_AUDIENCE_TVM_SECRET_VAR: tvm_api.get_secret(str(config[config_fields.AUDIENCE_SRC_TVM_ID])),
        upload.SHARE_AUDIENCE_OAUTH_TOKEN_VAR: "FAKE_TOKEN",
        time_utils.CRYPTA_FROZEN_TIME_ENV: FROZEN_TIME,
    }
    env.update(local_yt_and_yql_env)

    tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/upload_to_audience/bin/crypta-dmp-yandex-upload-to-audience"),
        args=["--config", yaml_config.dump(config)],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("yandexuid_bindings", config[config_fields.YANDEXUID_BINDINGS_TABLE], bindings.get_yandexuid_schema()), None),
            (tables.get_yson_table_with_schema("meta", config[config_fields.META_TABLE], meta.get_schema()), None),
            (tables.get_yson_table_with_schema("tariff_prices", config[config_fields.TARIFF_PRICES_TABLE], get_tariff_prices_schema()), None)
        ],
        output_tables=[
            (tables.YsonTable("errors", yt.ypath_join(config[config_fields.UPLOAD_TO_AUDIENCE_ERRORS_DIR], FROZEN_TIME)), tests.RowCount(0))
        ],
        env=env,
    )

    return mock_audience_server.dump_requests()
