from collections import namedtuple
import mock
import os
import pytest
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.segments.lib.constructor_segments import lal
from crypta.profile.runners.segments.lib.constructor_segments.common import utils
from crypta.profile.utils.config import config


def get_input_attrs():
    return {
        "schema": schema_utils.get_strict_schema([
            {"name": "id", "type": "string", "required": False},
            {"name": "id_type", "type": "string", "required": True},
            {"name": "rule_lab_ids", "type": "any", "required": False},
        ]),
        "_yql_row_spec": {
            'Type': ['StructType', [
                    ['id', ['OptionalType', ['DataType', 'String']]],
                    ['id_type', ['DataType', 'String']],
                    ['rule_lab_ids', ['ListType', ['DataType', 'String']]]
                ]],
            "UniqueKeys": False,
            "StrictSchema": True,
        }
    }


def get_indevice_yandexuid_schema():
    return schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "yandexuid": "uint64",
    })


def get_cryptaid_yandexuid_schema():
    return schema_utils.yt_schema_from_dict({
        "crypta_id": "uint64",
        "yandexuid": "uint64",
    })


def get_lal_config():
    return [
        {
            'rule_id': "rule-1",
            'include_input': True,
            'segment_type': 'audience_segments',
            'segment_id': 1234567,
            'exponent': 2,
        },
        {
            'rule_id': "rule-2",
            'include_input': False,
            'segment_type': 'audience_segments',
            'segment_id': 19876544346,
        },
        {
            'rule_id': "rule-3",
            'include_input': False,
            'segment_type': 'lal_internal',
            'segment_id': 765,
            'max_coverage': 5000000,
        }
    ]


@pytest.fixture
def task(date):
    return lal.GetLalSegments(date=date)


def test_lal(clean_local_yt, patched_config, task):
    input_table = namedtuple('input_table', ['table'])
    with mock.patch('crypta.profile.runners.segments.lib.constructor_segments.lal.GetLalSegments.input') as mock_get_input:
        mock_get_input.return_value = input_table(table=config.STANDARD_HEURISTIC_RESULT_TABLE)

        return tests.yt_test_func(
            yt_client=clean_local_yt.get_yt_client(),
            func=task.run,
            data_path=yatest.common.test_source_path('data/test_lal'),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'indevice_yandexuid.yson',
                        config.INDEVICE_YANDEXUID,
                        get_indevice_yandexuid_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'cryptaid_yandexuid.yson',
                        config.CRYPTAID_YANDEXUID_TABLE,
                        get_cryptaid_yandexuid_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'mobile_apps.yson',
                        os.path.join(config.AGGREGATED_STANDARD_HEURISTIC_DIRECTORY, 'GetStandardSegmentsByMobileApp'),
                        on_write=tables.OnWrite(attributes=get_input_attrs()),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'precalculated_tables.yson',
                        os.path.join(config.AGGREGATED_STANDARD_HEURISTIC_DIRECTORY, 'GetStandardSegmentsByPrecalculatedTables'),
                        on_write=tables.OnWrite(attributes=get_input_attrs()),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'yandexuids.yson',
                        os.path.join(config.AGGREGATED_STANDARD_HEURISTIC_DIRECTORY, 'DailyLogsAggregator'),
                        on_write=tables.OnWrite(attributes=get_input_attrs()),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'music_likes.yson',
                        os.path.join(config.AGGREGATED_STANDARD_HEURISTIC_DIRECTORY, 'GetStandardSegmentsByMusicLikes'),
                        on_write=tables.OnWrite(attributes=get_input_attrs()),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'standard_heuristic.yson',
                        task.input().table,
                        on_write=tables.OnWrite(attributes={
                            "schema": schema_utils.yt_schema_from_dict(utils.profile_schema),
                            'lal_config': get_lal_config(),
                        }),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (tables.YsonTable(
                    'rule_1.yson',
                    os.path.join(config.REGULAR_LAL_INPUT_DIRECTORY, 'audience_segments', '1234567'),
                    yson_format='pretty',
                ), (tests.Diff())),
                (tables.YsonTable(
                    'rule_3.yson',
                    os.path.join(config.REGULAR_LAL_INPUT_DIRECTORY, 'lal_internal', '765'),
                    yson_format='pretty',
                ), (tests.Diff())),
            ],
        )
