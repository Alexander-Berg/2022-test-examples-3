#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest
from StringIO import StringIO

from market.idx.marketindexer.marketindexer import regions_validator


correct_xml = '''<?xml version='1.0' encoding='utf-8'?>
<regions>
<region id="10000" name="ЗЕМЛЯ" type="0" locative="ЗЕМЛЕ" preposition="на" genitive="ЗЕМЛИ" syn="" lat="28.150000" lon="1.060000" chief_region="0"/>
<region id="138" name="Австралия и Океания" type="1" locative="Австралии и Океании" preposition="в" genitive="Австралии и Океании" syn="" lat="-25.816646" lon="133.439867" chief_region="0"/>
<region id="139" name="Новая Зеландия" type="3" locative="Новой Зеландии" preposition="в" genitive="Новой Зеландии" syn="" lat="-43.730417" lon="170.366563" chief_region="10147"/>
<region id="107920" name="Грютвикен" type="6" locative="Грютвикене" preposition="в" genitive="Грютвикена" syn="" lat="-54.281500" lon="-36.508000" chief_region="0"/>
</regions>'''


class MockFeed(object):
    def __init__(self, feed_id, mbi_params):
        self.id = feed_id
        self.mbi_params = mbi_params


class TestRegionsValidator(unittest.TestCase):
    def test_parse_allowed_regions(self):
        regions = regions_validator.parse_allowed_regions(StringIO(correct_xml))
        self.assertEquals(set([10000, 138, 139, 107920]), regions)

    def test_parse_regions_string(self):
        regions = regions_validator.parse_regions_string('23;34;56')
        self.assertEqual([23, 34, 56], regions)

        regions = regions_validator.parse_regions_string('56;3;')
        self.assertEqual([56, 3], regions)

        regions = regions_validator.parse_regions_string('')
        self.assertEqual(0, len(regions))

        regions = regions_validator.parse_regions_string(None)
        self.assertEqual(0, len(regions))

        self.assertRaises(ValueError, regions_validator.parse_regions_string, ' ')
        self.assertRaises(ValueError, regions_validator.parse_regions_string, 'z')
        self.assertRaises(ValueError, regions_validator.parse_regions_string, '1234;23;zx')

    def test_iterate_feeds(self):
        feeds = [MockFeed(11, {'region': '12;', 'region2': '34'}), MockFeed(15, {'region': '16;', 'region2': '12'})]
        feed_regions = list(regions_validator.iterate_feeds(feeds, ['region', 'region2']))

        self.assertEqual(2, len(feed_regions))
        self.assertEqual(11, feed_regions[0][0].id)
        self.assertEqual([12, 34], feed_regions[0][1])

        self.assertEqual(15, feed_regions[1][0].id)
        self.assertEqual([16, 12], feed_regions[1][1])

    def test_iterate_feeds_ivalid_regions(self):
        feeds = [MockFeed(11, {'region': 'zx', 'region2': '34'}), MockFeed(15, {'region': '16;', 'region2': '12'})]
        feed_regions = list(regions_validator.iterate_feeds(feeds, ['region', 'region2']))

        self.assertEqual(2, len(feed_regions))
        self.assertEqual(11, feed_regions[0][0].id)
        self.assertEqual(None, feed_regions[0][1])

        self.assertEqual(15, feed_regions[1][0].id)
        self.assertEqual([16, 12], feed_regions[1][1])

    def test_iterate_feeds_no_region_param(self):
        feeds = [MockFeed(11, {'region': '34'}), MockFeed(15, {'region': '16;', 'region2': '12'})]
        feed_regions = list(regions_validator.iterate_feeds(feeds, ['region', 'region2']))

        self.assertEqual(2, len(feed_regions))
        self.assertEqual(11, feed_regions[0][0].id)
        self.assertEqual([34], feed_regions[0][1])

    def test_iterate_feeds_no_region_params(self):
        feeds = [MockFeed(11, {}), MockFeed(15, {'region': '16;', 'region2': '12'})]
        feed_regions = list(regions_validator.iterate_feeds(feeds, ['region', 'region2']))

        self.assertEqual(2, len(feed_regions))
        self.assertEqual(11, feed_regions[0][0].id)
        self.assertEqual([], feed_regions[0][1])

    def test_validator(self):
        validator = regions_validator.RegionValidator(set([23, 45, 67]))

        self.assertEqual(validator.find_unknown_regions([]), [])
        self.assertEqual(validator.find_unknown_regions([45]), [])
        self.assertEqual(validator.find_unknown_regions([23, 67, 45, 23, 45]), [])

        self.assertEqual(validator.find_unknown_regions([23, 67, 3]), ['3'])


if __name__ == '__main__':
    unittest.main()
