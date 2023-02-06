# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import mock
import pytest

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from common.utils.date import MSK_TZ
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult, PassengerInfo, TicketInfo
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, OperationStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, TicketFactory, PassengerFactory
from travel.rasp.train_api.train_purchase.workflow.booking import (
    UpdateExpiredOrder, UpdateExpiredOrderStatuses, update_expired_order
)
from travel.rasp.train_api.train_purchase.workflow.booking.update_expired_order import ERROR_MESSAGE_PARTNER_STATUS_DONE

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def create_order_info(operation_status=OperationStatus.IN_PROCESS, ticket_status=RzhdStatus.NO_REMOTE_CHECK_IN):
    return OrderInfoResult(
        buy_operation_id=None, expire_set_er=None, status=operation_status, order_num=None,
        passengers=[PassengerInfo(doc_id=123456, blank_id=None)],
        tickets=[TicketInfo('12121212', ticket_status, 3000, False)]
    )


def create_train_order(reserved_to=MSK_TZ.localize(datetime(2018, 7, 18, 15, 55)),
                       status=OrderStatus.CONFIRM_FAILED,
                       rzhd_status=None):
    return TrainOrderFactory(
        reserved_to=reserved_to, status=status,
        passengers=[PassengerFactory(tickets=[TicketFactory(rzhd_status=rzhd_status, blank_id='12121212')])]
    )


def run_process(order):
    return process_state_action(
        UpdateExpiredOrder,
        (UpdateExpiredOrderStatuses.OK, UpdateExpiredOrderStatuses.FAILED,
         UpdateExpiredOrderStatuses.RETRY, UpdateExpiredOrderStatuses.CANCEL),
        order
    )


@replace_now('2018-07-18 16:00:00')
@mock.patch.object(update_expired_order, 'get_order_info', autospec=True)
def test_just_update_statuses(m_get_order_info):
    order = create_train_order(status=OrderStatus.CONFIRM_FAILED)
    m_get_order_info.return_value = create_order_info(operation_status=OperationStatus.FAILED,
                                                      ticket_status=RzhdStatus.CANCELLED)
    event, order = run_process(order)
    assert m_get_order_info.call_count == 1
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.CANCELLED
    assert order.current_partner_data.operation_status == OperationStatus.FAILED
    assert event == UpdateExpiredOrderStatuses.OK


@replace_now('2018-07-18 16:00:00')
@mock.patch.object(update_expired_order, 'get_order_info', autospec=True)
def test_update_statuses_and_cancel(m_get_order_info):
    order = create_train_order(status=OrderStatus.CONFIRM_FAILED)
    m_get_order_info.return_value = create_order_info(operation_status=OperationStatus.IN_PROCESS,
                                                      ticket_status=RzhdStatus.REMOTE_CHECK_IN)
    event, order = run_process(order)
    assert m_get_order_info.call_count == 1
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN
    assert order.current_partner_data.operation_status == OperationStatus.IN_PROCESS
    assert event == UpdateExpiredOrderStatuses.CANCEL


@replace_now('2018-07-18 16:00:00')
@replace_dynamic_setting('TRAIN_PURCHASE_ERRORS_EMAIL', 'ough@ya.ru')
@replace_setting('ORDER_CANCEL_ERRORS_CAMPAIGN', 'campaign!')
@mock.patch.object(update_expired_order, 'get_order_info', autospec=True)
@mock.patch.object(update_expired_order, 'guaranteed_send_email', autospec=True)
def test_send_error(m_guaranteed_send_email, m_get_order_info):
    order = create_train_order(status=OrderStatus.CONFIRM_FAILED)
    m_get_order_info.return_value = create_order_info(operation_status=OperationStatus.OK,
                                                      ticket_status=RzhdStatus.REMOTE_CHECK_IN)
    event, order = run_process(order)
    assert m_get_order_info.call_count == 1
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN
    assert order.current_partner_data.operation_status == OperationStatus.OK
    assert event == UpdateExpiredOrderStatuses.OK
    m_guaranteed_send_email.assert_called_once_with(
        key='send_problem_order_cant_be_cancelled_{}'.format(order.uid),
        to_email='ough@ya.ru',
        args={
            'order_uid': order.uid,
            'order_status': 'confirm_failed',
            'error_message': ERROR_MESSAGE_PARTNER_STATUS_DONE,
        },
        campaign='campaign!',
        log_context={'order_uid': order.uid},
    )


@replace_now('2018-07-18 16:00:00')
@mock.patch.object(update_expired_order, 'get_order_info', autospec=True)
def test_error(m_get_order_info):
    order = create_train_order(status=OrderStatus.CONFIRM_FAILED)
    m_get_order_info.side_effect = Exception('bah!')
    event, order = run_process(order)
    assert event == UpdateExpiredOrderStatuses.RETRY


@replace_now('2018-07-18 16:00:00')
@mock.patch.object(update_expired_order, 'get_order_info', autospec=True)
def test_error_no_retry(m_get_order_info):
    order = create_train_order(status=OrderStatus.CONFIRM_FAILED, reserved_to=MSK_TZ.localize(datetime(2011, 1, 1)))
    m_get_order_info.side_effect = Exception('bah!')
    event, order = run_process(order)
    assert event == UpdateExpiredOrderStatuses.FAILED
