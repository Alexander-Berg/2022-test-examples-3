import pytest

from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificatesHistoryEntry,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound
from crm.agency_cabinet.gateway.server.src.procedures.certificates import (
    FetchAgencyCertificatesHistory,
)
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return FetchAgencyCertificatesHistory(service_discovery)


@pytest.mark.parametrize(
    "certificates",
    [
        [],
        [
            AgencyCertificatesHistoryEntry(
                id=1337,
                project="Директ",
                start_time=dt("2020-01-07 00:00:00"),
                expiration_time=dt("2020-08-07 00:00:00"),
            ),
            AgencyCertificatesHistoryEntry(
                id=1489,
                project="Метрика",
                start_time=dt("2020-01-07 00:00:00"),
                expiration_time=dt("2020-08-07 00:00:00"),
            ),
        ],
    ],
)
async def test_returns_certificates(
    procedure, agency_info, service_discovery, certificates
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.certificates.fetch_agency_certificates_history.return_value = (
        certificates
    )

    result = await procedure(
        yandex_uid=4321, agency_id=1234, project=None, offset=0, limit=100
    )

    assert result == certificates


async def test_calls_external_services_for_info(
    procedure, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.certificates.fetch_agency_certificates_history.return_value = []

    await procedure(
        yandex_uid=4321, agency_id=1234, project="direct", offset=0, limit=100
    )

    service_discovery.agencies.get_agencies_info.assert_awaited_with(agency_ids=[1234])
    service_discovery.grants.check_access_level.assert_awaited_with(
        yandex_uid=4321, agency_id=1234
    )
    service_discovery.certificates.fetch_agency_certificates_history.assert_awaited_with(
        agency_id=1234, project="direct", offset=0, limit=100
    )


async def test_raises_if_agency_not_found(procedure, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = []

    with pytest.raises(NotFound):
        await procedure(
            yandex_uid=4321, agency_id=1234, project=None, offset=0, limit=100
        )


async def test_does_not_call_other_services_if_agency_not_found(
    procedure, service_discovery
):
    service_discovery.agencies.get_agencies_info.return_value = []

    try:
        await procedure(
            yandex_uid=4321, agency_id=1234, project=None, offset=0, limit=100
        )
    except NotFound:
        pass

    service_discovery.grants.check_access_level.assert_not_called()
    service_discovery.certificates.fetch_agency_certificates_history.assert_not_called()


async def test_raises_if_access_denied(procedure, service_discovery, agency_info):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(
            yandex_uid=4321, agency_id=1234, project=None, offset=0, limit=100
        )


async def test_does_not_call_certificates_if_access_denied(
    procedure, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(
            yandex_uid=4321, agency_id=1234, project=None, offset=0, limit=100
        )

    service_discovery.certificates.fetch_agency_certificates_history.assert_not_called()
