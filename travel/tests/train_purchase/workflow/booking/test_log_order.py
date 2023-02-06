# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import time

import mock
import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import PassengerFactory, SourceFactory, TrainOrderFactory
from travel.rasp.train_api.train_purchase.workflow.booking.log_order import LogOrder, LogOrderEvents, yt_order_logger

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@mock.patch.object(time, 'time', autospec=True, return_value=1000000000.1)
@mock.patch.object(yt_order_logger, 'info', autospec=True)
@pytest.mark.parametrize('source, expected_device', [
    (None, None),
    (SourceFactory(), 'desktop'),
    (SourceFactory(device=None), None),
])
def test_order_log(m_yt_order_logger_info, m_time, source, expected_device):
    order = TrainOrderFactory(passengers=(PassengerFactory(), PassengerFactory()), source=source)
    event, order = process_state_action(LogOrder, (LogOrderEvents.OK,), order)

    assert event == LogOrderEvents.OK
    m_yt_order_logger_info.assert_called_once_with(msg=None, extra={
        'timestamp': 1000000000,
        'order_uid': order.uid,
        'reserved_to': int(time.mktime(order.reserved_to.timetuple())),
        'num_of_tickets': 2,
        'amount': 200.0,
        'fee': 140.0,
        'fee_without_im': 80.0,
        'request_id': order.source.req_id if order.source else None,
        'device': expected_device,
        'yandex_uid': '1212121212121212121',
    })
