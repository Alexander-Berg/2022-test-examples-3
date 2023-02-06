#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


def test_calc_sigs(yt_stuff):
    ytcalcsigs = yatest.common.binary_path('extsearch/video/contstorage/ytcalcsigs/tool/ytcalcsigs')
    yt_server = yt_stuff.get_server()
    config_dir = 'vparser/'

    return yt_utils.yt_test(
        ytcalcsigs,
        args=[
            '-c', yt_server,
            '-i', '//input',
            '-o', '//output',
            '-e', '//errors',
            '-d', config_dir,
            '-s', str(5 * 60 * 1000)
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
                'output_tsv',
                table_name='//output',
                mapreduce_io_flags=['-format', '<columns=[url;algo;md5;ts_negative;signature;duration;ts]>schemaful_dsv'],
                sortby=['url', 'algo']
            ),
            TableSpec(
                'errors_tsv',
                table_name='//errors',
                mapreduce_io_flags=['-format', '<columns=[url;error;ts]>schemaful_dsv'],
                sortby=['url']
            )
        ],
        yt_stuff=yt_stuff
    )
