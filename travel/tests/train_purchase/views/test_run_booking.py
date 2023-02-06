# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from django.test import Client
from hamcrest import assert_that, has_properties, has_entries

from travel.rasp.train_api.train_purchase.core.enums import InsuranceStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, TrainOrder
from travel.rasp.train_api.train_purchase.views.insurance import run_process
from travel.rasp.train_api.train_purchase.workflow.booking import TRAIN_BOOKING_PROCESS

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _create_order(insurance_enabled=True):
    return TrainOrderFactory(
        insurance_enabled=insurance_enabled,
        process={'suspended': True},
    )


@pytest.fixture
def m_run_process():
    with mock.patch.object(run_process, 'apply_async', autospec=True, return_value=None) as m_run_process:
        yield m_run_process


@pytest.mark.parametrize('with_insurance, expected_status', [
    ('true', InsuranceStatus.ACCEPTED),
    ('false', InsuranceStatus.DECLINED),
])
def test_run_booking_ok(m_run_process, with_insurance, expected_status):
    order = _create_order()
    response = Client().get('/ru/api/train-purchase/orders/{}/run_booking/?with_insurance={}'
                            .format(order.uid, with_insurance))
    order.reload()

    assert response.status_code == 200
    assert_that(order, has_properties(
        insurance=has_properties(status=expected_status),
        process=has_entries(suspended=False),
    ))
    m_run_process.assert_called_once_with([TRAIN_BOOKING_PROCESS, str(order.id), {'order_uid': order.uid}])


@mock.patch.object(TrainOrder, 'update', side_effect=[Exception('Boom')])
def test_run_booking_failed(m_run_process):
    order = _create_order()
    response = Client().get('/ru/api/train-purchase/orders/{}/run_booking/?with_insurance=true'
                            .format(order.uid))

    assert response.status_code == 500


def test_run_process_in_run_booking_failed(m_run_process):
    m_run_process.side_effect = [Exception('Boom but it is ok')]
    order = _create_order()
    response = Client().get('/ru/api/train-purchase/orders/{}/run_booking/?with_insurance=true'
                            .format(order.uid))
    order.reload()

    assert response.status_code == 200
    assert_that(order, has_properties(
        insurance=has_properties(status=InsuranceStatus.ACCEPTED),
        process=has_entries(suspended=False),
    ))


def test_run_booking_invalid_query(m_run_process):
    order = _create_order()
    response = Client().get('/ru/api/train-purchase/orders/{}/run_booking/?with_insurance=something_invalid'
                            .format(order.uid))

    assert response.status_code == 400
    assert 'with_insurance' in response.data
