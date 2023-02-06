import copy

import yatest.common

from crypta.lib.python import time_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.siberia.bin.common import yt_schemas
from crypta.siberia.bin.make_id_to_crypta_id.lib import test_helpers
from crypta.siberia.bin.make_id_to_crypta_id.lib.maker import schema


def test_basic(local_yt, local_yt_and_yql_env, make_id_to_crypta_id_config, make_id_to_crypta_id_config_path):
    env = copy.deepcopy(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1579478400"

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/make_id_to_crypta_id/bin/crypta-siberia-make-id-to-crypta-id"),
        args=["--config", make_id_to_crypta_id_config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(
            tables.get_yson_table_with_schema(
                "direct_client_id_to_puid__yt.yson",
                make_id_to_crypta_id_config.DirectClientIdToPuidTable,
                test_helpers.get_direct_client_id_to_puid_table_schema(),
            ),
            tests.TableIsNotChanged(),
        ), (
            tables.get_yson_table_with_schema(
                "puid_to_crypta_id__yt.yson",
                make_id_to_crypta_id_config.PuidToCryptaIdTable,
                test_helpers.get_matching_table_schema(),
            ),
            tests.TableIsNotChanged(),
        ), (
            tables.get_yson_table_with_schema(
                "crypta_id_user_data__yt.yson",
                make_id_to_crypta_id_config.CryptaIdUserDataTable,
                yt_schemas.get_crypta_id_user_data_schema(),
            ),
            tests.TableIsNotChanged(),
        )],
        output_tables=[
            (tables.YsonTable("output.yson", make_id_to_crypta_id_config.OutputTable), [tests.Diff(), tests.SchemaEquals(schema.get_id_to_crypta_id_schema())]),
        ],
        env=env,
    )
