# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.utils.http import urlquote

from travel.rasp.library.python.common23.models.core.schedule.base_supplier import SaleURLFormatter


def test_sale_url_formatter():
    assert urlquote(u'Москва') == SaleURLFormatter().format(u'{title_ru}', title_ru=u'Москва')
