# -*- coding: utf-8 -*-

import mock
from travel.avia.library.python.common.utils.text import NBSP

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_currency


@mock.patch('django.utils.translation.get_language', return_value=u'ru')
class TestCurrencyFormatValueNegativeUsd(TestCase):
    """
    Для отрицательных значений долларов знак минуса должен выводиться перед знаком доллара, а не перед первой цифрой.
    "-$75" - корректно
    "$-75" - некорректно
    """

    def setUp(self):
        self.rur = create_currency(name=u'доллары', code='USD',
                                   template=u'$%d.%02d',
                                   template_whole=u'$%d',
                                   template_cents=u'$0.%02d')

    def test_format_0_05(self, m_get_language):
        assert self.rur.format_value(-0.05, show_cents=True) == u'-$0.05'
        assert self.rur.format_value(-0.05, show_cents=False) == u'-$0.05'

    def test_format_25_35(self, m_get_language):
        assert self.rur.format_value(-25.35, show_cents=True) == u'-$25.35'
        assert self.rur.format_value(-25.35, show_cents=False) == u'-$25.35'

    def test_format_75_00(self, m_get_language):
        assert self.rur.format_value(-75, show_cents=True) == u'-$75'
        assert self.rur.format_value(-75, show_cents=False) == u'-$75'

    def test_format_10500100500_55(self, m_get_language):
        assert self.rur.format_value(-10500100500, show_cents=True) == u'-$10{0}500{0}100{0}500'.format(NBSP)
        assert self.rur.format_value(-10500100500, show_cents=False) == u'-$10{0}500{0}100{0}500'.format(NBSP)
