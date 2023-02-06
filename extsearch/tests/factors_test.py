#!/usr/bin/env python
# -*- coding: utf-8 -*-

import yatest.common
import yt_utils

import os

from mapreduce.yt.python.yt_stuff import yt_stuff


_DIFF_TOOL_PATH = yatest.common.binary_path('extsearch/video/tools/json_diff/json_diff')
_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/postcalc/postcalc')
_INPUT_DATA_PATH = yatest.common.source_path('extsearch/video/robot/docbase/postcalc/tests/data')
_CONFIGURATION_DIR_PATH = os.path.join(os.getcwd(), 'config')
_CONFIGURATION_DATA_PATH = yatest.common.source_path('extsearch/video/robot/docbase/postcalc/tests/data/iconfiguration.cfg')
_OBJECT_TABLE = '//objects'
_ATTRS_TABLE = '//attrs'
_FUSION_TABLE = '//fusion'
_FACTORS_TABLE = '//factors'
_INDEX_TABLE = '//direct-index'


def test(yt_stuff):
    yt_server = yt_stuff.get_server()

    return yt_utils.yt_test(
        _BIN_PATH,
        args=[
            '--server', yt_server,
            '--direct-index', _INDEX_TABLE,
            '--objects', _OBJECT_TABLE,
            '--attrs', _ATTRS_TABLE,
            '--fusion', _FUSION_TABLE,
            '--factors', _FACTORS_TABLE,
            '--configdir', _CONFIGURATION_DIR_PATH,
            '--configfile', _CONFIGURATION_DATA_PATH
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            yt_utils.TableSpec('direct-index', table_name=_INDEX_TABLE, mapreduce_io_flags=["--format", "<format=binary>yson"], sort_on_load=True, sortby=['GroupingUrl']),
            yt_utils.TableSpec('objects', table_name=_OBJECT_TABLE, mapreduce_io_flags=["--format", "<format=binary>yson"], sort_on_load=True, sortby=['GroupingUrl']),
            yt_utils.TableSpec('attrs', table_name=_ATTRS_TABLE, mapreduce_io_flags=["--format", "<format=binary>yson"], sort_on_load=True, sortby=['GroupingUrl'])
        ],
        output_tables=[
            yt_utils.TableSpec('fusion', table_name=_FUSION_TABLE, mapreduce_io_flags=["--format", "json"], sort_on_load=True, sortby=['GroupingUrl']),
            yt_utils.TableSpec('factors', table_name=_FACTORS_TABLE, mapreduce_io_flags=["--format", "json"])
        ],
        yt_stuff=yt_stuff,
        #diff_tool=[_DIFF_TOOL_PATH]
    )
