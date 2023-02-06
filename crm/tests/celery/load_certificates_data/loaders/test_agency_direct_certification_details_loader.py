from unittest.mock import AsyncMock, Mock

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.loaders import (
    AgencyDirectCertificatesDetailsLoader,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def yt_client(data_rows):
    class YtClientMock:
        def __init__(self):
            self.read_table = Mock(return_value=data_rows)

    return YtClientMock()


@pytest.fixture
def yql_client():
    class YqlTableMock:
        def __init__(self, data):
            self.__data = data

        def get_iterator(self):
            return self.__data

    class YqlRequestResultsMock:
        status = "COMPLETED"
        table = Mock()
        table.refs = [["//", "tmp_table"]]

        def __iter__(self):
            fake_meta = YqlTableMock([("some_kpi_rate", 3, "Поиск")])
            return iter([fake_meta])

    class YqlRequestMock:
        run = Mock()
        get_results = Mock(return_value=YqlRequestResultsMock())

    class YqlClientMock:
        query = Mock(return_value=YqlRequestMock())

    return YqlClientMock()


@pytest.fixture
def loader(db, yql_client, yt_client):
    return AgencyDirectCertificatesDetailsLoader(db, yql_client, yt_client)


@pytest.fixture(autouse=True)
async def kpis_synchronizer(mocker):
    mock = AsyncMock()
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib.celery.tasks."
        "load_certificates_data.DirectKPISynchronizer.process_data",
        mock,
    )

    return mock


@pytest.fixture(autouse=True)
async def conditions_synchronizer(mocker):
    mock = AsyncMock()
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib.celery.tasks."
        "load_certificates_data.DirectConditionsSynchronizer.process_data",
        mock,
    )

    return mock


@pytest.fixture(autouse=True)
async def bonuses_synchronizer(mocker):
    mock = AsyncMock()
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib.celery.tasks."
        "load_certificates_data.DirectBonusPointsSynchronizer.process_data",
        mock,
    )

    return mock


@pytest.fixture(autouse=True)
async def prolongation_scores_synchronizer(mocker):
    mock = AsyncMock()
    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib.celery.tasks."
        "load_certificates_data.DirectProlongationScoreSynchronizer.process_data",
        mock,
    )

    return mock


async def test_executes_yqls(loader, yql_client, yt_client):
    await loader()

    yql_client.query.assert_called()
    yt_client.read_table.assert_called_with(table="//tmp_table")


async def test_calls_synchronizers(
    loader: AgencyDirectCertificatesDetailsLoader,
    kpis_synchronizer: AsyncMock,
    bonuses_synchronizer: AsyncMock,
    conditions_synchronizer: AsyncMock,
    prolongation_scores_synchronizer: AsyncMock,
    data_rows,
):
    await loader()

    kpis_synchronizer.assert_awaited_with(data_rows)
    bonuses_synchronizer.assert_awaited_with(data_rows)
    conditions_synchronizer.assert_awaited_with(data_rows)
    prolongation_scores_synchronizer.assert_awaited_with(data_rows)
