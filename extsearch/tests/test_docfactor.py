#!/usr/bin/env python
# -*- coding: utf-8 -*-

import yatest.common
import yt_utils

import os

from mapreduce.yt.python.yt_stuff import yt_stuff


_DIFF_TOOL_PATH = yatest.common.binary_path('extsearch/video/tools/json_diff/json_diff')
_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/docfactor/docfactor')
_INPUT_DATA_PATH = yatest.common.source_path('extsearch/video/robot/docbase/docfactor/tests/data')
_INPUT_TABLE = '//input'
_OUTPUT_TABLE = '//output'


def test(yt_stuff):
    yt_server = yt_stuff.get_server()

    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            '--server', yt_server,
            '--input', _INPUT_TABLE,
            '--output', _OUTPUT_TABLE,
            '--configdir', os.path.join(os.getcwd(), 'config')
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            yt_utils.TableSpec('input', table_name=_INPUT_TABLE, mapreduce_io_flags=["--format", "<format=binary>yson"])
        ],
        output_tables=[
            yt_utils.TableSpec('output', table_name=_OUTPUT_TABLE, mapreduce_io_flags=["--format", "json"])
        ],
        yt_stuff=yt_stuff,
        diff_tool=[_DIFF_TOOL_PATH, "--parse-inner-json", "value", "--add-field-to-diff", "key"]
    )
