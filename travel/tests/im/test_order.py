# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_entries

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting

from travel.rasp.suburban_selling.selling.im.order import ImServiceFactory
from travel.rasp.suburban_selling.selling.order.helpers import ForbiddenOrderError, WrongOrderDataError


def test_im_service_raise_if_order_forbidden():
    with replace_dynamic_setting('SUBURBAN_SELLING__IM_ORDER_ENABLED', False):
        with pytest.raises(ForbiddenOrderError) as ex:
            ImServiceFactory().raise_if_order_forbidden()
        assert ex.value.message == 'IM order is forbidden by the setting SUBURBAN_SELLING__IM_ORDER_ENABLED'

    with replace_dynamic_setting('SUBURBAN_SELLING_ENABLED', False):
        with pytest.raises(ForbiddenOrderError) as ex:
            ImServiceFactory().raise_if_order_forbidden()
        assert ex.value.message == 'IM order is forbidden by the setting SUBURBAN_SELLING_ENABLED'

    with replace_dynamic_setting('SUBURBAN_SELLING__IM_ORDER_ENABLED', True):
        with replace_dynamic_setting('SUBURBAN_SELLING_ENABLED', True):
            ImServiceFactory().raise_if_order_forbidden()


@replace_now(datetime(2021, 8, 27, 8))
def test_im_service_make_book_data():
    order_data = {
        'book_data': {
            'date': '2021-08-27',
            'station_from_express_id': '1111',
            'station_to_express_id': '2222',
            'train_number': '333',
            'departure_dt': '2021-08-27T12:00:00',
            'departure_tz': 'Europe/Moscow',
            'im_provider': 'P9'
        }
    }
    book_data = ImServiceFactory().make_book_data(order_data)

    assert_that(book_data.to_json(), has_entries({
        'date': '2021-08-27',
        'station_from_express_id': 1111,
        'station_to_express_id': 2222,
        'train_number': '333',
        'im_provider': 'P9'
    }))

    order_data['book_data']['date'] = '2021-08-26'
    with pytest.raises(WrongOrderDataError):
        ImServiceFactory().make_book_data(order_data)

    order_data['book_data']['date'] = '2021-08-27'
    order_data['book_data']['departure_dt'] = '2021-08-27T02:00:00'
    with pytest.raises(WrongOrderDataError):
        ImServiceFactory().make_book_data(order_data)
