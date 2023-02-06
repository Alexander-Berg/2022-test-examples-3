import pytest


@pytest.fixture
async def returned(trust_client, merchant, acquirer, codes):
    return await trust_client.mcc_get(codes=codes, acquirer=acquirer, uid=merchant.uid)


@pytest.mark.parametrize('codes', ([1234], [1234, 2345]))
@pytest.mark.asyncio
async def test_mcc_get(trust_client, codes, returned):
    assert all((
        trust_client.call_args[1] == 'GET',
        trust_client.call_args[2].endswith('/mcc'),
    ))


@pytest.mark.parametrize('codes', ([1234], [1234, 2345]))
@pytest.mark.asyncio
async def test_codes(trust_client, codes, returned):
    returned_codes = [int(code) for code in trust_client.call_kwargs['params']['codes'].split(',')]
    assert sorted(returned_codes) == sorted(codes)
