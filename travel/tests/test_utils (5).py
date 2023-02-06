# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from string import whitespace, punctuation

from travel.rasp.suggests_tasks.suggests.text_utils import keyboard_layout_variants, transliteration_variant, prepare_title_text
from travel.rasp.suggests_tasks.suggests.utils import split_values, get_multi, set_multi


def test_split_values():
    expected = {
        0: [1, 4],
        1: [2, 5],
        2: [3, 6],
        3: [None, 7],
        4: [None],
    }

    values = [1, 2, 3, None, None, 4, 5, 6, 7]
    assert split_values(values, 5) == expected

    values = [1]
    expected = {0: [1], 1: [], 2: []}
    assert split_values(values, 3) == expected

    values = []
    expected = {0: [], 1: [], 2: []}
    assert split_values(values, 3) == expected


def test_prepare_title_text():
    title = u'Я & % b #@    C'
    assert prepare_title_text(title) == u'я b c'

    for p in punctuation:
        for w in whitespace:
            assert prepare_title_text(u'{}a{}{}b{}'.format(p, w, w, p)) == u'a b'


def test_keyboard_layout_variants():
    text = u'djrpfk'
    assert keyboard_layout_variants(text) == {u'ru': [u'вокзал']}
    assert keyboard_layout_variants(text, [u'ru']) == {u'ru': [u'вокзал']}
    assert keyboard_layout_variants(text, [u'en']) == {}

    text = u'cscthnm'
    assert keyboard_layout_variants(text, [u'ru', u'uk']) == {u'ru': [u'сысерть'], u'uk': [u'сісерть']}

    text = u'ыефешщт'
    assert keyboard_layout_variants(text, [u'en']) == {u'en': [u'station', u'ыtation']}


def test_transliteration_variant():
    for lat, cyr in [(u'ekaterinburg', u'екатеринбург'),
                     (u'moskva', u'москва'),
                     (u'kiev', u'киев'),
                     (u'london', u'лондон')]:
        assert transliteration_variant(lat, u'ru') == cyr
        assert transliteration_variant(cyr, u'en') == lat


def test_set_multi():
    cache = {}
    keys = ['a', 'b', 'c', 'd']
    value = '1'
    set_multi(cache, keys, value)
    assert cache['a']['b']['c']['d'] == '1'

    del cache['a']['b']['c']['d']
    assert cache['a']['b']['c'] == {}


def test_get_multi():
    cache = {}
    keys = ['a', 'b', 'c', 'd']
    value = '1'
    set_multi(cache, keys, value)
    assert get_multi(cache, keys) == '1'
    assert get_multi(cache, ['z']) is None
    assert get_multi(cache, ['z'], default='nothing') == 'nothing'
