import yatest.common
from yt import yson

from crypta.cm.services.common.test_utils import yson_transformers
from crypta.lib.python import time_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


FROZEN_TIME = str(1600000000 + 30 * 86400)


def get_input_schema():
    schema = yson.YsonList([
        dict(name="key", type="string", required=True),
        dict(name="value", type="string", required=False),
    ])
    schema.attributes["strict"] = True
    return schema


def test_db_stats(local_yt, local_yt_and_yql_env, mock_solomon_server, config_path, db_path):
    env = dict(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = FROZEN_TIME

    tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/services/db_stats/bin/crypta-cm-db-stats"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("input.yson", db_path, on_write=yson_transformers.cm_db_on_write()), tests.TableIsNotChanged()),
        ],
        output_tables=[],
        env=env,
    )
    return mock_solomon_server.dump_push_requests()
