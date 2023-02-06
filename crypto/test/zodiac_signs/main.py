import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def get_passport_puids_schema():
    return schema_utils.get_strict_schema([
        {"name": "id", "type": "string", "required": False},
        {"name": "birth_date", "type": "string", "required": False},
    ])


def test_zodiac_signs(local_yt, local_yt_and_yql_env, config_file, config):
    diff = tests.Diff()
    test = tests.TestNodesInMapNode(tests_getter=[diff], tag="zodiac_signs")

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/profile/services/precalculate_tables/bin/crypta-profile-precalculate-tables"),
        args=[
            "--config", config_file,
            "zodiac_signs",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("passport_puids.yson", config.PassportPuidsTable, get_passport_puids_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (cypress.CypressNode(config.ZodiacSignsDir), [test])
        ],
        env=local_yt_and_yql_env,
    )
