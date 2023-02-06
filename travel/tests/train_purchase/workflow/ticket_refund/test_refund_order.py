# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import mock
import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult, PassengerInfo
from travel.rasp.train_api.train_partners.im.base import RZHD_STATUS_TO_BLANK_STATUS, OPERATION_STATUS_TO_IM_OPERATION_STATUS
from travel.rasp.train_api.train_partners.im.factories.order_info import (
    ImOrderInfoFactory, ImRailwayOrderItemFactory, ImOrderItemBlankFactory, ImOrderCustomerFactory
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im, create_im_response, mock_im_500, IM_500_RESPONSE
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.im.refund import REFUND_ENDPOINT as IM_REFUND_ENDPOINT
from travel.rasp.train_api.train_purchase.core.enums import OperationStatus, TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    PassengerFactory, TrainOrderFactory, TicketFactory, ClientContractsFactory,
    TicketPaymentFactory, TrainRefundFactory, InsuranceFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundStatus, TrainPartner
from travel.rasp.train_api.train_purchase.workflow.ticket_refund import refund_order
from travel.rasp.train_api.train_purchase.workflow.ticket_refund.refund_managers import BlankRefundManager
from travel.rasp.train_api.train_purchase.workflow.ticket_refund.refund_order import (
    RefundOrder, RefundOrderEvents, update_refund_status,
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@pytest.fixture(autouse=True)
def refund_never_sleep():
    with mock.patch.object(refund_order, 'sleep'):
        yield


def _process(refund):
    return process_state_action(RefundOrder, (RefundOrderEvents.DONE,
                                              RefundOrderEvents.FAILED,
                                              RefundOrderEvents.NO_ACTIVE_CONTRACT), refund)


def test_im_refund_1_blank_for_2_passengers(httpretty):
    ClientContractsFactory()

    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id='82288'),
        passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='1', payment=TicketPaymentFactory(amount=0))])]
    )
    refund = TrainRefundFactory(blank_ids=['1'], order_uid=order.uid)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(train_order=order))
    mock_im(httpretty, IM_REFUND_ENDPOINT, body="""{
    "$type":"ApiContracts.Railway.V1.Messages.Return.RailwayAutoReturnResponse, ApiContracts",
    "ServiceReturnResponse":{
        "Blanks":[
            {
                "PurchaseOrderItemBlankId":1,
                "ReturnOrderItemBlankId":2,
                "Amount":2055.3,
                "VatRateValues":[
                    {"Rate":0.0, "Value":0.00},
                    {"Rate":18.0, "Value":22.58},
                    {"Rate":18.0, "Value":0.0},
                    {"Rate":18.0, "Value":29.39}
                ]
            }
        ],
        "Amount":2055.30,
        "Fare":2248.0,
        "Tax":0.0,
        "Confirmed":"2017-08-17T16:26:59",
        "ReturnOrderItemId":82289,
        "AgentReferenceId":"fa2df1dcc7:all:refund",
        "ClientFeeCalculation":null,
        "AgentFeeCalculation":{
            "Charge":33.87,
            "Profit":0.0
        }
    }}""")

    event, refund = _process(refund)
    order.reload()

    assert event == RefundOrderEvents.DONE
    assert refund.status == RefundStatus.PARTNER_REFUND_DONE
    assert order.travel_status == TravelOrderStatus.IN_PROGRESS

    first_ticket = order.passengers[0].tickets[0]
    second_ticket = order.passengers[1].tickets[0]

    assert first_ticket.refund.operation_id == '82289'
    assert first_ticket.refund.amount == Decimal('2055.30')
    assert first_ticket.rzhd_status == RzhdStatus.REFUNDED

    assert second_ticket.refund.operation_id == '82289'
    assert second_ticket.refund.amount is None
    assert second_ticket.rzhd_status == RzhdStatus.REFUNDED


def test_im_one_refund_for_3_passengers_with_only_2_blanks(httpretty):
    ClientContractsFactory()
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id='82850'),
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(blank_id='1')],
                insurance=InsuranceFactory(amount=Decimal('100'), trust_order_id='some_trust_order_id',
                                           operation_id='222'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(blank_id='1', payment=TicketPaymentFactory(amount=0))],
                insurance=InsuranceFactory(amount=Decimal('70'), trust_order_id='some_trust_order_id',
                                           operation_id='223'),
            ),
            PassengerFactory(
                tickets=[TicketFactory(blank_id='2')],
                insurance=InsuranceFactory(amount=Decimal('100')),
            ),
        ]
    )
    refund = TrainRefundFactory(blank_ids=['1', '2'], order_uid=order.uid)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(train_order=order))
    mock_im(httpretty, IM_REFUND_ENDPOINT, body="""{
    "ServiceReturnResponse":{
        "$type":"ApiContracts.Railway.V1.Messages.Return.RailwayAutoReturnResponse, ApiContracts",
        "Blanks":[
            {
                "PurchaseOrderItemBlankId":1,
                "ReturnOrderItemBlankId":111,
                "Amount":3344.3,
                "VatRateValues":[
                    {"Rate":0.0, "Value":0.00},
                    {"Rate":18.0, "Value":106.47},
                    {"Rate":18.0, "Value":0.0},
                    {"Rate":18.0, "Value":29.39}
                ]
            },
            {
                "PurchaseOrderItemBlankId":2,
                "ReturnOrderItemBlankId":222,
                "Amount":3060.40,
                "VatRateValues":[
                    {"Rate":0.0, "Value":0.00},
                    {"Rate":18.0, "Value":106.47},
                    {"Rate":18.0, "Value":0.0},
                    {"Rate":18.0, "Value":29.39}
                ]
            }
        ],
        "Amount":6404.70,
        "Fare":6790.10,
        "Tax":0.0,
        "Confirmed":"2017-08-21T10:59:00",
        "ReturnOrderItemId":82852,
        "AgentReferenceId":"9130ff8a0c:all:refund",
        "ClientFeeCalculation":null,
        "AgentFeeCalculation":{
            "Charge":67.74,
            "Profit":0.0
        }
    }}""")

    event, refund = _process(refund)
    order.reload()

    number_of_refund_requests = len([r for r in httpretty.latest_requests if '/AutoReturn' in r.path])
    assert number_of_refund_requests == 1, 'Билеты должны быть сданы все сразу'

    assert event == RefundOrderEvents.DONE
    assert refund.status == RefundStatus.PARTNER_REFUND_DONE
    assert order.travel_status == TravelOrderStatus.IN_PROGRESS

    assert order.passengers[0].insurance.refund_uuid == refund.uuid
    assert order.passengers[1].insurance.refund_uuid == refund.uuid
    assert order.passengers[2].insurance.refund_uuid is None

    first_ticket = order.passengers[0].tickets[0]
    second_ticket = order.passengers[1].tickets[0]
    third_ticket = order.passengers[2].tickets[0]

    assert first_ticket.refund.operation_id == '82852'
    assert first_ticket.refund.amount == Decimal('3344.3')
    assert first_ticket.rzhd_status == RzhdStatus.REFUNDED

    assert second_ticket.refund.operation_id == '82852'
    assert second_ticket.refund.amount is None
    assert second_ticket.rzhd_status == RzhdStatus.REFUNDED

    assert third_ticket.refund.operation_id == '82852'
    assert third_ticket.refund.amount == Decimal('3060.4')
    assert third_ticket.rzhd_status == RzhdStatus.REFUNDED


@mock.patch.object(refund_order, 'update_refund_status')
@pytest.mark.parametrize('expected_event, expected_rzhd_status, final_response_builder', [
    (
        RefundOrderEvents.DONE,
        RzhdStatus.REFUNDED,
        lambda order, reference_id: create_im_response(json=ImOrderInfoFactory.add_refund_item(
            ImOrderInfoFactory(train_order=order, OrderItems=[
                ImRailwayOrderItemFactory(train_order=order, AgentReferenceId=reference_id, OrderItemBlanks=[
                    ImOrderItemBlankFactory(train_order_ticket=order.passengers[0].tickets[0], OrderItemBlankId=1)
                ])
            ]),
            blank_id_to_refund=1
        ))
    ),
    (
        RefundOrderEvents.FAILED,
        RzhdStatus.NO_REMOTE_CHECK_IN,
        lambda order, reference_id: create_im_response(json=ImOrderInfoFactory(train_order=order, OrderItems=[
            ImRailwayOrderItemFactory(
                train_order=order,
                AgentReferenceId=reference_id,
                SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.FAILED],
                OrderItemBlanks=[
                    ImOrderItemBlankFactory(train_order_ticket=ticket,
                                            BlankStatus=RZHD_STATUS_TO_BLANK_STATUS[RzhdStatus.NO_REMOTE_CHECK_IN])
                    for ticket in order.iter_tickets()
                ]
            )
        ]))
    ),
])
def test_im_refund_500_order_info_in_process(m_immediately_update_refund_status, httpretty, expected_event,
                                             expected_rzhd_status, final_response_builder):
    ClientContractsFactory()
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id='82288'),
        passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1', rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN)])]
    )
    refund = TrainRefundFactory(blank_ids=['1'], order_uid=order.uid,
                                uuid='---------------------------------some-refund-uuid')

    mock_im(httpretty, IM_ORDER_INFO_METHOD, responses=[
        create_im_response(json=ImOrderInfoFactory(train_order=order)),
        create_im_response(json=ImOrderInfoFactory(train_order=order, OrderItems=[ImRailwayOrderItemFactory(
            train_order=order,
            AgentReferenceId='--------------some-refund-uuid:1',
            SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.IN_PROCESS])])),
        final_response_builder(order, reference_id='--------------some-refund-uuid:1')
    ])
    mock_im_500(httpretty, IM_REFUND_ENDPOINT)

    event, refund = _process(refund)
    order.reload()

    assert event == expected_event
    first_ticket = order.passengers[0].tickets[0]
    assert first_ticket.rzhd_status == expected_rzhd_status
    assert order.travel_status == TravelOrderStatus.IN_PROGRESS

    number_of_refund_requests = len([r for r in httpretty.latest_requests if '/AutoReturn' in r.path])
    number_of_order_info_requests = len([r for r in httpretty.latest_requests if '/OrderInfo' in r.path])
    assert number_of_refund_requests == 1
    assert number_of_order_info_requests == 3

    assert m_immediately_update_refund_status.call_count == 1


def test_update_refund_status():
    refund = TrainRefundFactory(blank_ids=['1'], status=RefundStatus.NEW)

    update_refund_status(refund, RefundStatus.PARTNER_REFUND_UNKNOWN)

    refund.reload()
    assert refund.status == RefundStatus.PARTNER_REFUND_UNKNOWN


def test_im_partial_1_already_refunded_2_of_3(httpretty):
    ClientContractsFactory()
    refund_trans_id = '123'
    mock_im_500(httpretty, IM_REFUND_ENDPOINT)
    im_order = ImOrderInfoFactory(
        OrderCustomers=[ImOrderCustomerFactory(OrderCustomerId=1000 + i) for i in [1, 2, 3]],
        OrderItems=[ImRailwayOrderItemFactory(
            OrderItemBlanks=[ImOrderItemBlankFactory(OrderItemBlankId=blank_id) for blank_id in [1, 2, 3]])])
    im_order_refund = ImOrderInfoFactory.add_refund_item(im_order, 2,
                                                         AgentReferenceId='--------------some-refund-uuid:2')
    mock_im(httpretty, IM_ORDER_INFO_METHOD, responses=[
        create_im_response(json=im_order),
        create_im_response(json=im_order_refund),
        IM_500_RESPONSE,
    ])
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id=refund_trans_id),
        passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1', rzhd_status=RzhdStatus.REFUNDED)]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='2', rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN)]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='3', rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN)])]
    )
    refund = TrainRefundFactory(blank_ids=['2', '3'], order_uid=order.uid,
                                uuid='---------------------------------some-refund-uuid')

    event, refund = _process(refund)
    order.reload()

    assert event == RefundOrderEvents.FAILED
    assert refund.status == RefundStatus.FAILED
    assert order.travel_status == TravelOrderStatus.IN_PROGRESS

    first_ticket = order.passengers[0].tickets[0]
    second_ticket = order.passengers[1].tickets[0]
    third_ticket = order.passengers[2].tickets[0]

    assert first_ticket.rzhd_status == RzhdStatus.REFUNDED
    assert second_ticket.rzhd_status == RzhdStatus.REFUNDED
    assert third_ticket.rzhd_status == RzhdStatus.NO_REMOTE_CHECK_IN

    assert refund.is_partial_failed


@mock.patch.object(refund_order, 'update_refund_status')
def test_im_communication_error_on_refund(m_immediately_update_refund_status, httpretty):
    ClientContractsFactory()
    im_order = ImOrderInfoFactory(OrderItems=[ImRailwayOrderItemFactory(
        OrderItemBlanks=[ImOrderItemBlankFactory(OrderItemBlankId=1, Amount=2000)])])
    im_order_refund = ImOrderInfoFactory.add_refund_item(
        im_order, blank_id_to_refund=1, amount_to_refund=1111.11,
        AgentReferenceId='--------------some-refund-uuid:1', OrderItemId=1212)
    mock_im_500(httpretty, IM_REFUND_ENDPOINT)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, responses=[
        create_im_response(json=im_order),
        create_im_response(json=im_order_refund),
    ])
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id='1'),
        passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')])]
    )
    refund = TrainRefundFactory(blank_ids=['1'], order_uid=order.uid,
                                uuid='---------------------------------some-refund-uuid')

    event, refund = _process(refund)
    order.reload()

    assert event == RefundOrderEvents.DONE
    assert refund.status == RefundStatus.PARTNER_REFUND_DONE
    assert order.travel_status == TravelOrderStatus.IN_PROGRESS
    ticket = order.passengers[0].tickets[0]
    assert ticket.refund.operation_id == '1212'
    assert ticket.refund.amount == Decimal('1111.11')
    assert ticket.rzhd_status == RzhdStatus.REFUNDED

    assert m_immediately_update_refund_status.call_count == 1


@mock.patch.object(refund_order, 'update_refund_status')
def test_im_communication_error_on_refund_2_of_4(m_immediately_update_refund_status, httpretty):
    ClientContractsFactory()
    im_order = ImOrderInfoFactory(
        OrderCustomers=[ImOrderCustomerFactory(OrderCustomerId=1000 + i) for i in range(4)],
        OrderItems=[ImRailwayOrderItemFactory(
            OrderItemBlanks=[ImOrderItemBlankFactory(OrderItemBlankId=blank_id) for blank_id in range(4)]
        )]
    )
    im_order_refund = ImOrderInfoFactory.add_refund_item(
        im_order, blank_id_to_refund=1, amount_to_refund=111.11,
        AgentReferenceId='--------------some-refund-uuid:1', OrderItemId=111
    )
    im_order_refund = ImOrderInfoFactory.add_refund_item(
        im_order_refund, blank_id_to_refund=2, amount_to_refund=222.22,
        AgentReferenceId='--------------some-refund-uuid:2', OrderItemId=112
    )
    mock_im_500(httpretty, IM_REFUND_ENDPOINT)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, responses=[
        create_im_response(json=im_order),
        create_im_response(json=im_order_refund),
    ])
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id='1'),
        passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='2')]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='3')]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='4')])]
    )
    refund = TrainRefundFactory(blank_ids=['1', '2'], order_uid=order.uid,
                                uuid='---------------------------------some-refund-uuid')

    event, refund = _process(refund)
    order.reload()

    assert event == RefundOrderEvents.DONE
    assert refund.status == RefundStatus.PARTNER_REFUND_DONE
    assert order.travel_status == TravelOrderStatus.IN_PROGRESS

    first_ticket = order.passengers[0].tickets[0]
    second_ticket = order.passengers[1].tickets[0]
    third_ticket = order.passengers[2].tickets[0]
    fourth_ticket = order.passengers[3].tickets[0]

    assert first_ticket.refund.operation_id == '111'
    assert first_ticket.refund.amount == Decimal('111.11')
    assert first_ticket.rzhd_status == RzhdStatus.REFUNDED

    assert second_ticket.refund.operation_id == '112'
    assert second_ticket.refund.amount == Decimal('222.22')
    assert second_ticket.rzhd_status == RzhdStatus.REFUNDED

    assert third_ticket.rzhd_status != RzhdStatus.REFUNDED

    assert fourth_ticket.rzhd_status != RzhdStatus.REFUNDED

    assert m_immediately_update_refund_status.call_count == 1


@mock.patch.object(refund_order, 'update_refund_status')
def test_im_communication_error_on_refund_and_once_on_transinfo(m_immediately_update_refund_status, httpretty):
    ClientContractsFactory()
    im_order = ImOrderInfoFactory(
        OrderItems=[ImRailwayOrderItemFactory(OrderItemBlanks=[ImOrderItemBlankFactory(OrderItemBlankId=1)])]
    )
    im_order_refund = ImOrderInfoFactory.add_refund_item(
        im_order, blank_id_to_refund=1, amount_to_refund=111.11,
        AgentReferenceId='--------------some-refund-uuid:1', OrderItemId=111
    )
    mock_im_500(httpretty, IM_REFUND_ENDPOINT)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, responses=[
        create_im_response(json=im_order),
        IM_500_RESPONSE,
        create_im_response(json=im_order_refund),
    ])
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id='1'),
        passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')])]
    )
    refund = TrainRefundFactory(blank_ids=['1'], order_uid=order.uid,
                                uuid='---------------------------------some-refund-uuid')

    event, refund = _process(refund)
    order.reload()

    assert event == RefundOrderEvents.DONE
    assert refund.status == RefundStatus.PARTNER_REFUND_DONE
    assert order.travel_status == TravelOrderStatus.IN_PROGRESS
    ticket = order.passengers[0].tickets[0]
    assert ticket.refund.operation_id == '111'
    assert ticket.refund.amount == Decimal('111.11')
    assert ticket.rzhd_status == RzhdStatus.REFUNDED

    assert m_immediately_update_refund_status.call_count == 1


def test_generate_unique_reference_id():
    order_info = OrderInfoResult(
        buy_operation_id=None, expire_set_er=None, status=None, order_num=None,
        passengers=[PassengerInfo(doc_id=123456, blank_id='101'), PassengerInfo(doc_id=4324324, blank_id='102')]
    )
    order = TrainOrderFactory(
        partner=TrainPartner.IM,
        partner_data=dict(operation_id='1'),
        passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='101')]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='102')]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='103')]),
                    PassengerFactory(tickets=[TicketFactory(blank_id='104')])]
    )
    refund_uids = ['---------------------------------some-refund-uuid',
                   '---------------------------------some-other-uuid']
    refund_managers = [BlankRefundManager(
        order=order,
        passenger=passenger,
        blank_id=passenger.blank_id,
        refund_uuid=refund_uid,
    ) for passenger in order_info.passengers for refund_uid in refund_uids]
    assert len(refund_managers) == len({rm.reference_id for rm in refund_managers})
