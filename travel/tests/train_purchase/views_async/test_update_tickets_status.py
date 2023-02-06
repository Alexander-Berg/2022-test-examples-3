# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
from hamcrest import assert_that, has_entries, contains, has_properties, contains_string, empty

from common.tester.utils.datetime import replace_now
from travel.rasp.train_api.train_partners import im
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.im.ticket_blank import IM_TICKET_PDF_BLANK_ENDPOINT
from travel.rasp.train_api.train_partners.im.update_order import IM_UPDATE_BLANKS_METHOD
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PassengerFactory, TicketFactory
from travel.rasp.train_api.train_purchase.core.models import TrainOrder
from travel.rasp.train_api.train_purchase.factories import create_order_warnings
from travel.rasp.train_api.train_purchase.utils import order as order_utils
from travel.rasp.train_api.train_purchase.views.test_utils import create_order

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]


IM_UPDATE_BLANKS_RESULT = """{
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

IM_ORDER_INFO_RESULT = """{
  "Amount": 3971.7,
  "OrderCustomers": [
    {
      "BirthDate": "1981-09-26T00:00:00",
      "DocumentNumber": "6504888888",
      "OrderCustomerId": 163019
    },
    {
      "BirthDate": "1991-09-26T00:00:00",
      "DocumentNumber": "6504555555",
      "OrderCustomerId": 163018
    }
  ],
  "OrderId": 440044,
  "OrderItems": [
    {
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts",
      "ConfirmTimeLimit": "2018-01-01T00:15:00",
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
      "ConfirmTimeLimit": "2018-01-01T00:15:00",
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


@mock.patch.object(im, 'get_route_info', return_value=None, autospec=True)
def test_im_update_tickets_statuses(m_, httpretty, async_urlconf_client):
    """
    Тестируем обновление информации для заказа с двумя билетами,
    когда один из билетов сдали в кассе.
    """
    order = create_order(
        partner=TrainPartner.IM,
        partner_data={'operation_id': '111', 'im_order_id': 440044}
    )

    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body=IM_UPDATE_BLANKS_RESULT)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=IM_ORDER_INFO_RESULT)
    mock_im(httpretty, IM_TICKET_PDF_BLANK_ENDPOINT, body='ticket_blank')

    response = async_urlconf_client.get('/ru/api/update-tickets-status/{}/'.format(order.uid))

    assert response.status_code == 200
    response_order = json.loads(response.content)['order']
    assert_that(response_order['passengers'], contains(
        has_entries(tickets=contains(has_entries({
            'blankId': '1',
            'rzhdStatus': 'REMOTE_CHECK_IN',
        }))),
        has_entries(tickets=contains(has_entries({
            'blankId': '2',
            'rzhdStatus': 'REFUNDED',
        }))),
    ))
    assert_that(response_order['warnings'], contains(
        has_entries({'code': 'electronic_registration_expired'})
    ))

    assert_that(httpretty.latest_requests, contains(
        has_properties(
            path=contains_string(IM_UPDATE_BLANKS_METHOD),
            parsed_body=has_entries(OrderItemId=111)
        ),
        has_properties(
            path=contains_string(IM_ORDER_INFO_METHOD),
            parsed_body=has_entries(OrderId=440044)
        ),
    ))


@mock.patch.object(im, 'get_route_info', return_value=None, autospec=True)
def test_im_update_tickets_statuses_order_cancelled(m_, httpretty, async_urlconf_client):
    """
    Тестируем обновление информации для отмененного заказа
    """
    order = create_order(
        partner=TrainPartner.IM,
        partner_data={'operation_id': '111', 'im_order_id': 440044, 'is_order_cancelled': True}
    )

    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, status=500)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=IM_ORDER_INFO_RESULT)

    response = async_urlconf_client.get('/ru/api/update-tickets-status/{}/'.format(order.uid))

    assert response.status_code == 422
    assert_that(json.loads(response.content)['order']['passengers'], contains(
        has_entries(tickets=contains(has_entries({
            'blankId': '1',
            'rzhdStatus': 'REMOTE_CHECK_IN',
        }))),
        has_entries(tickets=contains(has_entries({
            'blankId': '2',
            'rzhdStatus': 'REFUNDED',
        }))),
    ))

    assert_that(httpretty.latest_requests, contains(
        has_properties(
            path=contains_string(IM_ORDER_INFO_METHOD),
            parsed_body=has_entries(OrderId=440044)
        ),
    ))


@replace_now('2019-01-01 23:30:00')
@mock.patch.object(order_utils, 'get_order_warnings', autospec=True, return_value=create_order_warnings())
def test_warnings(m_get_order_warnings, httpretty, async_urlconf_client):
    order = create_order(
        partner=TrainPartner.IM,
        partner_data={'operation_id': '111', 'im_order_id': 440044}
    )
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body=IM_UPDATE_BLANKS_RESULT)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=IM_ORDER_INFO_RESULT)

    response = async_urlconf_client.get('/ru/api/update-tickets-status/{}/'.format(order.uid))
    assert response.status_code == 200
    assert_that(response.data['order']['warnings'], contains(
        has_entries({
            'code': 'insurance_auto_return',
        }),
        has_entries({
            'code': 'tickets_taken_away',
        }),
    ))

    response = async_urlconf_client.get('/ru/api/update-tickets-status/{}/?firstActualWarningOnly=true'.format(
        order.uid
    ))
    assert response.status_code == 200
    assert_that(response.data['order']['warnings'], contains(
        has_entries({
            'code': 'insurance_auto_return',
        }),
    ))


IM_UPDATE_BLANKS_RESULT_1_BLANK_2_PASSENGERS = """{
  "Blanks": [
    {
      "OrderItemBlankId": 1,
      "Number": "71234567890000",
      "BlankStatus": "ElectronicRegistrationAbsent",
      "PendingElectronicRegistration": "NoValue"
    }
  ]
}"""

IM_ORDER_INFO_RESULT_1_BLANK_2_PASSENGERS = """{
  "Amount": 3971.7,
  "OrderCustomers": [
    {
      "BirthDate": "1981-09-26T00:00:00",
      "DocumentNumber": "6504888888",
      "OrderCustomerId": 163019
    },
    {
      "BirthDate": "1991-09-26T00:00:00",
      "DocumentNumber": "6504555555",
      "OrderCustomerId": 163018
    }
  ],
  "OrderId": 440044,
  "OrderItems": [
    {
      "$type": "ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts",
      "ConfirmTimeLimit": "2018-01-01T00:15:00",
      "ElectronicRegistrationExpirationDateTime": "2018-01-07T00:15:00",
      "OperationType": "Purchase",
      "OrderId": 440044,
      "OrderItemBlanks": [
        {
          "Amount": 3779.0,
          "BlankStatus": "ElectronicRegistrationAbsent",
          "ElectronicRegistrationSetDateTime": "2018-01-01T06:45:00",
          "IsElectronicRegistrationSet": true,
          "OrderItemBlankId": 1,
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
          "OrderItemBlankId": 1,
          "OrderItemCustomerId": 158796
        }
      ],
      "OrderItemId": 111,
      "ReservationNumber": "70180813178041",
      "SimpleOperationStatus": "Succeeded"
    }
  ]
}"""


@mock.patch.object(im, 'get_route_info', return_value=None, autospec=True)
def test_update_rzhd_statuses_1_blank_for_2_passengers(m_, httpretty, async_urlconf_client):
    """
    Проверяем обновление статусов билетов,
    для случая с одним бланком для двух пассажиров.
    """
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        passengers=[
            PassengerFactory(tickets=[TicketFactory(blank_id='1', places=['11'])]),
            PassengerFactory(tickets=[TicketFactory(blank_id='1', places=[])])
        ],
        partner_data={'operation_id': '111', 'im_order_id': 440044}
    )
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body=IM_UPDATE_BLANKS_RESULT_1_BLANK_2_PASSENGERS)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, body=IM_ORDER_INFO_RESULT_1_BLANK_2_PASSENGERS)

    response = async_urlconf_client.get('/ru/api/update-tickets-status/{}/'.format(order.uid))

    assert response.status_code == 200
    assert_that(json.loads(response.content)['order']['passengers'], contains(
        has_entries(tickets=contains(has_entries({
            'blankId': '1',
            'rzhdStatus': 'NO_REMOTE_CHECK_IN',
            'places': contains('11'),
        }))),
        has_entries(tickets=contains(has_entries({
            'blankId': '1',
            'rzhdStatus': 'NO_REMOTE_CHECK_IN',
            'places': empty(),
        }))),
    ))

    assert_that(httpretty.latest_requests, contains(
        has_properties(
            path=contains_string(IM_UPDATE_BLANKS_METHOD),
            parsed_body=has_entries(OrderItemId=111)
        ),
        has_properties(
            path=contains_string(IM_ORDER_INFO_METHOD),
            parsed_body=has_entries(OrderId=440044)
        ),
    ))

    updated_order = TrainOrder.objects.get(uid=order.uid)
    assert updated_order.passengers[0].tickets[0].rzhd_status == RzhdStatus.NO_REMOTE_CHECK_IN
    assert updated_order.passengers[1].tickets[0].rzhd_status == RzhdStatus.NO_REMOTE_CHECK_IN
