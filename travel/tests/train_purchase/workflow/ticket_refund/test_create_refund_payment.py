# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, has_properties

import travel.rasp.train_api.train_purchase.workflow.ticket_refund.create_refund_payment
from common.tester.utils.datetime import replace_now
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import TrainRefundFactory, PassengerFactory, InsuranceFactory
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus, RefundPayment
from travel.rasp.train_api.train_purchase.workflow.ticket_refund.create_refund_payment import (
    CreateRefundPayment, CreateRefundPaymentEvents
)
from travel.rasp.train_api.train_purchase.workflow.user_events import PaymentUserEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _refund_payment_process(refund):
    events = (CreateRefundPaymentEvents.CREATED, )
    return process_state_action(CreateRefundPayment, events, refund)


@replace_now('2000-01-01 12:00:00')
def test_create_refund_payment():
    original_refund = TrainRefundFactory(
        blank_ids=['1'],
        insurance_ids=['paid_insurance_id', 'not_paid_insurance_id'],
        factory_extra_params={
            'create_order': True,
            'create_order_kwargs': {
                'passengers': [
                    PassengerFactory(insurance=InsuranceFactory(operation_id='paid_insurance_id',
                                                                trust_order_id='some_id')),
                    PassengerFactory(insurance=InsuranceFactory(operation_id='not_paid_insurance_id')),
                ],
            },
        }
    )
    with mock.patch.object(travel.rasp.train_api.train_purchase.workflow.ticket_refund.create_refund_payment,
                           'send_event_to_payment', autospec=True) as m_send_event_to_payment:

        event, refund = _refund_payment_process(original_refund)

        refund_payment = RefundPayment.objects.get(refund_uuid=original_refund.uuid)
        m_send_event_to_payment.assert_called_once_with(
            original_refund.order.current_billing_payment, PaymentUserEvents.REFUND_PAYMENT, params=refund_payment.id
        )
    assert event == CreateRefundPaymentEvents.CREATED
    assert_that(refund_payment, has_properties(
        order_uid=original_refund.order_uid,
        refund_uuid=original_refund.uuid,
        refund_payment_status=RefundPaymentStatus.NEW,
        refund_created_at=original_refund.created_at,
        refund_blank_ids=original_refund.blank_ids,
        refund_insurance_ids=['paid_insurance_id'],
        purchase_token=original_refund.order.current_billing_payment.purchase_token,
        user_info=has_properties(
            ip=original_refund.user_info.ip,
            region_id=original_refund.user_info.region_id
        ),
    ))
