from datetime import datetime

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.synchronizers import (  # noqa: E501
    EmployeesCertificatesSynchronizer,
)
from smb.common.testing_utils import Any, dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return EmployeesCertificatesSynchronizer(con)


async def test_inserts_data_into_db(factory, synchronizer):
    await synchronizer.process_data(
        [
            (
                "111",
                "email1",
                "Альпак Альпакыч",
                5432,
                "metrika",
                "2021-05-31T13:35:23+03:00",
                "2022-05-31T03:00:00+03:00",
            ),
            (
                "222",
                "email2",
                "Капибар Капибарыч",
                2345,
                "direct",
                "2021-06-30T13:35:23+03:00",
                "2022-06-30T03:00:00+03:00",
            ),
        ]
    )

    assert await factory.list_employee_certificates() == [
        {
            "id": Any(int),
            "external_id": "111",
            "employee_email": "email1",
            "employee_name": "Альпак Альпакыч",
            "agency_id": 5432,
            "project": "metrika",
            "start_time": dt("2021-05-31 10:35:23"),
            "expiration_time": dt("2022-05-31 00:00:00"),
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "external_id": "222",
            "employee_email": "email2",
            "employee_name": "Капибар Капибарыч",
            "agency_id": 2345,
            "project": "direct",
            "start_time": dt("2021-06-30 10:35:23"),
            "expiration_time": dt("2022-06-30 00:00:00"),
            "created_at": Any(datetime),
        },
    ]


async def test_removes_existing_data(factory, synchronizer):
    for certificate_id in (1, 2, 3):
        await factory.create_employee_certificate(id_=certificate_id)

    await synchronizer.process_data(
        [
            (
                "111",
                "email1",
                "Альпак Альпакыч",
                5432,
                "metrika",
                "2021-05-31T13:35:23+03:00",
                "2022-05-31T03:00:00+03:00",
            ),
        ]
    )

    assert await factory.list_employee_certificates() == [
        {
            "id": Any(int),
            "external_id": "111",
            "employee_email": "email1",
            "employee_name": "Альпак Альпакыч",
            "agency_id": 5432,
            "project": "metrika",
            "start_time": dt("2021-05-31 10:35:23"),
            "expiration_time": dt("2022-05-31 00:00:00"),
            "created_at": Any(datetime),
        },
    ]
