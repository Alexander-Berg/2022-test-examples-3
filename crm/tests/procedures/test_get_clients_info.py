import pytest
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src.db import models
from crm.agency_cabinet.agencies.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetClientsInfo()


@pytest.mark.parametrize(('request_params', 'expected_agency_fixtures_list'), [
    ({}, ['fixture_client', 'fixture_client2']),
    ({
        'limit': 1
    }, ['fixture_client']),
    ({
        'limit': 1,
        'offset': 1
    }, ['fixture_client2']),
    (
        {
            'search_query': 'bil',
        },
        ['fixture_client']
    ),
    (
        {
            'search_query': 'Билл',
        },
        ['fixture_client']
    )
])
async def test_get_clients_info(procedure,
                                fixture_client, fixture_client2,
                                fixture_agency: models.Agency, request, request_params, expected_agency_fixtures_list):
    expected_clients = [request.getfixturevalue(fixture_name) for fixture_name in expected_agency_fixtures_list]
    result = await procedure(
        structs.GetClientsInfoRequest(
            agency_id=fixture_agency.id,
            **request_params
        )
    )

    assert result == structs.GetClientsInfoResponse(
        clients=[structs.ClientInfo(id=client_model.id,
                                    name=client_model.name,
                                    login=client_model.login) for client_model in expected_clients]
    )
