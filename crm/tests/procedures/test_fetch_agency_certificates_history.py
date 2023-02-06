import pytest

from crm.agency_cabinet.certificates.common.structs import (
    AgencyCertificatesHistoryEntry,
    FetchAgencyCertificatesHistoryRequest,
    Pagination,
)
from crm.agency_cabinet.certificates.server.lib.procedures import (
    FetchAgencyCertificatesHistory,
)
from smb.common.testing_utils import dt

pytestmark = [
    pytest.mark.asyncio,
]


@pytest.fixture
def procedure(db):
    return FetchAgencyCertificatesHistory(db)


@pytest.fixture
async def agency_certificates(factory):
    await factory.create_agency_certificate(
        id_=111,
        external_id="external_111",
        project="Метрика",
        start_time=dt("2020-02-09 00:00:00"),
        expiration_time=dt("2020-10-20 00:00:00"),
    )
    await factory.create_agency_certificate(
        id_=222,
        external_id="external_222",
        project="Директ",
        start_time=dt("2020-03-09 00:00:00"),
        expiration_time=dt("2020-11-20 00:00:00"),
    )
    await factory.create_agency_certificate(
        id_=333,
        external_id="external_333",
        project="Метрика",
        start_time=dt("2020-04-09 00:00:00"),
        expiration_time=dt("2020-06-18 00:00:00"),
    )
    await factory.create_agency_certificate(
        id_=444,
        external_id="external_444",
        project="Директ",
        start_time=dt("2020-05-09 00:00:00"),
        expiration_time=dt("2020-06-18 00:00:00"),
    )


async def test_returns_agency_certificates_history(procedure, agency_certificates):
    result = await procedure(
        FetchAgencyCertificatesHistoryRequest(
            agency_id=22, project=None, pagination=Pagination(limit=100, offset=0)
        )
    )

    assert result == [
        AgencyCertificatesHistoryEntry(
            id=222,
            project="Директ",
            start_time=dt("2020-03-09 00:00:00"),
            expiration_time=dt("2020-11-20 00:00:00"),
        ),
        AgencyCertificatesHistoryEntry(
            id=111,
            project="Метрика",
            start_time=dt("2020-02-09 00:00:00"),
            expiration_time=dt("2020-10-20 00:00:00"),
        ),
        AgencyCertificatesHistoryEntry(
            id=444,
            project="Директ",
            start_time=dt("2020-05-09 00:00:00"),
            expiration_time=dt("2020-06-18 00:00:00"),
        ),
        AgencyCertificatesHistoryEntry(
            id=333,
            project="Метрика",
            start_time=dt("2020-04-09 00:00:00"),
            expiration_time=dt("2020-06-18 00:00:00"),
        ),
    ]


async def test_returns_paginated_agency_certificates_history(
    procedure, agency_certificates
):
    result = await procedure(
        FetchAgencyCertificatesHistoryRequest(
            agency_id=22, project=None, pagination=Pagination(limit=2, offset=1)
        )
    )

    assert result == [
        AgencyCertificatesHistoryEntry(
            id=111,
            project="Метрика",
            start_time=dt("2020-02-09 00:00:00"),
            expiration_time=dt("2020-10-20 00:00:00"),
        ),
        AgencyCertificatesHistoryEntry(
            id=444,
            project="Директ",
            start_time=dt("2020-05-09 00:00:00"),
            expiration_time=dt("2020-06-18 00:00:00"),
        ),
    ]


@pytest.mark.parametrize(
    "pagination, expected_ids",
    (
        (dict(offset=0, limit=0), ()),
        (dict(offset=3, limit=2), (333,)),
        (dict(offset=0, limit=100), (222, 111, 444, 333)),
        (dict(offset=100, limit=100), ()),
    ),
)
async def test_count_entries_in_paginated_agency_certificates_history(
    procedure, agency_certificates, pagination, expected_ids
):
    result = await procedure(
        FetchAgencyCertificatesHistoryRequest(
            agency_id=22,
            project=None,
            pagination=Pagination(**pagination),
        )
    )

    assert tuple(cert.id for cert in result) == expected_ids


async def test_returns_filtered_agency_certificates_history(
    procedure, agency_certificates
):
    result = await procedure(
        FetchAgencyCertificatesHistoryRequest(
            agency_id=22, project="Метрика", pagination=Pagination(limit=100, offset=0)
        )
    )

    assert result == [
        AgencyCertificatesHistoryEntry(
            id=111,
            project="Метрика",
            start_time=dt("2020-02-09 00:00:00"),
            expiration_time=dt("2020-10-20 00:00:00"),
        ),
        AgencyCertificatesHistoryEntry(
            id=333,
            project="Метрика",
            start_time=dt("2020-04-09 00:00:00"),
            expiration_time=dt("2020-06-18 00:00:00"),
        ),
    ]


async def test_does_not_return_other_agencies_certificates_history(procedure, factory):
    await factory.create_agency_certificate(
        id_=222,
        agency_id=333,
        external_id="external_222",
        project="Директ",
        start_time=dt("2020-08-21 00:00:00"),
        expiration_time=dt("2020-11-19 00:00:00"),
    )

    result = await procedure(
        FetchAgencyCertificatesHistoryRequest(
            agency_id=22, project=None, pagination=Pagination(limit=100, offset=0)
        )
    )

    assert result == []


async def test_right_order_agencies_certificates_history(
    procedure, agency_certificates
):
    result = await procedure(
        FetchAgencyCertificatesHistoryRequest(
            agency_id=22, project=None, pagination=Pagination(limit=100, offset=0)
        )
    )

    assert [cert.id for cert in result] == [222, 111, 444, 333]
