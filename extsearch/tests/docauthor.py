#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


_DIFF_TOOL_PATH = yatest.common.binary_path('extsearch/video/tools/json_diff/json_diff')
_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/docauthor/docauthor')
_CONFIGURATION_DIR_PATH = 'config'
_CONFIGURATION_FILE_PATH = 'author_slice.lst'
_INPUT_TABLE='//input'
_OUTPUT_TABLE='//output'


def test(yt_stuff):
    yt_server = yt_stuff.get_server()

    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            '--server', yt_server,
            '--input', _INPUT_TABLE,
            '--output', _OUTPUT_TABLE,
            '--configdir', _CONFIGURATION_DIR_PATH,
            '--configfile', _CONFIGURATION_FILE_PATH
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec('author_factors', table_name=_INPUT_TABLE, mapreduce_io_flags=['-format', 'yson'], sort_on_read=False),
        ],
        output_tables=[
            TableSpec('author_slice', table_name=_OUTPUT_TABLE, mapreduce_io_flags=['-format', 'json'])
        ],
        yt_stuff=yt_stuff,
        diff_tool=[_DIFF_TOOL_PATH, "--parse-inner-json", "value", "--add-field-to-diff", "key"]
    )
