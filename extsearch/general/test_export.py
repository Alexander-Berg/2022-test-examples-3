import common

from mr_utils import TableSpec

import copy
import os
import yatest.common
import yt_utils


def test(yt_stuff):
    inputs_extended = copy.deepcopy(common.INPUTS)

    src_dir = '//src'
    first_dst_dir = '//first_dst'
    second_dst_dir = '//second_dst'

    prefix = 'prefix_'

    input_tables = []
    output_tables = []

    for item in inputs_extended:
        file_path = item['file_path']

        item['src_table_name'] = '{}/{}'.format(src_dir, file_path)
        item['first_dst_table_name'] = '{}/{}'.format(first_dst_dir, prefix + file_path)
        item['second_dst_table_name'] = '{}/{}'.format(second_dst_dir, prefix + file_path)

        input_tables.append(
            TableSpec(
                file_path=file_path,
                table_name=item['src_table_name'],
                mapreduce_io_flags=['-format', 'json'],
                sort_on_load=False,
                sort_on_read=False,
            )
        )

        if item['is_empty']:
            continue

        output_tables.extend([
            TableSpec(
                file_path='first_out_{}'.format(file_path),
                table_name=item['first_dst_table_name'],
                mapreduce_io_flags=['-format', 'json'],
                sort_on_load=False,
                sort_on_read=False,
            ),
            TableSpec(
                file_path='second_out_{}'.format(file_path),
                table_name=item['second_dst_table_name'],
                mapreduce_io_flags=['-format', 'json'],
                sort_on_load=False,
                sort_on_read=False,
            ),
        ])

    result = yt_utils.yt_test(
        yatest.common.binary_path(common.BINARY_PATH),
        [
            'export',
            '--cluster', yt_stuff.get_server(),
            '--src', src_dir,
            '--dst', first_dst_dir,
            '--dst', second_dst_dir,
            '--prefix', prefix,
            '--skip-empty',
        ],
        os.getcwd(),
        input_tables=input_tables,
        output_tables=output_tables,
        yt_stuff=yt_stuff,
    )

    non_empty_count = common.non_empty_count(inputs_extended)

    assert common.yt_path_count(yt_stuff, src_dir) == 0
    assert common.yt_path_count(yt_stuff, first_dst_dir) == non_empty_count
    assert common.yt_path_count(yt_stuff, second_dst_dir) == non_empty_count

    for item in inputs_extended:
        first_exists = common.yt_path_exists(yt_stuff, item['first_dst_table_name'])
        second_exists = common.yt_path_exists(yt_stuff, item['second_dst_table_name'])

        assert item['is_empty'] and not first_exists and not second_exists \
            or not item['is_empty'] and first_exists and second_exists

    return result
