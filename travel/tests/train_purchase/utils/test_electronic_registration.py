# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import PassengerFactory, TicketFactory
from travel.rasp.train_api.train_purchase.utils.electronic_registration import (
    get_blank_ids_to_change_er, WrongBlankIdError, update_tickets_er_status, RegistrationStatus
)
from travel.rasp.train_api.train_purchase.views.test_utils import create_order

pytestmark = pytest.mark.mongouser('module')


@pytest.mark.dbuser
@pytest.mark.parametrize('new_status,expected', [
    (RegistrationStatus.ENABLED, ['2']),
    (RegistrationStatus.DISABLED, ['1']),
])
def test_get_blank_ids_to_change_er(new_status, expected):
    order = create_order(
        status=OrderStatus.DONE,
        passengers=[
            PassengerFactory(
                tickets=[
                    TicketFactory(blank_id='1', rzhd_status=RzhdStatus.REMOTE_CHECK_IN),
                ]
            ),
            PassengerFactory(
                tickets=[
                    TicketFactory(blank_id='2', rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN)
                ]
            )
        ]
    )
    result = get_blank_ids_to_change_er(order, ['1', '2'], new_status)
    assert result == expected


@pytest.mark.dbuser
def test_get_blank_ids_to_change_er_wrong_blanks():
    order = create_order(
        status=OrderStatus.DONE,
        passengers=[
            PassengerFactory(
                tickets=[
                    TicketFactory(blank_id='1', rzhd_status=RzhdStatus.REFUNDED),
                ]
            ),
        ]
    )
    with pytest.raises(WrongBlankIdError):
        get_blank_ids_to_change_er(order, ['1'], RegistrationStatus.ENABLED)


@pytest.mark.dbuser
def test_update_tickets_er_status():
    order = create_order(
        status=OrderStatus.DONE,
        passengers=[
            PassengerFactory(
                tickets=[
                    TicketFactory(blank_id='1', rzhd_status=RzhdStatus.NO_REMOTE_CHECK_IN),
                ]
            ),
        ]
    )
    update_tickets_er_status(order, ['1'], RegistrationStatus.ENABLED)
    order.reload()
    assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN.value
