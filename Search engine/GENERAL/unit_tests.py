#!/usr/bin/env python
# -*- coding: utf-8 -*-

import query
from query import Query

import unittest

from urlparse import urlparse
from urlparse import parse_qs

import geov_parser
import emulator_parser

import sys
sys.path.append('..')
import ranking.common

import shutil
class TestQuery(unittest.TestCase):
    def test_read_from_string_geov(self):
        q = Query.from_input_string('кафе\t213\t@@\twizbiz-new')
        self.assertTrue(q.text == 'кафе')
        self.assertTrue(q.region_code == 213)
        self.assertTrue(q.origin == 'wizbiz-new')

        parsed_url = urlparse(q.url)
        parsed_cgi = parse_qs(parsed_url.query)
        self.assertTrue(parsed_cgi['text'] == ['кафе'])
        self.assertTrue(parsed_cgi['lr'] == ['213'])
        self.assertTrue(parsed_cgi['srcask'] == ['GEOV', 'BLENDER_FAKE'])


    def test_read_from_string_mobile(self):
        q = Query.from_input_string('cafe\t11508\t28.96,41.03@@1,1\tmobile-maps-searchnearby-text')
        self.assertTrue(q.text == 'cafe')
        self.assertTrue(q.region_code == 11508)
        self.assertTrue(q.ll == '28.96,41.03')
        self.assertTrue(q.spn == '1,1')
        self.assertTrue(q.origin == 'mobile-maps-searchnearby-text')

        parsed_url = urlparse(q.url)
        parsed_cgi = parse_qs(parsed_url.query)
        self.assertTrue(parsed_cgi['text'] == ['cafe'])
        self.assertTrue(parsed_cgi['ll'] == ['28.96,41.03'])
        self.assertTrue(parsed_cgi['spn'] == ['1,1'])
        self.assertTrue(parsed_cgi['lang'] == ['tr_TR'])

def count_objects_of_type(objects, type):
    return sum(isinstance(obj, type) for obj in objects)

class TestParsers(unittest.TestCase):
    def test_geov_parser(self):
        with_wiki_serp = geov_parser.parse_geov(open('tests_data/geov_json_with_wiki').read())
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.Document) == 6)
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.BusinessDocument) == 1)
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.WikimapDocument) == 5)

        with_geoshard_serp = geov_parser.parse_geov(open('tests_data/geov_json_with_geoshard').read())
        self.assertTrue(count_objects_of_type(with_geoshard_serp.docs, ranking.common.Document) == 6)
        self.assertTrue(count_objects_of_type(with_geoshard_serp.docs, ranking.common.BusinessDocument) == 6)

        empty_serp = geov_parser.parse_geov(open('tests_data/geov_json_empty').read())
        self.assertTrue(len(empty_serp.docs) == 0)

    def test_emulator_parser(self):
        with_wiki_serp = emulator_parser.parse_emulator(open('tests_data/emulator_xml_with_wiki').read())
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.Document) == 6)
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.BusinessDocument) == 1)
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.WikimapDocument) == 5)

        with_geoshard_serp = emulator_parser.parse_emulator(open('tests_data/emulator_xml_with_geoshard').read())
        self.assertTrue(count_objects_of_type(with_geoshard_serp.docs, ranking.common.Document) == 11)
        self.assertTrue(count_objects_of_type(with_geoshard_serp.docs, ranking.common.BusinessDocument) == 7)
        self.assertTrue(count_objects_of_type(with_geoshard_serp.docs, ranking.common.GeoshardDocument) == 4)

        geocoder_serp = emulator_parser.parse_emulator(open('tests_data/emulator_xml_geocoder').read())
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.Document) == 6)
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.BusinessDocument) == 1)
        self.assertTrue(count_objects_of_type(with_wiki_serp.docs, ranking.common.WikimapDocument) == 5)

        empty_serp = emulator_parser.parse_emulator(open('tests_data/emulator_xml_empty').read())
        self.assertTrue(len(empty_serp.docs) == 0)

class TestComplex(unittest.TestCase):
    def test(self):
       import generate
       import check_results

       generate.main(['--source=tests_data/test_input', '--out_dir=test_output', '--debug_level=1'])
       check_results.main(['--source_dir=test_output', '--debug_level=1'])
       shutil.rmtree('test_output')

       generate.main(['--source=tests_data/test_input_pairs', '--out_dir=test_output', '--debug_level=1', '--pairs'])
       check_results.main(['--source_dir=test_output', '--debug_level=1'])
       shutil.rmtree('test_output')

if __name__ == '__main__':
    unittest.main()
