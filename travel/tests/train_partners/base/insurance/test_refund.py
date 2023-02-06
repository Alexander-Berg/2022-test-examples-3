# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest

from travel.rasp.train_api.train_partners.base.insurance.refund import insurance_refund, check_insurance_refund
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.im.insurance.refund import IM_INSURANCE_RETURN_METHOD
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, InsuranceFactory, PartnerDataFactory
)

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

IM_INSURANCE_REFUND_RESPONSE = '''{
  "Amount": 100.0,
  "OrderCustomerId": 1001832,
  "OrderId": 653265,
  "OrderItemId": 703579
}'''


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_insurance_refund(httpretty):
    insurance_id = '703579'
    order = TrainOrderFactory(passengers=[PassengerFactory(insurance=InsuranceFactory(operation_id=insurance_id))])
    mock_im(httpretty, IM_INSURANCE_RETURN_METHOD, body=IM_INSURANCE_REFUND_RESPONSE)
    result = insurance_refund(order=order, insurance_id=insurance_id, reference_id='reference_id')

    assert result and result.insurance_refund
    assert result.insurance_refund.amount == Decimal(100)
    assert result.insurance_refund.buy_operation_id == insurance_id


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_check_insurance_refund(httpretty):
    order = TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=42, operation_id='110011'))
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=ORDER_INFO_RESULT)
    result = check_insurance_refund(order, 'eeb7343b89146a3619d0cef06:703560')

    assert result and result.insurance_refund
    assert result.insurance_refund.amount == Decimal(100)
    assert result.insurance_refund.buy_operation_id == '703560'
