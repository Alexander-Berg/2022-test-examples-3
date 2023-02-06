#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec


_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/factors/annfexport/annfexport')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/docbase/factors/annfexport')
_CONFIGURATION_DATA_PATH = yatest.common.source_path('yweb/webscripts/video/index/config/iconfiguration.cfg.pattern')


def test_export(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            'ultra',
            '--server', yt_server,
            '--plan', 'url2doc',
            '--factors', 'indexann',
            '--configuration', _CONFIGURATION_DATA_PATH,
            '--configpath', os.getcwd(),
            '--homepath', 'index',
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('url2doc', table_name='url2doc', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['key']),
            TableSpec('indexann', table_name='indexann', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['key'])
        ],
        output_tables=[
            TableSpec('indexann_items', table_name='index/input/indexann_items', mapreduce_io_flags=['-format', '<format=text>yson'], sortby=['GroupingUrl'])
        ],
        yt_stuff=yt_stuff
    )


def test_docbase_export(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            'full',
            '--server', yt_server,
            '--factors', 'indexann',
            '--index', 'iindex',
            '--configuration', _CONFIGURATION_DATA_PATH,
            '--configpath', os.getcwd(),
            '--homepath', 'index',
            '--output', 'direct_term_items'
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('indexann', table_name='indexann', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['key']),
            TableSpec('index', table_name='iindex', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['key'])
        ],
        output_tables=[
            TableSpec('direct_term_items', table_name='direct_term_items', mapreduce_io_flags=['-format', '<format=text>yson'], sortby=['GroupingUrl'])
        ],
        yt_stuff=yt_stuff
    )
