# -*- coding: utf-8 -*-

import pytest

from travel.avia.library.python.tester import transaction_context
from travel.avia.library.python.tester.factories import create_supplier


@pytest.fixture(scope='module')
@transaction_context.transaction_fixture
def supplier(request):
    return create_supplier(can_buy_ru=True)


def test_sale_enabled(supplier):
    assert supplier.is_sale_enabled('ru')
    assert not supplier.is_sale_enabled('tr')
    assert not supplier.is_sale_enabled('us')
