import pytest
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src.db import models
from crm.agency_cabinet.agencies.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetClientInfo()


async def test_get_client_info(procedure, fixture_agency: models.Agency, fixture_client: models.Client):
    result = await procedure(
        structs.GetClientInfoRequest(
            agency_id=fixture_agency.id,
            client_id=fixture_client.id
        )
    )

    assert result == structs.GetClientInfoResponse(client=structs.ClientInfo(
        id=fixture_client.id,
        name=fixture_client.name,
        login=fixture_client.login)
    )


async def test_get_client_info_no_such_client(procedure, fixture_agency: models.Agency, fixture_client: models.Client):

    with pytest.raises(procedures.NoSuchClient):
        await procedure(
            structs.GetClientInfoRequest(
                agency_id=fixture_agency.id,
                client_id=fixture_client.id + 10000
            )
        )


async def test_get_client_info_unsuitable_agency(procedure, fixture_agency2: models.Agency, fixture_client: models.Client):

    with pytest.raises(procedures.UnsuitableAgency):
        await procedure(
            structs.GetClientInfoRequest(
                agency_id=fixture_agency2.id,
                client_id=fixture_client.id
            )
        )
