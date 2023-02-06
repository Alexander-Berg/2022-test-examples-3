import pytest


@pytest.fixture
async def zora_images_client(create_client):
    from mail.payments.payments.interactions import ZoraImagesClient
    client = create_client(ZoraImagesClient)
    yield client
    await client.close()
