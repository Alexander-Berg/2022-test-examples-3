# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, timedelta
from decimal import Decimal

import mock
import pytest
import unicodecsv
from bson import ObjectId
from hamcrest import assert_that, contains, contains_string, has_entries, has_properties, anything
from six.moves import zip as izip

from common.data_api.billing.trust_client import TrustClient, TrustPaymentStatuses
from common.data_api.sendr.api import Campaign, Attachment
from common.tester.matchers import has_json
from common.utils.date import MSK_TZ, UTC_TZ
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult
from travel.rasp.train_api.train_partners.im.factories.order_info import ImOrderInfoFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.im.ticket_blank import IM_TICKET_PDF_BLANK_ENDPOINT
from travel.rasp.train_api.train_partners.im.update_order import IM_UPDATE_BLANKS_METHOD
from travel.rasp.train_api.train_purchase.backoffice.orders import views
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TrainPartner, OperationStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, UserInfoFactory, PassengerFactory, PartnerDataFactory, TicketFactory,
    TicketRefundFactory, TrainRefundFactory, SourceFactory, TicketPaymentFactory, RefundPaymentFactory,
    InsuranceFactory, PaymentFactory, RebookingInfoFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus, BackofficeActionHistory, TrainOrder
from travel.rasp.train_api.train_purchase.core.utils import hash_doc_id
from travel.rasp.train_api.train_purchase.tasks import unhold_invalid_payments
from travel.rasp.train_api.train_purchase.utils.tickets_email import campaign_send
from travel.rasp.train_api.train_purchase.workflow.booking import OrderState

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]

example_dt = datetime(2017, 12, 30)


class TestSearchOrders(object):
    def _assert_numbers(self, backoffice_client, query, numbers):
        data = backoffice_client.get('/ru/train-purchase-backoffice/orders/', query).data
        assert [to['trainNumber'] for to in data['results']] == numbers

    def _assert_count(self, backoffice_client, query, count):
        data = backoffice_client.get('/ru/train-purchase-backoffice/orders/count/', query).data
        assert data['count'] == count

    @pytest.mark.parametrize('query, numbers', [
        ({'firstName': 'елена'}, ['1']),
        ({'firstName': 'ольга'}, ['2']),
        ({'lastName': 'иванова'}, ['1', '2']),
        ({'lastName': 'петрова'}, []),
        ({'patronymic': 'сидоровна'}, ['1'])
    ])
    def test_search_by_names(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', passengers=[PassengerFactory(first_name='ЕЛЕНА', last_name='ИВАНОВА',
                                                                         patronymic='СИДОРОВНА')])
        TrainOrderFactory(train_number='2', passengers=[PassengerFactory(first_name='ОЛЬГА', last_name='ИВАНОВА')])
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, count', [
        ({}, 1),
        ({'process_state': 'paying'}, 0),
    ])
    def test_search_without_collation(self, backoffice_client, query, count):
        """
        Проверка, что collation не применяется к прочим параметрам.
        """
        TrainOrderFactory(process={'state': 'Paying'})
        self._assert_count(backoffice_client, query, count)

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1']),
        ({'email': 'kateov@yandex-team.ru'}, ['1']),
        ({'email': 'kateov2@yandex-team.ru'}, []),
    ])
    def test_search_by_emails(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', user_info=UserInfoFactory(email='KateOv@yandex-team.ru'))
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1', '2', '3']),
        ({'phone': '+71234567890'}, ['1', '2']),
        ({'phone': '1234567890'}, ['1', '2']),
        ({'phone': '1234'}, []),
    ])
    def test_search_by_phone(self, backoffice_client, query, numbers):
        phone = '+71234567890'
        TrainOrderFactory(train_number='1', user_info=UserInfoFactory(phone=phone))
        phone = '1234567890'
        TrainOrderFactory(train_number='2', user_info=UserInfoFactory(phone=phone))
        phone = '1111111111'
        TrainOrderFactory(train_number='3', user_info=UserInfoFactory(phone=phone))
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1']),
        ({'docId': '6060'}, ['1']),
        ({'docId': '7070'}, []),
    ])
    def test_search_by_doc_id(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', passengers=[PassengerFactory(doc_id_hash=hash_doc_id('6060'))])
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1']),
        ({'orderNum': '101'}, ['1']),
        ({'orderNum': '202'}, []),
    ])
    def test_search_by_order_num(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', partner_data=PartnerDataFactory(order_num='101'))
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1']),
        ({'status': 'reserved'}, ['1']),
        ({'status': 'paid'}, []),
    ])
    def test_search_by_status(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', status=OrderStatus.RESERVED)
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1', '2']),
        ({'dateFrom': example_dt - timedelta(1), 'dateTo': example_dt + timedelta(1)}, ['1']),
        ({'dateFrom': example_dt + timedelta(1), 'dateTo': example_dt + timedelta(2)}, ['2']),
        ({'dateFrom': example_dt + timedelta(2), 'dateTo': example_dt + timedelta(3)}, []),
    ])
    def test_search_by_dates(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', id=ObjectId.from_datetime(example_dt))
        TrainOrderFactory(train_number='2', id=ObjectId.from_datetime(example_dt + timedelta(1.1)))
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1']),
        ({'processState': 'done'}, ['1']),
        ({'processState': 'paying'}, []),
    ])
    def test_search_by_process_state(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', process={'state': OrderState.DONE})
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1']),
        ({'isFailed': 'true'}, ['1']),
        ({'isFailed': 'false'}, []),
    ])
    def test_search_failed_orders(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='1', status=OrderStatus.CONFIRM_FAILED)
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['1']),
        ({'hasRefunds': 'true'}, ['1']),
        ({'hasRefunds': 'false'}, []),
    ])
    def test_search_orders_with_refunds(self, backoffice_client, query, numbers):
        TrainRefundFactory(blank_ids=['1'],
                           factory_extra_params={'create_order': True, 'create_order_kwargs': {'train_number': '1'}})
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({}, ['101Я', '101ЯК', '101ЯМБ', '101ЯЗЬ']),
        (
            {
                'departureDateFrom': example_dt,
                'departureDateTo': example_dt + timedelta(days=1),
            },
            ['101Я', '101ЯК', '101ЯЗЬ']
        ),
        (
            {
                'departureDateFrom': example_dt,
                'departureDateTo': example_dt + timedelta(days=1),
                'station_from_id': 123456,
            },
            ['101Я', '101ЯЗЬ']
        ),
        (
            {
                'departureDateFrom': example_dt,
                'departureDateTo': example_dt + timedelta(days=1),
                'trainNumber': '101я'
            },
            ['101Я']
        ),
    ])
    def test_search_by_trip_parametrs(self, backoffice_client, query, numbers):
        TrainOrderFactory(train_number='101Я', departure=example_dt, station_from_id=123456)
        TrainOrderFactory(train_number='101ЯК', departure=example_dt, station_from_id=6543210)
        TrainOrderFactory(train_number='101ЯМБ', departure=example_dt - timedelta(days=2), station_from_id=123456)
        TrainOrderFactory(train_number='101ЯЗЬ', departure=example_dt, station_from_id=123456)
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    @pytest.mark.parametrize('query, numbers', [
        ({'purchaseToken': 'some_token', 'status': OrderStatus.DONE.value}, ['1']),
        ({'purchaseToken': 'some_token', 'status': OrderStatus.PAID.value}, []),
        ({'purchaseToken': 'not_that_token'}, []),
    ])
    def test_search_by_purchase_token(self, backoffice_client, query, numbers):
        order = TrainOrderFactory(train_number='1', status=OrderStatus.DONE)
        PaymentFactory(order_uid=order.uid, purchase_token='some_token')
        self._assert_numbers(backoffice_client, query, numbers)
        self._assert_count(backoffice_client, query, len(numbers))

    def test_purchase_tokens_in_order(self, backoffice_client):
        TrainOrderFactory(payments=[
            dict(purchase_token='3', trust_created_at=datetime(2018, 9, 11, 12, 5)),
            dict(purchase_token='2', trust_created_at=datetime(2018, 9, 11, 12, 3)),
            dict(purchase_token='1', trust_created_at=datetime(2018, 9, 11, 12, 1))
        ])
        data = backoffice_client.get('/ru/train-purchase-backoffice/orders/', {}).data
        assert_that(data, has_entries(results=contains(
            has_entries(
                purchaseToken='3',
                purchaseTokensHistory=contains('2', '1')
            )
        )))

    @pytest.mark.parametrize('query, expected', [
        ({'firstName': 'Ivanova'}, contains(has_entries({'status': 'reserved',
                                                         'orderPrice': Decimal('170.00'),
                                                         'stationTo': 'Куда',
                                                         'fio': 'ИВА Ivanova Дмитриевна',
                                                         'createdAt': '2017-12-30T00:00:00+00:00'}))),
        ({'orderBy': 'reserved_to'}, contains(has_entries({'fio': 'ИВА Ivanova Дмитриевна'}),
                                              has_entries({'fio': 'ИВА Иванова Дмитриевна'}))),
        ({'orderBy': '-reserved_to'}, contains(has_entries({'fio': 'ИВА Иванова Дмитриевна'}),
                                               has_entries({'fio': 'ИВА Ivanova Дмитриевна'}))),
    ])
    def test_search_orders_dump(self, backoffice_client, query, expected):
        TrainOrderFactory(id=ObjectId.from_datetime(example_dt),
                          passengers=[PassengerFactory(first_name='Ivanova', last_name='ИВА', patronymic='Дмитриевна')])
        order_with_refund = TrainOrderFactory(passengers=[PassengerFactory(first_name='Иванова', last_name='ИВА',
                                                                           patronymic='Дмитриевна')])
        TrainRefundFactory(order_uid=order_with_refund.uid)
        data = backoffice_client.get('/ru/train-purchase-backoffice/orders/', query).data
        assert_that(data['results'], expected)


class TestExport(object):
    def parse_export_result(self, raw_export_result):
        """
        Преобразует результаты экспорта в список заголовков и список словарей.
        Каждай словарь, - это строчка в данных экспорта.
        Ключ в словаре, - это название соответствующей колонки.
        """
        raw_reader = unicodecsv.reader(raw_export_result, encoding='utf-8-sig',
                                       dialect=unicodecsv.excel, delimiter=b',')

        header_fields = [k for k in raw_reader.next()]
        rows = [{k: v for k, v in izip(header_fields, row)} for row in raw_reader]

        return header_fields, rows

    def test_export_orders(self, backoffice_client):
        TrainOrderFactory(
            uid='1feeb7ce7d4d48958f2367ae97dca09e',
            partner_data=PartnerDataFactory(im_order_id=10, station_from_title='Откуда', station_to_title='Куда'),
            payments=[
                dict(clear_at=datetime(2017, 3, 5, 12, 30, 10), purchase_token='111',
                     trust_created_at=datetime(2017, 3, 5, 12, 20, 10)),
                dict(clear_at=datetime(2017, 2, 5, 12, 30, 10), purchase_token='222',
                     trust_created_at=datetime(2017, 2, 5, 12, 20, 10)),
                dict(clear_at=datetime(2017, 1, 5, 12, 30, 10), purchase_token='333',
                     trust_created_at=datetime(2017, 1, 5, 12, 20, 10)),
            ],
            scheme_id=555,
            station_from_id=123,
            station_to_id=456,
            departure=datetime(2017, 3, 6, 12, 30, 10),
            arrival=datetime(2017, 3, 7, 12, 30, 10),
            user_info=UserInfoFactory(email='syukhno@yandex.ru'),
            passengers=[
                PassengerFactory(first_name='Васисуалий', last_name='Лоханкин', sex='M',
                                 tickets=[TicketFactory(rzhd_status=1), TicketFactory()]),
            ],
            orders_created=['c09fe40fac8c4e3f978c27a61ed86008', '6fc20049c9af4f38b9dc6d1c132bff11'],
            source=SourceFactory(
                req_id='wizard',
                utm_source='utm_source',
                gclid='gugloid',
                terminal='someTerminal',
                is_transfer=True,
                partner='some partner',
                subpartner='some subpartner',
                partner_uid='some partner uid',
            ),
            rebooking_info=RebookingInfoFactory(service_class='лакшери'),
        )

        resp = backoffice_client.get('/ru/train-purchase-backoffice/orders/export_orders/')

        headers, rows = self.parse_export_result(resp)
        assert len(rows) == 1
        assert headers == [
            'дата',
            'мск. время завершения',
            'мск. время оплаты',
            'номер заказа партнера',
            'номер заказа РЖД',
            'UID заказа',
            'статус заказа',
            'кол-во билетов в заказе',
            'номер поезда',
            'номер вагона',
            'тип вагона',
            'класс сервиса',
            'перевозчик',
            'название станции от',
            'ID станции от',
            'название станции до',
            'ID станции до',
            'дата отправления со станции пользователя',
            'время отправления со станции пользователя',
            'дата прибытия',
            'время прибытия',
            'почта',
            'телефон',
            'возможно бронирование на 3 часа',
            'предыдущий заказ',
            'purchaseToken',
            'purchaseTokensHistory',
            'есть возвраты',
            'общая стоимость билетов',
            'общая комиссия по заказу',
            'id схемы',
            'req_id колдунщика',
            'устройство пользователя',
            'utm_source',
            'utm_medium',
            'utm_campaign',
            'utm_term',
            'utm_content',
            'from',
            'gclid',
            'terminal',
            'is_transfer',
            'partner',
            'subpartner',
            'partner_uid',
        ]

        assert rows[0] == {
            'дата': '',
            'мск. время завершения': '',
            'мск. время оплаты': '15:30:10',
            'номер заказа партнера': '10',
            'номер заказа РЖД': '',
            'UID заказа': '1feeb7ce7d4d48958f2367ae97dca09e',
            'статус заказа': 'reserved',
            'кол-во билетов в заказе': '2',
            'номер поезда': '001A',
            'номер вагона': '2',
            'тип вагона': 'compartment',
            'класс сервиса': 'лакшери',
            'перевозчик': 'ФПК СЕВЕРНЫЙ',
            'название станции от': 'Откуда',
            'ID станции от': '123',
            'название станции до': 'Куда',
            'ID станции до': '456',
            'дата отправления со станции пользователя': '2017-03-06',
            'время отправления со станции пользователя': '15:30:10',
            'дата прибытия': '2017-03-07',
            'время прибытия': '15:30:10',
            'почта': 'syukhno@yandex.ru',
            'телефон': '+71234567890',
            'возможно бронирование на 3 часа': 'False',
            'предыдущий заказ': 'c09fe40fac8c4e3f978c27a61ed86008',
            'purchaseToken': '111',
            'purchaseTokensHistory': '[222,333]',
            'есть возвраты': 'False',
            'общая стоимость билетов': '200.0',
            'общая комиссия по заказу': '140.0',
            'id схемы': '555',
            'req_id колдунщика': 'wizard',
            'устройство пользователя': 'desktop',
            'utm_source': 'utm_source',
            'utm_medium': '',
            'utm_campaign': '',
            'utm_term': '',
            'utm_content': '',
            'from': 'some from',
            'gclid': 'gugloid',
            'terminal': 'someTerminal',
            'is_transfer': 'True',
            'partner': 'some partner',
            'subpartner': 'some subpartner',
            'partner_uid': 'some partner uid',
        }

    def test_export_tickets(self, backoffice_client):
        TrainOrderFactory(
            uid='1feeb7ce7d4d48958f2367ae97dca09e',
            partner_data=PartnerDataFactory(im_order_id=10, station_from_title='Откуда', station_to_title='Куда',
                                            is_three_hours_reservation_available=True),
            payments=[dict(clear_at=datetime(2017, 3, 5, 12, 30, 10))],
            station_from_id=123,
            station_to_id=456,
            departure=datetime(2017, 3, 6, 12, 30, 10),
            arrival=datetime(2017, 3, 7, 12, 30, 10),
            user_info=UserInfoFactory(email='user@example.com'),
            passengers=[
                PassengerFactory(first_name='Васисуалий', last_name='Лоханкин', sex='M',
                                 tickets=[TicketFactory(rzhd_status=1), TicketFactory()]),
            ],
            orders_created=['c09fe40fac8c4e3f978c27a61ed86008', '6fc20049c9af4f38b9dc6d1c132bff11'],
        )

        resp = backoffice_client.get('/ru/train-purchase-backoffice/orders/export_tickets/')
        headers, rows = self.parse_export_result(resp)

        assert len(rows) == 2
        assert headers == [
            'дата',
            'мск. время завершения',
            'мск. время оплаты',
            'номер заказа партнера',
            'номер заказа РЖД',
            'UID заказа',
            'статус заказа',
            'кол-во билетов в заказе',
            'номер поезда',
            'номер вагона',
            'тип вагона',
            'класс сервиса',
            'перевозчик',
            'название станции от',
            'ID станции от',
            'название станции до',
            'ID станции до',
            'дата отправления со станции пользователя',
            'время отправления со станции пользователя',
            'дата прибытия',
            'время прибытия',
            'почта',
            'телефон',
            'возможно бронирование на 3 часа',
            'предыдущий заказ',
            'фамилия',
            'имя',
            'отчество',
            'пол',
            'возраст',
            'номер билета',
            'стоимость билета',
            'стоимость белья',
            'комиссия',
            'тариф',
            'место',
            'статус',
        ]
        assert rows[0] == {
            'дата': '',
            'мск. время завершения': '',
            'мск. время оплаты': '15:30:10',
            'номер заказа партнера': '10',
            'номер заказа РЖД': '',
            'UID заказа': '1feeb7ce7d4d48958f2367ae97dca09e',
            'статус заказа': 'reserved',
            'кол-во билетов в заказе': '2',
            'номер поезда': '001A',
            'номер вагона': '2',
            'тип вагона': 'compartment',
            'класс сервиса': '',
            'перевозчик': 'ФПК СЕВЕРНЫЙ',
            'название станции от': 'Откуда',
            'ID станции от': '123',
            'название станции до': 'Куда',
            'ID станции до': '456',
            'дата отправления со станции пользователя': '2017-03-06',
            'время отправления со станции пользователя': '15:30:10',
            'дата прибытия': '2017-03-07',
            'время прибытия': '15:30:10',
            'почта': 'user@example.com',
            'телефон': '+71234567890',
            'возможно бронирование на 3 часа': 'True',
            'предыдущий заказ': 'c09fe40fac8c4e3f978c27a61ed86008',
            'фамилия': 'Лоханкин',
            'имя': 'Васисуалий',
            'отчество': '',
            'пол': 'M',
            'возраст': '',
            'номер билета': '123456789',
            'стоимость билета': '100.0',
            'стоимость белья': '0.0',
            'комиссия': '70.0',
            'тариф': '',
            'место': '1',
            'статус': '1',
        }
        assert rows[1] == {
            'дата': '',
            'мск. время завершения': '',
            'мск. время оплаты': '15:30:10',
            'номер заказа партнера': '10',
            'номер заказа РЖД': '',
            'UID заказа': '1feeb7ce7d4d48958f2367ae97dca09e',
            'статус заказа': 'reserved',
            'кол-во билетов в заказе': '2',
            'номер поезда': '001A',
            'номер вагона': '2',
            'тип вагона': 'compartment',
            'класс сервиса': '',
            'перевозчик': 'ФПК СЕВЕРНЫЙ',
            'название станции от': 'Откуда',
            'ID станции от': '123',
            'название станции до': 'Куда',
            'ID станции до': '456',
            'дата отправления со станции пользователя': '2017-03-06',
            'время отправления со станции пользователя': '15:30:10',
            'дата прибытия': '2017-03-07',
            'время прибытия': '15:30:10',
            'почта': 'user@example.com',
            'телефон': '+71234567890',
            'возможно бронирование на 3 часа': 'True',
            'предыдущий заказ': 'c09fe40fac8c4e3f978c27a61ed86008',
            'фамилия': 'Лоханкин',
            'имя': 'Васисуалий',
            'отчество': '',
            'пол': 'M',
            'возраст': '',
            'номер билета': '123456789',
            'стоимость билета': '100.0',
            'стоимость белья': '0.0',
            'комиссия': '70.0',
            'тариф': '',
            'место': '1',
            'статус': '0',
        }

    def test_export_tickets_not_zero_service_amount(self, backoffice_client):
        TrainOrderFactory(passengers=[PassengerFactory(tickets=[TicketFactory(payment=TicketPaymentFactory(
            service_amount=100500
        ))])])

        resp = backoffice_client.get('/ru/train-purchase-backoffice/orders/export_tickets/')

        _, rows = self.parse_export_result(resp)
        assert rows[0]['стоимость белья'] == '100500.0'


def test_retrieve_order(backoffice_client):
    order = TrainOrderFactory(
        user_info=UserInfoFactory(email='kateov@yandex-team.ru'),
        passengers=[
            PassengerFactory(
                first_name='Ivanova',
                last_name='ИВА',
                patronymic='Дмитриевна',
                doc_id_hash=hash_doc_id('1'),
                tickets=[TicketFactory(blank_id='1')],
                insurance=InsuranceFactory(amount=Decimal('70.00'), trust_order_id='some_order_id'),
            ),
        ],
        reserved_to=datetime(2015, 9, 16, 20, 0),
        partner_data=PartnerDataFactory(order_num='100500'),
        orders_created=['c09fe40fac8c4e3f978c27a61ed86008', '6fc20049c9af4f38b9dc6d1c132bff11'],
        invalid_payments_unholded=True,
        source=SourceFactory(req_id='testing', utm_source='utm_source', device=None),
        max_pending_till=datetime(2015, 1, 1, 2, 30),
    )

    data = backoffice_client.get('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid)).data
    assert_that(data, has_entries({
        'status': 'reserved',
        'orderPrice': Decimal('170.00'),
        'insurancePrice': Decimal('70.00'),
        'stationTo': 'Куда',
        'fio': 'ИВА Ivanova Дмитриевна',
        'ordersCreated': ['c09fe40fac8c4e3f978c27a61ed86008', '6fc20049c9af4f38b9dc6d1c132bff11'],
        'invalidPaymentsUnholded': True,
        'source': has_entries({
            'reqId': 'testing',
            'device': None,
            'utmSource': 'utm_source',
            'from': 'some from'
        }),
        'maxPendingTill': '2015-01-01T02:30:00+00:00'
    }))


@pytest.mark.parametrize('payment_resized', [True, False])
def test_retrieve_order_with_refund_payment(backoffice_client, payment_resized):
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        payment_resized=payment_resized,
        trust_reversal_id='trust_reversal_id',
        refund_insurance_ids=['222222'],
        refund_blank_ids=['111111', '111112'],
    )

    data = backoffice_client.get('/ru/train-purchase-backoffice/orders/{}/'.format(refund_payment.order_uid)).data

    assert_that(data, has_entries(
        uid=refund_payment.order_uid,
        refunds=contains(has_entries(
            uuid=refund_payment.refund_uuid,
            paymentStatus=refund_payment.refund_payment_status,
            trustRefundId=refund_payment.trust_reversal_id if payment_resized else refund_payment.trust_refund_id,
            paymentResized=payment_resized,
            insuranceIds=['222222'],
            blankIds=['111111', '111112'],
        ))
    ))


def test_order_json_with_refund_payment(backoffice_client):
    refund_payment = RefundPaymentFactory(factory_extra_params={'create_order': True})

    data = backoffice_client.get('/ru/train-purchase-backoffice/orders/{}/json/'.format(refund_payment.order_uid)).data

    assert_that(data, has_json(has_entries(
        uid=refund_payment.order_uid,
        refunds=contains(has_entries(
            order_uid=refund_payment.order_uid,
            uuid=refund_payment.refund_uuid,
            refund_payments=contains(has_entries(
                order_uid=refund_payment.order_uid,
                refund_uuid=refund_payment.refund_uuid,
            ))
        )),
        payments=anything(),
    )))


def test_json(backoffice_client):
    order = TrainOrderFactory(
        arrival=MSK_TZ.localize(datetime(2018, 4, 23, 1, 2, 3)).astimezone(UTC_TZ).replace(tzinfo=None),
        payments=[{'purchase_token': 'trust-token'}]
    )
    refund = TrainRefundFactory(order_uid=order.uid)
    order.process['history'] = 'history'

    data = backoffice_client.get('/ru/train-purchase-backoffice/orders/{}/json/'.format(order.uid)).data
    data_dict = json.loads(data)
    assert_that(data_dict, has_entries({
        'status': 'reserved',
        'arrival': '2018-04-23T01:02:03',
        'refunds': contains(has_entries({
            'uuid': refund.uuid
        })),
        'payments': contains(has_entries({
            'purchase_token': 'trust-token'
        }))
    }))
    assert not data_dict['process'].get('history', None)


@pytest.mark.parametrize('order_params, check', [
    ({'payments': [dict(purchase_token='check_me')],
      'partner_data': {}}, True),
    ({'payments': [dict(purchase_token='do_not_check_me')],
      'partner_data': {'order_num': '100500'}}, False),
    ({}, False),
])
def test_unhold_payments(backoffice_client, order_params, check):
    order = TrainOrderFactory(**order_params)
    with mock.patch.object(TrustClient, 'get_payment_status',
                           autospec=True, return_value=TrustPaymentStatuses.AUTHORIZED) as m_get_payment_status:
        with mock.patch.object(TrustClient, 'unhold_payment', autospec=True) as m_unhold_payment:
            with mock.patch.object(unhold_invalid_payments, 'get_order_info', autospec=True) as m_get_order_info:
                m_get_order_info.return_value = OrderInfoResult(
                    buy_operation_id=None, expire_set_er=None, status=OperationStatus.FAILED, order_num=None,
                    passengers=[]
                )
                data = backoffice_client.get(
                    '/ru/train-purchase-backoffice/orders/{}/unhold-payments/'.format(order.uid)).data
                assert_that(data, has_entries({'status': 'ok'}))
                if check:
                    assert m_get_payment_status.call_count == 1
                    m_unhold_payment.assert_called_once_with(mock.ANY, 'check_me')
                else:
                    assert not m_get_payment_status.call_count


def test_update_ticket_statuses(httpretty, backoffice_client):
    order = TrainOrderFactory(partner=TrainPartner.IM,
                              passengers=[PassengerFactory(
                                  tickets=[TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN)])])
    im_order_info = ImOrderInfoFactory(train_order=order)

    im_order_info['OrderItems'][0]['OrderItemBlanks'][0]['BlankStatus'] = 'Returned'
    im_order_info['OrderItems'][0]['OrderItemBlanks'][0]['PendingElectronicRegistration'] = 'ToCancel'

    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=im_order_info)

    data = backoffice_client.get('/ru/train-purchase-backoffice/orders/{}/update-tickets-statuses/'.format(order.uid)
                                 ).data
    order_from_db = TrainOrder.objects.get(uid=order.uid)

    assert_that(httpretty.latest_requests, contains(
        has_properties(path=contains_string(IM_UPDATE_BLANKS_METHOD)),
        has_properties(path=contains_string(IM_ORDER_INFO_METHOD))
    ))

    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN
    assert not order.passengers[0].tickets[0].pending
    assert data['passengers'][0]['tickets'][0]['rzhdStatus'] == 'REFUNDED'
    assert data['passengers'][0]['tickets'][0]['pending']
    assert order_from_db.passengers[0].tickets[0].rzhd_status == RzhdStatus.REFUNDED
    assert order_from_db.passengers[0].tickets[0].pending


class TestUpdatePhone(object):
    def test_new_phone(self, backoffice_client):
        order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                  user_info=UserInfoFactory(phone='+71231231212'),
                                  process={'state': OrderState.WAITING_PAYMENT, 'history': [1, 2, 3]})
        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid),
                                         data=json.dumps({'userInfo': {'phone': '+74564564545'}}),
                                         content_type='application/json')
        order.reload()

        assert response.status_code == 200
        assert order.user_info.phone == '+74564564545'
        assert order.user_info.reversed_phone == '+74564564545'[::-1]

        assert BackofficeActionHistory.objects.count() == 1

        history = BackofficeActionHistory.objects[0]
        assert history.details == {
            'user_info__reversed_phone': '+74564564545'[::-1],
            'user_info__phone': '+74564564545'
        }
        assert history.prev_state['user_info']['phone'] == '+71231231212'

    def test_same_phone(self, backoffice_client):
        order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                  user_info=UserInfoFactory(phone='+71231231212'))
        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid),
                                         data=json.dumps({'userInfo': {'phone': '+71231231212'}}),
                                         content_type='application/json')
        order.reload()

        assert response.status_code == 200
        assert order.user_info.phone == '+71231231212'
        assert BackofficeActionHistory.objects.count() == 0


class TestUpdateEmail(object):
    def test_no_order(self, backoffice_client):
        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/aaaaaaaaaaaaaaaaaaaaaaaaaaa/',
                                         data=json.dumps({'userInfo': {'email': 'customer@example.com'}, }),
                                         content_type='application/json')
        assert response.status_code == 404

    def test_new_email(self, backoffice_client):
        order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                  user_info=UserInfoFactory(email='customer-err@example.com'),
                                  process={'state': OrderState.WAITING_PAYMENT, 'history': [1, 2, 3]})
        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid),
                                         data=json.dumps({'userInfo': {'email': 'customer@example.com'}}),
                                         content_type='application/json')
        order.reload()

        assert response.status_code == 200
        assert order.user_info.email == 'customer@example.com'
        assert BackofficeActionHistory.objects.count() == 1

        history = BackofficeActionHistory.objects[0]
        assert_that(history.user, has_entries({
            'user_id': backoffice_client.user.id,
            'username': backoffice_client.user.username
        }))
        assert history.details == {'user_info__email': 'customer@example.com'}
        assert history.prev_state['status'] == OrderStatus.RESERVED.value
        assert history.prev_state['process']['state'] == OrderState.WAITING_PAYMENT
        assert 'history' not in history.prev_state['process']
        assert history.prev_state['uid'] == order.uid
        assert history.prev_state['user_info']['email'] == 'customer-err@example.com'
        assert history.completed
        assert history.action == 'update'
        assert history.uid == order.uid

        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid),
                                         data=json.dumps({'userInfo': {'email': 'customer2@example.com'}}),
                                         content_type='application/json')
        order.reload()
        assert response.status_code == 200
        assert order.user_info.email == 'customer2@example.com'

    def test_same_email(self, backoffice_client):
        order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                  user_info=UserInfoFactory(email='customer@example.com'))
        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid),
                                         data=json.dumps({'userInfo': {'email': 'customer@example.com'}}),
                                         content_type='application/json')
        order.reload()

        assert response.status_code == 200
        assert order.user_info.email == 'customer@example.com'
        assert BackofficeActionHistory.objects.count() == 0

    @pytest.mark.parametrize('query', [{'userInfo': {}, }, {}])
    def test_missing_email(self, backoffice_client, query):
        order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                  user_info=UserInfoFactory(email='customer@example.com'))
        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid),
                                         data=json.dumps(query), content_type='application/json')
        order.reload()

        assert response.status_code == 200
        assert order.user_info.email == 'customer@example.com'
        assert BackofficeActionHistory.objects.count() == 0

    @pytest.mark.parametrize('email', ['a', '12345', '', None, True, 1])
    def test_bad_email(self, backoffice_client, email):
        order = TrainOrderFactory(status=OrderStatus.RESERVED,
                                  user_info=UserInfoFactory(email='customer@example.com'))
        response = backoffice_client.put('/ru/train-purchase-backoffice/orders/{}/'.format(order.uid),
                                         data=json.dumps({'userInfo': {'email': email}, }),
                                         content_type='application/json')
        order.reload()

        assert response.status_code == 400
        assert order.user_info.email == 'customer@example.com'
        assert BackofficeActionHistory.objects.count() == 0


class TestResendTicketsEmail(object):
    @pytest.mark.parametrize('email, send_to', [
        (None, 'customer@example.com'),
        ('manager@yandex-team.ru', 'manager@yandex-team.ru'),
    ])
    @mock.patch.object(TrustClient, 'get_receipt_pdf', autospec=True)
    @mock.patch.object(Campaign, 'send', autospec=True)
    def test_success(self, m_send, m_get_receipt_pdf, backoffice_client, httpretty, email, send_to):
        order = TrainOrderFactory(
            status=OrderStatus.RESERVED,
            partner=TrainPartner.IM,
            user_info=UserInfoFactory(email='customer@example.com'),
            payments=[dict(purchase_token='mytoken')],
            partner_data={'operation_id': '1234', 'order_num': 'order333'},
        )
        mock_im(httpretty, IM_TICKET_PDF_BLANK_ENDPOINT, body=b'pdfticket')
        m_get_receipt_pdf.return_value = b'pdfreceipt'

        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/orders/{}/resend-tickets/'.format(order.uid),
            json.dumps({'email': email}) if email else None,
            content_type='application/json',
        )

        assert response.status_code == 200
        m_get_receipt_pdf.assert_called_once_with(mock.ANY, 'mytoken', None, False)
        m_send.assert_called_once_with(
            campaign_send,
            send_to,
            mock.ANY,
            attachments=[
                Attachment(filename='order333.pdf', mime_type='application/pdf', content=b'pdfticket'),
                Attachment(filename='receipt.pdf', mime_type='application/pdf', content=b'pdfreceipt'),
            ],
        )

    def test_bad_email(self, backoffice_client):
        order = TrainOrderFactory()
        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/orders/{}/resend-tickets/'.format(order.uid),
            json.dumps({'email': 'bademail'}),
            content_type='application/json'
        )

        assert response.status_code == 400

    def test_bad_uid(self, backoffice_client):
        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/orders/aaaaaaaaaaa/resend-tickets/',
            content_type='application/json'
        )

        assert response.status_code == 404


class TestResendRefundEmails(object):
    @pytest.mark.parametrize('email, send_to', [
        (None, 'customer@example.com'),
        ('manager@yandex-team.ru', 'manager@yandex-team.ru'),
    ])
    @mock.patch.object(views, 'send_refund_email', autospec=True)
    def test_success(self, m_send, backoffice_client, email, send_to):
        ticket = TicketFactory(blank_id='111', refund=TicketRefundFactory(operation_id='123456'))
        order = TrainOrderFactory(status=OrderStatus.RESERVED, partner=TrainPartner.IM,
                                  passengers=[PassengerFactory(tickets=[ticket])],
                                  user_info=UserInfoFactory(email='customer@example.com'),
                                  payments=[dict(purchase_token='mytoken')])
        refund = TrainRefundFactory(order_uid=order.uid, blank_ids=['111'])
        RefundPaymentFactory(trust_refund_id='some_trust_id', refund_payment_status=RefundPaymentStatus.DONE,
                             refund_uuid=refund.uuid, order_uid=order.uid, refund_blank_ids=['111'],
                             purchase_token='mytoken')

        params = {'refundUuid': refund.uuid}
        if email:
            params['email'] = email

        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/orders/{}/resend-refunded-blanks/'.format(order.uid),
            json.dumps(params),
            content_type='application/json'
        )

        assert response.status_code == 200
        m_send.assert_called_once_with(refund, order=order, email=send_to)

    @pytest.mark.parametrize('query', [
        {},
        {'refundUuid': 'aa', 'email': 'bad'}
    ])
    def test_bad_request(self, backoffice_client, query):
        order = TrainOrderFactory()
        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/orders/{}/resend-refunded-blanks/'.format(order.uid),
            json.dumps(query),
            content_type='application/json'
        )

        assert response.status_code == 400

    def test_bad_uid(self, backoffice_client):
        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/orders/aaaaaaaaaaa/resend-refunded-blanks/',
            content_type='application/json'
        )

        assert response.status_code == 404
