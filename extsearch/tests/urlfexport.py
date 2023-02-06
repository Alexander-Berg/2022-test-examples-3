#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec


_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/factors/urlfexport/urlfexport')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/index/indexfactors/prepare')
_CONFIGURATION_DATA_PATH = yatest.common.source_path('yweb/webscripts/video/index/config/iconfiguration.cfg.pattern')


def test_export(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            '--server', yt_server,
            '--plan', 'plan',
            '--factors', 'urlfactors',
            '--configuration', _CONFIGURATION_DATA_PATH,
            '--homepath', 'index',
            '--last-update', 'factors_last_update',
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('url2doc', table_name='plan', mapreduce_io_flags=['-format', '<has_subkey=%true;value=GroupingUrl>yamr'], sort_on_load=True),
            TableSpec('urlfactors', table_name='urlfactors', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_load=True),
            TableSpec('factors_last_update', table_name='factors_last_update', mapreduce_io_flags=['-format', 'json']),
        ],
        output_tables=[
            TableSpec('output', table_name='index/tmp/urlfactors', mapreduce_io_flags=['-format', '<has_subkey=%true;key=GroupingUrl>yamr'], sortby=['GroupingUrl', 'subkey']),
            TableSpec('attrFactors', table_name='index/tmp/urlattrsfactors', mapreduce_io_flags=['-format', '<has_subkey=%true;key=GroupingUrl>yamr'], sortby=['GroupingUrl', 'subkey']),
        ],
        yt_stuff=yt_stuff
    )
