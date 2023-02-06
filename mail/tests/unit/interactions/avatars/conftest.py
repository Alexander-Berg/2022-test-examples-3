import pytest


@pytest.fixture
async def avatars_client(create_client):
    from mail.payments.payments.interactions import AvatarsClient
    client = create_client(AvatarsClient)
    yield client
    await client.close()
