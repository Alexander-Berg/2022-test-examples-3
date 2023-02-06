from unittest.mock import AsyncMock, Mock

import pytest

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data import (  # noqa: E501
    BonusesToActivateDataLoader,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2021-06-05"),
]


@pytest.fixture
def yt_client():
    class YtClientMock:
        exists = Mock(return_value=True)

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

        fake_clients_table = YqlTableMock(
            [
                (1, "login1", 11),
                (2, "login2", 22),
            ]
        )
        fake_client_bonuses_to_activate = YqlTableMock(
            [
                (1, "123.45"),
                (2, "321.54"),
            ]
        )

        table = fake_client_bonuses_to_activate

        def __iter__(self):
            return iter(
                [
                    self.fake_client_bonuses_to_activate,
                ]
            )

    class YqlRequestMock:
        run = Mock()
        get_results = Mock(return_value=YqlRequestResultsMock())

    class YqlClientMock:
        query = Mock(return_value=YqlRequestMock())

    return YqlClientMock()


@pytest.fixture(autouse=True)
async def common_data(factory):
    await factory.create_client(id_=1)


@pytest.fixture(autouse=True)
async def synchronizers_mocks(mocker):
    mock_manager = Mock()

    for sync in (
        "ClientBonusesToActivateTableSynchronizer",
    ):
        mock_manager.attach_mock(
            mocker.patch(
                "crm.agency_cabinet.client_bonuses.server.lib."
                f"celery.tasks.load_bonuses_data.{sync}.process_data",
                AsyncMock(),
            ),
            sync,
        )

    return mock_manager


@pytest.fixture
def loader(db, yt_client, yql_client):
    return BonusesToActivateDataLoader(db, yt_client, yql_client)


async def test_calls_table_synchronizers(loader, synchronizers_mocks):
    await loader()

    synchronizers_mocks.ClientBonusesToActivateTableSynchronizer.assert_awaited_with(
        [
            (1, "123.45"),
            (2, "321.54"),
        ]
    )
