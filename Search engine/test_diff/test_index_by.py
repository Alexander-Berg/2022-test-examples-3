# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import collections

from search.martylib.test_utils import TestCase
from search.martylib.diff import index_by


class TestObject(object):
    def __init__(self, first_name, last_name=None, recurse=True):
        self.first_name = first_name
        self.last_name = last_name
        self.inner = None
        if recurse:
            self.inner = TestObject(first_name, last_name, recurse=False)

    def __repr__(self):
        return '{}(first_name={}, last_name={}, inner={})'.format(self.__class__.__name__, self.first_name, self.last_name, self.inner)


class TestIndexBy(TestCase):
    def test_by_attr(self):
        a = TestObject('foo')
        b = TestObject('bar')

        self.assertEqual(index_by([a, b], 'first_name'), {'foo': a, 'bar': b})

        with self.assertRaises(KeyError):
            index_by([a, b, a], 'first_name', ensure_unique_keys=True)

    def test_by_key(self):
        a = {'first_name': 'foo'}
        b = {'first_name': 'bar'}

        self.assertEqual(index_by([a, b], 'first_name', by_attr=False), {'foo': a, 'bar': b})

        with self.assertRaises(KeyError):
            index_by([a, b, a], 'first_name', by_attr=False, ensure_unique_keys=True)

    def test_composite_by_attr(self):
        a = TestObject('foo', 'FOO')
        b = TestObject('bar', 'BAR')

        self.assertEqual(
            index_by([a, b], ('first_name', 'last_name'), composite_key=True),
            {('foo', 'FOO'): a, ('bar', 'BAR'): b}
        )

        with self.assertRaises(KeyError):
            index_by([a, b, a], ('first_name', 'last_name'), composite_key=True, ensure_unique_keys=True)

    def test_composite_by_key(self):
        a = {'first_name': 'foo', 'last_name': 'FOO'}
        b = {'first_name': 'bar', 'last_name': 'BAR'}

        self.assertEqual(
            index_by([a, b], ('first_name', 'last_name'), by_attr=False, composite_key=True),
            {('foo', 'FOO'): a, ('bar', 'BAR'): b}
        )

        with self.assertRaises(KeyError):
            index_by([a, b, a], ('first_name', 'last_name'), by_attr=False, composite_key=True, ensure_unique_keys=True)

    def test_recursive_by_attr(self):
        a = TestObject('foo')
        b = TestObject('bar')

        self.assertEqual(index_by([a, b], 'inner.first_name'), {'foo': a, 'bar': b})

        with self.assertRaises(KeyError):
            index_by([a, b, a], 'inner.first_name', ensure_unique_keys=True)

    def test_recursive_by_key(self):
        a = {'inner': {'first_name': 'foo'}}
        b = {'inner': {'first_name': 'bar'}}

        self.assertEqual(index_by([a, b], 'inner.first_name', by_attr=False), {'foo': a, 'bar': b})

        with self.assertRaises(KeyError):
            index_by([a, b, a], 'inner.first_name', by_attr=False, ensure_unique_keys=True)

    def test_recursive_composite_by_attr(self):
        a = TestObject('foo', 'FOO')
        b = TestObject('bar', 'BAR')

        self.assertEqual(
            index_by([a, b], ('inner.first_name', 'inner.last_name'), composite_key=True),
            {('foo', 'FOO'): a, ('bar', 'BAR'): b}
        )

        with self.assertRaises(KeyError):
            index_by([a, b, a], ('inner.first_name', 'inner.last_name'), composite_key=True, ensure_unique_keys=True)

    def test_recursive_composite_by_key(self):
        a = {'inner': {'first_name': 'foo', 'last_name': 'FOO'}}
        b = {'inner': {'first_name': 'bar', 'last_name': 'BAR'}}

        self.assertEqual(
            index_by([a, b], ('inner.first_name', 'inner.last_name'), by_attr=False, composite_key=True),
            {('foo', 'FOO'): a, ('bar', 'BAR'): b}
        )

        with self.assertRaises(KeyError):
            index_by([a, b, a], ('inner.first_name', 'inner.last_name'), by_attr=False, composite_key=True, ensure_unique_keys=True)

    def test_multiple_values(self):
        a = TestObject('foo', '1')
        b = TestObject('bar', '2')
        c = TestObject('foo', '3')

        expected = collections.defaultdict(list)
        expected.update({'foo': [a, c], 'bar': [b]})

        self.assertEqual(index_by([a, b, c], 'first_name', multiple_values=True), expected)

        expected = collections.defaultdict(list)
        expected.update({'foo': [a], 'bar': [b]})

        self.assertEqual(index_by([a, b], 'first_name', ensure_unique_keys=True, multiple_values=True), expected)

        with self.assertRaises(KeyError):
            index_by([a, b, c], 'first_name', multiple_values=True, ensure_unique_keys=True)

    def test_external_container(self):
        a = TestObject('foo', '1')
        b = TestObject('bar', '2')
        c = {}

        index_by([a, b], 'first_name', container=c)

        self.assertEqual(c, {'foo': a, 'bar': b})

    def test_unsupported_container(self):
        with self.assertRaises(ValueError):
            index_by([], 'key', container={}, multiple_values=True)

    def test_defaultdict_set(self):
        a = TestObject('foo', '1')
        b = TestObject('bar', '2')
        c = collections.defaultdict(set)

        expected = collections.defaultdict(set)
        expected['foo'].add(a)
        expected['bar'].add(b)

        self.assertEqual(index_by([a, b], 'first_name', container=c, multiple_values=True), expected)

    def test_defaultdict_set_composite_key(self):
        a = TestObject('foo', '1')
        b = TestObject('bar', '2')
        c = collections.defaultdict(set)

        expected = collections.defaultdict(set)
        expected[('foo', '1')].add(a)
        expected[('bar', '2')].add(b)

        self.assertEqual(index_by([a, b], ('first_name', 'last_name'), container=c, multiple_values=True, composite_key=True), expected)

    def test_defaultdict_set_recursive_composite_key(self):
        a = TestObject('foo', '1')
        b = TestObject('bar', '2')
        c = collections.defaultdict(set)

        expected = collections.defaultdict(set)
        expected[('foo', '1')].add(a)
        expected[('bar', '2')].add(b)

        self.assertEqual(index_by([a, b], ('inner.first_name', 'inner.last_name'), container=c, multiple_values=True, composite_key=True), expected)
