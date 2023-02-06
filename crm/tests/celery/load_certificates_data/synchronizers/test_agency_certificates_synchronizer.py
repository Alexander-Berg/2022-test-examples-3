from datetime import datetime

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.synchronizers import (  # noqa: E501
    AgencyCertificatesSynchronizer,
)
from smb.common.testing_utils import Any, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return AgencyCertificatesSynchronizer(con)


async def test_inserts_data_into_db(factory, synchronizer, agency_certificates):
    await synchronizer.process_data(agency_certificates)

    results = await factory.list_agency_certificates()
    assert results == [
        {
            "agency_id": 1234,
            "created_at": Any(datetime),
            "expiration_time": dt("2022-05-31 10:35:23"),
            "external_id": "some_id",
            "id": Any(int),
            "project": "direct",
            "start_time": dt("2021-05-31 10:35:23"),
        },
        {
            "agency_id": 4321,
            "created_at": Any(datetime),
            "expiration_time": dt("2022-05-31 10:35:23"),
            "external_id": "another_id",
            "id": Any(int),
            "project": "direct",
            "start_time": dt("2021-05-31 10:35:23"),
        },
    ]


async def test_removes_existing_data(factory, synchronizer, agency_certificates):
    for certificate_id in (1, 2, 3):
        await factory.create_agency_certificate(id_=certificate_id)

    await synchronizer.process_data(agency_certificates)

    results = await factory.list_agency_certificates()
    assert results == [
        {
            "agency_id": 1234,
            "created_at": Any(datetime),
            "expiration_time": dt("2022-05-31 10:35:23"),
            "external_id": "some_id",
            "id": Any(int),
            "project": "direct",
            "start_time": dt("2021-05-31 10:35:23"),
        },
        {
            "agency_id": 4321,
            "created_at": Any(datetime),
            "expiration_time": dt("2022-05-31 10:35:23"),
            "external_id": "another_id",
            "id": Any(int),
            "project": "direct",
            "start_time": dt("2021-05-31 10:35:23"),
        },
    ]
