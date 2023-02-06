import pytest


@pytest.fixture
def mock_auth(mock_action, merchant):
    from mail.payments.payments.api_sdk.handlers.base import GetUidByKeyAction
    return mock_action(GetUidByKeyAction, merchant.uid)


@pytest.fixture
async def sdk_client(mock_auth, sdk_app, aiohttp_client):
    return await aiohttp_client(sdk_app)
