# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, contains, has_properties

from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.core.models import TrainOrder
from travel.rasp.train_api.train_purchase.utils.pagination import MultipleQuerySetsPagination

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class PaginationRequestStub(object):
    def __init__(self, offset, limit):
        self.query_params = {
            'offset': offset,
            'limit': limit,
        }


@pytest.mark.parametrize('offset, limit, expected_train_numbers', [
    (0, 5, ['1', '2', '3', '4', '5']),
    (5, 5, ['6', '7', '8', '9', '10']),
    (10, 5, ['11', '12', '13', '14']),
])
def test_multiple_querysets_pagination(offset, limit, expected_train_numbers):
    paginator = MultipleQuerySetsPagination()
    request = PaginationRequestStub(offset, limit)

    for i in range(1, 6):
        TrainOrderFactory(status=OrderStatus.RESERVED, train_number='{}'.format(i))
    for i in range(6, 9):
        TrainOrderFactory(status=OrderStatus.DONE, train_number='{}'.format(i))
    for i in range(9, 15):
        TrainOrderFactory(status=OrderStatus.CANCELLED, train_number='{}'.format(i))

    queryset_1 = TrainOrder.objects.filter(status=OrderStatus.RESERVED)
    queryset_2 = TrainOrder.objects.filter(status=OrderStatus.DONE)
    queryset_3 = TrainOrder.objects.filter(status=OrderStatus.CANCELLED)

    result = paginator.paginate_querysets([queryset_1, queryset_2, queryset_3], request)
    assert_that(result, contains(*[has_properties(train_number=n) for n in expected_train_numbers]))


@pytest.mark.parametrize('offset, limit, expected_train_numbers', [
    (0, 5, ['1', '2', '3', '4', '5']),
    (5, 5, ['6', '7', '8', '9', '10']),
    (10, 5, ['11', '12', '13', '14']),
])
def test_multiple_lists_pagination(offset, limit, expected_train_numbers):
    paginator = MultipleQuerySetsPagination()
    request = PaginationRequestStub(offset, limit)
    set_1 = [TrainOrderFactory(status=OrderStatus.RESERVED, train_number='{}'.format(i)) for i in range(1, 6)]
    set_2 = [TrainOrderFactory(status=OrderStatus.DONE, train_number='{}'.format(i)) for i in range(6, 9)]
    set_3 = [TrainOrderFactory(status=OrderStatus.CANCELLED, train_number='{}'.format(i)) for i in range(9, 15)]

    result = paginator.paginate_querysets((set_1, set_2, set_3), request)
    assert_that(result, contains(*[has_properties(train_number=n) for n in expected_train_numbers]))
