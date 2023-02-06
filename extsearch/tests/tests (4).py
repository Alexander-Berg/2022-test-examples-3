import os

import yatest.common
import yt.yson as yson
import yt_utils
from mr_utils import TableSpec
from yt.wrapper import ypath_join as ypj

from . import data


def dump_yson_table(rows, file_path):
    with open(file_path, 'wb') as f:
        for row in rows:
            f.write(yson.dumps(row) + ';')


def do_test(test_id, portion, keys, extra_args, yt_stuff):
    portion_file = '{}_portion.yson'.format(test_id)
    keys_file = '{}_keys.yson'.format(test_id)

    portions_dir = '//{}_portions'.format(test_id)
    portion_table = ypj(portions_dir, 'portion')
    keys_table = '//{}_keys'.format(test_id)
    delta_table = '//{}_delta'.format(test_id)
    delta_keys_table = '//{}_delta_keys'.format(test_id)
    delta_urls_table = '//{}_delta_urls'.format(test_id)
    errors_table = '//{}_errors'.format(test_id)

    dump_yson_table(portion, portion_file)
    dump_yson_table(keys, keys_file)

    args = [
        '-c', yt_stuff.get_server(),
        '-i', portions_dir,
        '--keys', keys_table,
        '--delta', delta_table,
        '--delta-keys', delta_keys_table,
        '--delta-urls', delta_urls_table,
        '--errors', errors_table,
    ]
    args.extend(extra_args)

    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/crawling/merge_contents/merge_contents'),
        args,
        os.getcwd(),
        input_tables=[
            TableSpec(
                portion_file,
                table_name=portion_table,
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=False,
            ),
            TableSpec(
                keys_file,
                table_name=keys_table,
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
        ],
        output_tables=[
            TableSpec(
                'delta.json',
                table_name=delta_table,
                mapreduce_io_flags=['-format', 'json'],
                sort_on_read=False,
            ),
            TableSpec(
                'delta_keys.json',
                table_name=delta_keys_table,
                mapreduce_io_flags=['-format', 'json'],
                sort_on_read=False,
            ),
            TableSpec(
                'delta_urls.json',
                table_name=delta_urls_table,
                mapreduce_io_flags=['-format', 'json'],
                sort_on_read=True,
                sortby=['Url', 'Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'errors.json',
                table_name=errors_table,
                mapreduce_io_flags=['-format', 'json'],
                sort_on_read=True,
                sortby=['Url', 'Hash', 'ContentType', 'RowIndex', 'RecordTime'],
            ),
        ],
        yt_stuff=yt_stuff,
        diff_tool=[
            yatest.common.binary_path('extsearch/video/tools/json_diff/json_diff'),
        ],
    )


def test_with_check(yt_stuff):
    extra_args = [
        '--shard-id', '0063',
        '--num-shards', '64',
        '--check-shard',
    ]
    return do_test('with_check', data.PORTION, data.KEYS, extra_args, yt_stuff)


def test_without_check(yt_stuff):
    extra_args = []
    return do_test('without_check', data.PORTION, data.KEYS, extra_args, yt_stuff)


def test_exception(yt_stuff):
    extra_args = [
        '--shard-id', 'vh_0000',
        '--num-shards', '64',
        '--check-shard',
    ]
    try:
        do_test('exception', data.PORTION, data.KEYS, extra_args, yt_stuff)
        assert False, 'Expected exception due to wrong shard id'
    except:
        pass
