# -*- encoding: utf-8 -*-
from __future__ import absolute_import

from datetime import datetime
import pytest

from travel.avia.library.python.common.models.partner import UpdateHistoryRecord
from travel.avia.library.python.tester.factories import create_partner, create_update_history_record

from travel.avia.backend.main.api_types.partnerka import partner_statuses

BILLING_ORDER_ID = 45L
BILLING_CLIENT_ID = 123L


def history_record(time):
    return {
        'at': time,
        'by': {
            'yandex_login': u'some_login',
            'role': u'some_role'
        }
    }


@pytest.mark.dbuser
def test_empty_statuses():
    partner = create_partner(billing_order_id=BILLING_ORDER_ID, billing_client_id=BILLING_CLIENT_ID)

    statuses = partner_statuses({
        "campaign_id": BILLING_ORDER_ID,
    })

    assert statuses[0] == {
        'click_price': '0.00',
        'current_balance': '1.00',
        'is_aviacompany': False,
        'disabled': False,
        'unabailable_status_history': None,
        'updated': None,
        'code': partner.code,
        'title': u'',
        'hidden_status_history': None,
        'active_status_history': None,
        'id': partner.id,
        'campaign_id': BILLING_ORDER_ID,
        'billing_client_id': BILLING_CLIENT_ID,
    }


@pytest.mark.dbuser
def test_fill_statuses():
    partner = create_partner(billing_order_id=BILLING_ORDER_ID, billing_client_id=BILLING_CLIENT_ID)
    create_update_history_record(
        partner=partner,
        action=UpdateHistoryRecord.CHANGE_ACTIVE_STATUS_ACTION,
        time=datetime(2015, 1, 1)
    )
    create_update_history_record(
        partner=partner,
        action=UpdateHistoryRecord.CHANGE_UNAVAILABILITY_RANGE_ACTION,
        time=datetime(2015, 1, 2)
    )
    create_update_history_record(
        partner=partner,
        action=UpdateHistoryRecord.CHANGE_HIDDEN_STATUS,
        time=datetime(2015, 1, 3)
    )
    statuses = partner_statuses({
        "campaign_id": BILLING_ORDER_ID,
    })

    assert statuses[0] == {
        'disabled': False,
        'click_price': '0.00',
        'current_balance': '1.00',
        'is_aviacompany': False,

        'code': partner.code,
        'title': u'',

        'active_status_history': history_record('2015-01-01T00:00:00'),
        'updated': history_record('2015-01-01T00:00:00'),
        'unabailable_status_history': history_record('2015-01-02T00:00:00'),
        'hidden_status_history': history_record('2015-01-03T00:00:00'),
        'id': partner.id,
        'campaign_id': BILLING_ORDER_ID,
        'billing_client_id': BILLING_CLIENT_ID,
    }

# from random import randint, seed
#
# @pytest.mark.dbuser
# def test_stress_statuses():
#     partner = create_partner(billing_order_id=BILLING_ORDER_ID)
#     seed(42)

#     max_active = datetime(2010, 1, 1)
#     max_unavailability = datetime(2010, 1, 1)
#     max_hidden = datetime(2010, 1, 1)

#     models = []
#     for i in range(0, 10000):
#         active = datetime(2015, 1, randint(1, 30), second=randint(0, 59))
#         unavailability = datetime(2015, 1, randint(1, 30), second=randint(0, 59))
#         hidden = datetime(2015, 1, randint(1, 30), second=randint(0, 59))

#         max_active = max(active, max_active)
#         max_unavailability = max(unavailability, max_unavailability)
#         max_hidden = max(hidden, max_hidden)

#         models.append(create_update_history_record.create_model(
#             partner=partner,
#             action=UpdateHistoryRecord.CHANGE_ACTIVE_STATUS_ACTION,
#             time=active
#         ))
#         models.append(create_update_history_record.create_model(
#             partner=partner,
#             action=UpdateHistoryRecord.CHANGE_UNAVAILABILITY_RANGE_ACTION,
#             time=unavailability
#         ))
#         models.append(create_update_history_record.create_model(
#             partner=partner,
#             action=UpdateHistoryRecord.CHANGE_HIDDEN_STATUS,
#             time=hidden
#         ))
#     create_update_history_record.Model.objects.bulk_create(models)

#     statuses = partner_statuses({
#         "campaign_id": BILLING_ORDER_ID,
#     })
#     assert statuses[0] == {
#        'disabled': False,


#        'code': None,
#        'title': u'',

#        'active_status_history': history_record(max_active.strftime("%Y-%m-%dT%H:%M:%S")),
#        'updated': history_record(max_active.strftime("%Y-%m-%dT%H:%M:%S")),
#        'unabailable_status_history': history_record(max_unavailability.strftime("%Y-%m-%dT%H:%M:%S")),
#        'hidden_status_history': history_record(max_hidden.strftime("%Y-%m-%dT%H:%M:%S")),
#        'id': partner.id,
#        'campaign_id': BILLING_ORDER_ID
#     }
