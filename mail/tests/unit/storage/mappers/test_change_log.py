from copy import deepcopy
from datetime import datetime, timezone

import pytest

from sendr_utils import json_value

from mail.ohio.ohio.core.entities.change_log import ChangeLog


@pytest.fixture(autouse=True)
def utcnow_mock(mocker):
    now = datetime(2020, 6, 25, 10, 35, tzinfo=timezone.utc)
    return mocker.patch(
        'mail.ohio.ohio.storage.mappers.change_log.utcnow',
        mocker.Mock(return_value=now),
    )


@pytest.fixture
def change_log_entity(rands, customer, order):
    return ChangeLog(
        customer_uid=customer.customer_uid,
        order_id=order.order_id,
        data={
            rands(): rands(),
            'some_datetime': datetime(2020, 7, 7),
        },
    )


@pytest.mark.asyncio
async def test_create(storage, customer, utcnow_mock, change_log_entity):
    change_log = await storage.change_log.create(deepcopy(change_log_entity))
    change_log_entity.change_log_id = customer.next_change_log_id
    change_log_entity.created = change_log_entity.updated = utcnow_mock.return_value
    change_log_entity.data = {k: json_value(v) for k, v in change_log_entity.data.items()}
    assert change_log_entity == change_log
