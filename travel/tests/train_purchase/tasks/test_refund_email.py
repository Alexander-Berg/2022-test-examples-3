# coding: utf8

from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta
from decimal import Decimal

import mock
import pytest

from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date.environment import now_utc
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, UserInfoFactory, RefundBlankFactory, RefundEmailFactory,
    PartnerDataFactory, TicketFactory, TicketRefundFactory, TrainRefundFactory, RefundPaymentFactory,
    InsuranceFactory, TicketPaymentFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundEmail
from travel.rasp.train_api.train_purchase.tasks import refund_email
from travel.rasp.train_api.train_purchase.tasks.refund_email import close_refund_email_intents

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


@pytest.yield_fixture()
def freeze_created_at_for_refund():
    with replace_now('2017-11-23'):
        yield


def create_order_with_refund(refund_kwargs=None, pay_refund_kwargs=None):
    blank_1 = RefundBlankFactory(content=b'refund_1_blank')
    blank_3 = RefundBlankFactory.build()  # удалённый бланк
    refund_uuid = 'some_train_refund_uid_for_train_order'
    order = TrainOrderFactory(
        partner_data=PartnerDataFactory(operation_id='1'),
        passengers=[
            PassengerFactory(
                first_name='Selena',
                last_name='Ryan',
                tickets=[
                    TicketFactory(
                        blank_id='1',
                        places=['01'],
                        refund=TicketRefundFactory(amount=100, operation_id='refund_1', blank_id=blank_1.id),
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('50')),
                    ),
                ],
                insurance=InsuranceFactory(amount=Decimal('100'), trust_order_id='some_order_id',
                                           refund_uuid=refund_uuid),
            ),
            PassengerFactory(
                first_name='Elena',
                last_name='Ryan',
                tickets=[
                    TicketFactory(
                        blank_id='3',
                        places=['03', '04'],
                        refund=TicketRefundFactory(amount=200, operation_id='refund_3', blank_id=blank_3.id),
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('50')),
                    ),
                ],
                insurance=InsuranceFactory(amount=Decimal('100'), trust_order_id='some_order_id',
                                           refund_uuid='wrong_train_refund_uid_for_train_order'),
            ),
            PassengerFactory(
                first_name='Baby',
                last_name='Ryan',
                tickets=[
                    TicketFactory(
                        blank_id='4',
                        places=[],
                        refund=TicketRefundFactory(amount=0, operation_id='refund_4'),
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('50')),
                    ),
                ],
                insurance=InsuranceFactory(amount=Decimal('70'), trust_order_id='some_order_id',
                                           refund_uuid=refund_uuid),
            ),
        ],
        departure=datetime(2013, 9, 21, 6, 43),
        arrival=datetime(2013, 9, 23, 13, 31),
        user_info=UserInfoFactory(email='kateov@yandex-team.ru')
    )

    refund_factory_kwargs = dict(order_uid=order.uid, blank_ids=['1', '3'], uuid=refund_uuid)
    if refund_kwargs:
        refund_factory_kwargs.update(refund_kwargs)
    refund = TrainRefundFactory(**refund_factory_kwargs)
    payment_refund_kwargs = dict(
        order_uid=order.uid,
        refund_uuid=refund.uuid,
        refund_blank_ids=refund.blank_ids,
        trust_refund_id='590871ba795be2183d81c142',
        payment_resized=False,
    )
    if pay_refund_kwargs:
        payment_refund_kwargs.update(pay_refund_kwargs)
    RefundPaymentFactory(**payment_refund_kwargs)
    return order


@mock.patch.object(refund_email, 'send_refund_email', autospec=True)
def test_close_refund_email_intents(m_send_refund_email, freeze_created_at_for_refund):
    order = create_order_with_refund()
    refund = order.last_refund
    RefundEmailFactory(order_uid=order.uid, refund_uuid=refund.uuid,
                       created_at=now_utc() - timedelta(days=1), is_sent=False)

    close_refund_email_intents()

    refund_email = RefundEmail.objects.get(order_uid=order.uid, refund_uuid=refund.uuid)
    assert refund_email.is_sent
    m_send_refund_email.assert_called_once_with(refund)


@mock.patch.object(refund_email, 'send_refund_email', autospec=True)
def test_close_refund_email_intents_exception(m_send_refund_email, freeze_created_at_for_refund):
    m_send_refund_email.side_effect = Exception()
    order = create_order_with_refund()
    refund = order.last_refund
    RefundEmailFactory(order_uid=order.uid, refund_uuid=refund.uuid,
                       created_at=now_utc() - timedelta(days=1), is_sent=False)

    close_refund_email_intents()

    refund_email = RefundEmail.objects.get(order_uid=order.uid, refund_uuid=refund.uuid)
    assert not refund_email.is_sent
    m_send_refund_email.assert_called_once_with(refund)
