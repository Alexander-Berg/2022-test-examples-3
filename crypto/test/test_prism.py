import os
import pytest

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

from crypta.profile.lib.test_helpers import task_helpers as task_test_helpers
from crypta.profile.runners.segments.lib.coded_segments import prism

from yt import yson


def get_inputs(task, attributes=None):
    if attributes is None:
        attributes = {}

    schema = yson.YsonList(
        [
            {'name': 'cluster', 'type': 'string'},
            {'name': 'yandexuid', 'type': 'string'},
        ],
    )
    attributes['schema'] = schema

    return [
        (
            tables.YsonTable(
                'prism_user_weights.yson',
                task.input().table,
                on_write=tables.OnWrite(attributes=attributes),
            ),
            tests.TableIsNotChanged(),
        ),
    ]


def get_outputs(task, diff_check=True):
    output_test = tests.Diff() if diff_check else []

    table = task.output().table
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
    'attributes, correct_attribute',
    [
        ({}, False),
        ({'stage': 'pre'}, False),
        ({'stage': 'full'}, True),
    ],
    ids=['missing_attribute', 'wrong_attribute', 'correct_attribute'],
)
def test_prism(local_yt, patched_config, date, attributes, correct_attribute):
    task = prism.Prism(date=date)

    return task_test_helpers.run_and_test_task(
        task=task,
        yt=local_yt,
        input_tables=get_inputs(task, attributes),
        output_tables=get_outputs(task, diff_check=correct_attribute),
        dependencies_are_missing=not correct_attribute,
    )
