import os

import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers
from crypta.profile.runners.segments.lib.coded_segments import compulsory_auto_insurance


def get_outputs(patched_config):
    segment_table = os.path.join(
        patched_config.PROFILES_SEGMENT_PARTS_YT_DIRECTORY,
        patched_config.REGULAR_SEGMENTS,
        compulsory_auto_insurance.InterestedInCompulsoryAutoInsurance.__name__,
    )
    long_term_data_table = os.path.join(patched_config.LONG_TERM_DATA_FOLDER, 'car_insurance')
    return [
        (tables.YsonTable(
            '{}.yson'.format(os.path.basename(segment_table)),
            segment_table,
            yson_format='pretty',
        ), tests.Diff()),
        (tables.YsonTable(
            '{}.yson'.format(os.path.basename(long_term_data_table)),
            long_term_data_table,
            yson_format='pretty',
        ), tests.Diff()),
    ]


def get_inputs(patched_config, date):
    return [
        (tables.YsonTable(
            'car_insurance.yson',
            os.path.join(patched_config.LONG_TERM_DATA_FOLDER, 'car_insurance'),
            on_write=tables.OnWrite(attributes={
                'last_processed_date': date_helpers.get_yesterday(date),
                'schema': schema_utils.yt_schema_from_dict({
                    'id': 'string',
                    'id_type': 'string',
                    'date': 'string',
                    'category': 'uint64',
                }),
            }),
        ), tests.Exists()),
        (tables.get_yson_table_with_schema(
            'parsed_logs_bb.yson',
            os.path.join(patched_config.BB_PARSED_DIR, date),
            schema_utils.yt_schema_from_dict({
                'yandexuid': 'uint64',
                'category': 'uint64',
                'timestamp': 'uint64',
            })
        ), tests.TableIsNotChanged()),
    ]


def test_get_export_tables(local_yt, patched_config, date):

    def run_task():
        task = compulsory_auto_insurance.InterestedInCompulsoryAutoInsurance(date=date)
        task.run()

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=run_task,
        data_path=yatest.common.test_source_path('data'),
        input_tables=get_inputs(patched_config, date),
        output_tables=get_outputs(patched_config),
    )
