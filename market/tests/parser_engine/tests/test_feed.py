# -*- coding: utf-8 -*-
import unittest

from market.idx.datacamp.parser.lib.parser_engine.feed import FeedData
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FeedParsingTask


class TestFeed(unittest.TestCase):
    def test_generate_feed_info(self):
        feed = FeedData()
        feed.feed_parsing_task=FeedParsingTask(
            business_id=1
        )
        feed.feed_id = 10
        feed.real_feed_id = 10
        feed.shop_disabled_since_ts = 100
        feed.unixepoch_sessionid = '1505218555'
        feed.mbi_params = dict()
        feed_info = feed.generate_feed_info()

        self.assertEqual(feed_info['shop_disabled_since_ts'], 100)

if '__main__' == __name__:
    unittest.main()
