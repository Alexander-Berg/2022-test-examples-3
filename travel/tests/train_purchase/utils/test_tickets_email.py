# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, has_entries
from pytz import timezone

from common import email_sender
from common.data_api.billing.trust_client import TrustReceiptException
from common.data_api.sendr.api import Attachment
from common.email_sender import EmailIntent
from common.tester.factories import create_region, create_settlement
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.ticket_blank import IM_TICKET_PDF_BLANK_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_partners.ufs.ticket_blank import GET_TICKET_BLANK_ENDPOINT
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import (
    PassengerFactory, TicketFactory, TrainOrderFactory, UserInfoFactory, PartnerDataFactory, InsuranceFactory,
    RefundPaymentFactory, SourceFactory
)
from travel.rasp.train_api.train_purchase.core.models import TrainOrder
from travel.rasp.train_api.train_purchase.utils import tickets_email, billing
from travel.rasp.train_api.train_purchase.utils.tickets_email import (
    OrderRegistrationStatus, campaign_send, do_resend_tickets_email, get_main_passenger, make_mail_args,
    send_tickets_email, MOSCOW_REGION_GEO_ID, attachment_adder
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@mock.patch.object(billing, 'download_receipt', autospec=True, return_value='receipt')
@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_send_tickets_email(m_campaign, m_download_receipt, httpretty):
    mock_ufs(httpretty, GET_TICKET_BLANK_ENDPOINT, body='ticket_blank')

    order = TrainOrderFactory(partner=TrainPartner.UFS, payments=[dict(purchase_token='purchase_token')],
                              partner_data=PartnerDataFactory(order_num='100500'))
    send_tickets_email(order.uid)
    TrainOrder.fetch_stations([order])

    m_download_receipt.assert_called_once_with('purchase_token')
    assert m_campaign.mock_calls == [
        mock.call.create_rasp_campaign(campaign_send.campaign),
        mock.call.create_rasp_campaign().send(
            to_email=order.user_info.email,
            args=make_mail_args(order),
            attachments=[
                Attachment(filename='{}.pdf'.format(order.current_partner_data.order_num),
                           mime_type='application/pdf', content='ticket_blank'),
                Attachment(filename='receipt.pdf',
                           mime_type='application/pdf', content='receipt'),
            ]
        )
    ]


@mock.patch.object(billing, 'download_receipt', autospec=True, return_value='receipt')
@mock.patch.object(tickets_email.campaign_send, 'send', autospec=True)
def test_do_resend_tickets_email(m_send, m_download_receipt, httpretty):
    mock_ufs(httpretty, GET_TICKET_BLANK_ENDPOINT, body='ticket_blank')

    order = TrainOrderFactory(partner=TrainPartner.UFS, payments=[dict(purchase_token='purchase_token')],
                              partner_data=PartnerDataFactory(order_num='100500'))
    do_resend_tickets_email(order.uid, order.user_info.email)
    TrainOrder.fetch_stations([order])

    m_download_receipt.assert_called_once_with('purchase_token')
    m_send.assert_called_once_with(order.user_info.email, make_mail_args(order), attachments=[
        Attachment(filename='{}.pdf'.format(order.current_partner_data.order_num),
                   mime_type='application/pdf', content='ticket_blank'),
        Attachment(filename='receipt.pdf',
                   mime_type='application/pdf', content='receipt'),
    ])


class TestOrderRegistrationStatus(object):
    @pytest.mark.parametrize('tickets, expected', (
        ((TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN),
          TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN)), OrderRegistrationStatus.ENABLED),
        ((TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN, pending=True),
          TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN, pending=True)), OrderRegistrationStatus.DISABLED),
        ((TicketFactory(rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN),
          TicketFactory(rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN)), OrderRegistrationStatus.DISABLED),
        ((TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN),
          TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN, pending=True)), OrderRegistrationStatus.MIXED),
        ((TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN),
          TicketFactory(rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN)), OrderRegistrationStatus.MIXED),
    ))
    def test_from_order(self, tickets, expected):
        order = TrainOrderFactory(passengers=[PassengerFactory(tickets=[ticket]) for ticket in tickets])

        assert OrderRegistrationStatus.from_order(order) == expected


@pytest.mark.parametrize('region_id, expected', [
    (214, True),
    (1, True),
    (2, False),
])
def test_is_moscow_region(region_id, expected):
    create_settlement(_geo_id=214, region=create_region(id=MOSCOW_REGION_GEO_ID))
    order = TrainOrderFactory(user_info=UserInfoFactory(region_id=region_id))
    TrainOrder.fetch_stations([order])
    args = make_mail_args(order)

    assert args['is_moscow_region'] == expected


@replace_dynamic_setting('TRAIN_FRONT_URL', 'train.yandex.ru')
@replace_dynamic_setting('TRAVEL_FRONT_URL', 'travel.yandex.ru')
@mock.patch('common.utils.railway.get_railway_tz_by_express_code')
@pytest.mark.parametrize('terminal, expected_front_url, expected_is_travel', [
    (None, 'train.yandex.ru', False),
    ('travel', 'travel.yandex.ru', True),
])
def test_make_mail_args(get_railway_tz_by_express_code, terminal, expected_front_url, expected_is_travel):
    get_railway_tz_by_express_code.side_effect = [timezone('Europe/Moscow'), timezone('Asia/Almaty'),
                                                  timezone('Europe/Moscow'), timezone('Asia/Almaty')]
    order = TrainOrderFactory(
        user_info=UserInfoFactory(email='kateov@yandex-team.ru'),
        departure=datetime(2017, 9, 21, 6, 43), arrival=datetime(2017, 9, 23, 13, 31),
        passengers=[
            PassengerFactory(
                first_name='Li',
                last_name='Ryan',
                tickets=[TicketFactory(places=['0010', '300', '0004'])],
                insurance=InsuranceFactory(amount=Decimal(100)),
            ),
            PassengerFactory(
                first_name='Jackie',
                last_name='Chan',
                tickets=[TicketFactory(places=['0011'], blank_id='111')],
                insurance=InsuranceFactory(amount=Decimal(150.50), trust_order_id='12345'),
            ),
            PassengerFactory(
                first_name='Bruce',
                last_name='Lee',
                tickets=[TicketFactory(places=['0012'], blank_id='222')],
                insurance=InsuranceFactory(amount=Decimal(100), trust_order_id='54321'),
            ),
        ],
        coach_number='020',
        insurance_auto_return_uuid='refund_uuid_refund_uuid_refund_uuid',
        source=SourceFactory(terminal=terminal),
    )
    TrainOrder.fetch_stations([order])
    args = make_mail_args(order)

    assert_that(args, has_entries({
        'arrival_time': '16:31',
        'arrival_time_rw': '19:31',
        'arrival_tzname_rw': 'по алматинскому времени',
        'arrival_time_local': '16:31',
        'coach_number': '20',
        'coach_type': 'купе',
        'departure_date_dot': '21.09.2017',
        'departure_time': '09:43',
        'departure_date_dot_rw': '21.09.2017',
        'departure_time_rw': '09:43',
        'departure_tzname_rw': 'по московскому времени',
        'departure_date_dot_local': '21.09.2017',
        'departure_time_local': '09:43',
        'main_name': 'Li Ryan',
        'main_first_name': 'Li',
        'order_price': '510',
        'order_uid': order.uid,
        'passengers': [
            {'place': '10, 300, 4', 'name': 'Li Ryan', 'first_name': 'Li'},
            {'place': '11', 'name': 'Jackie Chan', 'first_name': 'Jackie'},
            {'place': '12', 'name': 'Bruce Lee', 'first_name': 'Bruce'},
        ],
        'registration_status': 'DISABLED',
        'station_from_title': 'Откуда',
        'station_to_title': 'Куда',
        'tickets': [{'blank_id': '123456789'}, {'blank_id': '111'}, {'blank_id': '222'}],
        'ticket_number': order.current_partner_data.order_num,
        'train_number': '002A',
        'train_title': 'Откуда — Куда',
        'front_url': expected_front_url,
        'insurance_price': '250,50',
        'insurance_auto_return': True,
        'initial_order_price': '760,50',
        'is_travel': expected_is_travel,
    }))

    order.passengers[0].tickets[0].payment.fee = Decimal('70.11')
    args = make_mail_args(order)
    assert_that(args, has_entries({
        'order_price': '510,11',
    }))


@pytest.mark.parametrize('insurance_auto_return_uuid, expected_order_price, expected_insurance_auto_return', [
    ('refund_uuid_refund_uuid_refund_uuid', '510', True),
    (None, '760,50', False),
])
@mock.patch('common.utils.railway.get_railway_tz_by_express_code')
def test_make_mail_args_insurance_auto_return(get_railway_tz_by_express_code, insurance_auto_return_uuid,
                                              expected_order_price, expected_insurance_auto_return):
    get_railway_tz_by_express_code.side_effect = [timezone('Europe/Moscow'), timezone('Asia/Almaty'),
                                                  timezone('Europe/Moscow'), timezone('Asia/Almaty')]
    order = TrainOrderFactory(
        passengers=[
            PassengerFactory(insurance=InsuranceFactory(amount=Decimal(100))),
            PassengerFactory(insurance=InsuranceFactory(amount=Decimal(150.50), trust_order_id='12345')),
            PassengerFactory(insurance=InsuranceFactory(amount=Decimal(100), trust_order_id='12346')),
        ],
        insurance_auto_return_uuid=insurance_auto_return_uuid,
    )
    TrainOrder.fetch_stations([order])
    args = make_mail_args(order)

    assert_that(args, has_entries({
        'order_price': expected_order_price,
        'insurance_price': '250,50',
        'insurance_auto_return': expected_insurance_auto_return,
        'initial_order_price': '760,50',
    }))


@replace_now('2013-01-10')
@pytest.mark.parametrize('departure, arrival, expected', [
    (datetime(2013, 9, 21, 6, 43), datetime(2013, 9, 23, 13, 31), has_entries({
        'arrival_date': '23\N{no-break space}сентября',
        'departure_date': '21\N{no-break space}сентября'
    })),
    (datetime(2014, 9, 21, 6, 43), datetime(2014, 9, 23, 13, 31), has_entries({
        'arrival_date': '23\N{no-break space}сентября 2014',
        'departure_date': '21\N{no-break space}сентября 2014'
    }))
])
def test_make_mail_args_dates(departure, arrival, expected):
    order = TrainOrderFactory(departure=departure, arrival=arrival)
    TrainOrder.fetch_stations([order])
    args = make_mail_args(order)

    assert_that(args, expected)


def test_get_main_passenger_baby():
    order = TrainOrderFactory(passengers=[PassengerFactory(first_name='Baby', last_name='Ryan',
                                                           age=1,
                                                           tickets=[dict(blank_id='4', places=[],
                                                                         refund=dict(amount='0',
                                                                                     operation_id='refund_4'))])])
    assert get_main_passenger(order).first_name == 'Baby'


@mock.patch.object(tickets_email, 'download_order_receipt', autospec=True, return_value='receipt')
@mock.patch.object(tickets_email, 'download_refund_receipt', autospec=True, return_value='insurance_refund_receipt')
@mock.patch('common.email_sender.tasks.Campaign', autospec=True)
def test_send_tickets_email_with_insurance_auto_return(m_campaign, m_download_refund_receipt,
                                                       m_download_order_receipt, httpretty):
    mock_im(httpretty, IM_TICKET_PDF_BLANK_ENDPOINT, body='ticket_blank')
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True}, purchase_token='purchase_token', payment_resized=True
    )
    order = TrainOrder.objects.get(uid=refund_payment.order_uid)
    order.insurance_auto_return_uuid = refund_payment.refund_uuid
    order.save()
    send_tickets_email(order.uid)
    TrainOrder.fetch_stations([order])

    m_download_order_receipt.assert_called_once_with(order)
    m_download_refund_receipt.assert_called_once_with(refund_payment)
    assert m_campaign.mock_calls == [
        mock.call.create_rasp_campaign(campaign_send.campaign),
        mock.call.create_rasp_campaign().send(
            to_email=order.user_info.email,
            args=make_mail_args(order),
            attachments=[
                Attachment(filename='{}.pdf'.format(order.current_partner_data.order_num),
                           mime_type='application/pdf', content='ticket_blank'),
                Attachment(filename='receipt.pdf',
                           mime_type='application/pdf', content='receipt'),
                Attachment(filename='refund_receipt.pdf',
                           mime_type='application/pdf', content='insurance_refund_receipt')
            ]
        )
    ]


@mock.patch.object(tickets_email, 'download_order_receipt', autospec=True, return_value='receipt')
@mock.patch.object(tickets_email, 'download_refund_receipt', autospec=True, return_value='insurance_refund_receipt')
@mock.patch.object(tickets_email.campaign_send, 'send', autospec=True)
def test_resend_tickets_email_with_insurance_auto_return(m_send, m_download_refund_receipt,
                                                         m_download_order_receipt, httpretty):
    mock_im(httpretty, IM_TICKET_PDF_BLANK_ENDPOINT, body='ticket_blank')
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True}, purchase_token='purchase_token', payment_resized=True
    )
    order = TrainOrder.objects.get(uid=refund_payment.order_uid)
    order.insurance_auto_return_uuid = refund_payment.refund_uuid
    order.save()

    do_resend_tickets_email(order.uid, order.user_info.email)
    TrainOrder.fetch_stations([order])

    m_download_order_receipt.assert_called_once_with(order)
    m_download_refund_receipt.assert_called_once_with(refund_payment)
    m_send.assert_called_once_with(order.user_info.email, make_mail_args(order), attachments=[
        Attachment(filename='{}.pdf'.format(order.current_partner_data.order_num),
                   mime_type='application/pdf', content='ticket_blank'),
        Attachment(filename='receipt.pdf',
                   mime_type='application/pdf', content='receipt'),
        Attachment(filename='refund_receipt.pdf',
                   mime_type='application/pdf', content='insurance_refund_receipt')
    ])


@mock.patch.object(tickets_email, 'report_error_email', autospec=True)
@mock.patch.object(tickets_email, 'download_order_receipt', autospec=True, side_effect=TrustReceiptException(''))
@mock.patch.object(tickets_email.campaign_send, 'send', autospec=True)
@mock.patch.object(email_sender.sender, 'send_email', autospec=True)
def test_attachment_adder_receipt_error(m_send_email, m_send, m_download_order_receipt,
                                        m_report_error_email, httpretty):
    mock_im(httpretty, IM_TICKET_PDF_BLANK_ENDPOINT, body='ticket_blank')
    order = TrainOrderFactory()

    send_tickets_email(order.uid)
    email_id = m_send_email.apply_async.call_args_list[0][0][0][0]
    email = EmailIntent.objects(id=email_id).get()

    with pytest.raises(TrustReceiptException):
        attachment_adder(email)

    email.reload()
    assert not email.attachments

    with replace_now(email.created_at + timedelta(hours=5)):
        attachment_adder(email)
        m_report_error_email.assert_called_once_with(email, mock.ANY)
        assert email.attachments


@mock.patch.object(tickets_email, 'report_error_email', autospec=True)
@mock.patch.object(tickets_email, 'download_order_receipt', autospec=True, return_value='receipt')
@mock.patch.object(tickets_email, 'download_refund_receipt', autospec=True, side_effect=TrustReceiptException(''))
@mock.patch.object(tickets_email.campaign_send, 'send', autospec=True)
@mock.patch.object(email_sender.sender, 'send_email', autospec=True)
def test_attachment_adder_refund_receipt_error(m_send_email, m_send, m_download_refund_receipt,
                                               m_download_order_receipt, m_report_error_email, httpretty):
    mock_im(httpretty, IM_TICKET_PDF_BLANK_ENDPOINT, body='ticket_blank')
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True}, purchase_token='purchase_token', payment_resized=True
    )
    order = TrainOrder.objects.get(uid=refund_payment.order_uid)
    order.insurance_auto_return_uuid = refund_payment.refund_uuid
    order.save()

    send_tickets_email(order.uid)
    email_id = m_send_email.apply_async.call_args_list[0][0][0][0]
    email = EmailIntent.objects(id=email_id).get()

    with pytest.raises(TrustReceiptException):
        attachment_adder(email)

    email.reload()
    assert not email.attachments

    with replace_now(email.created_at + timedelta(hours=5)):
        attachment_adder(email)
        m_report_error_email.assert_called_once_with(email, mock.ANY)
        assert email.attachments
