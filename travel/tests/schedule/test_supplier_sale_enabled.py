# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.tester import transaction_context
from travel.rasp.library.python.common23.tester.factories import create_supplier


@pytest.fixture(scope='module')
@transaction_context.transaction_fixture
def supplier(request):
    return create_supplier(can_buy_ru=True)


def test_sale_enabled(supplier):
    assert supplier.is_sale_enabled('ru')
    assert not supplier.is_sale_enabled('tr')
    assert not supplier.is_sale_enabled('us')
