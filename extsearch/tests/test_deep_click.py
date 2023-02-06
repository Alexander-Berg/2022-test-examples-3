#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import yatest.common
import quality.mapreduce.tests.common.yt_utils as yt_utils
from quality.mapreduce.tests.common.mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


def test(yt_stuff):
    os.environ['MR_RUNTIME'] = 'YT'
    os.environ['YT_PREFIX'] = '//'

    data_path = yatest.common.data_path('extsearch/video/quality/deep_click')
    blockstat_file = os.path.join(data_path, 'blockstat.dict')

    spec = TableSpec(
        'sessions',
        table_name='user_sessions/pub/video/daily/2021-01-01/columns/clean',
        mapreduce_io_flags=['-format', '<format=text>yson'],
        sort_on_load=True,
        sortby=['key', 'subkey'])

    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/quality/deep_click/video_deep_click'),
        args=[
            'build_deep_click_pool', '-s', yt_stuff.get_server()
            , '--us-path', 'user_sessions/pub/video/daily', '--output', 'pool'
            , '--start-date', '20210101', '--end-date', '20210101'
            , '--blockstat', blockstat_file, '--delete-factors', '91:256'
            , '--regions', 'ru'
        ],
        data_path=os.getcwd(),
        input_tables=[spec],
        output_tables=[
            TableSpec('features', table_name = 'pool/ru/features'),
            TableSpec('queries', table_name = 'pool/ru/queries'),
        ],
        yt_stuff=yt_stuff
    )
