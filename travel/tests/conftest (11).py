# coding=utf-8
import logging
from unittest import mock

import pytest
from travel.library.python.tvm_ticket_provider import (
    provider_fabric, AbstractTvmTicketProvider, ServiceTicket,
    UserTicket
)

from travel.avia.travelers.application import create_application
from travel.avia.travelers.application.lib.feature_flag_storage import flag_storage
from travel.avia.library.python.shared_objects import SharedFlag
from travel.avia.travelers.tests.custom_faker import faker as _faker
from travel.avia.travelers.tests.mocks.datasync import MockDataSyncClient
from travel.avia.travelers.tests.mocks.feature_flag import FeatureFlagClientMock
from travel.avia.travelers.tests.mocks.geodata import MockGeoDataClient

USER_UID = 1000


@pytest.fixture()
def custom_faker():
    return _faker


@pytest.fixture()
def faker():
    return _faker


@pytest.fixture()
def app(tvm_provider, data_sync_client, geobase):
    """Фикстура используемая pytest-tornado под капотом для поднятия сервера."""
    shutdown_flag = SharedFlag()
    return create_application(
        shutdown_flag=shutdown_flag,
        debug=True,
        data_sync_client=data_sync_client,
        enable_tracing=False,
    )


@pytest.fixture()
def data_sync_client():
    return MockDataSyncClient()


@pytest.yield_fixture()
def geobase():
    client = MockGeoDataClient()
    with mock.patch('travel.avia.travelers.application.services.geodata.client.GeoDataClient._instance', client):
        yield client


class FakeTvmTicketProvider(AbstractTvmTicketProvider):
    def __init__(self):
        logger = logging.getLogger('tvm_provider')
        super(FakeTvmTicketProvider, self).__init__(
            client=None,
            destinations=[],
            logger=logger,
        )

    def get_ticket(self, destination):
        return ''

    def check_service_ticket(self, ticket):
        return ServiceTicket(1)

    def check_user_ticket(self, ticket):
        return UserTicket(uids=[USER_UID])


@pytest.fixture(scope='session')
def tvm_provider():
    provider_fabric._instance = FakeTvmTicketProvider()


@pytest.fixture
def header():
    return {
        'Content-Type': 'application/json',
        'X-Ya-User-Ticket': 'secret',
        'X-Ya-Service-Ticket': 'secret',
    }


@pytest.yield_fixture
def feature_flag_client():
    _client = flag_storage.client
    flag_storage.client = FeatureFlagClientMock()
    yield flag_storage.client
    flag_storage.client = _client


@pytest.yield_fixture
def fixture_flag_storage(feature_flag_client):
    flag_storage.reset_context()
    yield flag_storage
    flag_storage.reset_context()
