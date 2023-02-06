# coding: utf-8

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff
import util


_DIFF_TOOL_PATH = yatest.common.binary_path('extsearch/video/tools/json_diff/json_diff')
_BIN_PATH = yatest.common.binary_path("extsearch/video/robot/index/indexfactors/indexfactors")
_CONFIGURATION_DATA_PATH = yatest.common.source_path('yweb/webscripts/video/index/config/iconfiguration.cfg.pattern')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/index/indexfactors/indexfactors')


def test(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'

    timestamp_val = ""
    with open(os.path.join(_INPUT_DATA_PATH, "timestamp"), "rb") as ts_file:
        timestamp_val = ts_file.read()

    indexconfig = yatest.common.source_path("yweb/webscripts/video/index/config")
    util.link_configs(indexconfig, yatest.common.data_path("extsearch/video/robot/index/indexfactors/config"), os.getcwd())

    return yt_utils.yt_test(
        _BIN_PATH,
        args=['index-factors'
                , '--server', 'local'
                , '--configdir', os.getcwd()
                , '--mr_home', 'index'
                , '--configfile', _CONFIGURATION_DATA_PATH
                , '--docs-limit', '10'
                , '--timestamp', timestamp_val
                , '--dump-erf'
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('fusion', table_name='index/output/fusion', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['GroupingUrl']),
            TableSpec('authorfactors', table_name='index/tmp/authorfactors', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['GroupingUrl']),
            TableSpec('urlfactors', table_name='index/tmp/urlfactors', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['GroupingUrl']),
            TableSpec('hostfactors', table_name='index/tmp/hostfactors', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['GroupingUrl']),
            TableSpec('indexann_items', table_name='index/input/indexann_items', mapreduce_io_flags=['-format', '<format=text>yson'], sort_on_load=True, sortby=['GroupingUrl']),
        ],
        output_tables=[
            TableSpec('factors_index.schema', table_name='index/output/factors', mapreduce_io_flags=['-format', 'json'], sort_on_read=True, sortby=['GroupingUrl']),
            TableSpec('reject.schema', table_name='index/output/sr_refused', mapreduce_io_flags=['-format', 'json'], sort_on_read=True),
            TableSpec('factors.schema', table_name='index/output/dump_erf', mapreduce_io_flags=['-format', 'json'], sort_on_read=True),
            TableSpec('erf_conf.pre.schema', table_name='index/tmp/erf_conf.pre', mapreduce_io_flags=['-format', 'json'], sort_on_read=True),
            TableSpec('spokreject.schema', table_name='index/tmp/spok_reject', mapreduce_io_flags=['-format', 'json'], sort_on_read=True),
            TableSpec('selrank_score.schema', table_name='index/tmp/selrank_score', mapreduce_io_flags=['-format', 'json'], sort_on_read=True),
        ],
        yt_stuff=yt_stuff
    )
