import pytest


@pytest.fixture
async def search_wizard_client(create_client):
    from mail.payments.payments.interactions import SearchWizardClient
    client = create_client(SearchWizardClient)
    yield client
    await client.close()
