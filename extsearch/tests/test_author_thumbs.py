#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


def test_author_thumbs(yt_stuff):
    authorthumbs = yatest.common.binary_path('extsearch/video/robot/authorthumbs/tool/authorthumbs')
    yt_server = yt_stuff.get_server()

    return yt_utils.yt_test(
        authorthumbs,
        args=[
            '-c', yt_server,
            '-i', '//input',
            '-o', '//output'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_yson',
                table_name='//input',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False
            )
        ],
        output_tables=[
            TableSpec(
                'output_yamr',
                table_name='//output'
            )
        ],
        yt_stuff=yt_stuff
    )
