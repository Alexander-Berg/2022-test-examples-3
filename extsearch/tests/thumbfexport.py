#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec


_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/factors/thumbfexport/thumbfexport')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/docbase/factors/thumbfexport')


def test_export(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            'ExportThumbFactors',
            '-s', yt_server,
            '-i', 'thumb2cano',
            '-i', 'thumb-info',
            '-o', 'thumbfactors'
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('thumb2cano.yt.json', table_name='thumb2cano', mapreduce_io_flags=['-format', 'json']),
            TableSpec('thumb-info.yt.json', table_name='thumb-info', mapreduce_io_flags=['-format', 'json'])
        ],
        output_tables=[
            TableSpec('output.yt.json', table_name='thumbfactors', mapreduce_io_flags=['-format', 'json'])
        ],
        yt_stuff=yt_stuff
    )
