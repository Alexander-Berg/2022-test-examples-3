import pytest
from .storage import StorageMock
from .nw import NwMock
from .internal_api import InternalApiMock


@pytest.fixture
def storage_mock(user_messages, user_messages_bodies):
    mock = StorageMock(10010, user_messages, user_messages_bodies)
    mock.start()
    yield mock
    mock.shutdown()


@pytest.fixture
def nw_mock():
    mock = NwMock(8027)
    mock.start()
    yield mock
    mock.shutdown()


@pytest.fixture
def internal_api_mock(user_folders, user_messages, pop3_messages, labels_from_messages):
    mock = InternalApiMock(
        10080, user_folders, user_folders, labels_from_messages, user_messages, pop3_messages
    )
    mock.start()
    yield mock
    mock.shutdown()
