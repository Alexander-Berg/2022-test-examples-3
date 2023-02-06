# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import locale

from travel.rasp.bus.library.locale_setter import LocaleHolder, RuLocaleHolder


def test_ru_locale():
    default = locale.getlocale()
    with RuLocaleHolder():
        assert locale.getlocale() == ('ru_RU', 'UTF-8')
    assert locale.getlocale() == default


def test_custom_locale():
    default = locale.getlocale()
    with LocaleHolder(('en_US', 'UTF-8')):
        assert locale.getlocale() == ('en_US', 'UTF-8')
    assert locale.getlocale() == default
