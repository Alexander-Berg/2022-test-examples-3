# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from decimal import Decimal

import pytest
from hamcrest import assert_that, has_properties, contains

from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im.base import IM_DATETIME_FORMAT
from travel.rasp.train_api.train_partners.im.factories.order_info import ImRailwayOrderItemFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import (
    IM_ORDER_INFO_METHOD, get_order_info, get_order_info_by_reference_id, get_sorted_refund_ticket_items, OrderItem
)
from travel.rasp.train_api.train_purchase.core.enums import OperationStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory

ORDER_ITEM_INSURANCE_PURCHASE = ''''''

ORDER_INFO_RESULT = """
{
  "PosSysName": "yandex_test2",
  "OrderCustomers": [
    {
      "CitizenshipCode": "RU",
      "DocumentType": "RussianPassport",
      "OrderCustomerId": 158816,
      "FirstName": "Сидор",
      "MiddleName": "Сидорович",
      "LastName": "Сидоров",
      "Sex": "Male",
      "BirthDate": "1990-09-08T00:00:00",
      "DocumentNumber": "6504989898",
      "DocumentValidTill": null
    }
  ],
  "OrderItems": [
    {
      "IsExternallyLoaded": false,
      "ProviderPaymentForm": "Card",
      "AgentFeeCalculation": null,
      "ClientFeeCalculation": null,
      "ConfirmDateTime": "2017-12-26T11:37:24",
      "ConfirmTimeLimit": "2017-12-26T11:52:07",
      "CreateDateTime": "2017-12-26T11:37:07",
      "OrderItemCustomers": [
        {
          "ClientFeeCalculation": null,
          "Amount": 2179.5,
          "OrderItemCustomerId": 154773,
          "OrderCustomerId": 158816,
          "PlaceQuantity": 1,
          "Places": "001Н",
          "OrderItemBlankId": 770077,
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayOrderItemCustomerInfo, ApiContracts"
        }
      ],
      "CarrierDescription": "ТВЕРСКОЙ ЭКСПР / ООО ТВЕРСКОЙ ЭКСПР (7705506536)",
      "AdditionalInformation": ". ВОЗВРАТ ТОЛЬКО В КАССАХ АГЕНТА ТВЕРСКОЙ ЭКСПРЕСС ТЕЛ.8-800-7777-020",
      "ServiceClass": "2К",
      "PlaceReservationType": "Usual",
      "ElectronicRegistrationExpirationDateTime": "2018-01-23T23:20:00",
      "CarType": "Compartment",
      "CarNumber": "11",
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts",
      "ServiceType": "Tickets",
      "PlaceQuantity": 1,
      "OriginStationName": "МОСКВА ОКТ",
      "DestinationStationName": "С-ПЕТЕР-ГЛ",
      "TrainNumber": "020УА",
      "BookingTrainNumber": "020У",
      "TrainNumberToGetRoute": "020У",
      "OrderItemBlanks": [
        {
          "Amount": 2179.5,
          "BlankNumber": "70630813146736",
          "PreviousOrderItemBlankId": 0,
          "BlankStatus": "Returned",
          "TariffType": "Full",
          "VatRateValues": [
            {"Value": 0, "Rate": 0},
            {"Value": 33.56, "Rate": 18}
          ],
          "ServicePrice": 220,
          "AdditionalPrice": 1164,
          "BaseFare": 1015.5,
          "VoucherNumber": null,
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayOrderItemBlankInfo, ApiContracts",
          "IsElectronicRegistrationSet": false,
          "IsMealOptionPossible": false,
          "PendingElectronicRegistration": "NoValue",
          "ElectronicRegistrationSetDateTime": "2017-12-26T11:37:00",
          "SignSequence": null,
          "TariffInfo": {
            "TariffName": "ПОЛНЫЙ",
            "TariffType": "Full"
          },
          "PlaceQuantity": 1,
          "OrderItemBlankId": 770077
        }
      ],
      "ArrivalDateTime": "2018-01-24T08:59:00",
      "OriginLocationCode": "2006004",
      "OriginLocationName": "МОСКВА ОКТЯБРЬСКАЯ",
      "DestinationLocationCode": "2004001",
      "DestinationLocationName": "САНКТ-ПЕТЕРБУРГ-ГЛАВН.",
      "OrderId": 42,
      "AgentReferenceId": "",
      "OrderItemId": 110011,
      "PosSysName": "yandex_test2",
      "Amount": 2179.5,
      "ReservationNumber": "70630813146736",
      "OperationType": "Purchase",
      "SimpleOperationStatus": "Succeeded",
      "DetailedOperationStatus": "Succeeded",
      "DepartureDateTime": "2018-01-24T00:20:00"
    },
    {
      "IsExternallyLoaded": false,
      "ProviderPaymentForm": "Card",
      "AgentFeeCalculation": null,
      "ClientFeeCalculation": null,
      "ConfirmDateTime": "2017-12-26T11:39:18",
      "ConfirmTimeLimit": null,
      "CreateDateTime": "2017-12-26T11:39:18",
      "OrderItemCustomers": [
        {
          "ClientFeeCalculation": null,
          "Amount": 1986.8,
          "OrderItemCustomerId": 154780,
          "OrderCustomerId": 158816,
          "PlaceQuantity": 0,
          "Places": "",
          "OrderItemBlankId": 550055,
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayOrderItemCustomerInfo, ApiContracts"
        }
      ],
      "CarrierDescription": "ТВЕРСКОЙ ЭКСПР / ООО ТВЕРСКОЙ ЭКСПР (7705506536)",
      "AdditionalInformation": ". ВОЗВРАТ ТОЛЬКО В КАССАХ АГЕНТА ТВЕРСКОЙ ЭКСПРЕСС ТЕЛ.8-800-7777-020",
      "ServiceClass": "2К",
      "PlaceReservationType": "Usual",
      "ElectronicRegistrationExpirationDateTime": "2018-01-23T23:20:00",
      "CarType": "Compartment",
      "CarNumber": "11",
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts",
      "ServiceType": "Tickets",
      "PlaceQuantity": 1,
      "OriginStationName": "МОСКВА ОКТ",
      "DestinationStationName": "С-ПЕТЕР-ГЛ",
      "TrainNumber": "020УА",
      "BookingTrainNumber": "020У",
      "TrainNumberToGetRoute": "020У",
      "OrderItemBlanks": [
        {
          "Amount": 1986.8,
          "BlankNumber": "70630813146736",
          "PreviousOrderItemBlankId": 770077,
          "BlankStatus": "ElectronicRegistrationAbsent",
          "TariffType": "Full",
          "VatRateValues": [
            {"Value": 0, "Rate": 0},
            {"Value": 33.56, "Rate": 18},
            {"Value": 0, "Rate": 18},
            {"Value": 29.39, "Rate": 18}
          ],
          "ServicePrice": 0,
          "AdditionalPrice": 0,
          "BaseFare": 0,
          "VoucherNumber": "40630771588311",
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayOrderItemBlankInfo, ApiContracts",
          "IsElectronicRegistrationSet": false,
          "IsMealOptionPossible": false,
          "PendingElectronicRegistration": "NoValue",
          "ElectronicRegistrationSetDateTime": null,
          "SignSequence": null,
          "TariffInfo": {
            "TariffName": "ПОЛНЫЙ",
            "TariffType": "Full"
          },
          "PlaceQuantity": 1,
          "OrderItemBlankId": 550055
        }
      ],
      "ArrivalDateTime": "2018-01-24T08:59:00",
      "OriginLocationCode": "2006004",
      "OriginLocationName": "МОСКВА ОКТЯБРЬСКАЯ",
      "DestinationLocationCode": "2004001",
      "DestinationLocationName": "САНКТ-ПЕТЕРБУРГ-ГЛАВН.",
      "OrderId": 42,
      "AgentReferenceId": "3b8ed442c8:770077:refund",
      "OrderItemId": 220022,
      "PosSysName": "yandex_test2",
      "Amount": 1986.8,
      "ReservationNumber": "70630813146736",
      "OperationType": "Return",
      "SimpleOperationStatus": "InProcess",
      "DetailedOperationStatus": "InProcess",
      "DepartureDateTime": "2018-01-25T00:20:00"
    },
    {
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Insurance.RailwayInsuranceFullOrderItemInfo, ApiContracts",
      "AgentFeeCalculation": {
        "Charge": 0.0,
        "Profit": 65.0
      },
      "AgentReferenceId": "",
      "Amount": 100.0,
      "ClientFeeCalculation": null,
      "ConfirmDateTime": "2019-02-19T13:53:37",
      "ConfirmTimeLimit": "2019-03-01T22:35:00",
      "CreateDateTime": "2019-02-19T13:52:59",
      "DepartureDateTime": "2019-03-01T22:35:00",
      "DetailedOperationStatus": "Succeeded",
      "IsExternallyLoaded": false,
      "MainOrderItemId": 110011,
      "OperationType": "Purchase",
      "OrderId": 42,
      "OrderItemBlanks": [
        {
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Insurance.InsuranceOrderItemBlankInfo, ApiContracts",
          "Amount": 100.0,
          "BlankNumber": "001IMZD-U1.000895-ZD",
          "OrderItemBlankId": 821790,
          "PreviousOrderItemBlankId": 0
        }
      ],
      "OrderItemCustomers": [
        {
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Insurance.InsuranceOrderItemCustomerInfo, ApiContracts",
          "AgentFeeCalculation": {
            "Charge": 0.0,
            "Profit": 65.0
          },
          "Amount": 100.0,
          "ClientFeeCalculation": null,
          "OrderCustomerId": 158816,
          "OrderItemCustomerId": 1019539
        }
      ],
      "OrderItemId": 703560,
      "Package": "AccidentWithFloatPremium",
      "PosSysName": "yandex_2test",
      "PreviousOrderItemId": 0,
      "ProviderPaymentForm": null,
      "ReservationNumber": "25287",
      "SimpleOperationStatus": "Succeeded",
      "Supplier": "Renessans"
    },
    {
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Insurance.RailwayInsuranceFullOrderItemInfo, ApiContracts",
      "AgentFeeCalculation": {
        "Charge": 0.0,
        "Profit": -65.0
      },
      "AgentReferenceId": "eeb7343b89146a3619d0cef06:703560",
      "Amount": 100.0,
      "ClientFeeCalculation": null,
      "ConfirmDateTime": "2019-02-19T13:56:55",
      "ConfirmTimeLimit": null,
      "CreateDateTime": "2019-02-19T13:56:54",
      "DepartureDateTime": "2019-03-01T22:35:00",
      "DetailedOperationStatus": "Succeeded",
      "IsExternallyLoaded": false,
      "MainOrderItemId": 110011,
      "OperationType": "Return",
      "OrderId": 42,
      "OrderItemBlanks": [
        {
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Insurance.InsuranceOrderItemBlankInfo, ApiContracts",
          "Amount": 100.0,
          "BlankNumber": "001IMZD-U1.000895-ZD",
          "OrderItemBlankId": 821808,
          "PreviousOrderItemBlankId": 821790
        }
      ],
      "OrderItemCustomers": [
        {
          "$type": "ApiContracts.Order.V1.Info.OrderItem.Insurance.InsuranceOrderItemCustomerInfo, ApiContracts",
          "AgentFeeCalculation": {
            "Charge": 0.0,
            "Profit": -65.0
          },
          "Amount": 100.0,
          "ClientFeeCalculation": null,
          "OrderCustomerId": 158816,
          "OrderItemCustomerId": 1019558
        }
      ],
      "OrderItemId": 703579,
      "Package": "AccidentWithFloatPremium",
      "PosSysName": "yandex_2test",
      "PreviousOrderItemId": 703560,
      "ProviderPaymentForm": null,
      "ReservationNumber": "25287",
      "SimpleOperationStatus": "Succeeded",
      "Supplier": "Renessans"
    }
  ],
  "OrderId": 42,
  "Amount": 192.7,
  "ContactPhone": "",
  "ContactEmails": [""],
  "Created": "2017-12-26T11:37:07",
  "Confirmed": "2017-12-26T11:37:24"
}
"""

EXPECTED = dict(
    buy_operation_id='110011',
    expire_set_er=MSK_TZ.localize(datetime(2018, 1, 23, 23, 20)),
    status=OperationStatus.IN_PROCESS,
    order_num='70630813146736',
    reserved_to=MSK_TZ.localize(datetime(2017, 12, 26, 11, 52, 7)),
    tickets=contains(
        has_properties(
            refund_blank_id='550055',
            rzhd_status=RzhdStatus.REFUNDED,
            blank_id='770077',
            amount=Decimal('2179.5'),
            refund_operation_id='220022',
            refund_amount=Decimal('1986.8')
        )
    ),
    passengers=contains(
        has_properties(
            blank_id='770077',
            doc_id='6504989898',
            refund_blank_id='550055',
        )
    ),
    insurances=contains(
        has_properties(
            operation_id='703560',
            amount=Decimal(100),
            operation_status=OperationStatus.OK,
            reference_id="eeb7343b89146a3619d0cef06:703560",
            refund_operation_id='703579',
            is_external=False,
            refund_amount=Decimal(100),
            customer_id='158816',
        )
    )
)


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_get_order_info(httpretty):
    order = TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=42, operation_id='110011'))
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=ORDER_INFO_RESULT)
    result = get_order_info(order)
    request = httpretty.last_request
    assert request.body == '{"OrderId": 42}'
    assert_that(result, has_properties(**EXPECTED))


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_get_order_info_by_reference_id(httpretty):
    order = TrainOrderFactory()
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=ORDER_INFO_RESULT)
    result = get_order_info_by_reference_id(order, '3b8ed442c8:770077:refund')
    request = httpretty.last_request
    assert request.body == '{"AgentReferenceId": "3b8ed442c8:770077:refund"}'
    assert_that(result, has_properties(**EXPECTED))


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_get_order_info_by_reference_id_interrupted(httpretty):
    order = TrainOrderFactory()
    mock_im(httpretty, IM_ORDER_INFO_METHOD,
            body=ORDER_INFO_RESULT.replace('"BlankStatus": "Returned"', '"BlankStatus": "TripWasInterrupted"'))
    result = get_order_info_by_reference_id(order, '3b8ed442c8:770077:refund')
    assert_that(result, has_properties(tickets=contains(has_properties(rzhd_status=RzhdStatus.INTERRUPTED))))


class TestGetSortedRefundOrderItems(object):
    @staticmethod
    def create_order_item(id, dt, is_refund):
        return ImRailwayOrderItemFactory(
            id=id,
            CreateDateTime=datetime.strftime(dt, IM_DATETIME_FORMAT),
            OperationType='Return' if is_refund else 'Purchase'
        )

    @pytest.mark.parametrize('order_items_data, sorted_refund_order_item_ids', (
        (
            (
                {'id': 1, 'dt': datetime(2018, 1, 1), 'is_refund': False},
                {'id': 2, 'dt': datetime(2018, 1, 2), 'is_refund': True},
                {'id': 3, 'dt': datetime(2018, 1, 3), 'is_refund': True},
            ),
            [2, 3]
        ),
        (
            (
                {'id': 1, 'dt': datetime(2018, 1, 3), 'is_refund': True},
                {'id': 2, 'dt': datetime(2018, 1, 2), 'is_refund': True},
            ),
            [2, 1]
        ),
        (
            (
                {'id': 1, 'dt': datetime(2018, 1, 3), 'is_refund': True},
                {'id': 2, 'dt': datetime(2018, 1, 2), 'is_refund': True},
                {'id': 3, 'dt': datetime(2018, 1, 5), 'is_refund': False},
            ),
            [2, 1]
        ),
    ))
    def test_get_sorted_refund_order_items(self, order_items_data, sorted_refund_order_item_ids):
        order_items = [OrderItem(self.create_order_item(**order_item_data)) for order_item_data in order_items_data]
        sorted_refund_order_items = get_sorted_refund_ticket_items(order_items)
        assert [order_item['id'] for order_item in sorted_refund_order_items] == sorted_refund_order_item_ids


class TestOrderInfo(object):
    def test_reserved_to(self):
        order_item = OrderItem(ImRailwayOrderItemFactory(ConfirmTimeLimit='2018-11-13T14:15:00'))
        assert order_item.reserved_to == MSK_TZ.localize(datetime(2018, 11, 13, 14, 15))

    def expire_set_er(self):
        order_item = OrderItem(
            ImRailwayOrderItemFactory(ElectronicRegistrationExpirationDateTime='2018-11-13T14:15:00')
        )
        assert order_item.expire_set_er == MSK_TZ.localize(datetime(2018, 11, 13, 14, 15))

    @pytest.mark.parametrize('operation_type, expected', [
        ('Purchase', True),
        ('Return', False)
    ])
    def test_is_buy(self, operation_type, expected):
        order_item = OrderItem(ImRailwayOrderItemFactory(OperationType=operation_type))
        assert order_item.is_buy is expected

    @pytest.mark.parametrize('operation_type, expected', [
        ('Purchase', False),
        ('Return', True)
    ])
    def test_is_refund(self, operation_type, expected):
        order_item = OrderItem(ImRailwayOrderItemFactory(OperationType=operation_type))
        assert order_item.is_refund is expected

    @pytest.mark.parametrize('item_type, expected', [
        ('ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts', True),
        ('ApiContracts.Order.V1.Info.OrderItem.Insurance.RailwayInsuranceFullOrderItemInfo, ApiContracts', False)
    ])
    def test_is_ticket(self, item_type, expected):
        order_item = OrderItem(ImRailwayOrderItemFactory(**{'$type': item_type}))
        assert order_item.is_ticket is expected

    @pytest.mark.parametrize('item_type, expected', [
        ('ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts', False),
        ('ApiContracts.Order.V1.Info.OrderItem.Insurance.RailwayInsuranceFullOrderItemInfo, ApiContracts', True)
    ])
    def test_is_insurance(self, item_type, expected):
        order_item = OrderItem(ImRailwayOrderItemFactory(**{'$type': item_type}))
        assert order_item.is_insurance is expected

    def test_creation_dt(self):
        order_item = OrderItem(ImRailwayOrderItemFactory(CreateDateTime='2018-11-13T14:15:00'))
        assert order_item.creation_dt == MSK_TZ.localize(datetime(2018, 11, 13, 14, 15))
