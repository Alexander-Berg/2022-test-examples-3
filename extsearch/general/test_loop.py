import common

from mr_utils import TableSpec
from yt.yson import YsonUint64

import pytest
import os
import yatest.common
import yt_utils


def _get_result(yt_stuff, inputs_dir, worker_dir, output_table, input_tables, output_tables):
    cluster = yt_stuff.get_server()

    return yt_utils.yt_test(
        yatest.common.binary_path(common.BINARY_PATH),
        [
            'loop',
            '--cluster', cluster,
            '--inputs', inputs_dir,
            '--worker', worker_dir,
            '--binary', yatest.common.binary_path(common.TEST_TOOL_PATH),
            '--',
            '--cluster', cluster,
            '--output-table', output_table,
        ],
        os.getcwd(),
        input_tables=input_tables,
        output_tables=output_tables,
        yt_stuff=yt_stuff,
    )


def test_simple(yt_stuff):
    inputs_dir = '//inputs_simple'
    worker_dir = '//worker_simple'
    output_table = '//output_simple'

    input_tables = [
        TableSpec(
            file_path='table1',
            table_name='{}/table1'.format(inputs_dir),
            mapreduce_io_flags=['-format', 'json'],
            sort_on_load=False,
            sort_on_read=False,
            attrs_on_load={
                common.RANGES_ATTR: [
                    {'row_offset': YsonUint64(0),   'row_limit': YsonUint64(100), 'status': 'new'},
                    {'row_offset': YsonUint64(100), 'row_limit': YsonUint64(100), 'status': 'new'},
                    {'row_offset': YsonUint64(200), 'row_limit': YsonUint64(100), 'status': 'new'},
                    {'row_offset': YsonUint64(300), 'row_limit': YsonUint64(100), 'status': 'new'},
                ],
            },
        ),
        TableSpec(
            file_path='table2',
            table_name='{}/table2'.format(inputs_dir),
            mapreduce_io_flags=['-format', 'json'],
            sort_on_load=False,
            sort_on_read=False,
            attrs_on_load={
                common.RANGES_ATTR: [
                    {'row_offset': YsonUint64(0),   'row_limit': YsonUint64(200), 'status': 'new'},
                    {'row_offset': YsonUint64(200), 'row_limit': YsonUint64(99),  'status': 'new'},
                ],
            },
        ),
    ]

    output_tables = [
        TableSpec(
            file_path='output',
            table_name=output_table,
            mapreduce_io_flags=['-format', 'json'],
            sort_on_load=False,
            sort_on_read=False,
        ),
    ]

    result = _get_result(yt_stuff, inputs_dir, worker_dir, output_table, input_tables, output_tables)

    assert common.yt_path_count(yt_stuff, inputs_dir) == 0

    return result


@pytest.mark.parametrize('worker_status, range_status', [
    ('ready',        'new'),
    ('not_finished', 'in_progress'),
    ('finished',     'in_progress'),
])
def test_recover(yt_stuff, worker_status, range_status):
    inputs_dir = '//inputs_recover_{}'.format(worker_status)
    worker_dir = '//worker_recover_{}'.format(worker_status)
    output_table = '//output_recover_{}'.format(worker_status)

    client = yt_stuff.get_yt_client()

    if worker_status == 'ready':
        pass

    elif worker_status == 'not_finished':
        client.create('map_node', worker_dir)
        client.create('map_node', '{}/lock'.format(worker_dir))
        client.create('map_node', '{}/task'.format(worker_dir), attributes={
            common.TASK_ATTR: {
                'table_name': 'table1',
                'range_index': YsonUint64(2),
                'row_offset': YsonUint64(200),
                'row_limit': YsonUint64(100),
                'done': False,
            },
        })

    elif worker_status == 'finished':
        client.create('map_node', worker_dir)
        client.create('map_node', '{}/lock'.format(worker_dir))
        client.create('map_node', '{}/task'.format(worker_dir), attributes={
            common.TASK_ATTR: {
                'table_name': 'table1',
                'range_index': YsonUint64(2),
                'row_offset': YsonUint64(200),
                'row_limit': YsonUint64(100),
                'done': True,
            },
        })

    else:
        assert False

    input_tables = [
        TableSpec(
            file_path='table1',
            table_name='{}/table1'.format(inputs_dir),
            mapreduce_io_flags=['-format', 'json'],
            sort_on_load=False,
            sort_on_read=False,
            attrs_on_load={
                common.RANGES_ATTR: [
                    {'row_offset': YsonUint64(0),   'row_limit': YsonUint64(100), 'status': 'done'},
                    {'row_offset': YsonUint64(100), 'row_limit': YsonUint64(100), 'status': 'done'},
                    {'row_offset': YsonUint64(200), 'row_limit': YsonUint64(100), 'status': range_status},
                    {'row_offset': YsonUint64(300), 'row_limit': YsonUint64(100), 'status': 'new'},
                ],
            },
        ),
    ]

    output_tables = [
        TableSpec(
            file_path='output',
            table_name=output_table,
            mapreduce_io_flags=['-format', 'json'],
            sort_on_load=False,
            sort_on_read=False,
        ),
    ]

    result = _get_result(yt_stuff, inputs_dir, worker_dir, output_table, input_tables, output_tables)

    assert common.yt_path_count(yt_stuff, inputs_dir) == 0

    return result
