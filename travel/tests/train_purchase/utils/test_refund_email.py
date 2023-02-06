# coding: utf8

from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, has_entries

from common import email_sender
from common.data_api.billing.trust_client import TrustReceiptException
from common.data_api.sendr.api import Attachment, Campaign
from common.email_sender import EmailIntent
from common.tester.utils.datetime import replace_now
from travel.rasp.train_api.train_partners.base.ticket_blank import BlankFormat
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, UserInfoFactory, RefundBlankFactory,
    PartnerDataFactory, TicketFactory, TicketRefundFactory, TrainRefundFactory, RefundPaymentFactory,
    InsuranceFactory, TicketPaymentFactory
)
from travel.rasp.train_api.train_purchase.core.models import TrainOrder
from travel.rasp.train_api.train_purchase.utils import refund_email
from travel.rasp.train_api.train_purchase.utils.refund_email import send_refund_email, make_refund_args, refund_attachment_adder

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


@pytest.yield_fixture()
def freeze_created_at_for_refund():
    with replace_now('2017-11-23'):
        yield


def create_order_with_refund(refund_kwargs=None, pay_refund_kwargs=None, refund_yandex_fee=False):
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
                        payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('50')),
                        refund=TicketRefundFactory(
                            amount=100,
                            operation_id='refund_1',
                            refund_yandex_fee_amount=Decimal('50') if refund_yandex_fee else Decimal(0),
                        ),
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
                        refund=TicketRefundFactory(amount=200, operation_id='refund_3'),
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


@pytest.mark.parametrize('refund_kwargs, pay_refund_kwargs, expected_campagn, expected_attachments', (
    ({'is_external': True}, {}, refund_email.REFUND_TICKET_CAMPAIGN, [
        Attachment(filename='receipt.pdf', mime_type='application/pdf', content=b'receipt')
    ]),
    ({}, {}, refund_email.REFUND_TICKET_CAMPAIGN, [
        Attachment(filename='refund_1.pdf', mime_type='application/pdf', content=b'refund_blank'),
        Attachment(filename='refund_3.pdf', mime_type='application/pdf', content=b'refund_blank'),
        Attachment(filename='receipt.pdf', mime_type='application/pdf', content=b'receipt')
    ]),
    ({}, {'payment_resized': True}, refund_email.RESIZE_TICKET_CAMPAIGN, [
        Attachment(filename='refund_1.pdf', mime_type='application/pdf', content=b'refund_blank'),
        Attachment(filename='refund_3.pdf', mime_type='application/pdf', content=b'refund_blank'),
        Attachment(filename='receipt.pdf', mime_type='application/pdf', content=b'receipt')
    ]),
))
@mock.patch.object(refund_email, 'download_refund_receipt', autospec=True, return_value=b'receipt')
@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
@mock.patch.object(refund_email, 'download_refund_blank', autospec=True)
def test_send_refund_email(m_download_refund_blank, m_campaign, m_download_refund_receipt,
                           refund_kwargs, pay_refund_kwargs, expected_campagn, expected_attachments):
    m_download_refund_blank.return_value = RefundBlankFactory(content=b'refund_blank')
    order = create_order_with_refund(refund_kwargs, pay_refund_kwargs)
    refund = order.last_refund
    refund_payment = refund.refund_payment
    send_refund_email(refund, order=order)

    m_download_refund_receipt.assert_called_once_with(refund_payment)
    m_download_refund_blank.call_args_list = [
        (order, 'refund_3', BlankFormat.PDF),
        (order, 'refund_1', BlankFormat.PDF)
    ]

    assert m_campaign.mock_calls == [
        mock.call.create_rasp_campaign(expected_campagn),
        mock.call.create_rasp_campaign().send(
            to_email=order.user_info.email, args=mock.ANY, attachments=expected_attachments
        )
    ]


@mock.patch.object(refund_email, 'download_refund_receipt', autospec=True, return_value=b'receipt')
@mock.patch.object(Campaign, 'send', autospec=True)
@mock.patch.object(refund_email, 'download_refund_blank', autospec=True)
def test_send_refund_email_do_not_download_external_blanks(m_download_refund_blank, m_send, m_download_refund_receipt):
    order = create_order_with_refund({'is_external': True})
    refund = order.last_refund
    send_refund_email(refund, order=order)

    assert m_download_refund_blank.call_count == 0
    m_send.assert_called_once_with(mock.ANY, to_email=order.user_info.email, args=mock.ANY, attachments=[
        Attachment(filename='receipt.pdf', mime_type='application/pdf', content='receipt')
    ])


@pytest.mark.parametrize('blank_ids, expected_args', [
    (['1', '3'], has_entries({'refund_sum': '400', 'payment_sum': '500', 'fee_sum': '100',
                              'tickets': [{'blank_id': '1'}, {'blank_id': '3'}],
                              'passengers': [{'place': '1', 'name': 'Selena Ryan', 'first_name': 'Selena'},
                                             {'place': '3, 4', 'name': 'Elena Ryan', 'first_name': 'Elena'}]})),
    (['1'], has_entries({'refund_sum': '200', 'payment_sum': '250', 'fee_sum': '50',
                         'tickets': [{'blank_id': '1'}],
                         'passengers': [{'place': '1', 'name': 'Selena Ryan', 'first_name': 'Selena'}]})),
    (['3'], has_entries({'refund_sum': '200', 'payment_sum': '250', 'fee_sum': '50',
                         'tickets': [{'blank_id': '3'}],
                         'passengers': [{'place': '3, 4', 'name': 'Elena Ryan', 'first_name': 'Elena'}]})),
    (['4'], has_entries({'refund_sum': '70', 'payment_sum': '220', 'fee_sum': '150',
                         'tickets': [{'blank_id': '4'}],
                         'passengers': [{'place': '', 'name': 'Baby Ryan', 'first_name': 'Baby'}]})),
])
def test_make_refund_args(blank_ids, expected_args):
    order = create_order_with_refund()
    TrainOrder.fetch_stations([order])
    args = make_refund_args(order, blank_ids, order.last_refund.uuid)
    assert_that(args, expected_args)
    assert_that(args, has_entries({
        'departure_time': '10:43',
        'departure_date': '21\N{no-break space}сентября 2013',
        'arrival_time': '17:31',
        'station_to_title': 'Куда',
        'coach_number': '2',
        'arrival_date': '23\N{no-break space}сентября 2013',
        'train_number': '002A',
        'train_title': 'Откуда — Куда',
        'station_from_title': 'Откуда',
        'main_name': 'Selena Ryan',
        'main_first_name': 'Selena',
        'coach_type': 'купе'
    }))


@mock.patch.object(refund_email, 'report_error_email', autospec=True)
@mock.patch.object(refund_email, 'download_refund_receipt', autospec=True, side_effect=TrustReceiptException(''))
@mock.patch.object(refund_email, 'download_refund_blank', autospec=True)
@mock.patch.object(email_sender.sender, 'send_email', autospec=True)
def test_refund_attachment_adder_receipt_error(m_send_email, m_download_refund_blank, m_download_refund_receipt,
                                               m_report_error_email):
    m_download_refund_blank.return_value = RefundBlankFactory(content=b'refund_blank')
    order = create_order_with_refund()
    send_refund_email(order.last_refund, order=order)
    email_id = m_send_email.apply_async.call_args_list[0][0][0][0]
    email = EmailIntent.objects(id=email_id).get()
    assert not email.attachments

    with pytest.raises(TrustReceiptException):
        refund_attachment_adder(email)

    email.reload()
    assert not email.attachments

    with replace_now(email.created_at + timedelta(hours=5)):
        refund_attachment_adder(email)
        m_report_error_email.assert_called_once_with(email, mock.ANY)
        assert email.attachments


@pytest.mark.parametrize('refund_yandex_fee, expected_refund_sum', [
    (False, '200'),
    (True, '250'),
])
def test_refund_yandex_fee(refund_yandex_fee, expected_refund_sum):
    order = create_order_with_refund(refund_yandex_fee=refund_yandex_fee)
    TrainOrder.fetch_stations([order])
    args = make_refund_args(order, ['1'], order.last_refund.uuid)
    assert_that(args, has_entries({
        'refund_sum': expected_refund_sum,
        'payment_sum': '250',
    }))
