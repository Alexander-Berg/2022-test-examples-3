#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/factors/authfexport/authfexport')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/index/indexfactors/prepare')
_CONFIGURATION_DATA_PATH = yatest.common.source_path('yweb/webscripts/video/index/config/iconfiguration.cfg.pattern')


def test_export(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            '--server', yt_server,
            '--factors', 'authorfactors',
            '--configuration', _CONFIGURATION_DATA_PATH,
            '--homepath', 'index',
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('url2author', table_name='index/tmp/authors', mapreduce_io_flags=['-format', '<has_subkey=%true;key=GroupingUrl;value=key>yamr'], sortby=['key', 'GroupingUrl'], sort_on_load=True),
            TableSpec('authorfactors', table_name='authorfactors', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_load=True)
        ],
        output_tables=[
            TableSpec('output', table_name='index/tmp/authorfactors', mapreduce_io_flags=['-format', 'json'], sort_on_read=True, sortby=['GroupingUrl'])
        ],
        yt_stuff=yt_stuff
    )
