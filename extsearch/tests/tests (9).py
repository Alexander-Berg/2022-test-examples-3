import os

import mr_utils
import yatest.common
import yt_utils


INPUT_TABLES = [
    ('input_tables/table1.json.tsv', '//{}/input_dir/table1'),
    ('input_tables/table2.json.tsv', '//{}/input_dir/table2'),
    ('input_tables/table3.json.tsv', '//{}/input_dir/table3'),
    ('input_tables/table4.json.tsv', '//{}/input_dir/table4'),
]


def _get_input_tables(tag):
    input_tables = []
    for file_path, table_name in INPUT_TABLES:
        input_tables.append(
            mr_utils.TableSpec(
                file_path=file_path,
                table_name=table_name.format(tag),
                mapreduce_io_flags=['-format', 'json'],
            )
        )
    return input_tables


def _run_binary(yt_stuff, tag, extra_args=[]):
    os.environ['YT_PROXY'] = yt_stuff.get_server()
    args = [
        '--src', '//{}/input_dir'.format(tag),
        '--dst', '//{}/output_dir'.format(tag),
        '--mkdir',
    ]
    yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/tools/get_portions/get_portions'),
        args + extra_args,
        os.getcwd(),
        input_tables=_get_input_tables(tag),
        yt_stuff=yt_stuff,
    )


def _yt_list(yt_stuff, path):
    return yt_stuff.get_yt_client().list(path)


def test_simple(yt_stuff):
    _run_binary(yt_stuff, 'simple')
    assert _yt_list(yt_stuff, '//simple/input_dir') == []
    assert _yt_list(yt_stuff, '//simple/output_dir') == ['table1', 'table2', 'table3', 'table4']


def test_skip_empty(yt_stuff):
    # NOTE(meow): cannot test --skip-empty option because empty and non-existent table are the same in yamr
    pass


def test_max_tables(yt_stuff):
    _run_binary(yt_stuff, 'max_tables', ['--max-tables', '3'])
    assert _yt_list(yt_stuff, '//max_tables/input_dir') == ['table4']
    assert _yt_list(yt_stuff, '//max_tables/output_dir') == ['table1', 'table2', 'table3']


def test_row_count_threshold(yt_stuff):
    _run_binary(yt_stuff, 'row_count_threshold', ['--row-count-threshold', '20'])
    assert _yt_list(yt_stuff, '//row_count_threshold/input_dir') == ['table3', 'table4']
    assert _yt_list(yt_stuff, '//row_count_threshold/output_dir') == ['table1', 'table2']
