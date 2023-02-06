import pytest

from crm.agency_cabinet.certificates.common.structs import AgencyCertificate
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound
from crm.agency_cabinet.gateway.server.src.procedures.certificates import (
    ListAgencyCertificates,
)
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return ListAgencyCertificates(service_discovery)


@pytest.mark.parametrize(
    "certificates",
    [
        [],
        [
            AgencyCertificate(
                id=1337,
                project="Директ",
                expiration_time=dt("2020-08-07 00:00:00"),
                auto_renewal_is_met=True,
            ),
            AgencyCertificate(
                id=1337,
                project="Метрика",
                expiration_time=dt("2020-08-07 00:00:00"),
                auto_renewal_is_met=False,
            ),
        ],
    ],
)
async def test_returns_certificates(
    procedure, agency_info, service_discovery, certificates
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.certificates.list_agency_certificates.return_value = certificates

    result = await procedure(yandex_uid=4321, agency_id=1234)

    assert result == certificates


async def test_calls_external_services_for_info(
    procedure, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.certificates.list_agency_certificates.return_value = []

    await procedure(yandex_uid=4321, agency_id=1234)

    service_discovery.agencies.get_agencies_info.assert_awaited_with(agency_ids=[1234])
    service_discovery.grants.check_access_level.assert_awaited_with(
        yandex_uid=4321, agency_id=1234
    )
    service_discovery.certificates.list_agency_certificates.assert_awaited_with(
        agency_id=1234
    )


async def test_raises_if_agency_not_found(procedure, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.agencies.get_agencies_info.return_value = []

    with pytest.raises(NotFound):
        await procedure(yandex_uid=4321, agency_id=1234)


async def test_does_not_call_other_services_if_agency_not_found(
    procedure, service_discovery
):
    service_discovery.agencies.get_agencies_info.return_value = []

    with pytest.raises(NotFound):
        await procedure(yandex_uid=4321, agency_id=1234)

    service_discovery.grants.check_access_level.assert_not_called()
    service_discovery.certificates.list_agency_certificates.assert_not_called()


async def test_raises_if_access_denied(procedure, service_discovery, agency_info):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=4321, agency_id=1234)


async def test_does_not_call_certificates_if_access_denied(
    procedure, service_discovery, agency_info
):
    service_discovery.agencies.get_agencies_info.return_value = [agency_info]
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=4321, agency_id=1234)

    service_discovery.certificates.list_agency_certificates.assert_not_called()
