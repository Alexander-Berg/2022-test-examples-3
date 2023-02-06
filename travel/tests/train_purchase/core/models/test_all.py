# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from decimal import Decimal

import mock
import pytest

from common.apps.train.models import TariffInfo
from common.apps.train_order.enums import CoachType
from common.models.geo import Country
from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from travel.rasp.train_api.train_purchase.core import models
from travel.rasp.train_api.train_purchase.core.enums import DocumentType, Gender, TrainPartner, OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    PassengerFactory, TrainOrderFactory, ClientContractsFactory, ClientContractFactory,
    RefundPaymentFactory, TrainRefundFactory, TicketPaymentFactory, PaymentFactory,
)
from travel.rasp.train_api.train_purchase.core.models import (
    ClientContracts, Passenger, Tax, Ticket, TicketPayment, TrainOrder
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@pytest.fixture(autouse=True, name='m_trust_client')
def _mock_trust_client():
    with mock.patch.object(models, 'TrustClient', autospec=True) as m_trust_client:
        m_instance = m_trust_client.return_value
        m_instance.get_receipt_url.return_value = 'https://trust-test.yandex.ru/receipts/some/?mode=mobile'
        m_instance.get_receipt_clearing_url.return_value = 'https://trust-test.yandex.ru/clearing/some/?mode=mobile'
        m_instance.get_refund_receipt_url.return_value = 'https://trust-test.yandex.ru/refunds/some/?mode=mobile'
        yield m_instance


@pytest.mark.parametrize('tax, expected', (
    (Tax(rate=Decimal(0), amount=Decimal(100)), None),
    (Tax(rate=Decimal(10), amount=Decimal(5)), Decimal(55)),
    (Tax(rate=Decimal(18), amount=Decimal('213.56')), Decimal(1400)),
    (Tax(rate=Decimal(18), amount=Decimal('21.08')), Decimal('138.2')),
))
def test_tax_calculate_full_amount(tax, expected):
    assert tax.calculate_full_amount() == expected


def test_ticket_payment_total():
    assert TicketPayment(amount=Decimal(100), fee=Decimal(23)).total == Decimal(123)


@pytest.mark.parametrize('fee, partner_fee, partner_refund_fee, expected_refundable_fee', [
    [100, 30, 20, 50],
    [100, 500, 0, 0],
    [100, 30, 500, 0],
])
def test_ticket_payment_refundable_yandex_fee_amount(fee, partner_fee, partner_refund_fee, expected_refundable_fee):
    assert TicketPaymentFactory(
        fee=fee,
        partner_fee=partner_fee,
        partner_refund_fee=partner_refund_fee
    ).refundable_yandex_fee_amount == expected_refundable_fee


def test_passenger_citizenship_country():
    passenger = Passenger(citizenship_country_id=Country.RUSSIA_ID)
    assert passenger.citizenship_country == Country.objects.get(id=Country.RUSSIA_ID)


def test_ticket_tariff_info():
    assert Ticket().tariff_info is None

    full_tariff_info = TariffInfo.objects.create(code='full', title_ru='Полный')
    assert Ticket(tariff_info_code=full_tariff_info.code).tariff_info == full_tariff_info


def test_create_fetch_train_order():
    uuid = '82b7e80df16c47a09477cc45aa664d81'
    departure = datetime(2017, 1, 1, 10)

    TrainOrderFactory(
        uid=uuid,
        status=OrderStatus.RESERVED,
        station_from_id=1,
        station_to_id=2,
        train_number='001A',
        train_ticket_number='002A',
        departure=departure,
        arrival=datetime(2017, 1, 2, 1),
        coach_type=CoachType.COMPARTMENT,
        coach_number='2',
        lang='ru',
        passengers=[Passenger(
            first_name='Илья',
            last_name='Ильин',
            patronymic='Ильич',
            citizenship_country_id=Country.RUSSIA_ID,
            sex=Gender.MALE,
            doc_type=DocumentType.RUSSIAN_PASSPORT
        ), Passenger(
            first_name='Иванна',
            last_name='Иванова',
            patronymic='Ивановна',
            citizenship_country_id=Country.RUSSIA_ID,
            sex=Gender.FEMALE,
            doc_type=DocumentType.RUSSIAN_PASSPORT
        )]
    )

    order = TrainOrder.objects.get(uid=uuid)
    assert order.status == OrderStatus.RESERVED
    assert order.departure == departure
    assert order.train_number == '001A'
    assert order.train_ticket_number == '002A'


def test_iter_ticket_to_lookup_name():
    ticket_1 = Ticket(blank_id='1')
    ticket_2 = Ticket(blank_id='2')
    ticket_3 = Ticket(blank_id='3')
    order = TrainOrderFactory(passengers=[PassengerFactory(tickets=[ticket_1]),
                                          PassengerFactory(tickets=[ticket_2, ticket_3])])

    assert tuple(order.iter_ticket_to_lookup_name()) == (
        (ticket_1, 'passengers__0__tickets__0'),
        (ticket_2, 'passengers__1__tickets__0'),
        (ticket_3, 'passengers__1__tickets__1'),
    )


def test_train_order_fetch_stations():
    station_a = create_station(__={'codes': {'express': '000'}})
    station_b = create_station(__={'codes': {'express': '111'}})
    order = TrainOrderFactory(station_from_id=station_a.id, station_to_id=station_b.id)
    TrainOrder.fetch_stations([order])
    assert order.station_from == station_a
    assert order.station_to == station_b


def test_train_order_fetch_stations_station_not_found():
    station_a = create_station(__={'codes': {'express': '000'}})
    order = TrainOrderFactory(station_from_id=station_a.id)
    order.station_to_id = 102030
    TrainOrder.fetch_stations([order], raise_on_station_not_found=False)
    assert order.station_from == station_a
    assert order.station_to is None

    with pytest.raises(TrainOrder.StationNotFound):
        TrainOrder.fetch_stations([order], raise_on_station_not_found=True)


def test_train_order_fiscal_title():
    station_a = create_station(title='Откуда')
    station_b = create_station(title='Куда', title_ru_preposition_v_vo_na='в')
    order = TrainOrderFactory(departure=datetime(2018, 4, 16), coach_number='7',
                              train_ticket_number='012А', station_from=station_a, station_to=station_b)
    fiscal_title = order.get_fiscal_title()
    assert fiscal_title == 'Заказ билета на поезд 012А из Откуда в Куда 16 апреля 2018 года в 03:00, вагон 7'
    station_a = create_station(time_zone='Asia/Yekaterinburg', title='Откуда')
    station_b = create_station(time_zone='Asia/Yekaterinburg', title='Куда', title_ru_preposition_v_vo_na='в')
    order = TrainOrderFactory(departure=datetime(2018, 4, 16, hour=23), coach_number='7',
                              train_ticket_number='012А', passengers=[PassengerFactory(), PassengerFactory()],
                              station_from=station_a, station_to=station_b)
    fiscal_title = order.get_fiscal_title()
    assert fiscal_title == 'Заказ билетов на поезд 012А из Откуда в Куда 17 апреля 2018 года в 04:00, вагон 7'


def test_refund_and_refund_payment_properties():
    refund = TrainRefundFactory()
    refund_payment = RefundPaymentFactory(refund_uuid=refund.uuid)
    refund_without_refund_payment = TrainRefundFactory()
    assert refund.refund_payment == refund_payment
    assert refund_without_refund_payment.refund_payment is None


class TestClientContracts(object):
    partner = TrainPartner.UFS

    def test_get_active_contract_no_contracts(self):
        """Возвращаем None если нет заведенных контрактов"""
        assert ClientContracts.get_active_contract(self.partner) is None

    @replace_now('2002-01-01')
    def test_get_active_contract_latest_active_contract(self):
        """Если есть несколько контрактов - возвращаем последний активный контракт"""
        old_one = ClientContractsFactory(  # noqa
            updated_at=datetime(2001, 1, 1),
            partner=self.partner,
            contracts=[
                ClientContractFactory(is_active=True, is_cancelled=False),
                ClientContractFactory(is_active=True, is_cancelled=False)
            ]
        )
        new_one = ClientContractsFactory(
            updated_at=datetime(2002, 1, 1),
            partner=self.partner,
            contracts=[
                ClientContractFactory(is_active=True, is_cancelled=False, partner_commission_sum=Decimal('20.0')),
            ]
        )
        assert ClientContracts.get_active_contract(self.partner) == new_one.contracts[0]

    def test_get_active_contract_only_cancelled(self):
        """Не возвращаем отмененные контракты"""
        ClientContractsFactory(
            updated_at=datetime(2002, 1, 1),
            partner=self.partner,
            contracts=[
                ClientContractFactory(is_active=True, is_cancelled=True),
            ]
        )
        assert ClientContracts.get_active_contract(self.partner) is None

    def test_get_active_contract_only_suspended(self):
        """Не возвращаем приостановленные контракты"""
        ClientContractsFactory(
            updated_at=datetime(2002, 1, 1),
            partner=self.partner,
            contracts=[
                ClientContractFactory(is_active=True, is_suspended=True),
            ]
        )
        assert ClientContracts.get_active_contract(self.partner) is None

    @replace_now('2001-01-10')
    def test_get_active_contract_correct_but_not_updated(self):
        """Если мы давно не обновляли контракты, но у нас есть активные контракт - вернем его"""
        ClientContractsFactory(
            updated_at=datetime(2001, 1, 1),
            partner=self.partner,
            contracts=[
                ClientContractFactory(is_active=True, start_dt=datetime(2001, 1, 9), finish_dt=None),
            ]
        )
        assert ClientContracts.get_active_contract(self.partner) is not None


def test_payment_receipt_url(m_trust_client):
    payment = PaymentFactory(purchase_token='some-token')

    assert payment.receipt_url == 'https://trust-test.yandex.ru/receipts/some/?mode=pdf'

    payment.reload()

    assert payment.receipt_url == 'https://trust-test.yandex.ru/receipts/some/?mode=pdf'
    m_trust_client.get_receipt_url.assert_called_once_with(payment.purchase_token)


def test_new_payment_receipt_url(m_trust_client):
    payment = PaymentFactory(purchase_token=None)

    assert payment.receipt_url is None
    assert m_trust_client.get_receipt_url.call_count == 0


def test_payment_receipt_url_with_exception(m_trust_client):
    payment = PaymentFactory(purchase_token='some-token')
    m_trust_client.get_receipt_url.side_effect = Exception('Ka-Boom')

    assert payment.receipt_url is None
    m_trust_client.get_receipt_url.assert_called_once_with(payment.purchase_token)


def test_refund_receipt_url(m_trust_client):
    refund_payment = RefundPaymentFactory(payment_resized=False)

    assert refund_payment.refund_receipt_url == 'https://trust-test.yandex.ru/refunds/some/?mode=pdf'

    refund_payment.reload()

    assert refund_payment.refund_receipt_url == 'https://trust-test.yandex.ru/refunds/some/?mode=pdf'
    m_trust_client.get_refund_receipt_url.assert_called_once_with(refund_payment.trust_refund_id)


def test_new_refund_receipt_url(m_trust_client):
    refund_payment = RefundPaymentFactory(payment_resized=False, trust_refund_id=None)

    assert refund_payment.refund_receipt_url is None
    m_trust_client.get_refund_receipt_url.call_count == 0


def test_refund_receipt_url_with_exception(m_trust_client):
    refund_payment = RefundPaymentFactory(payment_resized=False)
    m_trust_client.get_refund_receipt_url.side_effect = Exception('Ka-Boom')

    assert refund_payment.refund_receipt_url is None
    m_trust_client.get_refund_receipt_url.assert_called_once_with(refund_payment.trust_refund_id)


def test_resize_receipt_url(m_trust_client):
    refund_payment = RefundPaymentFactory(payment_resized=True)

    assert refund_payment.refund_receipt_url == 'https://trust-test.yandex.ru/clearing/some/?mode=pdf'

    refund_payment.reload()

    assert refund_payment.refund_receipt_url == 'https://trust-test.yandex.ru/clearing/some/?mode=pdf'
    m_trust_client.get_receipt_clearing_url.assert_called_once_with(refund_payment.purchase_token)


def test_new_resize_receipt_url(m_trust_client):
    refund_payment = RefundPaymentFactory(payment_resized=True, purchase_token=None)

    assert refund_payment.refund_receipt_url is None
    m_trust_client.get_receipt_clearing_url.call_count == 0


def test_resize_receipt_url_with_exception(m_trust_client):
    refund_payment = RefundPaymentFactory(payment_resized=True)
    m_trust_client.get_receipt_clearing_url.side_effect = Exception('Ka-Boom')

    assert refund_payment.refund_receipt_url is None
    m_trust_client.get_receipt_clearing_url.assert_called_once_with(refund_payment.purchase_token)
