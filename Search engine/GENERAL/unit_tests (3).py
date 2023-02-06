#!/usr/bin/env python
# -*- coding: utf-8 -*-

import make_geosearch_queries
from make_geosearch_queries import make_cgi_query
import unittest
from urlparse import urlparse
from urlparse import parse_qs

class TestMakeGeosearchQueries(unittest.TestCase):
    def test_meta_from_maps(self):
        url = make_cgi_query('кафе', 213, '37.617671,55.75576', '1,1', 'p1=v1&p2=v2\tp3=v3', '37.61,55.75', address = 'addrs:17140', report = 'xml')
        parsed = urlparse(url)
        parsed_cgi = parse_qs(parsed.query)
        self.assertTrue(parsed.scheme == 'http')
        self.assertTrue(parsed.netloc == 'addrs:17140')
        self.assertTrue(parsed.path == '/yandsearch')
        self.assertTrue(parsed_cgi['text'] == ['кафе'])
        self.assertTrue(parsed_cgi['lang'] == ['ru_RU'])
        self.assertTrue(parsed_cgi['ll'] == ['37.617671,55.75576'])
        self.assertTrue(parsed_cgi['spn'] == ['1,1'])
        self.assertTrue(parsed_cgi['p1'] == ['v1'])
        self.assertTrue(parsed_cgi['p2'] == ['v2'])
        self.assertTrue(parsed_cgi['p3'] == ['v3'])
        self.assertTrue(parsed_cgi['ull'] == ['37.61,55.75'])
        self.assertTrue(parsed_cgi['xml'] == ['yes'])
        self.assertTrue(parsed_cgi['origin'][0] != '')
        self.assertTrue(parsed_cgi['distinguish_layers'] == ['1'])
        self.assertTrue(parsed_cgi['alternative_verticals'] == ['1'])

    def test_meta_form_geov(self):
        url = make_cgi_query('cafe', 11508, '', '', '', '28.96,41.03', mode = make_geosearch_queries.META_FROM_GEOV, report = 'proto')
        parsed = urlparse(url)
        parsed_cgi = parse_qs(parsed.query)
        self.assertTrue(parsed.scheme == '')
        self.assertTrue(parsed.netloc == '')
        self.assertTrue(parsed.path == '/yandsearch')
        self.assertTrue(parsed_cgi['text'] == ['cafe'])
        self.assertTrue(parsed_cgi['lang'] == ['tr_TR'])
        self.assertTrue(parsed_cgi['ll'][0].split(',')[0].startswith('28.'))
        self.assertTrue(parsed_cgi['ll'][0].split(',')[1].startswith('41.'))
        self.assertTrue(len(parsed_cgi['spn'][0].split(',')) == 2)
        self.assertTrue(parsed_cgi['sort_origin'] == ['28.96,41.03'])
        self.assertTrue(parsed_cgi['sort'] == ['distance'])
        self.assertTrue(parsed_cgi['ms'] == ['proto'])
        self.assertTrue(parsed_cgi['origin'][0] != '')
        self.assertTrue(parsed_cgi['minres'] == ['5'])
        self.assertTrue(parsed_cgi['maxspn'] == parsed_cgi['spn'])
        self.assertTrue(parsed_cgi['type'] == ['biz'])

    def test_exceptions(self):
        self.assertRaises(Exception, make_cgi_query, 'cafe', '', '', '')
        self.assertRaises(Exception, make_cgi_query, 'cafe', 'Moscow', '', '')
        self.assertRaises(Exception, make_cgi_query, 'cafe', '213', '37.61,55.75', '1,1', mode=10)

    def test_get_llspn(self):
        fake_ll = '1,1'
        fake_spn = '1,1'
        ll, spn = make_geosearch_queries.get_llspn(11508, '', '')
        self.assertTrue(ll.split(',')[0].startswith('28.'))
        self.assertTrue(ll.split(',')[1].startswith('41.'))
        self.assertTrue(len(spn.split(',')) == 2)
        ll, spn = make_geosearch_queries.get_llspn(11508, fake_ll, '')
        self.assertTrue(ll.split(',')[0].startswith('28.'))
        self.assertTrue(ll.split(',')[1].startswith('41.'))
        self.assertTrue(len(spn.split(',')) == 2)
        ll, spn = make_geosearch_queries.get_llspn(11508, fake_ll, fake_spn)
        self.assertTrue(ll == fake_ll)
        self.assertTrue(spn == fake_spn)

class TestGeobaseUtils(unittest.TestCase):
    def test_all(self):
        import geobase_utils
        ll, spn = geobase_utils.llspn_by_region(213)
        self.assertTrue(',' in ll and ',' in spn)

        self.assertTrue(geobase_utils.get_country(213) == geobase_utils.RUSSIA)
        self.assertTrue(geobase_utils.get_country(11508) == geobase_utils.TURKEY)
        self.assertTrue(geobase_utils.get_country(157) == geobase_utils.BELARUS)

        self.assertTrue(geobase_utils.region_name(213) == 'Москва')
        self.assertTrue(geobase_utils.region_name(225) == 'Россия')
        self.assertTrue(geobase_utils.region_name(983) == 'Турция')

if __name__ == '__main__':
    unittest.main()
