from unittest.mock import AsyncMock, Mock

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data import (  # noqa: E501
    EmployeeCertificatesDataLoader,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def yql_client():
    class YqlTableMock:
        def __init__(self, data):
            self.__data = data

        def get_iterator(self):
            return self.__data

    class YqlRequestResultsMock:
        status = "COMPLETED"
        table = YqlTableMock(
            [
                (
                    "111",
                    "email1",
                    "Альпак Альпакыч",
                    5432,
                    "metrika",
                    "2021-05-31T13:35:23.496+03:00",
                    "2022-05-31T03:00:00+03:00",
                ),
                (
                    "222",
                    "email2",
                    "Капибар Капибарыч",
                    2345,
                    "direct",
                    "2021-06-31T13:35:23.496+03:00",
                    "2022-06-31T03:00:00+03:00",
                ),
            ]
        )

        def __iter__(self):
            return iter([self.table])

    class YqlRequestMock:
        run = Mock()
        get_results = Mock(return_value=YqlRequestResultsMock())

    class YqlClientMock:
        query = Mock(return_value=YqlRequestMock())

    return YqlClientMock()


@pytest.fixture(autouse=True)
async def employees_certificates_synchronizer(mocker):
    mock = AsyncMock()
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib.celery.tasks."
        "load_certificates_data.EmployeesCertificatesSynchronizer.process_data",
        mock,
    )

    return mock


@pytest.fixture
def loader(db, yql_client):
    return EmployeeCertificatesDataLoader(db, yql_client)


async def test_executes_yql(loader, yql_client):
    await loader()

    yql_client.query.assert_called()


async def test_calls_table_synchronizers(loader, employees_certificates_synchronizer):
    await loader()

    employees_certificates_synchronizer.assert_awaited_with(
        [
            (
                "111",
                "email1",
                "Альпак Альпакыч",
                5432,
                "metrika",
                "2021-05-31T13:35:23.496+03:00",
                "2022-05-31T03:00:00+03:00",
            ),
            (
                "222",
                "email2",
                "Капибар Капибарыч",
                2345,
                "direct",
                "2021-06-31T13:35:23.496+03:00",
                "2022-06-31T03:00:00+03:00",
            ),
        ]
    )
