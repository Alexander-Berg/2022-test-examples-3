#!/usr/bin/env python

import os
import io
import yatest.common
import yt_utils
import json
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/indexdocs/indexdocs')
_DATA_PATH = yatest.common.data_path('extsearch/video/robot/docbase/indexdocs')
_CONFIG_PATH = yatest.common.data_path('extsearch/video/robot/index/vlinkindex/config')
_DUMP_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/docbase/indexdocs/dumptool/directdump')

def link_configs(index_config_dir, dynamic_config_dir, target_dir):
    def link_if_needed(src, link):
        if not os.path.exists(link):
            os.symlink(src, link)

    map(lambda filename: link_if_needed(os.path.join(index_config_dir, filename),
        os.path.join(target_dir, filename)), os.listdir(index_config_dir))
    map(lambda filename: link_if_needed(os.path.join(dynamic_config_dir, filename),
        os.path.join(target_dir, filename)), os.listdir(dynamic_config_dir))

def test_index(yt_stuff):
    link_configs(_DATA_PATH, _CONFIG_PATH, os.getcwd())

    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'
    configfile = yatest.common.source_path("yweb/webscripts/video/index/config/iconfiguration.cfg.pattern")

    yt_stuff.get_yt_client().create("map_node", path = '//index/output', recursive = True, ignore_existing = True)

    result = yt_utils.yt_test(
        _BIN_PATH,
        args=[
              'IndexLinks'
            , '--server', yt_server
            , '--configdir', os.getcwd()
            , '--home', 'index'
            , '--index-type', 'full'
            , '--prototype', 'json'
            , '--configfile', configfile
            , '--dump-index', 'index/output/direct_index'
            , '--current-time', '1499161966'
            ],
            data_path=os.getcwd(),
            input_tables=[
                TableSpec('docids', table_name = 'index/input/docids', mapreduce_io_flags=['-format', 'yson'], sortby=['GroupingUrl'], sort_on_load = True),
                TableSpec('index_items', table_name = 'index/input/index_items', mapreduce_io_flags=['-format', 'json'], sortby=['GroupingUrl'], sort_on_load = True),
                TableSpec('thumbs_items', table_name = 'index/input/thumbs_items', mapreduce_io_flags=['-format', 'json'], sortby=['GroupingUrl'], sort_on_load = True),
                TableSpec('related_items', table_name = 'index/input/related_items', mapreduce_io_flags=['-format', 'json'], sortby=['GroupingUrl'], sort_on_load = True)
            ],
            output_tables=[
                TableSpec('direct_index', table_name = 'index/output/direct_index', mapreduce_io_flags=['-format', '<format=text>yson'], sortby=['GroupingUrl'], sort_on_read = True),
                TableSpec('fusion', table_name = 'index/output/fusion', mapreduce_io_flags=['-format', 'json'], sortby=['GroupingUrl'], sort_on_read = True),
                TableSpec('spoklang', table_name = 'index/output/spoklang', mapreduce_io_flags=['-format', 'json'], sort_on_read = True),
                TableSpec('dups.portion', table_name = 'index/output/dups.portion', mapreduce_io_flags=['-format', 'json'], sortby=['GroupingUrl']),
                TableSpec('archive', table_name = 'index/tmp/archive', mapreduce_io_flags=['-format', 'json'], sort_on_read = True),
                TableSpec('authors', table_name = 'index/tmp/authors', mapreduce_io_flags=['-format', 'json'], sort_on_read = True),
                TableSpec('hosts', table_name = 'index/tmp/hosts', mapreduce_io_flags=['-format', 'json']),
                TableSpec('index.failed', table_name = 'index/output/index.failed', mapreduce_io_flags=['-format', 'json'], sortby=['GroupingUrl']),
            ],
            yt_stuff=yt_stuff
    )

    yatest.common.execute([_DUMP_BIN_PATH]
        , stdin=open('direct_index', 'r')
        , stdout=open('direct_index_dump', 'w'))
    result.append(yatest.common.canonical_file('direct_index_dump'))

    return result
