# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.test import Client
from hamcrest import anything, assert_that, has_entries

from common.tester.matchers import has_json
from travel.rasp.train_api.train_purchase.core.factories import UserInfoFactory
from travel.rasp.train_api.train_purchase.views.test_utils import create_order

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]


def test_bad_params_order_num():
    response = Client().get('/ru/api/train-purchase/orders/find/')

    assert response.status_code == 400
    assert_that(response.content,
                has_json(has_entries('errors', has_entries({'order_num': anything()}))))


def test_bad_params_phone_email():
    response = Client().get('/ru/api/train-purchase/orders/find/', {'order_num': '123456'})

    assert response.status_code == 400
    assert_that(response.content, has_json(has_entries({'errors': anything()})))


def test_not_found():
    response = Client().get('/ru/api/train-purchase/orders/find/', {'order_num': '123456', 'email': 'user@example.org'})

    assert response.status_code == 404
    assert_that(response.content, has_json(has_entries(errors=has_entries(order=anything()))))


def test_found_by_email():
    order = create_order(partner_data={'order_num': '123456'}, user_info=UserInfoFactory(email='user@example.org'))
    # проверка нечувствительности почты к регистру
    response = Client().get('/ru/api/train-purchase/orders/find/', {'order_num': '123456', 'email': 'User@example.org'})

    assert response.status_code == 200
    assert_that(response.content, has_json(has_entries('order', has_entries(uid=order.uid))))


def test_found_by_phone():
    order = create_order(partner_data={'order_num': '123456'}, user_info=UserInfoFactory(phone='899912345678'))
    response = Client().get('/ru/api/train-purchase/orders/find/', {'order_num': '123456', 'phone': '899912345678'})

    assert response.status_code == 200
    assert_that(response.content, has_json(has_entries('order', has_entries(uid=order.uid))))
