# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime
from decimal import Decimal

import mock
import pytest
from django.test import Client
from hamcrest import assert_that, has_entries, contains

from common.apps.train.models import TariffInfo
from common.apps.train_order.enums import CoachType
from common.models.geo import Country
from common.tester.matchers import has_json
from common.tester.utils.datetime import replace_now
from common.utils.date import UTC_TZ
from travel.rasp.train_api.train_partners import im
from travel.rasp.train_api.train_purchase.core.enums import DocumentType, Gender, TrainPartner, OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TicketPaymentFactory, TrainOrderFactory, UserInfoFactory, PartnerDataFactory
)
from travel.rasp.train_api.train_purchase.core.models import Passenger, Ticket
from travel.rasp.train_api.train_purchase.factories import create_order_warnings
from travel.rasp.train_api.train_purchase.utils import order
from travel.rasp.train_api.train_purchase.views.test_utils import create_order

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]

ORDER_UID = '82b7e80df16c47a09477cc45aa664d81'


def create_test_order():
    return TrainOrderFactory(
        uid=ORDER_UID,
        partner=TrainPartner.IM,
        station_from_id=1000,
        station_to_id=2000,
        train_number='001A',
        train_ticket_number='002A',
        departure=datetime(2017, 1, 1, 10),
        arrival=datetime(2017, 1, 2, 1),
        coach_type=CoachType.COMPARTMENT,
        coach_number='2',
        lang='ru',
        passengers=[Passenger(
            age=Decimal('67.5'),
            first_name='Илья',
            last_name='Ильин',
            patronymic='Ильич',
            sex=Gender.MALE,
            doc_type=DocumentType.RUSSIAN_PASSPORT,
            citizenship_country_id=Country.RUSSIA_ID,
            tickets=[Ticket(
                blank_id='1',
                payment=TicketPaymentFactory(
                    amount=Decimal(1000),
                    fee=Decimal(100),
                    service_amount=Decimal(200),
                    service_fee=Decimal(20),
                ),
                places=['1А'],
                tariff_info_code=TariffInfo.FULL_CODE,
            )]
        ), Passenger(
            age=Decimal('67.4'),
            first_name='Иванна',
            last_name='Иванова',
            patronymic='Ивановна',
            sex=Gender.FEMALE,
            doc_type=DocumentType.RUSSIAN_PASSPORT,
            citizenship_country_id=Country.RUSSIA_ID
        )],
        user_info=UserInfoFactory(ip='1.2.3.4', uid='1111', region_id=213),
        payments=[dict(payment_url='some_url', status='err', resp_code='3ds_fail')]
    )


@mock.patch.object(im, 'get_route_info', return_value=None, autospec=True)
def test_get_order_main_fields(m_, full_tariff_info):
    create_test_order()
    response = Client().get('/ru/api/train-purchase/orders/{}/'.format(ORDER_UID))

    assert response.status_code == 200

    result = json.loads(response.content)

    assert_that(result['order'], has_entries({
        'uid': ORDER_UID,
        'partner': 'im',
        'stationFrom': has_entries(id=1000, title='Откуда'),
        'stationTo': has_entries(id=2000, title='Куда'),
        'passengers': contains(
            has_entries({
                'age': 67.5,
                'firstName': 'Илья',
                'lastName': 'Ильин',
                'patronymic': 'Ильич',
                'sex': 'M',
                'docType': 'ПН',
                'citizenship': 'RUS',
                'tickets': contains(has_entries({
                    'blankId': '1',
                    'places': ['1А'],
                    'payment': has_entries({
                        'amount': 1000,
                        'fee': 100,
                        'beddingAmount': 200,
                        'beddingFee': 20,
                    }),
                    'tariffInfo': has_entries({'code': TariffInfo.FULL_CODE, 'title': 'Полный'})
                }))
            }),
            has_entries({
                'age': 67.4,
                'firstName': 'Иванна',
                'lastName': 'Иванова',
                'patronymic': 'Ивановна',
                'sex': 'F',
                'docType': 'ПН',
                'citizenship': 'RUS'
            }),
        ),
        'paymentUrl': 'some_url',
        'trainNumber': '001A',
        'trainTicketNumber': '002A',
        'carType': 'compartment',
        'carNumber': '2',
        'paymentCode': '3ds_fail',
        'paymentStatus': 'err',
        'userInfo': has_entries({
            'ip': '1.2.3.4',
            'uid': '1111',
            'regionId': 213
        })
    }))


@pytest.mark.parametrize('gender', ['male', 'female', 'mixed'])
def test_compartment_gender(gender):
    create_order(uid=ORDER_UID, partner_data=PartnerDataFactory(compartment_gender=gender))
    response = Client().get('/ru/api/train-purchase/orders/{}/'.format(ORDER_UID))

    assert response.status_code == 200
    assert_that(response.content, has_json(has_entries(order=has_entries(compartmentGender=gender))))


@mock.patch.object(order, 'get_order_info', autospec=True)
@mock.patch.object(order, 'get_order_warnings', autospec=True, return_value=create_order_warnings())
def test_warnings(m_get_order_warnings, m_get_order_info):
    create_order(uid=ORDER_UID, status=OrderStatus.DONE)
    response = Client().get('/ru/api/train-purchase/orders/{}/'.format(ORDER_UID))

    assert response.status_code == 200
    assert_that(response.data['order']['warnings'], contains(
        has_entries({
            'code': 'insurance_auto_return',
            'from': datetime(2019, 1, 1, 20, 00, tzinfo=UTC_TZ),
            'to': datetime(2019, 1, 1, 21, 00, tzinfo=UTC_TZ),
        }),
        has_entries({
            'code': 'tickets_taken_away',
        }),
    ))


@replace_now('2019-01-01 23:30:00')
@mock.patch.object(order, 'get_order_info', autospec=True)
@mock.patch.object(order, 'get_order_warnings', autospec=True, return_value=create_order_warnings())
def test_first_actual_warning_only(m_get_order_warnings, m_get_order_info):
    create_order(uid=ORDER_UID, status=OrderStatus.DONE)
    response = Client().get('/ru/api/train-purchase/orders/{}/?firstActualWarningOnly=true'.format(ORDER_UID))

    assert response.status_code == 200
    assert_that(response.data['order']['warnings'], contains(
        has_entries({
            'code': 'insurance_auto_return',
            'from': datetime(2019, 1, 1, 20, 00, tzinfo=UTC_TZ),
            'to': datetime(2019, 1, 1, 21, 00, tzinfo=UTC_TZ),
        }),
    ))
