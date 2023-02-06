# -*- coding: utf-8 -*-
from extsearch.ymusic.indexer.libs.python.extensions.numeric import NumericExtensions


def test__no_extensions():
    extensions = NumericExtensions.extend_title('abc def')
    assert len(extensions) == 0


def test__single_extension():
    extensions = NumericExtensions.extend_title('sum 41')
    assert extensions == [u'сорок', u'один', u'4', u'четыре', u'1', u'forty', u'one', u'four']


def test__long_numeric():
    extensions = NumericExtensions.extend_title('12345678900000000000000000000000000000000000000000000000')
    assert len(extensions) == 10 * 3  # each digit as (digit, ru, en). total of 10 digits


def test__single_numeric_at_the_end():
    extensions = NumericExtensions.extend_title('playlist number 1')
    assert len(extensions) == 3


def test__superscript_numeric__not_a_numeric():
    extensions = NumericExtensions.extend_title('¹⁰⁰⁰')
    assert len(extensions) == 0
