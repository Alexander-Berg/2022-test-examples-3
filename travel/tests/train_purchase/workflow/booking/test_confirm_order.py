# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import httpretty as httpretty
import pytest
from hamcrest import assert_that, has_properties, has_entries
from requests import ConnectionError

from common.tester.utils.replace_setting import replace_dynamic_setting
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.helpers.error import ErrorType
from travel.rasp.train_api.train_partners.im.confirm_ticket import CANCEL_TICKET_ENDPOINT
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.ufs.confirm_ticket import CONFIRM_TICKET_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TrainPartner, TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PassengerFactory
from travel.rasp.train_api.train_purchase.workflow.booking.confirm_order import ConfirmOrder, ConfirmOrderEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@pytest.mark.parametrize('xml, expected_event, order_match', [
    (
        '''\
        <UFS_RZhD_Gate>
            <Status>0</Status>
            <OrderNum>70864898287763</OrderNum>
        </UFS_RZhD_Gate>''',
        ConfirmOrderEvents.OK,
        has_properties(status=OrderStatus.RESERVED, travel_status=TravelOrderStatus.RESERVED,
                       current_partner_data=has_properties(order_num='70864898287763', expire_set_er=None))
    ),
    (
        '''\
        <UFS_RZhD_Gate>
            <Status>0</Status>
            <OrderNum>70864898287763</OrderNum>
            <ExpireSetEr>03.02.2016 16:23:00</ExpireSetEr>
        </UFS_RZhD_Gate>''',
        ConfirmOrderEvents.OK,
        has_properties(status=OrderStatus.RESERVED, travel_status=TravelOrderStatus.RESERVED,
                       current_partner_data=has_properties(order_num='70864898287763',
                                                           expire_set_er=datetime(2016, 2, 3, 13, 23)))
    ),
    (
        '''\
        <UFS_RZhD_Gate>
            <Status>1</Status>
            <Error />
            <OrderNum>70864898287763</OrderNum>
            <ExpireSetEr>03.02.2016 16:23:00</ExpireSetEr>
        </UFS_RZhD_Gate>''',
        ConfirmOrderEvents.FAILED,
        has_properties(status=OrderStatus.RESERVED, travel_status=TravelOrderStatus.RESERVED),
    )
])
def test_confirm_order(httpretty, xml, expected_event, order_match):
    order = TrainOrderFactory(partner=TrainPartner.UFS, partner_data={'operation_id': '1'})
    mock_ufs(httpretty, CONFIRM_TICKET_ENDPOINT, body=xml)
    event, order = process_state_action(
        ConfirmOrder,
        (ConfirmOrderEvents.OK, ConfirmOrderEvents.FAILED),
        order
    )
    assert event == expected_event
    assert_that(order, order_match)


IM_CONFIRM_RESPONSE = {
    "OrderId": 51978,
    "Customers": [
        {
            "OrderCustomerId": 66650,
            "FirstName": "Иван",
            "MiddleName": "Иванович",
            "LastName": "Иванов",
            "Sex": "Male",
            "BirthDate": "1976-10-12T00:00:00",
            "DocumentNumber": "4601123450",
            "DocumentValidTill": "9999-12-31T23:59:59",
            "DocumentType": "RussianPassport",
            "CitizenshipCode": "RU",
            "BirthPlace": None
        }
    ],
    "ConfirmResults": [
        {
            "$type": "ApiContracts.Railway.V1.Messages.Reservation.RailwayConfirmResponse, ApiContracts",
            "ReservationNumber": "71234567890000",
            "Blanks": [
                {
                    "OrderItemBlankId": 51948,
                    "Number": "71234567890000",
                    "BlankStatus": "ElectronicRegistrationPresent",
                    "PendingElectronicRegistration": "NoValue"
                }
            ],
            "ExpirationElectronicRegistrationDateTime": None,
            "OrderItemId": 52159,
            "Amount": 9481.7,
            "Fare": 9481.7,
            "Tax": 0.0,
            "Confirmed": "2016-10-27T19:16:17",
            "VoidTill": "1753-01-01T00:00:00",
            "ClientFeeCalculation": {
                "Charge": 100.0,
                "Profit": 0.0
            },
            "AgentFeeCalculation": {
                "Charge": 100.0,
                "Profit": 0.0
            },
            "OrderItemCustomers": [
                {
                    "OrderCustomerId": 66652,
                    "Amount": 9481.7,
                    "Fare": 9481.7,
                    "Tax": 0.0,
                    "ClientFeeCalculation": {
                        "Charge": 100.0,
                        "Profit": 0.0
                    },
                    "AgentFeeCalculation": {
                        "Charge": 100.0,
                        "Profit": 0.0
                    }
                }
            ],
            "Warnings": None,
            "ErrorResult": None
        }
    ]
}


@pytest.mark.parametrize('response, expected_event, order_match', [
    (
        httpretty.Response(json.dumps(IM_CONFIRM_RESPONSE)),
        ConfirmOrderEvents.OK,
        has_properties(status=OrderStatus.RESERVED, travel_status=TravelOrderStatus.RESERVED,
                       current_partner_data=has_properties(order_num='71234567890000', expire_set_er=None))
    ),
    (
        httpretty.Response('''{
                            "OrderId": 51978,
                            "ConfirmResults": [{
                                "OrderItemId": 100,
                                "ExpirationElectronicRegistrationDateTime": "2016-10-27T19:16:17",
                                "ReservationNumber": "71234567890000"
                            },{
                                "OrderItemId": 52159,
                                "ExpirationElectronicRegistrationDateTime": "2016-10-27T19:16:17",
                                "ReservationNumber": "71234567890000"
                            }]}'''),
        ConfirmOrderEvents.OK,
        has_properties(status=OrderStatus.RESERVED, travel_status=TravelOrderStatus.RESERVED,
                       current_partner_data=has_properties(order_num='71234567890000',
                                                           expire_set_er=datetime(2016, 10, 27, 16, 16, 17)))
    ),
    (
        httpretty.Response('{"Code": 1, "Message": "FUUUU!", "MessageParams":[]}', status=500),
        ConfirmOrderEvents.FAILED,
        has_properties(status=OrderStatus.RESERVED, travel_status=TravelOrderStatus.RESERVED)
    ),
    (
        ConnectionError(),
        ConfirmOrderEvents.FAILED,
        has_properties(status=OrderStatus.RESERVED, travel_status=TravelOrderStatus.RESERVED)
    )
])
def test_im_confirm_order_no_cancel(httpretty, response, expected_event, order_match):
    order = TrainOrderFactory(partner=TrainPartner.IM,
                              partner_data={'im_order_id': 1, 'operation_id': '52159'},
                              passengers=[
                                  PassengerFactory(),
                                  PassengerFactory()],
                              )
    from travel.rasp.train_api.train_partners.im.confirm_ticket import CONFIRM_TICKET_ENDPOINT
    mock_im(httpretty, CONFIRM_TICKET_ENDPOINT, responses=[response, ])

    event, order = process_state_action(
        ConfirmOrder,
        (ConfirmOrderEvents.OK, ConfirmOrderEvents.FAILED, ConfirmOrderEvents.IM_FAILED_NO_RETRY),
        order
    )
    assert event == expected_event
    assert_that(order, order_match)
    assert len(httpretty.latest_requests) == 1


@replace_dynamic_setting('TRAIN_PARTNERS_IM_ENDUSER_ERROR_MSG', {'2': 'Ошибка при взаимодействии с партнером'})
@pytest.mark.parametrize('response, expected_event, order_match', [
    (
        httpretty.Response('{"Code": 2, "Message": "FUUUU!", "MessageParams":[]}', status=500),
        ConfirmOrderEvents.IM_FAILED_NO_RETRY,
        has_properties(
            status=OrderStatus.CONFIRM_FAILED, travel_status=TravelOrderStatus.CANCELLED, error=has_properties({
                'type': ErrorType.PARTNER_ERROR,
                'message': 'Ошибка при взаимодействии с партнером',
                'data': has_entries({
                    'code': 2,
                    'message': 'FUUUU!'
                })
            }), current_partner_data=has_properties(is_order_cancelled=True)
        )
    ),
])
def test_im_confirm_order_with_cancel(httpretty, response, expected_event, order_match):
    order = TrainOrderFactory(partner=TrainPartner.IM, partner_data={'im_order_id': 1, 'operation_id': '52159'},
                              passengers=[
                                  PassengerFactory(),
                                  PassengerFactory()],
                              )
    from travel.rasp.train_api.train_partners.im.confirm_ticket import CONFIRM_TICKET_ENDPOINT
    mock_im(httpretty, CONFIRM_TICKET_ENDPOINT, responses=[response, ])
    mock_im(httpretty, CANCEL_TICKET_ENDPOINT, json={})

    event, order = process_state_action(
        ConfirmOrder,
        (ConfirmOrderEvents.OK, ConfirmOrderEvents.FAILED, ConfirmOrderEvents.IM_FAILED_NO_RETRY),
        order
    )
    assert event == expected_event
    assert_that(order, order_match)
    assert len(httpretty.latest_requests) == 2
    assert httpretty.last_request.path.endswith(CANCEL_TICKET_ENDPOINT)
