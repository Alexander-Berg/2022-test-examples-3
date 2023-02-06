from unittest.mock import AsyncMock, Mock

import pytest

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data import (  # noqa: E501
    BonusesCorrectionsDataLoader,
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

        fake_gained_client_bonuses_table = YqlTableMock(
            [
                (1, 1, "222.33"),
                (1, 2, "333.44"),
            ]
        )

        table = fake_gained_client_bonuses_table

        def __iter__(self):
            return iter(
                [
                    self.fake_gained_client_bonuses_table,
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
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_client_program(client_id=1, program_id=1)


@pytest.fixture(autouse=True)
async def synchronizers_mocks(mocker):
    mock_manager = Mock()

    for sync in (
        "GainedClientBonusesTableSynchronizer",
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
    return BonusesCorrectionsDataLoader(db, yt_client, yql_client, '202112')


async def test_calls_table_synchronizers(loader, synchronizers_mocks):
    await loader()

    synchronizers_mocks.GainedClientBonusesTableSynchronizer.assert_awaited_with(
        [
            (1, 1, "222.33"),
            (1, 2, "333.44"),
        ]
    )
