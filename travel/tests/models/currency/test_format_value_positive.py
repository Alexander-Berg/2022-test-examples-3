# -*- coding: utf-8 -*-

import mock

from travel.avia.library.python.common.utils.text import NBSP

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_currency


@mock.patch('django.utils.translation.get_language', return_value=u'ru')
class TestCurrencyFormatValuePositive(TestCase):
    def setUp(self):
        self.rur = create_currency(name=u'рубли', code='RUR',
                                   template=u'%d rub. %d kop.',
                                   template_whole=u'%d rub.',
                                   template_cents=u'%d kop.',
                                   template_tr=u'%d.%02d r',
                                   template_whole_tr=u'%d r',
                                   template_cents_tr=u'0.%02d r')

    def test_format_0(self, m_get_langiage):
        assert self.rur.format_value(0.0, show_cents=True) == u'0 rub.'
        assert self.rur.format_value(0.0, show_cents=False) == u'0 rub.'

    def test_format_0_004(self, m_get_langiage):
        assert self.rur.format_value(0.004, show_cents=True) == u'0 rub.'
        assert self.rur.format_value(0.004, show_cents=False) == u'0 rub.'

    def test_format_0_006(self, m_get_langiage):
        assert self.rur.format_value(0.006, show_cents=True) == u'1 kop.'
        assert self.rur.format_value(0.006, show_cents=False) == u'1 kop.'

    def test_format_0_994(self, m_get_langiage):
        assert self.rur.format_value(0.994, show_cents=True) == u'99 kop.'
        assert self.rur.format_value(0.994, show_cents=False) == u'99 kop.'

    def test_format_0_996(self, m_get_langiage):
        assert self.rur.format_value(0.996, show_cents=True) == u'1 rub.'
        assert self.rur.format_value(0.996, show_cents=False) == u'1 rub.'

    def test_format_49_994(self, m_get_langiage):
        assert self.rur.format_value(49.994, show_cents=True) == u'49 rub. 99 kop.'
        assert self.rur.format_value(49.994, show_cents=False) == u'49 rub. 99 kop.'

    def test_format_49_996(self, m_get_langiage):
        assert self.rur.format_value(49.996, show_cents=True) == u'50 rub.'
        assert self.rur.format_value(49.996, show_cents=False) == u'50 rub.'

    def test_format_50_004(self, m_get_langiage):
        assert self.rur.format_value(50.004, show_cents=True) == u'50 rub.'
        assert self.rur.format_value(50.004, show_cents=False) == u'50 rub.'

    def test_format_50_006(self, m_get_langiage):
        assert self.rur.format_value(50.006, show_cents=True) == u'50 rub. 1 kop.'
        assert self.rur.format_value(50.006, show_cents=False) == u'50 rub.'

    def test_format_100500100500_45(self, m_get_langiage):
        assert self.rur.format_value(10500100500.45, show_cents=True) == u'10{0}500{0}100{0}500 rub. 45 kop.'.format(NBSP)
        assert self.rur.format_value(10500100500.45, show_cents=False) == u'10{0}500{0}100{0}500 rub.'.format(NBSP)
