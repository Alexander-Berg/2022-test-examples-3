#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


_DIFF_TOOL_PATH = yatest.common.binary_path('extsearch/video/tools/json_diff/json_diff')
_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/crawling/custom_html_parser/bin/custom_html_parser')
_CONFIG_PATH = yatest.common.source_path('extsearch/video/robot/crawling/custom_html_parser/config/arjlover.cfg')
_INPUT_TABLE = '//input'
_OUTPUT_TABLE = '//output'


def test(yt_stuff):
    yt_server = yt_stuff.get_server()

    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            '--cluster', yt_server,
            '--input-table', _INPUT_TABLE,
            '--output-table', _OUTPUT_TABLE,
            '--config', _CONFIG_PATH,
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec('arjlover_sample', table_name=_INPUT_TABLE, mapreduce_io_flags=['-format', 'yson'], sort_on_read=False),
        ],
        output_tables=[
            TableSpec('arjlover_parsed', table_name=_OUTPUT_TABLE, mapreduce_io_flags=['-format', 'json'], sort_on_read=False)
        ],
        yt_stuff=yt_stuff,
        diff_tool=[_DIFF_TOOL_PATH]
    )
