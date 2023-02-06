# coding: utf-8

from __future__ import division
from __future__ import unicode_literals

from search.martylib.http import urljoin
from search.martylib.test_utils import TestCase


class TestHttpUtils(TestCase):
    def test_urljoin(self):
        self.assertEqual(
            urljoin('https://yandex.ru', 'foo', 'bar'),
            'https://yandex.ru/foo/bar'
        )

    def test_urljoin_with_trailing_slash(self):
        self.assertEqual(
            urljoin('https://yandex.ru', 'foo', 'bar/'),
            'https://yandex.ru/foo/bar/'
        )

    def test_urljoin_with_starting_slash(self):
        self.assertEqual(
            urljoin('https://yandex.ru', '/foo', 'bar'),
            'https://yandex.ru/foo/bar'
        )

    def test_urljoin_with_several_starting_slashes(self):
        self.assertEqual(
            urljoin('https://yandex.ru//', '///foo/bar', '//bar'),
            'https://yandex.ru/foo/bar/bar'
        )

    def test_urljoin_for_uri(self):
        self.assertEqual(
            urljoin('/foo', 'bar'),
            '/foo/bar'
        )
        self.assertEqual(
            urljoin('/foo', '/bar'),
            '/foo/bar'
        )
        self.assertEqual(
            urljoin('/foo', 'bar/'),
            '/foo/bar/'
        )

    def test_urljoin_file_schema(self):
        self.assertEqual(
            urljoin('file:foo', 'bar/'),
            'file:///foo/bar/'
        )
        self.assertEqual(
            urljoin('file:foo', 'bar'),
            'file:///foo/bar'
        )
