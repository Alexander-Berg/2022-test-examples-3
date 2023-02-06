# -*- coding: utf-8 -*-

from django.utils.http import urlquote

from travel.avia.library.python.common.models_abstract.schedule import SaleURLFormatter


def test_sale_url_formatter():
    assert urlquote(u'Москва') == SaleURLFormatter().format(u'{title_ru}', title_ru=u'Москва')
