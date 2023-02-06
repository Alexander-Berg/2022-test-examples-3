import pytest


@pytest.fixture
def mocked_logger(mocker):
    return mocker.MagicMock()


@pytest.fixture
def handler(mocker):
    return mocker.AsyncMock(spec=[])
