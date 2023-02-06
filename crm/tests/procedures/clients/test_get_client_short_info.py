import pytest

from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.tests.procedures.conftest import AGENCY_ID
from crm.agency_cabinet.ord.common.exceptions import UnsuitableReportException, UnsuitableAgencyException, \
    UnsuitableClientException


@pytest.fixture
def procedure():
    return procedures.GetClientShortInfo()


async def test_get_client_short_info(
    procedure, fixture_clients2,
):
    result = await procedure(structs.ClientShortInfoInput(
        agency_id=AGENCY_ID,
        report_id=fixture_clients2[0].report_id,
        client_id=fixture_clients2[0].id
    ))

    assert result == structs.ClientShortInfo(
        id=fixture_clients2[0].id,
        client_id=fixture_clients2[0].client_id,
        login=fixture_clients2[0].login,
        name=fixture_clients2[0].name,
    )


async def test_get_client_short_info_incorrect_agency(
    procedure, fixture_clients2,
):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(structs.ClientShortInfoInput(
            agency_id=123456,
            report_id=fixture_clients2[0].report_id,
            client_id=fixture_clients2[0].id
        ))


async def test_get_client_short_info_incorrect_report(
    procedure, fixture_clients2,
):
    with pytest.raises(UnsuitableReportException):
        await procedure(structs.ClientShortInfoInput(
            agency_id=AGENCY_ID,
            report_id=9999,
            client_id=fixture_clients2[0].id
        ))


async def test_get_client_short_info_incorrect_client(
    procedure, fixture_clients2,
):
    with pytest.raises(UnsuitableClientException):
        await procedure(structs.ClientShortInfoInput(
            agency_id=AGENCY_ID,
            report_id=fixture_clients2[0].report_id,
            client_id=9999
        ))
