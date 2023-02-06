import logging
import os

import mock
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
import crypta.lib.python.yql.client as yql_helpers
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.services.features_export.lib import caesar_goal_features
from crypta.siberia.bin.make_id_to_crypta_id.lib.maker.id_to_crypta_id_pb2 import TIdToCryptaId


logger = logging.getLogger(__name__)


def get_goal_audiences_schema():
    return schema_utils.get_strict_schema([
        {"name": "goal_id", "type": "uint64"},
        {"name": "ts", "type": "int64"},
        {"name": "yandexuid", "type": "uint64"},
    ])


def get_goal_features_for_caesar_full_schema():
    return schema_utils.get_strict_schema([
        {"name": "GoalID", "type": "uint64"},
        {"name": "affinitive_sites", "type": "any"},
        {"name": "affinitive_apps", "type": "any"},
        {"name": "affinitive_words", "type": "any"},
        {"name": "last_update_date", "type": "string"},
    ])


def get_caesar_goals_dump_latest_schema():
    return schema_utils.get_strict_schema([
        {"name": "GoalID", "type": "uint64"},
    ])


def test_compute(local_yt, local_yt_and_yql_env, crypta_id_user_data_table):
    date = "2022-01-26"
    os.environ.update(local_yt_and_yql_env)
    yt_client = local_yt.get_yt_client()
    yql_client = yql_helpers.create_yql_client(
        yt_proxy=local_yt.get_server(),
        pool="fake_pool",
        token=os.getenv("YQL_TOKEN"),
    )

    with mock.patch("crypta.lookalike.lib.python.utils.utils.get_yt_client", return_value=yt_client), \
            mock.patch("crypta.lookalike.lib.python.utils.utils.get_yql_client", return_value=yql_client):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: caesar_goal_features.compute(date=date),
            data_path=yatest.common.test_source_path("data/test_caesar_goal_features"),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        "goal_audiences.yson",
                        config.GOALS_TABLE,
                        schema=get_goal_audiences_schema(),
                    ),
                    [tests.TableIsNotChanged()],
                ),
                (
                    tables.get_yson_table_with_schema(
                        "goal_features_for_caesar_full.yson",
                        config.GOAL_FEATURES_FOR_CAESAR_FULL_TABLE,
                        schema=get_goal_features_for_caesar_full_schema(),
                    ),
                    [],
                ),
                (
                    tables.get_yson_table_with_schema(
                        "caesar_goals_dump_latest.yson",
                        config.CAESAR_GOALS_DUMP_LATEST_TABLE,
                        schema=get_caesar_goals_dump_latest_schema(),
                    ),
                    [tests.TableIsNotChanged()],
                ),
                (
                    crypta_id_user_data_table('crypta_id_user_data.yson', config.FOR_DESCRIPTION_BY_CRYPTAID_TABLE),
                    [tests.TableIsNotChanged()],
                ),
                (
                    tables.YsonTable(
                        "id_to_crypta_id.yson",
                        "//home/crypta/production/siberia/id_to_crypta_id",
                        on_write=tables.OnWrite(
                            sort_by=["id", "id_type"],
                            attributes={"schema": schema_utils.get_schema_from_proto(TIdToCryptaId)},
                        ),
                    ),
                    [tests.TableIsNotChanged()],
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        "daily_latest.yson",
                        config.GOAL_FEATURES_FOR_CAESAR_DAILY_LATEST_TABLE,
                        yson_format="pretty",
                    ),
                    [tests.Diff()],
                ),
                (
                    tables.YsonTable(
                        "daily.yson",
                        os.path.join(config.GOAL_FEATURES_FOR_CAESAR_DAILY_DIR, date),
                        yson_format="pretty",
                    ),
                    [tests.Diff()],
                ),
                (
                    tables.YsonTable(
                        "full.yson",
                        config.GOAL_FEATURES_FOR_CAESAR_FULL_TABLE,
                        yson_format="pretty",
                    ),
                    [tests.Diff()],
                ),
            ],
        )
