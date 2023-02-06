# encoding: UTF-8

import unittest

from ws_properties.conversion.service import StandardConversionService
from ws_properties.environ.environment import Environment
from ws_properties.environ.mapper import ValueMapper, MissingError, ListMapper, \
    ObjectMapper, get_mapper, Mapper
from ws_properties.environ.properties import DictPropertySource


class MapperTestCase(unittest.TestCase):
    def setUp(self):
        source = DictPropertySource({
            'int': 1,
            'int_invalid': 'abc',
            'int_list': ['1', '2', '3', '4'],
            'int_list_inline': '1,2,3,4',
            'int_list_inline_invalid': '1,2,3,abc',
            'int_list_empty': '',
            'complex_list': [
                {
                    'id': '1',
                },
                {
                    'id': '2',
                },
            ],
            'complex_list_invalid': [
                {
                    'id': '1',
                },
                {
                    'id': 'abc'
                },
            ],
            'complex_list_missing': [
                {
                    'id': '1',
                    'name': 'test1',
                },
                {
                    'name': 'test2',
                },
            ],
        })

        self.environemnt = Environment()
        self.environemnt.conversion_service = StandardConversionService()
        self.environemnt.property_sources.append(source)

    def _map(self, mapper, path):
        return mapper.map(self.environemnt, path)

    def test_mapper_map(self):
        with self.assertRaises(NotImplementedError):
            Mapper.map(ValueMapper(int), self.environemnt, 'test')

    def test_get_mapper_invalid(self):
        with self.assertRaisesRegexp(
                ValueError,
                'can not be transformed to mapper',
        ):
            get_mapper(1)

    def test_value_mapper(self):
        value = self._map(ValueMapper(int), 'int')

        self.assertEqual(value, 1)
        self.assertIsInstance(value, int)

    def test_value_mapper_invalid(self):
        with self.assertRaisesRegexp(ValueError, 'int_invalid'):
            self._map(ValueMapper(int), 'int_invalid')

    def test_value_mapper_missing(self):
        with self.assertRaises(MissingError):
            self._map(ValueMapper(int), 'int_nonexistent')

    def test_list_mapper(self):
        value = self._map(ListMapper(int), 'int_list')

        self.assertIsInstance(value, list)
        self.assertListEqual(
            value,
            [1, 2, 3, 4],
        )

    def test_list_mapper_insufficient_occurrence(self):
        with self.assertRaisesRegexp(MissingError, '\[4\]'):
            self._map(ListMapper(int, 5), 'int_list')

    def test_list_inline(self):
        value = self._map(ListMapper(int), 'int_list_inline')

        self.assertIsInstance(value, list)
        self.assertListEqual(
            value,
            [1, 2, 3, 4],
        )

    def test_list_mapper_inline_insufficient_occurrence(self):
        with self.assertRaisesRegexp(MissingError, '\[4\]'):
            self._map(ListMapper(int, 5), 'int_list_inline')

    def test_list_mapper_inline_invalid(self):
        with self.assertRaisesRegexp(ValueError,
                                     'int_list_inline_invalid\[3\]'):
            self._map(ListMapper(int), 'int_list_inline_invalid')

    def test_list_mapper_empty(self):
        self._map(ListMapper(int, 0), 'int_list_empty')

    def test_list_mapper_nonexistent(self):
        self._map(ListMapper('int', 0), 'int_list_nonexistent')

    def test_list_mapper_complex(self):
        value = self._map(
            ListMapper(ObjectMapper(id=ValueMapper(int))),
            'complex_list',
        )

        self.assertIsInstance(value, list)
        self.assertListEqual(
            value,
            [{'id': 1}, {'id': 2}],
        )

    def test_list_mapper_complex_insufficient_occurrence(self):
        with self.assertRaisesRegexp(MissingError, '\[2\]'):
            self._map(
                ListMapper(ObjectMapper(id=ValueMapper(int)), 3),
                'complex_list',
            )

    def test_list_mapper_complex_invalid(self):
        with self.assertRaisesRegexp(ValueError, '\[1\]'):
            self._map(
                ListMapper(ObjectMapper(id=ValueMapper(int))),
                'complex_list_invalid',
            )

    def test_list_mapper_complex_missing(self):
        with self.assertRaisesRegexp(ValueError, '\[1\]'):
            self._map(
                ListMapper(ObjectMapper(
                    id=ValueMapper(int),
                    name=ValueMapper(str),
                )),
                'complex_list_missing',
            )
