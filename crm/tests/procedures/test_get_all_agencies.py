import pytest
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src.db import models
from crm.agency_cabinet.agencies.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetAllAgenciesInfo()


async def test_get_all_agencies_info(procedure, fixture_agency: models.Agency, fixture_agency2: models.Agency, fixture_agency3: models.Agency):
    all_agencies = [fixture_agency, fixture_agency2, fixture_agency3]

    result = await procedure()

    agencies_list = [structs.AgencyInfo(agency_id=fixture.id,
                                        name=fixture.name,
                                        phone=fixture.phone,
                                        email=fixture.email,
                                        site=fixture.site,
                                        actual_address=fixture.actual_address,
                                        legal_address=fixture.legal_address) for fixture in all_agencies]

    assert result == structs.GetAgenciesInfoResponse(
        agencies=agencies_list
    )
