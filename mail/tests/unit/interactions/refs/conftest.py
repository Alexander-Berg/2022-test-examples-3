import pytest


@pytest.fixture
async def refs_client(create_client):
    from mail.payments.payments.interactions.refs import RefsClient
    client = create_client(RefsClient)
    yield client
    await client.close()
