import common

from mr_utils import TableSpec

import copy
import pytest
import os
import yatest.common
import yt_utils


@pytest.mark.parametrize('row_limit', [0, 75, 100])
def test(yt_stuff, row_limit):
    inputs_extended = copy.deepcopy(common.INPUTS)

    src_dir = '//src_{}'.format(row_limit)
    dst_dir = '//dst_{}'.format(row_limit)

    input_tables = []
    output_tables = []

    for item in inputs_extended:
        file_path = item['file_path']
        out_file_path = 'out_{}'.format(file_path)

        item['src_table_name'] = '{}/{}'.format(src_dir, file_path)
        item['dst_table_name'] = '{}/{}'.format(dst_dir, file_path)

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

        output_tables.append(
            TableSpec(
                file_path=out_file_path,
                table_name=item['dst_table_name'],
                mapreduce_io_flags=['-format', 'json'],
                sort_on_load=False,
                sort_on_read=False,
                attrs_on_read=[common.RANGES_ATTR],
            )
        )

    result = yt_utils.yt_test(
        yatest.common.binary_path(common.BINARY_PATH),
        [
            'prepare-inputs',
            '--cluster', yt_stuff.get_server(),
            '--src', src_dir,
            '--dst', dst_dir,
            '--row-limit', str(row_limit),
        ],
        os.getcwd(),
        input_tables=input_tables,
        output_tables=output_tables,
        yt_stuff=yt_stuff,
    )

    assert common.yt_path_count(yt_stuff, src_dir) == 0
    assert common.yt_path_count(yt_stuff, dst_dir) == common.non_empty_count(inputs_extended)

    for item in inputs_extended:
        dst_exists = common.yt_path_exists(yt_stuff, item['dst_table_name'])

        assert item['is_empty'] and not dst_exists \
            or not item['is_empty'] and dst_exists

    return result
