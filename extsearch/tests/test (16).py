import pytest
import sys
import os
import logging

import yatest.common
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff


BINARY_PATH = 'extsearch/video/robot/hostsdb/tool/hostsdb'

logger = logging.getLogger('HostsdbPrepareTest')


def get_schema_robots():
    schema = [
        {'name': 'Host', 'required': True, 'sort_order': 'ascending', 'type': 'string'},
        {'name': 'AcknowledgementTime', 'required': False, 'type': 'uint64'},
        {'name': 'HasSslCertErrors', 'required': False, 'type': 'boolean'},
        {'name': 'HostStatus', 'required': False, 'type': 'uint64'},
        {'name': 'IP', 'required': False, 'type': 'uint64'},
        {'name': 'IPv6', 'required': False, 'type': 'string'},
        {'name': 'LastAccess', 'required': False, 'type': 'uint64'},
        {'name': 'Robots', 'required': False, 'type': 'string'},
        {'name': 'RobotsHTTPCode', 'required': False, 'type': 'uint64'},
        {'name': 'RobotsHTTPResponse', 'required': False, 'type': 'string'},
        {'name': 'RobotsHeaders', 'required': False, 'type': 'string'},
        {'name': 'RobotsLastAccess', 'required': False, 'type': 'uint64'},
        {'name': 'RobotsResponseBody', 'required': False, 'type': 'string'}
    ]
    return schema


def test_prepare_faked_command(yt_stuff):
    yt_server = yt_stuff.get_server()
    yt_client = yt_stuff.get_yt_client()
    os.environ['YT_PREFIX'] = '//'
    yt_client.create('map_node', path='//prepare', recursive=True, ignore_existing=True)
    faked_dir = 'prepare'
    config_path = yatest.common.source_path(os.getcwd() + '/prepare.test.config')

    binary = yatest.common.binary_path(BINARY_PATH)
    args = ['PrepareFaked', '--proxy', yt_server, '--faked-path', faked_dir, '--config-file', config_path]

    return yt_utils.yt_test(
        binary,
        args,
        data_path=os.getcwd(),
        output_tables=[
            TableSpec('robots_prepared', table_name='prepare/robots', mapreduce_io_flags=['-format', '<format=text>yson'], sortby=['Host'], sort_on_read=True)
        ],
        yt_stuff=yt_stuff
    )


def test_reducer(yt_stuff):
    yt_server = yt_stuff.get_server()
    yt_client = yt_stuff.get_yt_client()
    os.environ['YT_PREFIX'] = '//'
    yt_client.create('map_node', path='//faked', recursive=True, ignore_existing=True)
    yt_client.create('table', path='//faked/prevdata', ignore_existing=True,
        attributes={"schema": [{"name": "Host", "type": "string", "sort_order": "ascending"}]})

    binary = yatest.common.binary_path(BINARY_PATH)
    args = ['Prepare', '--proxy', yt_server, '--video-hosts', 'faked/video_hosts',
        '--hostdb-prev', 'faked/prevdata',
        '--robots-table', 'faked/robots', '--status-table', 'faked/status',
        '--faked-table', 'faked/faked_robots', '--output', 'faked/data']

    if yt_client.exists('//faked/robots'):
        yt_client.remove('//faked/robots')

    return yt_utils.yt_test(
        binary,
        args,
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'prepare.test.fakedrobots',
                table_name='faked/faked_robots',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Host'],
            ),
            TableSpec(
                'prepare.test.robots',
                table_name='faked/robots',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Host'],
                attrs_on_load={'schema': get_schema_robots()}
            ),
            TableSpec(
                'prepare.test.status',
                table_name='faked/status',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Host'],
            ),
            TableSpec(
                'prepare.test.hosts',
                table_name='faked/video_hosts',
                mapreduce_io_flags=['-format', 'yson'],
            )
        ],
        output_tables=[
            TableSpec(
                'data',
                table_name='faked/data',
                mapreduce_io_flags=['-format', '<format=text>yson'],
                sortby=['Host'],
                sort_on_read=True
            )
        ],
        yt_stuff=yt_stuff
    )
