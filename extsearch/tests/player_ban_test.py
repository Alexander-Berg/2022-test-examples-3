#!/usr/bin/env python

import os
import logging
import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff

_BIN_PATH = yatest.common.binary_path('extsearch/video/robot/deletes/player_ban/player_ban')

logger = logging.getLogger('PlayerBanTest')


def test_convert_portion(yt_stuff):
    yt_server = yt_stuff.get_server()
    os.environ["YT_PREFIX"] = '//'

    yt_client = yt_stuff.get_yt_client()
    yt_client.create("map_node", path = '//player', recursive = True, ignore_existing = True)
    yt_client.smart_upload_file(
        yatest.common.source_path('yweb/webscripts/video/player/connectors_urlbase.xml'),
        destination='//player/connectors_urlbase.xml',
        placement_strategy='replace')
    yt_client.smart_upload_file(
        yatest.common.source_path('yweb/webscripts/video/player/dups_canonizer.xml'),
        destination='//player/dups_canonizer.xml',
        placement_strategy='replace')

    result = yt_utils.yt_test(
        _BIN_PATH,
        args=[
            'Convert',
            '--server', yt_server,
            '--input', 'player/banned.portion',
            '--output', 'player/converted.portion',
            '--connectors-config', 'player/connectors_urlbase.xml',
            '--canonizers-config', 'player/dups_canonizer.xml'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec('banned.portion', table_name = 'player/banned.portion', mapreduce_io_flags=['-format', 'yson'])
        ],
        output_tables=[
            TableSpec('converted.portion', table_name = 'player/converted.portion', mapreduce_io_flags=['-format', '<format=text>yson'], sortby=['playerData'], sort_on_read = True)
        ],
        yt_stuff=yt_stuff
    )
    return result

