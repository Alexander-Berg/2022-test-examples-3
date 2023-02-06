# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from django.test import Client
from hamcrest import assert_that, has_entry

from common.tester.matchers import has_json
from travel.rasp.train_api.train_purchase import views
from travel.rasp.train_api.train_purchase.views.test_utils import create_order


def test_order_not_found():
    response = Client().post('/ru/api/train-purchase/orders/12345/cancel/')

    assert response.status_code == 404
    assert_that(
        response.content,
        has_json(has_entry('errors', has_entry('uid', 'Order was not found')))
    )


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_order_cancelled():
    order = create_order()
    with mock.patch.object(views, 'send_event_to_payment') as m_send_event_to_payment:
        response = Client().post('/ru/api/train-purchase/orders/{}/cancel/'.format(order.uid))

        assert m_send_event_to_payment.called
        assert response.status_code == 200
        assert_that(
            response.content,
            has_json(has_entry('ok', True))
        )
