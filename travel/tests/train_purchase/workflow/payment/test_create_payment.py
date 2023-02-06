# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta
from decimal import Decimal

import mock
import pytest
import pytz
from hamcrest import assert_that, contains, has_properties

from common.data_api.billing.trust_client import (
    FiscalNdsType, FiscalTaxationType, TrustClientRequestError, TrustFiscalData, TrustPaymentOrder
)
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from common.utils.date import MSK_TZ
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner, InsuranceStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, TicketFactory, TicketPaymentFactory, InsuranceFactory,
    InsuranceProcessFactory, SourceFactory
)
from travel.rasp.train_api.train_purchase.workflow.payment import create_payment
from travel.rasp.train_api.train_purchase.workflow.payment.create_payment import (
    get_payment_timeout, PAYMENT_MARGIN_BEFORE_TICKET_RESERVATION_ENDS, CreatePayment, CreatePaymentEvents,
    create_payment_orders
)

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


@pytest.fixture
def m_guaranteed_send_email():
    with mock.patch.object(create_payment, 'guaranteed_send_email', autospec=True) as m_guaranteed_send_email:
        yield m_guaranteed_send_email


@replace_setting('YASMS_DONT_SEND_ANYTHING', True)
def _process(payment):
    return process_state_action(CreatePayment, (CreatePaymentEvents.OK,
                                                CreatePaymentEvents.NEED_RETRY,
                                                CreatePaymentEvents.FAILED), payment)


def _create_order(insurance_status=None, terminal=None, carrier_inn=None):
    order = TrainOrderFactory(
        partner=TrainPartner.UFS,
        reserved_to=datetime(2017, 4, 1),
        insurance=InsuranceProcessFactory(status=insurance_status) if insurance_status else None,
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(
                    payment=TicketPaymentFactory(
                        amount=100,
                        fee=5,
                        fee_order_id='old_order_id',
                        service_order_id='old_order_id',
                        service_amount=Decimal(55),
                        service_vat=dict(rate=Decimal(16.67), amount=Decimal(5)),
                        tariff_vat=dict(rate=Decimal(20), amount=Decimal(5)),
                        ticket_order_id='old_order_id',
                    ),
                    places=['09', '10'],
                    carrier_inn=carrier_inn,
                )],
                insurance=InsuranceFactory(amount=Decimal(100), operation_id='101'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(
                    payment=TicketPaymentFactory(
                        amount=0,
                        fee=0,
                        fee_order_id='old_order_id',
                        service_order_id='old_order_id',
                        ticket_order_id='old_order_id',
                    ),
                    places=['11'],
                    carrier_inn=carrier_inn,
                )],
                insurance=InsuranceFactory(amount=Decimal(50), operation_id='102'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(
                    payment=TicketPaymentFactory(
                        amount=100,
                        fee=5,
                        fee_order_id='old_order_id',
                        service_order_id='old_order_id',
                        ticket_order_id='old_order_id'
                    ),
                    places=['12'],
                    carrier_inn=carrier_inn,
                )],
            )
        ],
        source=SourceFactory(terminal=terminal) if terminal else None,
    )
    return order


@mock.patch.object(create_payment, 'TrustClient', autospec=True)
def test_create_payment_ok(m_trust_client, m_guaranteed_send_email):
    original_order = _create_order(terminal='someTerminal')
    fiscal_title = original_order.get_fiscal_title()
    trust_client = m_trust_client.return_value
    m_create_order = trust_client.create_order
    m_create_order.return_value = 'some_order_id'
    m_create_payment = trust_client.create_payment
    m_create_payment.return_value = 'some_purchase_token'
    trust_client.start_payment.return_value = 'some_payment_url'
    event, payment = _process(original_order.current_billing_payment)

    assert event == CreatePaymentEvents.OK
    assert m_create_order.call_args_list == [
        mock.call('ufs_service'), mock.call('ufs_ticket'), mock.call('ufs_rasp_fee'),
        mock.call('ufs_ticket'), mock.call('ufs_rasp_fee'), mock.call('ufs_insurance'),
        mock.call('ufs_insurance')
    ]
    m_create_payment.assert_called_once_with(
        [
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20,
                              fiscal_title='Услуги перевозки пассажира ж/д транспортом, места 09, 10',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(45)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20_120,
                              fiscal_title='Дополнительные услуги перевозчика, места 09, 10',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(55)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20, fiscal_title='Сервисный сбор, места 09, 10',
                              order_id='some_order_id', price=Decimal(5)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_NONE,
                              fiscal_title='Услуги перевозки пассажира ж/д транспортом, место 12',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(100)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20, fiscal_title='Сервисный сбор, место 12',
                              order_id='some_order_id', price=Decimal(5)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_0,
                              fiscal_title='Страхование поездки (страховая премия), места 09, 10',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(100)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_0,
                              fiscal_title='Страхование поездки (страховая премия), место 11',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(50)),
        ],
        mock.ANY,
        fiscal_data=TrustFiscalData(fiscal_partner_inn='7708510731',
                                    fiscal_partner_phone='+74952698365',
                                    fiscal_taxation_type=FiscalTaxationType.OSN),
        is_mobile=False,
        user_email=original_order.user_info.email,
        fiscal_title=fiscal_title,
        pass_params={'terminal_route_data': {'description': 'someTerminal'}},
    )
    assert_that(payment, has_properties(
        purchase_token='some_purchase_token',
        payment_url='some_payment_url',
        order=has_properties(passengers=contains(
            has_properties(
                tickets=contains(has_properties(payment=has_properties(
                    fee_order_id='some_order_id', service_order_id='some_order_id', ticket_order_id='some_order_id'
                ))),
                insurance=has_properties(trust_order_id='some_order_id')
            ),
            has_properties(
                tickets=contains(has_properties(payment=has_properties(
                    fee_order_id='old_order_id', service_order_id='old_order_id', ticket_order_id='old_order_id'
                ))),
                insurance=has_properties(trust_order_id='some_order_id')
            ),
            has_properties(
                tickets=contains(has_properties(payment=has_properties(
                    fee_order_id='some_order_id', service_order_id='old_order_id', ticket_order_id='some_order_id'
                ))),
            )
        ))
    ))


@mock.patch.object(create_payment, 'TrustClient', autospec=True)
def test_create_payment_with_failed_insurance(m_trust_client, m_guaranteed_send_email):
    original_order = _create_order(insurance_status=InsuranceStatus.FAILED)
    fiscal_title = original_order.get_fiscal_title()
    trust_client = m_trust_client.return_value
    m_create_order = trust_client.create_order
    m_create_order.return_value = 'some_order_id'
    m_create_payment = trust_client.create_payment
    m_create_payment.return_value = 'some_purchase_token'
    trust_client.start_payment.return_value = 'some_payment_url'
    event, payment = _process(original_order.current_billing_payment)

    assert event == CreatePaymentEvents.OK
    assert m_create_order.call_args_list == [
        mock.call('ufs_service'), mock.call('ufs_ticket'), mock.call('ufs_rasp_fee'),
        mock.call('ufs_ticket'), mock.call('ufs_rasp_fee'),
    ]
    m_create_payment.assert_called_once_with(
        [
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20,
                              fiscal_title='Услуги перевозки пассажира ж/д транспортом, места 09, 10',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(45)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20_120,
                              fiscal_title='Дополнительные услуги перевозчика, места 09, 10',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(55)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20, fiscal_title='Сервисный сбор, места 09, 10',
                              order_id='some_order_id', price=Decimal(5)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_NONE,
                              fiscal_title='Услуги перевозки пассажира ж/д транспортом, место 12',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(100)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20, fiscal_title='Сервисный сбор, место 12',
                              order_id='some_order_id', price=Decimal(5)),
        ],
        mock.ANY,
        fiscal_data=TrustFiscalData(fiscal_partner_inn='7708510731',
                                    fiscal_partner_phone='+74952698365',
                                    fiscal_taxation_type=FiscalTaxationType.OSN),
        is_mobile=False,
        user_email=original_order.user_info.email,
        fiscal_title=fiscal_title,
        pass_params=None,
    )
    assert_that(payment, has_properties(
        purchase_token='some_purchase_token',
        payment_url='some_payment_url',
        order=has_properties(passengers=contains(
            has_properties(
                tickets=contains(has_properties(payment=has_properties(
                    fee_order_id='some_order_id', service_order_id='some_order_id', ticket_order_id='some_order_id'
                ))),
            ),
            has_properties(
                tickets=contains(has_properties(payment=has_properties(
                    fee_order_id='old_order_id', service_order_id='old_order_id', ticket_order_id='old_order_id'
                ))),
            ),
            has_properties(
                tickets=contains(has_properties(payment=has_properties(
                    fee_order_id='some_order_id', service_order_id='old_order_id', ticket_order_id='some_order_id'
                ))),
            )
        ))
    ))


@replace_now('2000-01-01 12:00:00')
@replace_dynamic_setting('TRUST_USE_PRODUCTION_FOR_TESTING', True)
@mock.patch.object(create_payment, 'TrustClient', autospec=True)
def test_create_payment_force_use_production(m_trust_client, m_guaranteed_send_email):
    original_order = TrainOrderFactory()
    trust_client = m_trust_client.return_value
    m_create_order = trust_client.create_order
    m_create_order.return_value = 'some_order_id'
    m_create_payment = trust_client.create_payment
    m_create_payment.return_value = 'some_purchase_token'
    trust_client.start_payment.return_value = 'some_payment_url'
    event, payment = _process(original_order.current_billing_payment)

    assert event == CreatePaymentEvents.OK
    assert payment.immediate_return is True
    assert payment.trust_created_at.date() == datetime(2000, 1, 1).date()


@mock.patch.object(create_payment, 'TrustClient', autospec=True)
def test_create_payment_need_retry(m_trust_client, m_guaranteed_send_email):
    original_order = TrainOrderFactory(reserved_to=datetime(2017, 4, 1))
    m_trust_client.return_value.create_order.side_effect = TrustClientRequestError('Fail!')
    event, payment = _process(original_order.current_billing_payment)

    assert event == CreatePaymentEvents.NEED_RETRY
    assert payment.create_payment_counter == 1


@mock.patch.object(create_payment, 'TrustClient', autospec=True)
def test_create_payment_failed(m_trust_client, m_guaranteed_send_email):
    original_order = TrainOrderFactory(reserved_to=datetime(2017, 4, 1),
                                       payments=[dict(create_payment_counter=5)])
    m_trust_client.return_value.create_order.side_effect = TrustClientRequestError('Fail!')
    event, payment = _process(original_order.current_billing_payment)

    assert event == CreatePaymentEvents.FAILED


@pytest.mark.parametrize('reserved_to, now, has_time_to_pay', [
    [datetime(2017, 4, 20, 10, 0), datetime(2017, 4, 20, 9, 45), True],
    [datetime(2017, 4, 20, 10, 0), datetime(2017, 4, 20, 10, 15), False],
    [datetime(2017, 4, 20, 10, 0), datetime(2017, 4, 20, 10, 0), False],
    [datetime(2017, 4, 20, 10, 0), datetime(2017, 4, 20, 10, 0) - PAYMENT_MARGIN_BEFORE_TICKET_RESERVATION_ENDS, False],
    [
        datetime(2017, 4, 20, 10, 0),
        datetime(2017, 4, 20, 10, 0) - PAYMENT_MARGIN_BEFORE_TICKET_RESERVATION_ENDS - timedelta(minutes=1),
        True
    ],
])
def test_get_payment_timeout(reserved_to, now, has_time_to_pay):
    utc_reserved_to = MSK_TZ.localize(reserved_to).astimezone(pytz.UTC).replace(tzinfo=None)
    with replace_now(now):
        assert has_time_to_pay == (get_payment_timeout(utc_reserved_to) > 0)


def test_create_payment_orders_use_service_amount(m_guaranteed_send_email):
    order = TrainOrderFactory(partner=TrainPartner.IM)
    order_ticket = TicketFactory(payment=TicketPaymentFactory(
        amount=Decimal(100500),
        fee=Decimal(200300),
        service_amount=Decimal(42),
        service_vat=dict(rate=Decimal(10), amount=Decimal(100500)),
    ))
    trust_client = mock.MagicMock()
    ticket_order, service_order, fee_order = create_payment_orders(trust_client, order, order_ticket)

    assert ticket_order.price == Decimal(100500) - Decimal(42)
    assert service_order.price == Decimal(42)
    assert fee_order.price == Decimal(200300)


@mock.patch.object(create_payment, 'TrustClient', autospec=True)
@pytest.mark.parametrize('carrier_inn, expected_fiscal_inn, expected_email_sent_count', [
    ('0123456789', '0123456789', 0),
    (None, '7708510731', 2),
])
def test_create_payment_with_carrier_inn(m_trust_client, m_guaranteed_send_email,
                                         carrier_inn, expected_fiscal_inn, expected_email_sent_count):
    original_order = _create_order(carrier_inn=carrier_inn)
    trust_client = m_trust_client.return_value
    m_create_order = trust_client.create_order
    m_create_order.return_value = 'some_order_id'
    m_create_payment = trust_client.create_payment
    m_create_payment.return_value = 'some_purchase_token'
    trust_client.start_payment.return_value = 'some_payment_url'
    event, payment = _process(original_order.current_billing_payment)

    assert event == CreatePaymentEvents.OK
    m_create_payment.assert_called_once_with(
        [
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20,
                              fiscal_title='Услуги перевозки пассажира ж/д транспортом, места 09, 10',
                              fiscal_inn=expected_fiscal_inn,
                              order_id='some_order_id', price=Decimal(45)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20_120,
                              fiscal_title='Дополнительные услуги перевозчика, места 09, 10',
                              fiscal_inn=expected_fiscal_inn,
                              order_id='some_order_id', price=Decimal(55)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20, fiscal_title='Сервисный сбор, места 09, 10',
                              order_id='some_order_id', price=Decimal(5)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_NONE,
                              fiscal_title='Услуги перевозки пассажира ж/д транспортом, место 12',
                              fiscal_inn=expected_fiscal_inn,
                              order_id='some_order_id', price=Decimal(100)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_20, fiscal_title='Сервисный сбор, место 12',
                              order_id='some_order_id', price=Decimal(5)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_0,
                              fiscal_title='Страхование поездки (страховая премия), места 09, 10',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(100)),
            TrustPaymentOrder(fiscal_nds=FiscalNdsType.NDS_0,
                              fiscal_title='Страхование поездки (страховая премия), место 11',
                              fiscal_inn='7708510731',
                              order_id='some_order_id', price=Decimal(50)),
        ],
        mock.ANY,
        fiscal_data=TrustFiscalData(fiscal_partner_inn='7708510731',
                                    fiscal_partner_phone='+74952698365',
                                    fiscal_taxation_type=FiscalTaxationType.OSN),
        is_mobile=False,
        user_email=original_order.user_info.email,
        fiscal_title=original_order.get_fiscal_title(),
        pass_params=None,
    )
    assert expected_email_sent_count == m_guaranteed_send_email.call_count
