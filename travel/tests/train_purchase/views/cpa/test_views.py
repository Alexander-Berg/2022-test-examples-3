# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest
from bson import ObjectId
from django.core.urlresolvers import reverse
from rest_framework import status

from travel.rasp.library.python.common23.date.environment import now_utc
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, TrainRefundFactory
from travel.rasp.train_api.train_purchase.core.models import RefundStatus
from travel.rasp.train_api.train_purchase.views.cpa.views import SHIFT_FROM_CREATION

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def test_orders(async_urlconf_client):
    now = now_utc()
    TrainOrderFactory(train_number='1', id=ObjectId.from_datetime(now - SHIFT_FROM_CREATION - timedelta(seconds=1)))
    TrainOrderFactory(train_number='2', id=ObjectId.from_datetime(now - SHIFT_FROM_CREATION - timedelta(seconds=2)))
    TrainOrderFactory(train_number='3', id=ObjectId.from_datetime(now - SHIFT_FROM_CREATION - timedelta(seconds=3)))
    TrainOrderFactory(train_number='4', id=ObjectId.from_datetime(now - SHIFT_FROM_CREATION - timedelta(seconds=4)))
    TrainOrderFactory(train_number='5', id=ObjectId.from_datetime(now - SHIFT_FROM_CREATION - timedelta(seconds=5)))

    response = async_urlconf_client.get(reverse('cpa_orders'), {'limit': 3, 'offset': 0})
    response2 = async_urlconf_client.get(reverse('cpa_orders'), {'limit': 3, 'offset': 3})

    assert response.status_code == status.HTTP_200_OK
    assert response2.status_code == status.HTTP_200_OK
    numbers = [o['train_number'] for o in response.data['results']['orders']]
    numbers2 = [o['train_number'] for o in response2.data['results']['orders']]
    assert len(numbers) == 3
    assert len(numbers2) == 2
    assert set(numbers + numbers2) == {'1', '2', '3', '4', '5'}


def test_orders_time_limits(async_urlconf_client):
    TrainOrderFactory(train_number='1', id=ObjectId.from_datetime(datetime(2019, 1, 1)))
    TrainOrderFactory(train_number='3', id=ObjectId.from_datetime(datetime(2019, 3, 3)))
    TrainOrderFactory(train_number='5', id=ObjectId.from_datetime(datetime(2019, 5, 5)))

    response = async_urlconf_client.get(reverse('cpa_orders'),
                                        {'utc_from': '2019-03-02T23:59:59', 'utc_to': '2019-03-03T00:00:01'})

    assert response.status_code == status.HTTP_200_OK
    numbers = [o['train_number'] for o in response.data['results']['orders']]
    assert numbers == ['3']


def test_orders_time_shift_from_creation(async_urlconf_client):
    now = now_utc()
    TrainOrderFactory(train_number='1', id=ObjectId.from_datetime(now))
    TrainOrderFactory(train_number='2', id=ObjectId.from_datetime(now - SHIFT_FROM_CREATION - timedelta(seconds=1)))

    response = async_urlconf_client.get(reverse('cpa_orders'))

    assert response.status_code == status.HTTP_200_OK
    numbers = [o['train_number'] for o in response.data['results']['orders']]
    assert numbers == ['2']


def test_orders_with_refunds(async_urlconf_client):
    TrainRefundFactory(status=RefundStatus.DONE,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '1'}})
    TrainRefundFactory(status=RefundStatus.DONE,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '2'}})
    TrainRefundFactory(status=RefundStatus.DONE,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '3'}})
    TrainRefundFactory(status=RefundStatus.DONE,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '4'}})
    TrainRefundFactory(status=RefundStatus.DONE,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '5'}})

    response = async_urlconf_client.get(reverse('cpa_orders_with_refunds'), {'limit': 3, 'offset': 0})
    response2 = async_urlconf_client.get(reverse('cpa_orders_with_refunds'), {'limit': 3, 'offset': 3})

    assert response.status_code == status.HTTP_200_OK
    assert response2.status_code == status.HTTP_200_OK
    numbers = [o['train_number'] for o in response.data['results']['orders']]
    numbers2 = [o['train_number'] for o in response2.data['results']['orders']]
    assert len(numbers) == 3
    assert len(numbers2) == 2
    assert set(numbers + numbers2) == {'1', '2', '3', '4', '5'}


def test_orders_with_refunds_status(async_urlconf_client):
    TrainRefundFactory(status=RefundStatus.DONE,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '1'}})
    TrainRefundFactory(status=RefundStatus.NEW,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '2'}})
    TrainRefundFactory(status=RefundStatus.FAILED,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '3'}})
    TrainRefundFactory(status=RefundStatus.PARTNER_REFUND_UNKNOWN,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '4'}})
    TrainRefundFactory(status=RefundStatus.PARTNER_REFUND_DONE,
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '5'}})

    response = async_urlconf_client.get(reverse('cpa_orders_with_refunds'))

    assert response.status_code == status.HTTP_200_OK
    numbers = [o['train_number'] for o in response.data['results']['orders']]
    assert set(numbers) == {'1', '5'}


def test_orders_with_refunds_time_limits(async_urlconf_client):
    TrainRefundFactory(status=RefundStatus.DONE, created_at=datetime(2019, 1, 1),
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '1'}})
    TrainRefundFactory(status=RefundStatus.DONE, created_at=datetime(2019, 3, 3),
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '3'}})
    TrainRefundFactory(status=RefundStatus.DONE, created_at=datetime(2019, 5, 5),
                       factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '5'}})

    response = async_urlconf_client.get(reverse('cpa_orders_with_refunds'),
                                        {'utc_from': '2019-03-02T23:59:59', 'utc_to': '2019-03-03T00:00:01'})

    assert response.status_code == status.HTTP_200_OK
    numbers = [o['train_number'] for o in response.data['results']['orders']]
    assert numbers == ['3']
