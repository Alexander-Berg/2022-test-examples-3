# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.tester.factories import create_currency


@pytest.mark.dbuser
def test_unit():
    bitcoin = create_currency(code='BTC', template_whole='%d <unit>BTC</unit>')

    assert bitcoin.format_value(100) == '100 BTC'

    assert bitcoin.format_value(100, unit_formatter='unit({})'.format) == '100 unit(BTC)'
