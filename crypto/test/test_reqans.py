import os.path

import mock
import pytest

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers
from crypta.profile.lib.test_helpers import task_helpers as task_test_helpers
from crypta.profile.runners.interests.lib.log_processors import reqans
from crypta.profile.runners.interests.lib.log_processors.test import schema as schema_builder
from crypta.profile.utils.config import config


lab_to_shortterm = {
    'segment-2324b3a4': 270,  # Жилая недвижимость в новостройках
    'segment-c9dab41b': 271,  # Мобильные телефоны и смартфоны до 15 тыс. руб.
    'segment-890a3c73': 272,  # Мобильные телефоны и смартфоны за 15–25 тыс. руб.
    'segment-a337e706': 273,  # Мобильные телефоны и смартфоны за 25–35 тыс. руб.
    'segment-4061a117': 274,  # Мобильные телефоны и смартфоны за 35–50 тыс. руб.
    'segment-cb71dc34': 275,  # Мобильные телефоны и смартфоны от 50 тыс. руб.
}


def get_inputs(task, input_exists):
    log_schema = schema_builder.get_reqans_log_schema()
    phones_schema = schema_builder.get_phone_lemmas_schema()

    log_table = tables.YsonTable(
        'reqans_log.yson',
        task.input()['log'].table,
        on_write=tables.OnWrite(attributes={'schema': log_schema}),
    )
    phones_table = tables.YsonTable(
        'phone_lemmas.yson',
        task.input()['phone_lemmas'].table,
        on_write=tables.OnWrite(attributes={'schema': phones_schema})
    )

    inputs = []
    if input_exists['phone_lemmas']:
        inputs.append((phones_table, tests.TableIsNotChanged()))
    if input_exists['log']:
        inputs.append((log_table, tests.TableIsNotChanged()))
    return inputs


def get_outputs(task, current_timestamp, output_exists):
    processed_table = tables.YsonTable(
        'processed_table.yson',
        task.processed_table,
        yson_format='pretty',
    )
    bigb_table = tables.YsonTable(
        'bigb_table.yson',
        os.path.join(
            config.INTERESTS_TO_BIGB_FOLDER,
            current_timestamp,
        ),
        yson_format='pretty',
    )
    temp_to_bigb_table = tables.YsonTable(
        '',
        task.to_bigb_raw_table,
    )

    test_class = tests.Diff if output_exists else tests.IsAbsent

    return [
        (processed_table, test_class()),
        (bigb_table, test_class()),
        (temp_to_bigb_table, tests.IsAbsent()),
    ]


@pytest.mark.parametrize('input_exists', [
    pytest.param(dict(log=False, phone_lemmas=True), id='log_table_is_missing'),
    pytest.param(dict(log=True, phone_lemmas=False), id='phone_lemmas_table_is_missing'),
    pytest.param(dict(log=True, phone_lemmas=True), id='all_input_tables_exist'),
])
def test_reqans(clean_local_yt, patched_config, current_timestamp, earliest_log_timestamp, input_exists):
    with mock.patch('crypta.profile.runners.interests.lib.log_processors.reqans.LabSegmentsInfo') as MockLabSegmentsInfo:
        MockLabSegmentsInfo.return_value.lab_segment_id_to_shortterm_interest_id = lab_to_shortterm

        task = reqans.ReqansProcessor(
            date=date_helpers.from_timestamp_to_datetime(earliest_log_timestamp).strftime(date_helpers.DATE_FORMAT),
            log_name='reqans',
            log_path='//home/logfeller/logs/search-proto-reqans-log/30min/{}'.format(
                date_helpers.from_timestamp_to_datetime(earliest_log_timestamp).strftime('%Y-%m-%dT%H:%M:%S'),
            ),
        )

        input_is_full = all(input_exists.values())

        return task_test_helpers.run_and_test_task(
            task=task,
            yt=clean_local_yt,
            input_tables=get_inputs(task, input_exists),
            output_tables=get_outputs(task, current_timestamp, output_exists=input_is_full),
            dependencies_are_missing=not input_is_full,
        )
