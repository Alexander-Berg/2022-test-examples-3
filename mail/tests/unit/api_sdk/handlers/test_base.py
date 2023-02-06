import pytest


@pytest.fixture
def token(rands):
    return rands()


@pytest.fixture
def user_ip(rands):
    return rands()


@pytest.mark.asyncio
async def test_auth(mock_auth, sdk_client, token, randn, user_ip):
    await sdk_client.get(f'/v1/order/{randn()}', headers={'Authorization': token, 'X-REAL-IP': user_ip})
    mock_auth.assert_called_once_with(key=token, user_ip=user_ip)


@pytest.mark.asyncio
async def test_empty_auth(mock_auth, sdk_client, randn):
    await sdk_client.get(f'/v1/order/{randn()}')
    mock_auth.assert_called_once_with(key=None, user_ip='127.0.0.1')
