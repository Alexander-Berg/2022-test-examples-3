from copy import deepcopy
from datetime import datetime, timedelta, timezone

import pytest

from mail.ohio.ohio.core.entities.customer import Customer
from mail.ohio.ohio.storage.exceptions import CustomerNotFoundStorageError


@pytest.fixture(autouse=True)
def utcnow_mock(mocker):
    now = datetime(2020, 6, 23, 13, 41, tzinfo=timezone.utc)
    return mocker.patch(
        'mail.ohio.ohio.storage.mappers.customer.utcnow',
        mocker.Mock(return_value=now)
    )


@pytest.fixture
def customer_entity(randn):
    return Customer(customer_uid=randn())


@pytest.fixture
async def customer(storage, customer_entity):
    return await storage.customer.create(customer_entity)


@pytest.mark.asyncio
async def test_create(utcnow_mock, storage, customer_entity):
    customer = await storage.customer.create(deepcopy(customer_entity))
    customer_entity.created = customer_entity.updated = utcnow_mock.return_value
    assert customer_entity == customer


@pytest.mark.asyncio
async def test_get_not_found(storage, randn):
    with pytest.raises(CustomerNotFoundStorageError):
        await storage.customer.get(customer_uid=randn())


@pytest.mark.asyncio
async def test_get_found(storage, customer):
    assert await storage.customer.get(customer.customer_uid) == customer


@pytest.mark.asyncio
async def test_acquire_next_order_id(storage, utcnow_mock, customer):
    utcnow_mock.return_value += timedelta(minutes=1)
    expected = customer.next_order_id
    returned = await storage.customer.acquire_next_order_id(customer.customer_uid)
    updated_customer = await storage.customer.get(customer.customer_uid)
    assert all((
        expected == returned,
        expected + 1 == updated_customer.next_order_id,
        utcnow_mock.return_value == updated_customer.updated,
    ))
