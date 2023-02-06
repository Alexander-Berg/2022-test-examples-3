# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import logging
import unittest

import yatest.common

from search.geo.tools.ranking import tsd_parser
from search.geo.tools.ranking.common import BUSINESS, GEOCODER, WIKI, RUSSIA, TURKEY
from search.geo.tools.ranking.tsd_recognizers import recognize, has_chars_from_lang
from search.geo.tools.ranking.tsd_recognizers import RecordNotRecognizedError


class SampleParser(tsd_parser.TsdParser):
    def __init__(self):
        super(SampleParser, self).__init__()
        self.recs = []

    def _process_record(self, record):
        try:
            recognizer = recognize(record)
        except RecordNotRecognizedError:
            logging.exception('Not recognized: %s', record)
            recognizer = None
        self.recs.append((record, recognizer))


class TestTsdRecognizers(unittest.TestCase):
    def test_has_chars_from_lang(self):
        self.assertTrue(has_chars_from_lang('Ресторан McDonald’s', 'ru'))
        self.assertTrue(has_chars_from_lang('Şehir Işıkları Reklam', 'tr'))
        self.assertFalse(has_chars_from_lang('Yandex', 'ru'))
        self.assertFalse(has_chars_from_lang('Yandex', 'tr'))

    def test_sample_tsd_recognizing(self):
        parser = SampleParser()
        with open(yatest.common.test_source_path('geosearch-sample-attraction-dump.tsd')) as file:
            parser.parse(file)

        expected = [
            ('Белорусский Государственный университет', BUSINESS, '1125639111', RUSSIA),
            ('Лицей БГУ ГУО', BUSINESS, '1112678425', RUSSIA),
            None,  # Google Maps
            ('Metro Cash & Carry', BUSINESS, '1076214736', RUSSIA),
            ('Metro Cash', GEOCODER, 'metro cash#37.9,55.7', RUSSIA),
            ('центр оптово-розничной торговли «Международный»', WIKI, '37493049', RUSSIA),
            ('Беларусь, Минская область, Минск', GEOCODER, 'беларусь, минская область, минск#27.6,53.9', RUSSIA),
            ('İstanbul', GEOCODER, 'istanbul#29.2,41.0', TURKEY),
            ('Nil Eczanesi', BUSINESS, '1106505652', TURKEY),
            ('Минск', GEOCODER, 'минск#27.6,53.9', RUSSIA)
        ]

        self.assertEqual(len(expected), len(parser.recs))

        for rec, exp in zip(parser.recs, expected):
            record, recognizer = rec
            if exp is None:
                self.assertTrue(recognizer is None)
            else:
                self.assertEqual(exp[0], record.title)
                self.assertEqual(exp[1], recognizer.get_source_name())
                # TODO(py2 vs py3 incompatible) self.assertEqual(exp[2], recognizer.get_signature())
                self.assertEqual(exp[3], recognizer.get_formula_region())
