#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/vegas/adaptor/adaptor')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/docbase/vegas/adaptor')


def test_export(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            '--server', yt_server,
            '--docbase', 'docbase',
            '--adapted', 'output',
            '--dump',
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('docbase', table_name='docbase', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['Host', 'Path', 'source', 'itemhash', 'ts']),
        ],
        output_tables=[
            TableSpec('output', table_name='output', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_read=True)
        ],
        yt_stuff=yt_stuff
    )
