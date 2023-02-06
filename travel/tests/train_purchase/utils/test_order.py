# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, timedelta
from decimal import Decimal

import mock as mock
import pytest
from hamcrest import assert_that, has_properties, contains_inanyorder

import travel.rasp.train_api.train_purchase.utils.order
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from common.utils.date import UTC_TZ
from travel.rasp.library.python.common23.date.environment import now_utc
from travel.rasp.train_api.train_partners.base import PartnerError, RzhdStatus
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult, PassengerInfo, TicketInfo
from travel.rasp.train_api.train_partners.base.test_utils import create_blank
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.im.refund_amount import RETURN_AMOUNT_ENDPOINT
from travel.rasp.train_api.train_partners.im.update_order import IM_UPDATE_BLANKS_METHOD
from travel.rasp.train_api.train_partners.ufs.get_order_info import TRANSACTION_INFO_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.refund_amount import REFUND_AMOUNT_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, OperationStatus, OrderWarningCode
from travel.rasp.train_api.train_purchase.core.factories import (
    PassengerFactory, TicketFactory, TrainOrderFactory, TicketPaymentFactory, OrderRouteInfoFactory, StationInfoFactory
)
from travel.rasp.train_api.train_purchase.core.models import TrainPartner
from travel.rasp.train_api.train_purchase.utils.order import (
    update_refund_amount, get_blanks_with_refund_amount, update_tickets_statuses, UpdateTicketStatusMode,
    get_order_warnings, INSURANCE_WARNING_ACTUAL_TIME, get_first_actual_warning,
)
from travel.rasp.train_api.train_purchase.views.test_utils import create_order

pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


def test_update_refund_amount(httpretty):
    order = create_order(status=OrderStatus.DONE)
    mock_ufs(httpretty, TRANSACTION_INFO_ENDPOINT, body='''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
  <TStatus>0</TStatus>
  <TransID>48716057</TransID>
  <Passenger ID="88940393" BlankID="71130150">
    <Type>ПЛ</Type>
    <DocType>ПН</DocType>
    <DocNum>1111222222</DocNum>
    <Name>АВВ ПАА АВВ</Name>
    <Place>041</Place>
    <PlaceTier>-</PlaceTier>
    <R>ЖЕН</R>
    <BirthDay>12.12.1980</BirthDay>
  </Passenger>
</UFS_RZhD_Gate>''')
    mock_ufs(httpretty, REFUND_AMOUNT_ENDPOINT, body='''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
  <Status>0</Status>
  <Blank ID="1" PrevID="0">
    <Amount>2000.10</Amount>
    <STV1>1.1</STV1>
    <STV2>2.2</STV2>
    <STV3>3.3</STV3>
    <STV4>4.4</STV4>
    <ETF4>1.11</ETF4>
    <ETF5>2.22</ETF5>
    <ETFC>3.33</ETFC>
    <ETFB>4.44</ETFB>
  </Blank>
  <Blank ID="2" PrevID="0">
    <Amount>3000.10</Amount>
    <STV1>1.0</STV1>
    <STV2>2.0</STV2>
    <STV3>3.0</STV3>
    <STV4>4.0</STV4>
    <ETF4>1.01</ETF4>
    <ETF5>2.02</ETF5>
    <ETFC>3.03</ETFC>
    <ETFB>4.04</ETFB>
  </Blank>
</UFS_RZhD_Gate>''')
    update_refund_amount(order, blank_ids=['1', '2'])
    assert_that(order.passengers[0].tickets[0].refund, has_properties(
        amount=Decimal('2000.1'),
        tariff_vat=has_properties(rate=Decimal('1.1'), amount=Decimal('1.11')),
        service_vat=has_properties(rate=Decimal('2.2'), amount=Decimal('2.22')),
        commission_fee_vat=has_properties(rate=Decimal('3.3'), amount=Decimal('3.33')),
        refund_commission_fee_vat=has_properties(rate=Decimal('4.4'), amount=Decimal('4.44')),
    ))
    assert_that(order.passengers[1].tickets[0].refund, has_properties(
        amount=Decimal('3000.1'),
        tariff_vat=has_properties(rate=Decimal('1.0'), amount=Decimal('1.01')),
        service_vat=has_properties(rate=Decimal('2.0'), amount=Decimal('2.02')),
        commission_fee_vat=has_properties(rate=Decimal('3.0'), amount=Decimal('3.03')),
        refund_commission_fee_vat=has_properties(rate=Decimal('4.0'), amount=Decimal('4.04')),
    ))


@replace_now('2018-01-01')
@pytest.mark.parametrize('finished_at_lambda, expected_refund_yandex_fee', [
    (
        None,
        Decimal(0),
    ),
    (
        lambda: now_utc() - timedelta(hours=25),
        Decimal(0),
    ),
    (
        lambda: now_utc() - timedelta(hours=12),
        Decimal('20'),
    ),
])
def test_update_refund_amount_with_yandex_fee(httpretty, finished_at_lambda, expected_refund_yandex_fee):
    order = create_order(
        status=OrderStatus.DONE,
        finished_at=finished_at_lambda() if finished_at_lambda else None,
    )
    mock_ufs(httpretty, TRANSACTION_INFO_ENDPOINT, body='''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
  <TStatus>0</TStatus>
  <TransID>48716057</TransID>
  <Passenger ID="88940393" BlankID="71130150">
    <Type>ПЛ</Type>
    <DocType>ПН</DocType>
    <DocNum>1111222222</DocNum>
    <Name>АВВ ПАА АВВ</Name>
    <Place>041</Place>
    <PlaceTier>-</PlaceTier>
    <R>ЖЕН</R>
    <BirthDay>12.12.1980</BirthDay>
  </Passenger>
</UFS_RZhD_Gate>''')
    mock_ufs(httpretty, REFUND_AMOUNT_ENDPOINT, body='''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
  <Status>0</Status>
  <Blank ID="1" PrevID="0">
    <Amount>2000.10</Amount>
    <STV1>1.1</STV1>
    <STV2>2.2</STV2>
    <STV3>3.3</STV3>
    <STV4>4.4</STV4>
    <ETF4>1.11</ETF4>
    <ETF5>2.22</ETF5>
    <ETFC>3.33</ETFC>
    <ETFB>4.44</ETFB>
  </Blank>
</UFS_RZhD_Gate>''')

    update_refund_amount(order, blank_ids=['1'])

    assert_that(order.passengers[0].tickets[0].refund, has_properties(
        amount=Decimal('2000.1'),
        tariff_vat=has_properties(rate=Decimal('1.1'), amount=Decimal('1.11')),
        service_vat=has_properties(rate=Decimal('2.2'), amount=Decimal('2.22')),
        commission_fee_vat=has_properties(rate=Decimal('3.3'), amount=Decimal('3.33')),
        refund_commission_fee_vat=has_properties(rate=Decimal('4.4'), amount=Decimal('4.44')),
        refund_yandex_fee_amount=expected_refund_yandex_fee,
    ))


def test_update_refund_amount_blank_with_zero_amount(httpretty):
    order = create_order(status=OrderStatus.DONE)
    order.passengers[1].tickets[0].payment = TicketPaymentFactory(amount=0)
    order.save()
    mock_ufs(httpretty, TRANSACTION_INFO_ENDPOINT, body="""
        <?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
        <UFS_RZhD_Gate>
            <TStatus>0</TStatus>
            <TransID>48716057</TransID>
            <Passenger ID="88940393" BlankID="2">
                <DocType>СР</DocType>
                <DocNum>1111222222</DocNum>
            </Passenger>
        </UFS_RZhD_Gate>""".strip())
    mock_ufs(httpretty, REFUND_AMOUNT_ENDPOINT, body="""
        <?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
        <UFS_RZhD_Gate>
            <Status>0</Status>
            <Blank ID="2" PrevID="0">
                <Amount>0.00</Amount>
            </Blank>
        </UFS_RZhD_Gate>""".strip())
    update_refund_amount(order, blank_ids=['2'])
    assert order.passengers[1].tickets[0].refund is None


@replace_setting('TRY_HARD_NEVER_SLEEP', True)
def test_update_refund_amount_get_order_info_retry(httpretty):
    mock_ufs(httpretty, REFUND_AMOUNT_ENDPOINT, body='''<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
    <UFS_RZhD_Gate>
      <Status>0</Status>
      <Blank ID="1" PrevID="0">
        <Amount>2000.10</Amount>
        <STV1>1.1</STV1>
        <STV2>2.2</STV2>
        <STV3>3.3</STV3>
        <STV4>4.4</STV4>
        <ETF4>1.11</ETF4>
        <ETF5>2.22</ETF5>
        <ETFC>3.33</ETFC>
        <ETFB>4.44</ETFB>
      </Blank>
      <Blank ID="2" PrevID="0">
        <Amount>3000.10</Amount>
        <STV1>1.0</STV1>
        <STV2>2.0</STV2>
        <STV3>3.0</STV3>
        <STV4>4.0</STV4>
        <ETF4>1.01</ETF4>
        <ETF5>2.02</ETF5>
        <ETFC>3.03</ETFC>
        <ETFB>4.04</ETFB>
      </Blank>
    </UFS_RZhD_Gate>''')
    order = create_order(status=OrderStatus.DONE)
    with mock.patch('travel.rasp.train_api.train_purchase.utils.order.get_order_info', side_effect=[
            PartnerError(42, 'zagzag'),
            OrderInfoResult(buy_operation_id=None, expire_set_er=None, status=None, order_num=None,
                            passengers=[PassengerInfo(None, None, '1111222222')])
    ]):
        update_refund_amount(order, blank_ids=['1', '2'])

    assert_that(order.passengers[0].tickets[0].refund, has_properties(
        amount=Decimal('2000.1'),
        tariff_vat=has_properties(rate=Decimal('1.1'), amount=Decimal('1.11')),
        service_vat=has_properties(rate=Decimal('2.2'), amount=Decimal('2.22')),
        commission_fee_vat=has_properties(rate=Decimal('3.3'), amount=Decimal('3.33')),
        refund_commission_fee_vat=has_properties(rate=Decimal('4.4'), amount=Decimal('4.44')),
    ))
    assert_that(order.passengers[1].tickets[0].refund, has_properties(
        amount=Decimal('3000.1'),
        tariff_vat=has_properties(rate=Decimal('1.0'), amount=Decimal('1.01')),
        service_vat=has_properties(rate=Decimal('2.0'), amount=Decimal('2.02')),
        commission_fee_vat=has_properties(rate=Decimal('3.0'), amount=Decimal('3.03')),
        refund_commission_fee_vat=has_properties(rate=Decimal('4.0'), amount=Decimal('4.04')),
    ))


def test_im_update_refund_amount_1_blank_2_passengers(httpretty):
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        status=OrderStatus.DONE,
        partner_data=dict(operation_id='1'),
        passengers=[
            PassengerFactory(tickets=[TicketFactory(blank_id='1', places=['11'])]),
            PassengerFactory(tickets=[TicketFactory(blank_id='1', places=[], payment=TicketPaymentFactory(amount=0))])
        ],
    )
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body="""{
        "OrderCustomers":[
            {
                "OrderCustomerId":108790,
                "FirstName":"Лялька",
                "MiddleName":"Ляльковна",
                "LastName":"Лялька",
                "Sex":"Female",
                "BirthDate":"2016-09-08T00:00:00",
                "DocumentNumber":"XXвв123123",
                "DocumentValidTill":null,
                "DocumentType":"BirthCertificate",
                "CitizenshipCode":"RU"
            },
            {
                "OrderCustomerId":108789,
                "FirstName":"Взрослый",
                "MiddleName":"Взрослович",
                "LastName":"Взрослый",
                "Sex":"Male",
                "BirthDate":"1990-09-08T00:00:00",
                "DocumentNumber":"6504321321",
                "DocumentValidTill":null,
                "DocumentType":"RussianPassport",
                "CitizenshipCode":"RU"
            }
        ],
        "OrderItems":[
            {
                "$type":"ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts",
                "ServiceType":"Tickets",
                "PlaceQuantity":1,
                "OriginStationName":"МОСКВА ОКТ",
                "DestinationStationName":"С-ПЕТЕР-ГЛ",
                "TrainNumber":"062АА",
                "BookingTrainNumber":"062А",
                "TrainNumberToGetRoute":"062А",
                "CarNumber":"13",
                "CarType":"Compartment",
                "ElectronicRegistrationExpirationDateTime":"2017-08-24T00:53:00",
                "PlaceReservationType":"Usual",
                "ServiceClass":"2У",
                "AdditionalInformation":"РАЗРЕШЕН ПРОВОЗ ЖИВОТНЫХ. ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ",
                "CarrierDescription":"ФПК СЕВ-ЗАПАДНЫЙ / АО ФПК (7708709686)",
                "OrderItemCustomers":[
                    {
                        "$type":"ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayOrderItemCustomerInfo, ApiContracts",
                        "OrderItemBlankId":1,
                        "Places":"006В",
                        "PlaceQuantity":1,
                        "OrderCustomerId":108789,
                        "OrderItemCustomerId":104740,
                        "Amount":1618.0,
                        "ClientFeeCalculation":null
                    },
                    {
                        "$type":"ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayOrderItemCustomerInfo, ApiContracts",
                        "OrderItemBlankId":1,
                        "Places":"",
                        "PlaceQuantity":0,
                        "OrderCustomerId":108790,
                        "OrderItemCustomerId":104741,
                        "Amount":0.0,
                        "ClientFeeCalculation":null
                    }
                ],
                "OrderItemBlanks":[
                    {
                        "$type":"ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayOrderItemBlankInfo, ApiContracts",
                        "VoucherNumber":null,
                        "BaseFare":736.0,
                        "AdditionalPrice":882.0,
                        "ServicePrice":0.0,
                        "VatRateValues":[
                            {"Rate":0.0, "Value":0.0},
                            {"Rate":18.0, "Value":22.58}
                        ],
                        "TariffType":"Full",
                        "BlankStatus":"ElectronicRegistrationPresent",
                        "IsElectronicRegistrationSet":true,
                        "IsMealOptionPossible":false,
                        "PendingElectronicRegistration":"NoValue",
                        "ElectronicRegistrationSetDateTime":"2017-08-18T14:09:00",
                        "PlaceQuantity":1,
                        "OrderItemBlankId":1,
                        "PreviousOrderItemBlankId":0,
                        "BlankNumber":"75920793969750",
                        "Amount":1618.0
                    }
                ],
                "OriginLocationCode":"2006004",
                "OriginLocationName":"МОСКВА ОКТЯБРЬСКАЯ",
                "DestinationLocationCode":"2004001",
                "DestinationLocationName":"САНКТ-ПЕТЕРБУРГ-ГЛАВН.",
                "OrderId":80349,
                "AgentReferenceId":"",
                "OrderItemId":1,
                "Amount":1618.0,
                "ReservationNumber":"75920793969750",
                "OperationType":"Purchase",
                "SimpleOperationStatus":"Succeeded",
                "DetailedOperationStatus":"Succeeded",
                "DepartureDateTime":"2017-08-24T01:53:00",
                "CreateDateTime":"2017-08-18T14:03:43",
                "ConfirmTimeLimit":"2017-08-18T14:18:43",
                "ConfirmDateTime":"2017-08-18T14:09:34",
                "ClientFeeCalculation":null,
                "AgentFeeCalculation":null,
                "ProviderPaymentForm":"Card",
                "IsExternallyLoaded":false
            }
        ],
        "OrderId":80349,
        "Amount":1618.0,
        "ContactPhone":"",
        "ContactEmails":[""],
        "Created":"2017-08-18T14:03:43",
        "Confirmed":"2017-08-18T14:09:34",
        "PosSysName":"yandex_test"
    }""")
    mock_im(httpretty, RETURN_AMOUNT_ENDPOINT, body="""{
        "ServiceReturnResponse":{
            "$type":"ApiContracts.Railway.V1.Messages.Return.RailwayReturnAmountResponse, ApiContracts",
            "Blanks":[
                {
                    "PurchaseOrderItemBlankId":1,
                    "ReturnOrderItemBlankId":0,
                    "Amount":1425.3,
                    "VatRateValues":[
                        {"Rate":1.1, "Value":1.11},
                        {"Rate":2.2, "Value":2.22},
                        {"Rate":3.3, "Value":3.33},
                        {"Rate":4.4, "Value":4.44}
                    ]
                }
            ],
            "Amount":7777.77
        }
    }""")

    update_refund_amount(order, blank_ids=['1'])

    assert_that(order.passengers[0].tickets[0].refund, has_properties(
        amount=Decimal('1425.3'),
        tariff_vat=has_properties(rate=Decimal('1.1'), amount=Decimal('1.11')),
        service_vat=has_properties(rate=Decimal('2.2'), amount=Decimal('2.22')),
        commission_fee_vat=has_properties(rate=Decimal('3.3'), amount=Decimal('3.33')),
        refund_commission_fee_vat=has_properties(rate=Decimal('4.4'), amount=Decimal('4.44')),
    ))
    assert order.passengers[1].tickets[0].refund is None


def test_get_blank_id_to_refund_amount_for_order():
    """Проверяем получение сумм к возврату, в случае если возвращаем все билеты"""
    order = create_order(status=OrderStatus.DONE)
    with mock.patch.object(travel.rasp.train_api.train_purchase.utils.order, 'get_refund_amount',
                           autospec=True) as m_get_refund_amount:
        blank = create_blank('1', Decimal(100))
        blank2 = create_blank('2', Decimal(200))
        m_get_refund_amount.return_value = [blank, blank2]

        blanks = get_blanks_with_refund_amount(order, 'foo', blank_ids=['1', '2'])

        assert m_get_refund_amount.call_count == 1
        assert_that(blanks, contains_inanyorder(blank, blank2))


IM_UPDATE_BLANKS_OK_RESULT = """{
  "Blanks": [
    {
      "OrderItemBlankId": 1,
      "Number": "71234567890000",
      "BlankStatus": "ElectronicRegistrationPresent",
      "PendingElectronicRegistration": "NoValue"
    },
    {
      "OrderItemBlankId": 2,
      "Number": "71234567890000",
      "BlankStatus": "Returned",
      "PendingElectronicRegistration": "NoValue"
    }
  ]
}"""

IM_UPDATE_BLANKS_ERROR_RESULT = """{
  "Code": 100,
  "Message": "Error",
}"""

IM_UPDATE_BLANKS_ERROR_61_RESULT = """{
  "Code": 61,
  "Message": "Статус заказа или его позиции не подходит для выполнения данной операции",
  "MessageParams": []
}"""

IM_ORDER_INFO_RESULT = """{
  "Amount": 3971.7,
  "OrderCustomers": [
    {
      "BirthDate": "1981-09-26T00:00:00",
      "DocumentNumber": "6504888888",
      "OrderCustomerId": 163019
    },
    {
      "BirthDate": "1982-09-26T00:00:00",
      "DocumentNumber": "6504555555",
      "OrderCustomerId": 163018
    }
  ],
  "OrderId": 440044,
  "OrderItems": [
    {
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts",
      "ConfirmTimeLimit": "2017-10-20T17:30:00",
      "CreateDateTime": "2017-10-20T17:40:05",
      "ElectronicRegistrationExpirationDateTime": "2018-01-07T00:15:00",
      "OperationType": "Purchase",
      "OrderId": 440044,
      "OrderItemBlanks": [
        {
          "Amount": 3779.0,
          "BlankStatus": "ElectronicRegistrationPresent",
          "ElectronicRegistrationSetDateTime": "2018-01-01T06:45:00",
          "IsElectronicRegistrationSet": true,
          "OrderItemBlankId": 1,
          "PendingElectronicRegistration": "NoValue",
          "PreviousOrderItemBlankId": 0
        },
        {
          "Amount": 3415.9,
          "BlankStatus": "Returned",
          "ElectronicRegistrationSetDateTime": "2018-01-01T06:45:00",
          "IsElectronicRegistrationSet": false,
          "OrderItemBlankId": 2,
          "PendingElectronicRegistration": "NoValue",
          "PreviousOrderItemBlankId": 0
        }
      ],
      "OrderItemCustomers": [
        {
          "OrderCustomerId": 163018,
          "OrderItemBlankId": 1,
          "OrderItemCustomerId": 158795
        },
        {
          "OrderCustomerId": 163019,
          "OrderItemBlankId": 2,
          "OrderItemCustomerId": 158796
        }
      ],
      "OrderItemId": 111,
      "ReservationNumber": "70180813178041",
      "SimpleOperationStatus": "Succeeded",
      "IsExternallyLoaded": false
    },
    {
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts",
      "ConfirmTimeLimit": "2017-10-20T17:30:00",
      "CreateDateTime": "2017-10-20T17:50:05",
      "AgentReferenceId": "fea3b1ce92:120308:refund",
      "Amount": 3223.2,
      "DetailedOperationStatus": "Succeeded",
      "ElectronicRegistrationExpirationDateTime": "2018-01-07T00:15:00",
      "OperationType": "Return",
      "OrderId": 440044,
      "OrderItemBlanks": [
        {
          "Amount": 3223.2,
          "BlankStatus": "ElectronicRegistrationPresent",
          "ElectronicRegistrationSetDateTime": null,
          "IsElectronicRegistrationSet": false,
          "OrderItemBlankId": 222,
          "PendingElectronicRegistration": "NoValue",
          "PreviousOrderItemBlankId": 2
        }
      ],
      "OrderItemCustomers": [
        {
          "OrderCustomerId": 163019,
          "OrderItemBlankId": 222,
          "OrderItemCustomerId": 158797
        }
      ],
      "OrderItemId": 777,
      "ReservationNumber": "70180813178041",
      "SimpleOperationStatus": "Succeeded",
      "IsExternallyLoaded": false
    }
  ]
}"""


@pytest.mark.parametrize('update_blanks_status, update_blanks_result, update_tickets_mode, error_expected', [
    (200, IM_UPDATE_BLANKS_OK_RESULT, UpdateTicketStatusMode.UPDATE_FROM_EXPRESS, False),
    (200, IM_UPDATE_BLANKS_OK_RESULT, UpdateTicketStatusMode.TRY_UPDATE_FROM_EXPRESS, False),
    (500, IM_UPDATE_BLANKS_ERROR_RESULT, UpdateTicketStatusMode.UPDATE_FROM_EXPRESS, True),
    (500, IM_UPDATE_BLANKS_ERROR_RESULT, UpdateTicketStatusMode.TRY_UPDATE_FROM_EXPRESS, True),
    (500, IM_UPDATE_BLANKS_ERROR_61_RESULT, UpdateTicketStatusMode.UPDATE_FROM_EXPRESS, True),
    (500, IM_UPDATE_BLANKS_ERROR_61_RESULT, UpdateTicketStatusMode.TRY_UPDATE_FROM_EXPRESS, False),
    (500, IM_UPDATE_BLANKS_ERROR_RESULT, UpdateTicketStatusMode.SIMPLE, False),
])
def test_im_update_tickets_statuses(httpretty, update_blanks_status, update_blanks_result,
                                    update_tickets_mode, error_expected):
    """
    Тестируем обновление информации для заказа с двумя билетами,
    когда один из билетов сдали в кассе.
    """
    order = create_order(
        partner=TrainPartner.IM,
        partner_data={'operation_id': '111', 'im_order_id': 440044},
        passengers=[
            PassengerFactory(tickets=[TicketFactory(
                blank_id='1',
                rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN
            )], customer_id='163018'),
            PassengerFactory(tickets=[TicketFactory(
                blank_id='2',
                rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN
            )], customer_id='163019'),
        ]
    )

    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, status=update_blanks_status, body=update_blanks_result)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=IM_ORDER_INFO_RESULT)

    if error_expected:
        with pytest.raises(PartnerError):
            update_tickets_statuses(order, mode=update_tickets_mode)
    else:
        update_tickets_statuses(order, mode=update_tickets_mode, set_personal_data=True, set_order_warnings=True)
        assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN
        assert order.passengers[1].tickets[0].rzhd_status == RzhdStatus.REFUNDED
        assert order.passengers[0].doc_id == '6504555555'
        assert order.passengers[1].doc_id == '6504888888'
        assert order.passengers[0].birth_date.date() == date(1982, 9, 26)
        assert order.passengers[1].birth_date.date() == date(1981, 9, 26)
        assert order.warnings == [{'code': OrderWarningCode.ELECTRONIC_REGISTRATION_EXPIRED.value,
                                   'from': UTC_TZ.localize(datetime(2018, 1, 6, 21, 15))}]


@pytest.mark.parametrize('expire_set_er, start_station_departure, departure, expected_warnings', [
    (UTC_TZ.localize(datetime(2019, 2, 27)), datetime(2019, 2, 27, 1), datetime(2019, 2, 27, 18), [{
        'code': OrderWarningCode.ELECTRONIC_REGISTRATION_ALMOST_EXPIRED.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 26, 23, 45)),
        'to': UTC_TZ.localize(datetime(2019, 2, 27)),
    }, {
        'code': OrderWarningCode.ELECTRONIC_REGISTRATION_EXPIRED.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 27)),
        'to': UTC_TZ.localize(datetime(2019, 2, 27, 1)),
    }, {
        'code': OrderWarningCode.TRAIN_LEFT_START_STATION.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 27, 1)),
    }]),
    (UTC_TZ.localize(datetime(2019, 2, 25, 15, 14)), None, datetime(2019, 2, 25, 18), [{
        'code': OrderWarningCode.ELECTRONIC_REGISTRATION_ALMOST_EXPIRED.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 25, 14, 59)),
        'to': UTC_TZ.localize(datetime(2019, 2, 25, 15, 14)),
    }, {
        'code': OrderWarningCode.ELECTRONIC_REGISTRATION_EXPIRED.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 25, 15, 14)),
    }]),
    (UTC_TZ.localize(datetime(2019, 2, 25, 15)), None, datetime(2019, 2, 25, 18), [{
        'code': OrderWarningCode.ELECTRONIC_REGISTRATION_EXPIRED.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 25, 15)),
    }]),
    (None, datetime(2019, 2, 25), datetime(2019, 2, 27), [{
        'code': OrderWarningCode.TRAIN_ALMOST_LEFT_DEPARTURE_STATION.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 26, 23, 45)),
        'to': UTC_TZ.localize(datetime(2019, 2, 27)),
    }, {
        'code': OrderWarningCode.TRAIN_LEFT_DEPARTURE_STATION.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 27)),
    }]),
    (None, None, datetime(2019, 2, 25, 15, 14), [{
        'code': OrderWarningCode.TRAIN_ALMOST_LEFT_DEPARTURE_STATION.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 25, 14, 59)),
        'to': UTC_TZ.localize(datetime(2019, 2, 25, 15, 14)),
    }, {
        'code': OrderWarningCode.TRAIN_LEFT_DEPARTURE_STATION.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 25, 15, 14)),
    }]),
    (None, None, datetime(2019, 2, 25, 15), [{
        'code': OrderWarningCode.TRAIN_LEFT_DEPARTURE_STATION.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 25, 15)),
    }]),
    (UTC_TZ.localize(datetime(2019, 2, 25, 14)), datetime(2019, 2, 25, 15), datetime(2019, 2, 27, 15), [{
        'code': OrderWarningCode.TRAIN_LEFT_START_STATION.value,
        'from': UTC_TZ.localize(datetime(2019, 2, 25, 15)),
    }]),
])
@replace_now(datetime(2019, 2, 25, 18))
def test_get_order_warnings(expire_set_er, start_station_departure, departure, expected_warnings):
    order = TrainOrderFactory(
        route_info=OrderRouteInfoFactory(start_station=StationInfoFactory(departure=start_station_departure)),
        departure=departure,
    )
    order_info = OrderInfoResult(
        buy_operation_id=None, expire_set_er=expire_set_er, status=OperationStatus.OK, order_num='123',
        passengers=[PassengerInfo(doc_id=123456, blank_id=None)],
        tickets=[TicketInfo('12121212', RzhdStatus.REMOTE_CHECK_IN, 3000, False)],
    )
    res = get_order_warnings(order, order_info)
    assert res == expected_warnings


@replace_now(datetime(2019, 2, 25, 18))
def test_get_order_warnings_one_ticket_without_er():
    expire_set_er = UTC_TZ.localize(datetime(2019, 2, 25))
    start_station_departure = datetime(2019, 2, 25)
    departure = datetime(2019, 2, 27)
    order = TrainOrderFactory(
        route_info=OrderRouteInfoFactory(start_station=StationInfoFactory(departure=start_station_departure)),
        departure=departure,
    )
    order_info = OrderInfoResult(
        buy_operation_id=None, expire_set_er=expire_set_er, status=OperationStatus.OK, order_num='123',
        passengers=[PassengerInfo(doc_id=123456, blank_id=None)],
        tickets=[TicketInfo('12121212', RzhdStatus.REMOTE_CHECK_IN, 3000, False),
                 TicketInfo('12121213', RzhdStatus.NO_REMOTE_CHECK_IN, 3000, False),
                 TicketInfo('12121214', RzhdStatus.REFUNDED, 3000, False)],
    )
    res = get_order_warnings(order, order_info)
    assert res == [
        {
            'code': OrderWarningCode.TRAIN_ALMOST_LEFT_DEPARTURE_STATION.value,
            'from': UTC_TZ.localize(datetime(2019, 2, 26, 23, 45)),
            'to': UTC_TZ.localize(datetime(2019, 2, 27)),
        },
        {
            'code': OrderWarningCode.TRAIN_LEFT_DEPARTURE_STATION.value,
            'from': UTC_TZ.localize(datetime(2019, 2, 27)),
        }
    ]


@replace_now(datetime(2019, 2, 25, 18))
def test_get_order_warnings_tickets_returned():
    expire_set_er = UTC_TZ.localize(datetime(2019, 2, 25))
    start_station_departure = datetime(2019, 2, 25)
    departure = datetime(2019, 2, 27)
    order = TrainOrderFactory(
        route_info=OrderRouteInfoFactory(start_station=StationInfoFactory(departure=start_station_departure)),
        departure=departure,
    )
    order_info = OrderInfoResult(
        buy_operation_id=None, expire_set_er=expire_set_er, status=OperationStatus.OK, order_num='123',
        passengers=[PassengerInfo(doc_id=123456, blank_id=None)],
        tickets=[TicketInfo('12121212', RzhdStatus.REFUNDED, 3000, False),
                 TicketInfo('12121213', RzhdStatus.REFUNDED, 3000, False),
                 TicketInfo('12121214', RzhdStatus.REFUNDED, 3000, False)],
    )
    res = get_order_warnings(order, order_info)
    assert res == []


@pytest.mark.parametrize('now_in_msk, expected_warning_code', [
    ('2019-04-16 18:30:00', OrderWarningCode.INSURANCE_AUTO_RETURN.value),
    ('2019-04-16 19:05:00', OrderWarningCode.INSURANCE_AUTO_RETURN.value),
    ('2019-04-16 19:20:00', OrderWarningCode.TICKETS_TAKEN_AWAY.value),
    ('2019-04-16 19:30:00', OrderWarningCode.ELECTRONIC_REGISTRATION_ALMOST_EXPIRED.value),
    ('2019-04-16 20:05:00', None),
    ('2019-04-16 20:30:00', OrderWarningCode.TRAIN_LEFT_START_STATION.value),
])
def test_first_actual_warning_only(now_in_msk, expected_warning_code):
    warnings = [
        {
            'code': OrderWarningCode.INSURANCE_AUTO_RETURN.value,
            'to': datetime(2019, 4, 16, 16, 10, tzinfo=UTC_TZ),
        },
        {
            'code': OrderWarningCode.TICKETS_TAKEN_AWAY.value,
            'from': datetime(2019, 4, 16, 16, 15, tzinfo=UTC_TZ),
            'to': datetime(2019, 4, 16, 16, 25, tzinfo=UTC_TZ),
        },
        {
            'code': OrderWarningCode.ELECTRONIC_REGISTRATION_ALMOST_EXPIRED.value,
            'from': datetime(2019, 4, 16, 16, 00, tzinfo=UTC_TZ),
            'to': datetime(2019, 4, 16, 17, 00, tzinfo=UTC_TZ),
        },
        {
            'code': OrderWarningCode.TRAIN_LEFT_START_STATION.value,
            'from': datetime(2019, 4, 16, 17, 10, tzinfo=UTC_TZ),
        },
    ]
    with replace_now(now_in_msk):
        result = get_first_actual_warning(warnings)

    if not expected_warning_code:
        assert len(result) == 0
    else:
        assert len(result) == 1
        assert result[0]['code'] == expected_warning_code


@pytest.mark.parametrize('rzhd_statuses, expect_tickets_taken_warning', [
    ([RzhdStatus.STRICT_BOARDING_PASS], True),
    ([RzhdStatus.STRICT_BOARDING_PASS, RzhdStatus.STRICT_BOARDING_PASS], True),
    ([RzhdStatus.NO_REMOTE_CHECK_IN, RzhdStatus.STRICT_BOARDING_PASS], False),
    ([RzhdStatus.NO_REMOTE_CHECK_IN], False),
])
def test_get_order_warnings_tickets_taken(rzhd_statuses, expect_tickets_taken_warning):
    order = TrainOrderFactory()
    order_info = OrderInfoResult(
        buy_operation_id=None, expire_set_er=UTC_TZ.localize(datetime(2019, 2, 25, 15)),
        status=OperationStatus.OK, order_num='123',
        passengers=[PassengerInfo(doc_id=123456, blank_id=None)],
        tickets=[TicketInfo('12121212', rzhd_status, 3000, False) for rzhd_status in rzhd_statuses],
    )
    res = get_order_warnings(order, order_info)
    if expect_tickets_taken_warning:
        assert res == [{'code': OrderWarningCode.TICKETS_TAKEN_AWAY.value}]
    else:
        assert not any(w['code'] == OrderWarningCode.TICKETS_TAKEN_AWAY.value for w in res)


@pytest.mark.parametrize('finished_at, insurance_auto_return_uuid, warning_expected', [
    (datetime(2019, 2, 25, 15), None, False),
    (None, 'refund_uuid_refund_uuid_refund_uuid', False),
    (datetime(2019, 2, 25, 15), 'refund_uuid_refund_uuid_refund_uuid', True),
    (datetime(2019, 2, 25, 14, 59), 'refund_uuid_refund_uuid_refund_uuid', True),
    (datetime(2019, 2, 25, 14, 57), 'refund_uuid_refund_uuid_refund_uuid', False),
])
@replace_now(datetime(2019, 2, 25, 18))
def test_get_order_warnings_insurance_auto_return(finished_at, insurance_auto_return_uuid, warning_expected):
    order = TrainOrderFactory(finished_at=finished_at, insurance_auto_return_uuid=insurance_auto_return_uuid)
    order_info = OrderInfoResult(
        buy_operation_id=None, expire_set_er=UTC_TZ.localize(datetime(2019, 3, 25, 15)),
        status=OperationStatus.OK, order_num='123',
        passengers=[PassengerInfo(doc_id=123456, blank_id=None)],
        tickets=[TicketInfo('12121212', RzhdStatus.NO_REMOTE_CHECK_IN, 3000, False)],
    )
    res = get_order_warnings(order, order_info)
    if warning_expected:
        assert res[0] == {
            'code': OrderWarningCode.INSURANCE_AUTO_RETURN.value,
            'from': UTC_TZ.localize(finished_at),
            'to': UTC_TZ.localize(finished_at + INSURANCE_WARNING_ACTUAL_TIME),
        }
    else:
        assert not any(w['code'] == OrderWarningCode.INSURANCE_AUTO_RETURN.value for w in res)
