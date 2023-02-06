import datetime
import os

import pytest

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.test_helpers import task_helpers as task_test_helpers
from crypta.profile.runners.export_profiles.lib.affinity import host_affinity
from crypta.profile.utils.config import config


INPUT_TABLE_TYPES = ['flattened_hits_bar', 'flattened_hits_metrics', 'idf_bar', 'idf_metrics', 'hosts_counter']


def get_date_before(date):
    date_format = '%Y-%m-%d'
    return (datetime.datetime.strptime(date, date_format) - datetime.timedelta(days=1)).strftime(date_format)


def get_flattened_hits_table(task, id_type, source_type, generate_date):
    schema = schema_utils.yt_schema_from_dict({
        id_type: 'uint64',
        'host': 'string',
    })
    attributes = {
        'schema': schema,
        'generate_date': generate_date,
    }

    table = tables.YsonTable(
        '{}_{}_flattened_hits.yson'.format(source_type, id_type),
        task.input()['flattened_{}_hits'.format(source_type)].table,
        on_write=tables.OnWrite(attributes=attributes),
    )
    return table, tests.TableIsNotChanged()


def get_idf_table(task, source_type, generate_date):
    schema = schema_utils.yt_schema_from_dict({
        'host': 'string',
        'count': 'uint64',
        'weight': 'uint64',
    })
    attributes = {
        'schema': schema,
        'generate_date': generate_date,
    }

    table = tables.YsonTable(
        '{}_IDF.yson'.format(source_type),
        task.input()['{}_idf'.format(source_type)].table,
        on_write=tables.OnWrite(attributes=attributes)
    )
    return table, tests.TableIsNotChanged()


def get_hosts_counter_table(task, generate_date):
    schema = schema_utils.yt_schema_from_dict({
        'bar_visitors_count': 'uint64',
        'metrica_visitors_count': 'uint64',
        'site': 'string',
    })
    attributes = {
        'schema': schema,
        'generate_date': generate_date,
    }

    table = tables.YsonTable(
        'visitors_counter.yson',
        task.input()['hosts_counter'].table,
        on_write=tables.OnWrite(attributes=attributes)
    )
    return table, tests.TableIsNotChanged()


def get_inputs(task, id_type, dates):
    return [
        get_flattened_hits_table(task, id_type, source_type='bar', generate_date=dates['flattened_hits_bar']),
        get_flattened_hits_table(task, id_type, source_type='metrics', generate_date=dates['flattened_hits_metrics']),
        get_idf_table(task, source_type='bar', generate_date=dates['idf_bar']),
        get_idf_table(task, source_type='metrics', generate_date=dates['idf_metrics']),
        get_hosts_counter_table(task, generate_date=dates['hosts_counter']),
    ]


def get_outputs(task, make_diff_check):
    output_test = tests.Diff() if make_diff_check else []

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


@pytest.mark.parametrize('id_type', [config.CRYPTA_ID, config.YANDEXUID])
@pytest.mark.parametrize('outdated_table', INPUT_TABLE_TYPES + [pytest.param(None, id='up_to_date')])
def test_host_affinity(local_yt, patched_config, date, id_type, outdated_table):
    task = host_affinity.PrecalculateHostsAffinity(date=date, id_type=id_type)

    dates = {
        table_type: get_date_before(date) if table_type == outdated_table else date
        for table_type in INPUT_TABLE_TYPES
    }

    return task_test_helpers.run_and_test_task(
        task=task,
        yt=local_yt,
        input_tables=get_inputs(task, id_type, dates),
        output_tables=get_outputs(task, make_diff_check=outdated_table is None),
        dependencies_are_missing=outdated_table is not None,
    )
