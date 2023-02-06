from copy import deepcopy
from datetime import datetime, timedelta, timezone

import pytest

from mail.ohio.ohio.core.entities.service import Service, ServiceType
from mail.ohio.ohio.storage.exceptions import ServiceNotFoundStorageError


@pytest.fixture(autouse=True)
def utcnow_mock(mocker):
    now = datetime(202, 6, 23, 14, 33, tzinfo=timezone.utc)
    return mocker.patch(
        'mail.ohio.ohio.storage.mappers.service.utcnow',
        mocker.Mock(return_value=now)
    )


@pytest.fixture
def service_entity(randn, rands):
    return Service(
        service_type=ServiceType.PAYMENTS,
        tvm_id=randn(),
        trust_service_id=rands(),
        payments_service_id=randn(),
        enabled=True,
    )


@pytest.fixture
async def service(storage, service_entity):
    return await storage.service.create(service_entity)


@pytest.mark.asyncio
async def test_create(storage, utcnow_mock, service_entity):
    service = await storage.service.create(deepcopy(service_entity))
    service_entity.created = service_entity.updated = utcnow_mock.return_value
    service_entity.service_id = service.service_id
    assert service_entity == service


@pytest.mark.asyncio
async def test_get_not_found(storage, randn):
    with pytest.raises(ServiceNotFoundStorageError):
        await storage.service.get(service_id=randn())


@pytest.mark.asyncio
async def test_get(storage, service):
    assert service == await storage.service.get(service_id=service.service_id)


@pytest.mark.asyncio
async def test_get_by_payments_service_id_not_found(storage, randn):
    with pytest.raises(ServiceNotFoundStorageError):
        await storage.service.get_by_payments_service_id(randn())


@pytest.mark.asyncio
async def test_get_by_payments_service_id(storage, service):
    assert service == await storage.service.get_by_payments_service_id(service.payments_service_id)


@pytest.mark.asyncio
async def test_save(storage, utcnow_mock, service):
    # updatable fields
    service.tvm_id += 1
    service.trust_service_id += '1'
    service.payments_service_id += 1
    service.enabled = not service.enabled

    updated_service = deepcopy(service)
    # non updatable fields
    updated_service.service_type = ServiceType.TRUST
    updated_service.created += timedelta(hours=1)
    updated_service.updated += timedelta(hours=1)

    utcnow_mock.return_value += timedelta(minutes=1)
    service.updated = utcnow_mock.return_value

    updated_service = await storage.service.save(updated_service)
    assert service == updated_service
