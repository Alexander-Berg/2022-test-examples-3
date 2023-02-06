# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

import common
import travel.rasp.train_api.train_purchase.workflow.payment.check_payment
from common.data_api.billing.trust_client import (
    TrustPaymentStatuses, TrustClientInvalidStatus, TrustClientRequestError, TRUST_HOSTS_BY_ENVIRONMENT,
    TrustPaymentInfo
)
from common.settings.configuration import Configuration
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.date import environment
from common.utils.date import UTC_TZ
from travel.rasp.library.python.common23.date.environment import now_aware
from common.utils.yasmutil import Metric
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.payment import CheckPaymentEvents, CheckPayment

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]
PURCHASE_TOKEN = 'some_purchase_token'


@replace_setting('YASMS_DONT_SEND_ANYTHING', True)
def _process(payment):
    events = (CheckPaymentEvents.OK, CheckPaymentEvents.PENDING, CheckPaymentEvents.FAILED)
    return process_state_action(CheckPayment, events, payment)


@pytest.yield_fixture
def m_trust_client():
    with mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.check_payment,
                           'TrustClient', autospec=True) as m_trust_client:
        yield m_trust_client


@replace_now('2000-01-01 12:00:00')
@mock.patch.object(common.utils.yasmutil.YasmMetricSender, 'send_many')
def test_check_payment_ok(m_send_many, m_trust_client):
    payment_status = TrustPaymentStatuses.AUTHORIZED
    m_trust_client.return_value.get_payment_info.return_value = TrustPaymentInfo({
        'payment_status': payment_status.value
    })

    original_order = TrainOrderFactory(payments=[
        dict(purchase_token=PURCHASE_TOKEN, trust_created_at=environment.now_utc())
    ])
    event, payment = _process(original_order.current_billing_payment)
    assert event == CheckPaymentEvents.OK
    assert m_send_many.mock_calls[0] == mock.call(
        [
            Metric(name='payment_paid_cnt', value=1, suffix='ammm'),
            Metric(name='payment_paid_time', value=0.0, suffix='avvv')
        ]
    )
    assert payment.status == payment_status.value
    assert payment.hold_at == datetime(2000, 1, 1, 9)


@replace_now('2000-01-01 12:00:00')
@mock.patch.object(common.utils.yasmutil.YasmMetricSender, 'send_many')
def test_check_payment_ok_need_unhold(m_send_many, m_trust_client):
    payment_status = TrustPaymentStatuses.AUTHORIZED
    m_trust_client.return_value.get_payment_info.return_value = TrustPaymentInfo({
        'payment_status': payment_status.value
    })

    original_order = TrainOrderFactory(payments=[{'purchase_token': PURCHASE_TOKEN, 'immediate_return': True}])
    event, payment = _process(original_order.current_billing_payment)
    assert m_trust_client.mock_calls == [
        mock.call(),
        mock.call().get_payment_info(PURCHASE_TOKEN),
        mock.call(trust_host=TRUST_HOSTS_BY_ENVIRONMENT[Configuration.PRODUCTION]),
        mock.call().unhold_payment(PURCHASE_TOKEN)
    ]
    assert event == CheckPaymentEvents.OK
    assert payment.status == payment_status.value
    assert payment.hold_at == datetime(2000, 1, 1, 9)


@mock.patch.object(common.utils.yasmutil.YasmMetricSender, 'send_many')
def test_check_payment_failed(m_send_many, m_trust_client):
    payment_status = TrustPaymentStatuses.NOT_AUTHORIZED
    resp_code = 'not_enough_funds'
    resp_desc = 'Not enough funds, RC = 51'
    m_trust_client.return_value.get_payment_info.return_value = TrustPaymentInfo({
        'payment_status': payment_status.value,
        'payment_resp_code': resp_code,
        'payment_resp_desc': resp_desc,
    })

    original_order = TrainOrderFactory(payments=[dict(purchase_token=PURCHASE_TOKEN)])
    event, payment = _process(original_order.current_billing_payment)
    assert event == CheckPaymentEvents.FAILED
    assert payment.status == payment_status.value
    assert payment.resp_code == resp_code
    assert payment.resp_desc == resp_desc


@mock.patch.object(common.utils.yasmutil.YasmMetricSender, 'send_many')
def test_check_payment_outdated(m_send_many, m_trust_client):
    payment_status = TrustPaymentStatuses.NOT_AUTHORIZED
    m_trust_client.return_value.get_payment_info.return_value = TrustPaymentInfo({
        'payment_status': payment_status.value
    })

    original_order = TrainOrderFactory(
        payments=[
            dict(purchase_token=PURCHASE_TOKEN, trust_created_at=environment.now_utc())
        ],
        reserved_to=now_aware().astimezone(UTC_TZ))
    event, payment = _process(original_order.current_billing_payment)
    assert m_trust_client.call_count == 0
    assert event == CheckPaymentEvents.FAILED
    assert m_send_many.call_count == 1


def test_check_payment_pending(m_trust_client):
    payment_status = TrustPaymentStatuses.STARTED
    m_trust_client.return_value.get_payment_info.return_value = TrustPaymentInfo({
        'payment_status': payment_status.value
    })

    original_order = TrainOrderFactory(payments=[dict(purchase_token=PURCHASE_TOKEN)])
    event, payment = _process(original_order.current_billing_payment)
    assert event == CheckPaymentEvents.PENDING


@mock.patch.object(common.utils.yasmutil.YasmMetricSender, 'send_many')
def test_trust_client_connection_error_occurred(m_send_many, m_trust_client):
    payment_status = TrustPaymentStatuses.AUTHORIZED
    m_trust_client.return_value.get_payment_info.side_effect = [
        TrustClientRequestError('foo'),
        TrustPaymentInfo({'payment_status': payment_status.value})
    ]
    order = TrainOrderFactory(payments=[dict(purchase_token=PURCHASE_TOKEN)])

    event, payment = _process(order.current_billing_payment)
    assert event == CheckPaymentEvents.OK


@mock.patch.object(common.utils.yasmutil.YasmMetricSender, 'send_many')
def test_trust_client_invalid_status_occurred(m_send_many, m_trust_client):
    payment_status = TrustPaymentStatuses.AUTHORIZED
    m_trust_client.return_value.get_payment_info.side_effect = [
        TrustClientInvalidStatus('foo'),
        TrustPaymentInfo({'payment_status': payment_status.value})
    ]
    order = TrainOrderFactory(payments=[dict(purchase_token=PURCHASE_TOKEN)])

    event, payment = _process(order.current_billing_payment)
    assert event == CheckPaymentEvents.FAILED
