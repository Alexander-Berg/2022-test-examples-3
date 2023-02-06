import os
from copy import deepcopy
from decimal import Decimal

import aiohttp.pytest_plugin
import pytest

from sendr_pytest import *  # noqa
from sendr_utils import utcnow

from mail.ohio.ohio.api.app import OhioApplication
from mail.ohio.ohio.core.entities.customer import Customer
from mail.ohio.ohio.core.entities.order import NDS, Item, Order, OrderData, OrderStatus, Refund, RefundStatus
from mail.ohio.ohio.core.entities.service import Service, ServiceType
from mail.ohio.ohio.storage import Storage


def pytest_configure(config):
    os.environ['QLOUD_TVM_TOKEN'] = 'x' * 32


pytest_plugins = ['aiohttp.pytest_plugin']
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
def ohio_settings():
    from mail.ohio.ohio.conf import settings
    data = deepcopy(settings._settings)
    yield settings
    settings._settings = data


@pytest.fixture
async def app(aiohttp_client, db_engine, ohio_settings):
    ohio_settings.TVM_CHECK_SERVICE_TICKET = False
    return await aiohttp_client(OhioApplication(db_engine=db_engine))


@pytest.fixture
def mock_action(mocker):
    def _inner(action_cls, action_result=None):
        async def run(self):
            return action_result

        mocker.patch.object(action_cls, 'run', run)
        return mocker.patch.object(action_cls, '__init__', mocker.Mock(return_value=None))
    return _inner


@pytest.fixture
async def dbconn(app, db_engine):
    # app dependency is required to ensure exit order
    async with db_engine.acquire() as conn:
        yield conn


@pytest.fixture
async def storage(dbconn):
    return Storage(dbconn)


@pytest.fixture
async def returned(returned_func):
    return await returned_func()


@pytest.fixture
async def response(response_func):
    return await response_func()


@pytest.fixture
def customer_entity(randn):
    return Customer(customer_uid=randn())


@pytest.fixture
def create_customer(randn, storage, customer_entity):
    async def _inner(**kwargs):
        entity = deepcopy(customer_entity)
        entity.customer_uid = randn()
        for k, v in kwargs.items():
            setattr(entity, k, v)
        return await storage.customer.create(entity)

    return _inner


@pytest.fixture
async def customer(create_customer):
    return await create_customer()


@pytest.fixture
def service_entity(randn):
    return Service(
        service_type=ServiceType.PAYMENTS,
        payments_service_id=randn(),
    )


@pytest.fixture
def create_service(randn, storage, service_entity):
    async def _inner(**kwargs):
        entity = deepcopy(service_entity)
        entity.payments_service_id = randn()  # unique in db
        for k, v in kwargs.items():
            setattr(entity, k, v)
        return await storage.service.create(entity)

    return _inner


@pytest.fixture
async def service(create_service):
    return await create_service()


@pytest.fixture
def order_entity(randn, rands, customer, service):
    return Order(
        customer_uid=customer.customer_uid,
        service_id=service.service_id,
        subservice_id=rands(),
        merchant_uid=randn(),
        service_merchant_id=randn(),
        payments_order_id=randn(),
        trust_payment_id=rands(),
        trust_purchase_token=rands(),
        status=OrderStatus.PAID,
        order_data=OrderData(
            total=Decimal('123.45'),
            currency=rands(),
            description=rands(),
            items=[
                Item(
                    amount=Decimal('123.45'),
                    price=Decimal('67.89'),
                    currency=rands(),
                    nds=NDS.NDS_NONE,
                    name=rands(),
                    image_path='image/path',
                    image_url='http://image.test',
                ),
            ],
            refunds=[
                Refund(
                    trust_refund_id=rands(),
                    refund_status=RefundStatus.CREATED,
                    total=Decimal('123.45'),
                    currency=rands(),
                    items=[
                        Item(
                            amount=Decimal('123.45'),
                            price=Decimal('67.89'),
                            currency=rands(),
                            nds=NDS.NDS_NONE,
                            name=rands(),
                        ),
                    ],
                )
            ],
        ),
        order_revision=randn(),
        service_data={'custom': 'id'},
        service_revision=randn(),
        created=utcnow(),
    )


@pytest.fixture
def create_order(rands, storage, order_entity):
    async def _inner(**kwargs):
        entity = deepcopy(order_entity)
        entity.trust_purchase_token = rands()
        for k, v in kwargs.items():
            setattr(entity, k, v)
        return await storage.order.create(entity)

    return _inner


@pytest.fixture
async def order(create_order):
    return await create_order()
