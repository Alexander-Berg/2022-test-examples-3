# -*- coding: utf-8 -*-

import unittest

from market.idx.snippets.src.snippet import snippet_make, snippet_split


def _make_parse(url, props):
    return snippet_split(snippet_make(url, props))


_url_props = [
    ('a.ru', [('a', '1='), ('b', '2')]),
    ('b.ru', [('a', '11;'), ('b', '22')]),
]


class TestSnippet(unittest.TestCase):
    def test(self):
        for url, props in _url_props:
            self.assertEqual(_make_parse(url, props), (url, props))
