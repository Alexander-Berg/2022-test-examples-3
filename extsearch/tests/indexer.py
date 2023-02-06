# coding: utf-8

from __future__ import absolute_import, division, print_function

import logging
import os
import yatest.common
import yt.yson as yson
from mapreduce.yt.python.yt_stuff import yt_stuff
from collections import namedtuple
import pytest


TableSpec = namedtuple('TableSpec', ["table_name", "sorted_by"])

_BIN_PATH = yatest.common.binary_path('extsearch/video/vh/indexer/indexvh')
_DATA_PATH = yatest.common.data_path('extsearch/video/vh/indexer')

__IN_TABLES = [
    # WARNING: custom sort order for table is used
    # sorted by (ContentVersionID) instead of usual (ContentGrpoupID, ContentVersionID)
    TableSpec(table_name='ContentVersionGroup', sorted_by=['ContentVersionID']),
    TableSpec(table_name='ContentResource', sorted_by=['ContentGroupID', 'ResourceName']),
    TableSpec(table_name='ContentGroup', sorted_by=['ContentGroupID']),
    # WARNING: custom sort order for table is used
    # sorted by (ContentVersionID) instead of usual (OutputStreamID)
    TableSpec(table_name='OutputStream', sorted_by=['ContentVersionID']),
    TableSpec(table_name='ContentTemplate', sorted_by=['ContentTypeID']),
    TableSpec(table_name='ContentType', sorted_by=['ContentTypeID', 'ResourceName']),
    TableSpec(table_name='Regions2ContentGroupId', sorted_by=['ContentGroupID']),
    TableSpec(table_name='VHSLicenses', sorted_by=['ContentGroupID']),
]

__OUT_TABLES = [
    TableSpec(table_name='Index', sorted_by=["ContentId"]),
    TableSpec(table_name='Index.dump', sorted_by=["ContentId"]),
    TableSpec(table_name='ContentGroupId2ParentUUID', sorted_by=['ContentGroupID']),
    TableSpec(table_name='OutputStream_groups', sorted_by=['ContentGroupID']),
]


def load_table(yt_client, table_spec, data_path):
    logging.info("loading %s", table_spec)
    path = yt_client.TablePath(name=table_spec.table_name, sorted_by=table_spec.sorted_by)
    yt_client.create("table", path, recursive=True, ignore_existing=True)
    with open(os.path.join(data_path, table_spec.table_name), "r") as fd:
        yt_client.write_table(table=path, input_stream=fd, raw=True, format="<format=text>yson")


def dump_table(yt_client, table_spec, dump_path):
    logging.info("dumping %s", table_spec)
    with open(os.path.join(dump_path, table_spec.table_name), "w") as fd:
        for row in yt_client.read_table(table_spec.table_name, raw=True, format="<format=text>yson"):
            fd.write(row)


@pytest.mark.xfail(True, reason="YT-12877: This test canonizes row order in the group of rows with same sort key")
def test_index(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    yt_server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_PROXY': yt_server,
        'YT_PATH': '//',
    }

    for table_spec in __IN_TABLES:
        load_table(yt_client, table_spec, _DATA_PATH)

    def run_stage(mode):
        yatest.common.execute(
            [
                _BIN_PATH,
                mode,
                '--server', yt_server,
                '--dstpath', '//',
            ],
            env=yt_env,
        )

    run_stage('restore-streams')
    run_stage('restore-parentuuid')

    yt_client.run_sort('OutputStream_groups', sort_by='ContentGroupID')
    yt_client.run_sort('ContentGroupId2ParentUUID', sort_by='ContentGroupID')

    run_stage('index')

    yt_client.run_sort('Index', sort_by='ContentId')
    yt_client.run_sort('Index.dump', sort_by='ContentId')

    dump_path = os.path.join(os.getcwd(), "result")
    os.makedirs(dump_path)
    for table_spec in __OUT_TABLES:
        dump_table(yt_client, table_spec, dump_path)

    return [yatest.common.canonical_dir(dump_path)]
