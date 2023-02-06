# coding=utf-8
from __future__ import unicode_literals

import json

import six
import yatest.common

from travel.cpa.collectors.avia.aeroflot import (AeroflotCollector, safe_fromstring)
from travel.cpa.lib.order_snapshot import OrderCurrencyCode


def test_aeroflot_parse_price():
    message_path = 'travel/cpa/tests/ut/data/collectors/aeroflot/message_with_price.xml'
    message = open(yatest.common.source_path(message_path)).read()
    for booking in safe_fromstring(message).xpath('//bookings/booking'):
        amount, currency = AeroflotCollector.parse_price(booking)
        assert amount == 44000.0
        assert currency == OrderCurrencyCode.RUB


class MockedAeroflotCollector(AeroflotCollector):
    def __init__(self):
        self.partner_id = 1000
        self.billing_order_id = 2000
        super(AeroflotCollector, self).__init__()

    def get_raw_snapshots(self):
        marker = 'YA0QI8SC77'
        message_paths = [
            ('travel/cpa/tests/ut/data/collectors/aeroflot/duplicate_test_1.xml', '2019-10-18 13:40:14'),
            ('travel/cpa/tests/ut/data/collectors/aeroflot/duplicate_test_2.xml', '2019-10-18 14:40:14'),
        ]
        for message_path, date_sent in message_paths:
            xml = open(yatest.common.source_path(message_path)).read()
            body = {
                'message': xml,
                'marker': marker,
                'date_sent': date_sent,
            }
            message = {
                'Body': json.dumps(body)
            }

            yield message


def test_aeroflot_mock_sqs():
    desired_list = [{
        'partner_name': 'aeroflot',
        'partner_id': 1000,
        'billing_order_id': 2000,
        'partner_order_id': 'YA0QI8SC77_HSDLQB',
        'travel_order_id': 'aeroflot:YA0QI8SC77_HSDLQB',
        'order_amount': 44000.0,
        'currency_code': 'RUB',
        'profit_amount': 215.0,
        'status': 'confirmed',
        'label': 'YA0QI8SC77',
        'created_at': 1571380500,
        'updated_at': 1571409614,
    }]
    actual_list = list(MockedAeroflotCollector()._get_snapshots())
    assert len(actual_list) == len(desired_list)
    for desired, actual in zip(desired_list, actual_list):
        actual_dict = actual.as_dict()
        checked_actual_dict = {k: v for k, v in six.iteritems(actual_dict) if k in desired}
        assert checked_actual_dict == desired
