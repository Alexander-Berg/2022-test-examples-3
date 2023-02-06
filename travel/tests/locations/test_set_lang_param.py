# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from collections import OrderedDict

import six
from django.utils.http import urlencode

from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.utils_db.locations import set_lang_param


class TestSetLangParam(TestCase):
    def test_ru(self):
        query = 'foo=bar&big=bone&foo=baz'
        assert set_lang_param(query, 'ru', 'ru') == 'foo=bar&big=bone&foo=baz'

    def test_ua_uk(self):
        query = 'foo=bar&big=bone&foo=baz'
        assert set_lang_param(query, 'uk', 'ua') == 'foo=bar&big=bone&foo=baz'

    def test_ua_ru(self):
        query = 'foo=bar&big=bone&foo=baz'
        assert set_lang_param(query, 'ru', 'ua') == 'foo=bar&big=bone&foo=baz&lang=ru'

    def test_ua_ru_dict(self):
        query = OrderedDict([('foo', ['bar', 'baz']), ('big', 'bone')])
        assert set_lang_param(query, 'ru', 'ua') == 'foo=bar&foo=baz&big=bone&lang=ru'

    def test_change_lang_ru_to_uk(self):
        query = 'foo=bar&big=bone&foo=baz&lang=ru'
        assert set_lang_param(query, 'uk', 'ua') == 'foo=bar&big=bone&foo=baz'

    def test_unicode(self):
        query = {'unicode': u'я'}
        assert set_lang_param(query, 'ru', 'ru') == urlencode(query)

        query = urlencode({'unicode': u'я'})
        assert set_lang_param(query, 'ru', 'ru') == query

        query = six.text_type(urlencode({'unicode': u'я'}))
        assert set_lang_param(query, 'ru', 'ru') == query
