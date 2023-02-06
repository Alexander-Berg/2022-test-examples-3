# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from decimal import Decimal
from hamcrest import assert_that, greater_than, contains, has_entries

import pytest
from django.test import Client

from common.tester.utils.datetime import replace_now
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


@pytest.fixture(autouse=True)
def fix_now():
    with replace_now('2017-09-05 10:00:00'):
        yield


def test_calculate_tickets_fee():
    ClientContractsFactory()
    response = Client().post('/ru/api/travel/calculate-tickets-fee/', '''{
        "tickets": [
            {
                "id": "ticket_number_one",
                "coach_type": "platzkarte",
                "amount": 100.00,
                "service_amount": 0.00
            },
            {
                "id": "ticket_without_place",
                "coach_type": "platzkarte",
                "amount": 0.00,
                "service_amount": 0.00
            }
        ]
    }''', content_type='application/json')

    assert response.status_code == 200

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'tickets': contains(
            has_entries({
                'id': 'ticket_number_one',
                'fee': greater_than(0),
                'partner_fee': Decimal('10.0'),
                'partner_refund_fee': Decimal('10.0'),
            }),
            has_entries({
                'id': 'ticket_without_place',
                'fee': Decimal(0),
                'partner_fee': Decimal(0),
                'partner_refund_fee': Decimal(0),
            }),
        ),
    }))
