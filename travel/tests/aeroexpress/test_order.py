# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_entries

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting

from travel.rasp.suburban_selling.selling.aeroexpress.order import AeroexpressServiceFactory
from travel.rasp.suburban_selling.selling.order.helpers import ForbiddenOrderError

pytestmark = [pytest.mark.dbuser]


def test_aeroexpress_service_raise_if_order_forbidden():
    with replace_dynamic_setting('SUBURBAN_SELLING__AEROEX_ORDER_ENABLED', False):
        with pytest.raises(ForbiddenOrderError) as ex:
            AeroexpressServiceFactory().raise_if_order_forbidden()
        assert ex.value.message == 'Aeroexpress order is forbidden by the setting SUBURBAN_SELLING__AEROEX_ORDER_ENABLED'

    with replace_dynamic_setting('SUBURBAN_SELLING_ENABLED', False):
        with pytest.raises(ForbiddenOrderError) as ex:
            AeroexpressServiceFactory().raise_if_order_forbidden()
        assert ex.value.message == 'Aeroexpress order is forbidden by the setting SUBURBAN_SELLING_ENABLED'

    with replace_dynamic_setting('SUBURBAN_SELLING__AEROEX_ORDER_ENABLED', True):
        with replace_dynamic_setting('SUBURBAN_SELLING_ENABLED', True):
            AeroexpressServiceFactory().raise_if_order_forbidden()


@replace_now(datetime(2022, 2, 18, 2))
def test_aeroexpress_service_make_book_data():
    order_data = {
        'book_data': {
            'menu_id': 11,
            'order_type': 22
        },
        'departure_date': '2022-02-18'
    }

    book_data = AeroexpressServiceFactory().make_book_data(order_data)

    assert_that(book_data.to_json(), has_entries({
        'menu_id': 11,
        'order_type': 22,
        'date': '2022-02-18'
    }))
