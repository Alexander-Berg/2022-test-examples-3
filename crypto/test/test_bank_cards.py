import datetime
import os
import pytest

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.test_helpers import task_helpers as task_test_helpers
from crypta.profile.runners.segments.lib.coded_segments import bank_cards


def get_day_before(date):
    date_format = '%Y-%m-%d'
    return (datetime.datetime.strptime(date, date_format) - datetime.timedelta(days=1)).strftime(date_format)


def get_inputs(task, generate_date):
    return [
        (
            tables.YsonTable(
                'trust_bindings.yson',
                task.input().table,
                on_write=tables.OnWrite(attributes={'generate_date': generate_date}),
            ),
            tests.TableIsNotChanged()
        ),
    ]


def get_outputs(task, make_diff_check):
    table = task.output().table
    output_test = tests.Diff() if make_diff_check else []

    return [
        (
            tables.YsonTable(
                '{}.yson'.format(os.path.basename(table)),
                table,
                yson_format='pretty',
            ),
            output_test,
        ),
    ]


@pytest.mark.parametrize(
    'correct_date',
    [False, True],
    ids=['wrong_date', 'correct_date'],
)
def test_get_export_tables(local_yt, patched_config, date, correct_date):
    task = bank_cards.BankCards(date=date)
    generate_date = date if correct_date else get_day_before(date)

    return task_test_helpers.run_and_test_task(
        task=task,
        yt=local_yt,
        input_tables=get_inputs(task, generate_date),
        output_tables=get_outputs(task, make_diff_check=correct_date),
        dependencies_are_missing=not correct_date,
    )
