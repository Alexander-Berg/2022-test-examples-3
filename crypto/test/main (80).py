import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.lib.proto.user_data import (
    token_dict_item_pb2,
    user_data_pb2,
)
from crypta.lookalike.lib.python import test_utils
from crypta.siberia.bin.common import yt_schemas
from crypta.siberia.bin.custom_audience.ca_builder.proto import (
    build_ca_job_config_pb2,
    ca_binding_pb2,
)


FROZEN_TIME = "1500000000"


def get_input_user_data_encoded_table(file_name, path):
    on_write = tables.OnWrite(
        row_transformer=row_transformers.proto_dict_to_yson(user_data_pb2.TUserData),
        attributes={
            "schema": yt_schemas.get_user_data_schema(),
        }
    )

    return tables.YsonTable(file_name, path, on_write=on_write)


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/siberia/bin/custom_audience/ca_builder/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "threshold": 2,
        },
    )

    return config_file_path


def test_basic(yt_stuff, config_file):
    config = yaml_config.parse_config(build_ca_job_config_pb2.TBuildCaJobConfig, config_file)

    diff_test = tests.Diff()

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/custom_audience/ca_builder/bin/crypta-siberia-custom-audience-builder"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTableWithExpressions("lal_state.yson", config.LalStateTable, on_write=test_utils.lals_on_write()), tests.TableIsNotChanged()),
            (get_input_user_data_encoded_table("user_data_encoded.yson", config.UserDataTable), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema("host_dict.yson", config.HostDictTable, schema_utils.get_schema_from_proto(token_dict_item_pb2.TTokenDictItem)), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema("word_dict.yson", config.WordDictTable, schema_utils.get_schema_from_proto(token_dict_item_pb2.TTokenDictItem)), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema("app_dict.yson", config.AppDictTable, schema_utils.get_schema_from_proto(token_dict_item_pb2.TTokenDictItem)), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable("ca_bindings", config.CaBindingsTable, yson_format="pretty"),
             [diff_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(ca_binding_pb2.TCaBinding, ["ca_parent_id"]))]),
        ],
        env={
            "YT_TOKEN": "FAKE",
            time_utils.CRYPTA_FROZEN_TIME_ENV: FROZEN_TIME,
        },
    )
