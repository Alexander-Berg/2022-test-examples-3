# coding: utf-8

import os
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff
import util


_BIN_PATH = yatest.common.binary_path("extsearch/video/robot/index/indexfactors/indexfactors")
_CONFIGURATION_DATA_PATH = yatest.common.source_path('yweb/webscripts/video/index/config/iconfiguration.cfg.pattern')
_INPUT_DATA_PATH = yatest.common.data_path('extsearch/video/robot/index/indexfactors/indexfactors')


'''custom iconfiguration for spok boosts'''
spok_cfg = \
'SpokConfig {\n\
    SpokBoosts: [\n\
        {\n\
            QuotaType: 0\n\
            QuotaValue: "3"\n\
            Description: "SPOK test boost"\n\
            Domain: "ru"\n\
            Language: 1\n\
        },\n\
        {\n\
            QuotaType: 1\n\
            QuotaValue: "50"\n\
            Description: "SPOK percentage test boost"\n\
            Domain: "tr"\n\
            Language: 3\n\
        }\n\
    ]\n\
}\n'

def test(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'

    timestamp_val = ""
    with open(os.path.join(_INPUT_DATA_PATH, "timestamp"), "rb") as ts_file:
        timestamp_val = ts_file.read()

    indexconfig = yatest.common.source_path("yweb/webscripts/video/index/config")
    util.link_configs(indexconfig, yatest.common.data_path("extsearch/video/robot/index/indexfactors/config"), os.getcwd())
    cfg_modified = os.path.join(os.getcwd(), 'iconfiguration.cfg.modified')
    with open(cfg_modified, 'a') as cfgf:
        with open(_CONFIGURATION_DATA_PATH, 'r') as default:
            for line in default:
                if line == ' SpokConfig {\n':
                    break
                cfgf.write(line)
            cfgf.write(spok_cfg)

    return yt_utils.yt_test(
        _BIN_PATH,
        args=['index-factors'
            ,'--server', 'local'
            , '--configdir', os.getcwd()
            , '--mr_home', 'index'
            , '--configfile', cfg_modified
            , '--docs-limit', '10'
            , '--timestamp', timestamp_val
            , '--dump-erf'
            , '--spok'
            , '--spok-stats', 'spok_stats.txt'
        ],
        data_path=_INPUT_DATA_PATH,
        input_tables=[
            TableSpec('fusion', table_name = 'index/output/fusion', mapreduce_io_flags=['-format', '<lenval=%true;has_subkey=%true>yamr'], sort_on_load = True),
            TableSpec('authorfactors', table_name = 'index/tmp/authorfactors', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_load = True),
            TableSpec('urlfactors', table_name = 'index/tmp/urlfactors', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_load = True),
            TableSpec('hostfactors', table_name = 'index/tmp/hostfactors', mapreduce_io_flags=['-format', '<lenval=%true;has_subkey=%true>yamr'], sort_on_load = True),
            TableSpec('indexann_items', table_name = 'index/input/indexann_items', mapreduce_io_flags=['-format', '<lenval=%true;has_subkey=%true>yamr'], sort_on_load = True),
        ],
        output_files=['spok_stats.txt'],
        output_tables=[
            TableSpec('factors_index.pruning', table_name = 'index/output/factors_index', mapreduce_io_flags=['-format', '<lenval=%true;has_subkey=%true>yamr'], sort_on_read = True),
            TableSpec('reject', table_name = 'index/output/sr_refused', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_read = True),
            TableSpec('factors', table_name = 'index/output/dump_erf', mapreduce_io_flags=['-format', '<has_subkey=%true>yamr'], sort_on_read = True),
            TableSpec('erf_conf.pre', table_name = 'index/tmp/erf_conf.pre', mapreduce_io_flags=['-format', '<lenval=%true;has_subkey=%true>yamr'], sort_on_read = True),
            TableSpec('selrank_score.spok', table_name = 'index/tmp/selrank_score', mapreduce_io_flags=['-format', 'json'], sort_on_read = True),
        ],
        yt_stuff=yt_stuff
    )
