#!/usr/bin/env python

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec


_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/factors/hostfexport/hostfexport')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/index/indexfactors/prepare')
_CONFIGURATION_DATA_PATH = yatest.common.source_path('yweb/webscripts/video/index/config/iconfiguration.cfg.pattern')
_CONFIGDATA_PATH = yatest.common.source_path('yweb/webscripts/video/index/config')


def test_export(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            'HostsExport',
            '--server', yt_server,
            '--factors', 'hostfactors',
            '--configuration', _CONFIGURATION_DATA_PATH,
            '--homepath', 'index',
            '--configdir', _CONFIGDATA_PATH,
            '--output-prototype', 'json'
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('shardhosts', table_name='index/input/shardhost', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_load=True),
            TableSpec('hostfactors', table_name='hostfactors', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_load=True)
        ],
        output_tables=[
            TableSpec('hostmap', table_name='index/output/hostfactors_index.hostmap', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_read=True),
            TableSpec('hostfactors_index', table_name='index/output/hostfactors_index', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_read=True),
            TableSpec('hostfactors_with_shard', table_name='index/tmp/hostfactors_with_shard', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_read=True)
        ],
        yt_stuff=yt_stuff
    )
