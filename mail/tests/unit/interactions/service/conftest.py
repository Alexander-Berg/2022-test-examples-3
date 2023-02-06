import pytest


@pytest.fixture
def tvm_id(unique_rand, randn):
    return unique_rand(randn, basket='tvm_id')


@pytest.fixture
async def service_client(create_client, tvm_id):
    from mail.payments.payments.interactions.service import ServiceClient
    client = create_client(ServiceClient, tvm_id=tvm_id)
    yield client
    await client.close()
