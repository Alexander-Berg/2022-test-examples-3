#!/usr/bin/env python
# -*- coding: utf-8 -*-

import yaml
import yatest.common as common
import unittest


class GeoTest(unittest.TestCase):
    def setUp(self):
        self.cfg = yaml.load(open(common.source_path('search/wizard/data/wizard/conf/service_configs/geo.yaml')))

    def get_section(self, name):
        section = [x for x in self.cfg['Sections'] if x.get('Name', '') == name]
        self.assertEqual(len(section), 1, 'Section names must be unique. Offending sections: {}'.format(name))
        return section[0]

    def impl_test_diff_two_sets(self, left_name, right_name, subsection, only_left, only_right):
        left = self.get_section(left_name)
        right = self.get_section(right_name)

        left_rules = set(left.get(subsection, []))
        right_rules = set(right.get(subsection, []))

        self.assertEqual(left_rules.difference(right_rules), only_left)
        self.assertEqual(right_rules.difference(left_rules), only_right)

    def test_geosearch(self):
        turned_optional = {'GeoTravel'}

        self.impl_test_diff_two_sets('', 'Geosearch', 'OptionalRules', set(), turned_optional)
        self.impl_test_diff_two_sets('', 'Geosearch', 'EnabledRules', turned_optional, set())

    def test_serp(self):
        turned_off = {'GeoAdv', 'GeoTravel'}

        self.impl_test_diff_two_sets('', 'Serp', 'OptionalRules', set(), set())
        self.impl_test_diff_two_sets('', 'Serp', 'EnabledRules', turned_off, set())

    def test_travel(self):
        turned_off = {'GeoAdv'}

        self.impl_test_diff_two_sets('', 'Travel', 'OptionalRules', set(), set())
        self.impl_test_diff_two_sets('', 'Travel', 'EnabledRules', turned_off, set())

    def test_maps(self):
        turned_off = {'GeoTravel'}

        self.impl_test_diff_two_sets('', 'Maps', 'OptionalRules', set(), set())
        self.impl_test_diff_two_sets('', 'Maps', 'EnabledRules', turned_off, set())

    def test_geocoder(self):
        turned_off = {'BusinessNav',
                      'CommercialMx',
                      'DirtyLang',
                      'DssmGeo',
                      'GeoRelev',
                      'GeoRelevPre',
                      'GeosearchStopwords',
                      'LightSyntax',
                      'OrgNav',
                      'PPO',
                      'PornQuery',
                      'PornoQuery',
                      'Rubrics',
                      'ShortReq',
                      'StopWords',
                      'SubquerySearch',
                      'Tovar',
                      'Transit',
                      'TurkAffix',
                      'GeoTravel',
                      'GeoAdv'}
        turned_off_optiional = {'ProximByPairFreq'}

        self.impl_test_diff_two_sets('', 'Geocoder', 'OptionalRules', turned_off_optiional, set())
        self.impl_test_diff_two_sets('', 'Geocoder', 'EnabledRules', turned_off, set())

    def test_serp_travel(self):
        turned_on = {'GeoTravel'}

        self.impl_test_diff_two_sets('Serp', 'Travel', 'OptionalRules', set(), set())
        self.impl_test_diff_two_sets('Serp', 'Travel', 'EnabledRules', set(), turned_on)
