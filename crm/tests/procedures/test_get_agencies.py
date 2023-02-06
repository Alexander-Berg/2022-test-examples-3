import pytest
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src.db import models
from crm.agency_cabinet.agencies.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetAgenciesInfo()


async def test_get_agencies_info_empty(procedure):
    result = await procedure(
        structs.GetAgenciesInfoRequest(
            agency_ids=[404]
        )
    )

    assert result == structs.GetAgenciesInfoResponse(agencies=[])


async def test_get_agencies_info(fixture_agency: models.Agency, fixture_agency2: models.Agency, procedure):
    requested_agencies = [fixture_agency, fixture_agency2]

    result = await procedure(structs.GetAgenciesInfoRequest(agency_ids=[a.id for a in requested_agencies]))

    agencies_list = [structs.AgencyInfo(agency_id=fixture.id,
                                        name=fixture.name,
                                        phone=fixture.phone,
                                        email=fixture.email,
                                        site=fixture.site,
                                        actual_address=fixture.actual_address,
                                        legal_address=fixture.legal_address) for fixture in requested_agencies]
    assert result == structs.GetAgenciesInfoResponse(
        agencies=agencies_list
    )
