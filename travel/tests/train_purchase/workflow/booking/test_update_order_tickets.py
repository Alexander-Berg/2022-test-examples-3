# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im.base import OPERATION_STATUS_TO_IM_OPERATION_STATUS
from travel.rasp.train_api.train_partners.im.factories.order_info import ImOrderInfoFactory, ImRailwayOrderItemFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.ufs.get_order_info import TRANSACTION_INFO_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_partners.ufs.test_utils.response_factories import make_trans_info_response
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner, OperationStatus, OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, TicketFactory, PartnerDataFactory
)
from travel.rasp.train_api.train_purchase.workflow.booking.update_order_tickets import UpdateOrderTickets, UpdateTicketStatuses

pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


def test_ufs_update_order_tickets_success(httpretty):
    order = TrainOrderFactory(partner=TrainPartner.UFS, passengers=[PassengerFactory(tickets=[
        TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)
    ])])
    mock_ufs(httpretty, TRANSACTION_INFO_ENDPOINT, body=make_trans_info_response(order, {
        'blanks': {'123': {'rzhd_status': RzhdStatus.REMOTE_CHECK_IN}}
    }))
    event, order = process_state_action(
        UpdateOrderTickets,
        (UpdateTicketStatuses.FAILED, UpdateTicketStatuses.DONE),
        order
    )
    assert event == UpdateTicketStatuses.DONE
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN


def test_ufs_update_order_tickets_fail(httpretty):
    order = TrainOrderFactory(partner=TrainPartner.UFS, passengers=[PassengerFactory(tickets=[
        TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)
    ])])
    mock_ufs(httpretty, TRANSACTION_INFO_ENDPOINT, body=make_trans_info_response(order, {
        'status': OperationStatus.FAILED,
        'blanks': {'123': {'rzhd_status': RzhdStatus.REMOTE_CHECK_IN}}
    }))
    event, order = process_state_action(
        UpdateOrderTickets,
        (UpdateTicketStatuses.FAILED, UpdateTicketStatuses.DONE),
        order
    )
    assert event == UpdateTicketStatuses.FAILED
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.RESERVATION


def test_im_update_order_tickets_success(httpretty):
    order = TrainOrderFactory(
        partner=TrainPartner.IM, partner_data=PartnerDataFactory(operation_id='11'),
        passengers=[PassengerFactory(tickets=[TicketFactory(rzhd_status=RzhdStatus.REMOTE_CHECK_IN)])],
    )

    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(train_order=order))
    order.passengers[0].tickets[0].rzhd_status = RzhdStatus.RESERVATION
    order.save()
    event, order = process_state_action(
        UpdateOrderTickets,
        (UpdateTicketStatuses.FAILED, UpdateTicketStatuses.DONE),
        order
    )
    assert event == UpdateTicketStatuses.DONE
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN
    assert order.status == OrderStatus.DONE


def test_im_update_order_tickets_fail(httpretty):
    order = TrainOrderFactory(
        partner=TrainPartner.IM, partner_data=PartnerDataFactory(operation_id='11'),
        passengers=[PassengerFactory(tickets=[TicketFactory(rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN)])]
    )

    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
        train_order=order,
        OrderItems=[ImRailwayOrderItemFactory(
            train_order=order,
            SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.FAILED],
        )]
    ))
    order.passengers[0].tickets[0].rzhd_status = RzhdStatus.RESERVATION
    order.save()

    event, order = process_state_action(
        UpdateOrderTickets,
        (UpdateTicketStatuses.FAILED, UpdateTicketStatuses.DONE),
        order
    )
    assert event == UpdateTicketStatuses.FAILED
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.RESERVATION


def test_fill_unfilled_expire_set_er(httpretty):
    order = TrainOrderFactory(
        partner=TrainPartner.IM, partner_data=PartnerDataFactory(operation_id='11', expire_set_er=None),
    )
    order_info = ImOrderInfoFactory(train_order=order)
    for order_item in order_info['OrderItems']:
        order_item['ElectronicRegistrationExpirationDateTime'] = '2018-12-17T22:40:00'
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=order_info)

    event, order = process_state_action(
        UpdateOrderTickets,
        (UpdateTicketStatuses.FAILED, UpdateTicketStatuses.DONE),
        order
    )

    assert event == UpdateTicketStatuses.DONE
    assert order.status == OrderStatus.DONE
    assert order.current_partner_data.expire_set_er == datetime(2018, 12, 17, 19, 40)


@pytest.mark.parametrize('expire_set_er_in_order_info', [None, '2018-12-17T22:40:00'])
def test_dont_override_expire_set_er(httpretty, expire_set_er_in_order_info):
    order = TrainOrderFactory(partner=TrainPartner.IM, partner_data=PartnerDataFactory(
        operation_id='11',
        expire_set_er=datetime(2018, 11, 1, 0, 0)),
    )
    order_info = ImOrderInfoFactory(train_order=order)
    for order_item in order_info['OrderItems']:
        order_item['ElectronicRegistrationExpirationDateTime'] = expire_set_er_in_order_info
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=order_info)

    event, order = process_state_action(
        UpdateOrderTickets,
        (UpdateTicketStatuses.FAILED, UpdateTicketStatuses.DONE),
        order
    )

    assert event == UpdateTicketStatuses.DONE
    assert order.status == OrderStatus.DONE
    assert order.current_partner_data.expire_set_er == datetime(2018, 11, 1, 0, 0)
