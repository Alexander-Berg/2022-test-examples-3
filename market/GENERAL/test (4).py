#!/usr/bin/env python
# -*- coding: utf-8 -*-

import logging
import subprocess
import sys
import unittest
import yatest

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
from market.idx.yatf.resources.tovar_tree_pb import MboCategory

CATEGORY_PANTHER_MMAP = yatest.common.output_path("category_panther.mmap")
CATEGORY_PANTHER_CONVERTER = yatest.common.binary_path("market/category_panther/stats/stats")

YT_SHOWLOG_RECORDS = [
    {
        "record_type": "0",
        "feed_id": "554251",
        "offer_id": "4968",
        "hyper_cat_id": "289014",
        "original_query": "колпачок",
        "geo_id": "10748",
    }
]


class T(unittest.TestCase):
    maxDiff = None

    def test_read_from_yt_and_write_to_mmap(self):
        log = logging.getLogger("T.test_read_from_yt_and_write_to_mmap")
        yt_server = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
        yt_server.start_local_yt()
        yt_client = yt_server.get_yt_client()

        YT_SHOWLOG_TABLE_PATH = '//tmp/local_showlog_table/30min/0000-00-00T00:00:00'
        yt_client.create('table', YT_SHOWLOG_TABLE_PATH, ignore_existing=True, recursive=True)
        yt_client.write_table(YT_SHOWLOG_TABLE_PATH, YT_SHOWLOG_RECORDS)

        category = MboCategory(hid=289014, tovar_id=1646, parent_hid=-1,
                               unique_name="Прочие элементы тюнинга автомобиля",
                               name="Прочие элементы тюнинга",
                               output_type=MboCategory.GURULIGHT)
        YT_TOVAR_RECORDS = [
            {
                "data": category.as_pb().SerializeToString()
            }
        ]

        YT_TOVAR_TABLE_PATH = '//tmp/local_tovar_tree'
        yt_client.create('table', YT_TOVAR_TABLE_PATH, ignore_existing=True, recursive=True)
        yt_client.write_table(YT_TOVAR_TABLE_PATH, YT_TOVAR_RECORDS)

        cmd_list = [
            CATEGORY_PANTHER_CONVERTER,
            '--proxy', yt_server.get_server(),
            '--tovar-tree-table-src', YT_TOVAR_TABLE_PATH,
            '--dst-mms-file', CATEGORY_PANTHER_MMAP,
            '--showlog-src', YT_SHOWLOG_TABLE_PATH,
        ]

        proc = subprocess.Popen(args=cmd_list, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

        assert proc.stdout is not None
        output = proc.stdout.read()
        log.debug('Binary output:\n%s', output)
        assert b'Found 1 records' in output
        assert b'All done!' in output


if __name__ == '__main__':
    logging.basicConfig(stream=sys.stderr)
    logging.getLogger("T.test_read_from_yt_and_write_to_mmap").setLevel(logging.DEBUG)
    unittest.main()
