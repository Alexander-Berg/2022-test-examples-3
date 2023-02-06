# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from decimal import Decimal

import pytest
from bson import ObjectId
from hamcrest import assert_that, has_entries
from pytz import UTC

from common.apps.train.models import TariffInfo
from travel.rasp.train_api.train_purchase.core.enums import (
    GenderChoice, TrainPurchaseSource, TravelOrderStatus, OperationStatus
)
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PartnerDataFactory, PaymentFactory, OrderRouteInfoFactory, StationInfoFactory,
    PassengerFactory, TicketFactory, TicketPaymentFactory, UserInfoFactory, TrainRefundFactory,
    SourceFactory, TicketRefundFactory, InsuranceFactory, RebookingInfoFactory
)
from travel.rasp.train_api.train_purchase.core.models import Tax, RefundStatus
from travel.rasp.train_api.train_purchase.views.cpa.serialization import CpaOrderResponseSchema

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def test_flatten():
    order = TrainOrderFactory(
        uid='12345678901234567890123456789012',
        finished_at=datetime(2019, 5, 18, 12),
        arrival=datetime(2019, 6, 18, 12),
        departure=datetime(2019, 6, 18, 11),
        gender=GenderChoice.MIXED,
        partner_data_history=[PartnerDataFactory(
            order_num='im-order-number',
            im_order_id=100500,
            operation_id='100501',
        )],
        payments=[
            PaymentFactory(uid='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', trust_created_at=datetime(2019, 5, 5, 12)),
            PaymentFactory(purchase_token='purchase_token_2', trust_created_at=datetime(2019, 5, 5, 11)),
            PaymentFactory(purchase_token='purchase_token_1', trust_created_at=datetime(2019, 5, 5, 10)),
        ],
        scheme_id=33,
        travel_status=TravelOrderStatus.IN_PROGRESS,
        route_info=OrderRouteInfoFactory(
            start_station=StationInfoFactory(
                id=1,
                title='Название станции отправления поезда',
                settlement_title='Название города отправления поезда',
                departure=datetime(2019, 6, 1, 12),
            ),
            end_station=StationInfoFactory(
                id=2,
                title='Название станции прибытия поезда',
                settlement_title='Название города прибытия поезда',
                departure=datetime(2019, 6, 30, 12),
            ),
            from_station=StationInfoFactory(
                id=3,
                title='Название станции отправления пассажира',
                settlement_title='Название города отправления пассажира',
                departure=datetime(2019, 6, 10, 12),
            ),
            to_station=StationInfoFactory(
                id=4,
                title='Название станции прибытия пассажира',
                settlement_title='Название города прибытия пассажира',
                departure=datetime(2019, 6, 20, 12),
            ),
        ),
        rebooking_info=RebookingInfoFactory(
            enabled=True,
            service_class='Ф1',
            international_service_class='24/7',
        ),
        station_from_id=13,
        station_to_id=14,
        passengers=[
            PassengerFactory(
                customer_id='300100',
                tickets=[TicketFactory(
                    places=['1'],
                    tariff_info_code=TariffInfo.FULL_CODE,
                    payment=TicketPaymentFactory(
                        amount=Decimal('111.11'),
                        service_amount=Decimal('22.22'),
                        service_fee=Decimal('33.33'),
                        fee=Decimal('44.44'),
                        fee_percent=Decimal('5.5'),
                        fee_percent_range=Decimal('0.6'),
                        partner_fee=Decimal('7.7'),
                        tariff_vat=Tax(amount=Decimal('8.8'), rate=Decimal('9.9')),
                        service_vat=Tax(amount=Decimal('2.1'), rate=Decimal('2.3')),
                        commission_fee_vat=Tax(amount=Decimal('2.4'), rate=Decimal('2.5')),
                    )
                )],
                insurance=InsuranceFactory(
                    amount=Decimal('10.00'),
                    trust_order_id='trust_order_id1234',
                    operation_status=OperationStatus.OK,
                ),
            ),
            PassengerFactory(
                customer_id='300102',
                tickets=[TicketFactory(
                    places=[],
                    tariff_info_code=TariffInfo.CHILD_CODE,
                    payment=TicketPaymentFactory(
                        amount=Decimal('111.13'),
                        service_amount=Decimal('22.25'),
                        service_fee=Decimal('33.37'),
                        fee=Decimal('44.49'),
                        fee_percent=Decimal('5.1'),
                        fee_percent_range=Decimal('0.8'),
                        partner_fee=Decimal('7.9'),
                        tariff_vat=Tax(amount=Decimal('8.5'), rate=Decimal('9.1')),
                        service_vat=Tax(amount=Decimal('2.7'), rate=Decimal('2.1')),
                        commission_fee_vat=Tax(amount=Decimal('2.5'), rate=Decimal('2.9')),
                    )
                )],
            ),
        ],
        user_info=UserInfoFactory(
            ip='222.111.22.11',
            yandex_uid='1234567890123456789',
            uid='some-passport-uid',
        ),
        source=SourceFactory(
            req_id='req id',
            device=TrainPurchaseSource.TOUCH,
            utm_source='utm source',
            utm_medium='utm medium',
            utm_campaign='utm campaign',
            utm_term='utm term',
            utm_content='utm content',
            from_='source from',
            gclid='gclid',
            terminal='terminal',
            partner='partner',
            subpartner='subpartner',
            partner_uid='partner uid',
            test_id='test id',
            test_buckets='test buckets',
            icookie='icookie',
            serp_uuid='serp uuid',
        ),
    )

    result = CpaOrderResponseSchema.flatten_order(order)

    assert_that(result, has_entries(
        arrival=1560859200,
        arrival_str='2019-06-18T12:00:00Z',
        coach_number='2',
        coach_owner='ФПК СЕВЕРНЫЙ',
        coach_type='compartment',
        departure=1560855600,
        departure_str='2019-06-18T11:00:00Z',
        displayed_coach_owner='ФПК',
        finished_at=1558180800,
        finished_at_str='2019-05-18T12:00:00Z',
        gender='mixed',
        order_number='im-order-number',
        orders_created='',
        partner='im',
        partner_data_compartment_gender='mixed',
        partner_data_end_station_title=None,
        partner_data_expire_set_er=None,
        partner_data_expire_set_er_str=None,
        partner_data_im_order_id=100500,
        partner_data_is_only_full_return_possible=False,
        partner_data_is_order_cancelled=False,
        partner_data_is_reservation_prolonged=False,
        partner_data_is_suburban=None,
        partner_data_operation_id='100501',
        partner_data_reservation_datetime=None,
        partner_data_reservation_datetime_str=None,
        partner_data_start_station_title=None,
        partner_data_station_from_title=None,
        partner_data_station_to_title=None,
        payment_clear_at=None,
        payment_clear_at_str=None,
        payment_hold_at=None,
        payment_hold_at_str=None,
        payment_purchase_token=None,
        payment_status=None,
        payment_trust_created_at=1557057600,
        payment_trust_created_at_str='2019-05-05T12:00:00Z',
        payment_uid='aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
        payment_use_deferred_clearing=True,
        purchase_tokens_history='purchase_token_2, purchase_token_1',
        rebooking_available=True,
        route_info_end_station_departure=1561896000,
        route_info_end_station_departure_str='2019-06-30T12:00:00Z',
        route_info_end_station_id=2,
        route_info_end_station_settlement_title='Название города прибытия поезда',
        route_info_end_station_title='Название станции прибытия поезда',
        route_info_from_station_departure=1560168000,
        route_info_from_station_departure_str='2019-06-10T12:00:00Z',
        route_info_from_station_id=3,
        route_info_from_station_settlement_title='Название города отправления пассажира',
        route_info_from_station_title='Название станции отправления пассажира',
        route_info_start_station_departure=1559390400,
        route_info_start_station_departure_str='2019-06-01T12:00:00Z',
        route_info_start_station_id=1,
        route_info_start_station_settlement_title='Название города отправления поезда',
        route_info_start_station_title='Название станции отправления поезда',
        route_info_to_station_departure=1561032000,
        route_info_to_station_departure_str='2019-06-20T12:00:00Z',
        route_info_to_station_id=4,
        route_info_to_station_settlement_title='Название города прибытия пассажира',
        route_info_to_station_title='Название станции прибытия пассажира',
        scheme_id='33',
        service_class='Ф1',
        international_service_class='24/7',
        source_device='touch',
        source_from='source from',
        source_gclid='gclid',
        source_req_id='req id',
        source_terminal='terminal',
        source_utm_campaign='utm campaign',
        source_utm_content='utm content',
        source_utm_medium='utm medium',
        source_utm_source='utm source',
        source_utm_term='utm term',
        source_partner='partner',
        source_subpartner='subpartner',
        source_partner_uid='partner uid',
        source_test_id='test id',
        source_test_buckets='test buckets',
        source_icookie='icookie',
        source_serp_uuid='serp uuid',
        station_from_id=13,
        station_to_id=14,
        status='reserved',
        tickets_with_places_count=1,
        tickets_without_places_count=1,
        train_name=None,
        train_number='001A',
        train_ticket_number='002A',
        travel_status='In progress',
        two_storey=False,
        uid='12345678901234567890123456789012',
        user_ip='222.111.22.11',
        user_is_mobile=False,
        user_passport_uid='some-passport-uid',
        user_region_id=123,
        user_yandex_uid='1234567890123456789',
        profit_amount=Decimal('88.93') + Decimal('6.50'),
        order_amount=Decimal('321.17'),
        insurance_auto_return=False,
        total_tariff_amount=Decimal('177.77'),
        total_service_amount=Decimal('44.47'),
        total_fee_amount=Decimal('88.93'),
        total_insurance_amount=Decimal('10.00'),
        total_insurance_profit_amount=Decimal('6.50'),
        total_partner_fee_amount=Decimal('15.6'),
        total_refund_ticket_amount=Decimal(0),
        total_refund_fee_amount=Decimal(0),
        total_refund_insurance_amount=Decimal(0),
        total_partner_refund_fee_amount=Decimal(0),
        adult_passengers_count=1,
        children_with_seats_count=1,
        children_without_seats_count=0,
        requested_ticket_count=2,
        total_ticket_count=0,
        refunded_ticket_count=0,
        bought_insurance_count=1,
        payment_attempts=3,
    ))


def test_flatten_with_refunds():
    order = TrainOrderFactory(
        id=ObjectId.from_datetime(UTC.localize(datetime(1970, 1, 1, 1))),
        passengers=[
            PassengerFactory(
                customer_id='300100',
                tickets=[TicketFactory(
                    blank_id='1',
                    payment=TicketPaymentFactory(partner_refund_fee=Decimal('3.11')),
                    refund=TicketRefundFactory(
                        amount=Decimal('111.11'),
                        refund_yandex_fee_amount=Decimal('1.11'),
                    ),
                )],
            ),
            PassengerFactory(
                customer_id='300102',
                tickets=[TicketFactory(
                    blank_id='2',
                    payment=TicketPaymentFactory(partner_refund_fee=Decimal('3.12')),
                    refund=TicketRefundFactory(
                        amount=Decimal('111.12'),
                        refund_yandex_fee_amount=Decimal('1.12'),
                    ),
                )],
            ),
        ],
    )
    TrainRefundFactory(order_uid=order.uid, status=RefundStatus.NEW)
    TrainRefundFactory(
        uuid='12345678901234567890123456789012',
        order_uid=order.uid,
        status=RefundStatus.DONE,
        blank_ids=['1', '2'],
        created_at=datetime(2019, 1, 1, 10, 30),
        finished_at=datetime(2019, 2, 2, 12, 30),
    )

    result = CpaOrderResponseSchema.flatten_order(order)

    assert_that(result, has_entries(
        created_at=3600,
        refunds_count=1,
        total_refund_ticket_amount=Decimal('222.23'),
        total_refund_fee_amount=Decimal('2.23'),
        total_refund_insurance_amount=Decimal(0),
        total_partner_refund_fee_amount=Decimal('6.23'),
        refunded_ticket_count=2,
    ))
