# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.library.python.common23.tester.factories import create_currency
from travel.rasp.library.python.common23.tester.testcase import TestCase


@mock.patch('django.utils.translation.get_language', return_value=u'tr')
class TestCurrencyFormatValueTurkish(TestCase):
    def setUp(self):
        self.rur = create_currency(name=u'рубли', code='RUR',
                                   template=u'%d rub. %d kop.',
                                   template_whole=u'%d rub.',
                                   template_cents=u'%d kop.',
                                   template_tr=u'%d.%02d r',
                                   template_whole_tr=u'%d r',
                                   template_cents_tr=u'0.%02d r')

    def test_format_0_05(self, m_get_language):
        assert self.rur.format_value(0.05, show_cents=True) == u'0.05 r'
        assert self.rur.format_value(0.05, show_cents=False) == u'0.05 r'

    def test_format_25_35(self, m_get_language):
        assert self.rur.format_value(25.35, show_cents=True) == u'25.35 r'
        assert self.rur.format_value(25.35, show_cents=False) == u'25.35 r'

    def test_format_75_00(self, m_get_language):
        assert self.rur.format_value(75, show_cents=True) == u'75 r'
        assert self.rur.format_value(75, show_cents=False) == u'75 r'
