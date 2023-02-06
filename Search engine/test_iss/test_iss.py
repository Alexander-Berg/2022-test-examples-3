# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from library.python import resource

import ujson

from search.proto.iss import iss_pb2

from search.martylib.iss import load_dump, parse_dump, BadDump
from search.martylib.test_utils import TestCase


DUMP = {
    'configurationId': 'martylib#test',
    'properties': {
        'tags': 'one two three',
        'foo': 'bar',
    },
}

DUMP_NO_TAGS = {
    'configurationId': 'martylib#test',
}


class TestLoadDump(TestCase):
    def test_raises(self):
        with self.assertRaises(BadDump):
            load_dump('does_not_exist', safe=False)


class TestParseDump(TestCase):
    def test(self):
        expected = iss_pb2.IssDump(
            configuration_family='martylib',
            configuration_name='test',
            tags=['one', 'two', 'three'],
            properties={
                'foo': 'bar',
            },
        )
        actual = parse_dump(DUMP)
        self.assertEqual(actual, expected)

    def test_raises(self):
        for data in (
            {},
            {'configurationId': ''},
            {'configurationId': '#'},
            {'configurationId': 'test'},
            {'configurationId': 'test#'},
        ):
            with self.assertRaises(BadDump):
                parse_dump(data, safe=False)

    def test_works_without_tags(self):
        parse_dump(DUMP_NO_TAGS)

    # noinspection SpellCheckingInspection
    def test_gencfg_dump(self):
        dump_dict = ujson.loads(resource.find('gencfg-dump.json').decode('utf-8'))
        dump = parse_dump(dump_dict)
        self.assertEqual(dump.hostname, 'sas1-0469.search.yandex.net')
        self.assertEqual(dump.port, 32046)
        self.assertEqual(dump.container_name, '')

    def test_yp_dump(self):
        dump_dict = ujson.loads(resource.find('yp-dump.json').decode('utf-8'))
        dump = parse_dump(dump_dict)
        self.assertEqual(dump.hostname, 'sas3-7190.search.yandex.net')
        self.assertEqual(dump.port, 80)
        self.assertEqual(dump.container_name, 'horizon-production-2')

    def test_nanny_snapshot_detection(self):
        dump_dict = ujson.loads(resource.find('yp-dump.json').decode('utf-8'))
        dump = parse_dump(dump_dict)
        self.assertEqual(dump.nanny_snapshot_id, '40ecd4bc4823c63faa74a82adefa7f3d86bccc8f')
