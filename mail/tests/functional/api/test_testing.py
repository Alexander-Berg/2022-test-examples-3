import pytest


@pytest.fixture
def testing(mocker):
    mocker.patch('yenv.type', 'testing')


@pytest.fixture
def production(mocker):
    mocker.patch('yenv.type', 'production')


@pytest.mark.asyncio
async def test_delete_user_should_403(production, client, merchant_uid):
    r = await client.delete(f'/testing/users/{merchant_uid}')
    assert r.status == 403


@pytest.mark.asyncio
async def test_delete_user_ok(testing, client, merchant_uid):
    r = await client.delete(f'/testing/users/{merchant_uid}')
    assert r.status == 200
