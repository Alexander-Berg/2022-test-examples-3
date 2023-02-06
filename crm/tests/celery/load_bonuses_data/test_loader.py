from unittest.mock import AsyncMock, Mock

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data import (  # noqa: E501
    BonusesDataLoader,
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

        def __iter__(self):
            fake_clients_table = YqlTableMock(
                [
                    (1, "login1", 11),
                    (2, "login2", 22),
                ]
            )
            fake_clients_programs_table = YqlTableMock(
                [
                    (1, 1),
                    (1, 2),
                ]
            )
            fake_gained_client_bonuses_table = YqlTableMock(
                [
                    (1, 1, "222.33"),
                    (1, 2, "333.44"),
                ]
            )
            fake_spent_client_bonuses_table = YqlTableMock(
                [
                    (1, "555.66"),
                    (2, "666.77"),
                ]
            )

            return iter(
                [
                    fake_clients_table,
                    fake_clients_programs_table,
                    fake_gained_client_bonuses_table,
                    fake_spent_client_bonuses_table,
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
        "ClientsTableSynchronizer",
        "ClientsProgramsTableSynchronizer",
        "GainedClientBonusesTableSynchronizer",
        "SpentClientBonusesTableSynchronizer",
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
    return BonusesDataLoader(db, yt_client, yql_client)


async def test_executes_yql_if_new_data_available(
    factory, loader, yt_client, yql_client
):
    await factory.create_gained_client_bonuses(
        client_id=1, program_id=1, gained_at=dt("2021-04-01 00:00:00"), currency="RUR"
    )

    await loader()

    yt_client.exists.assert_any_call(
        "//home/balance/prod/yb-ar/cashback/direct/bonus-cashback-2020/202105"
    )
    yt_client.exists.assert_any_call(
        "//home/agency_analytics/bonuses/funnel/spent/2021-05-01"
    )
    yql_client.query.assert_called()


@pytest.mark.parametrize(
    ("gained_table_exists", "spent_table_exists"),
    [
        (False, False),
        (True, False),
        (False, True),
    ],
)
async def test_does_not_execute_yql_if_no_new_data_available(
    factory, loader, yt_client, yql_client, gained_table_exists, spent_table_exists
):
    yt_client.exists.side_effect = [gained_table_exists, spent_table_exists]
    await factory.create_gained_client_bonuses(
        client_id=1, program_id=1, gained_at=dt("2021-04-01 00:00:00"), currency="RUR"
    )

    await loader()

    yql_client.query.assert_not_called()


async def test_does_not_execute_yql_if_no_new_data_to_date(
    factory, loader, yt_client, yql_client
):
    await factory.create_gained_client_bonuses(
        client_id=1, program_id=1, gained_at=dt("2021-04-01 00:00:00"), currency="RUR"
    )
    await factory.create_gained_client_bonuses(
        client_id=1, program_id=1, gained_at=dt("2021-05-01 00:00:00"), currency="RUR"
    )

    await loader()

    yt_client.exists.assert_not_called()
    yql_client.query.assert_not_called()


async def test_start_date_for_empty_db(loader, yt_client, yql_client):
    await loader()

    yt_client.exists.assert_any_call(
        "//home/balance/prod/yb-ar/cashback/direct/bonus-cashback-2020/202103"
    )
    yt_client.exists.assert_any_call(
        "//home/agency_analytics/bonuses/funnel/spent/2021-03-01"
    )
    yql_client.query.assert_called()


async def test_calls_table_synchronizers(loader, synchronizers_mocks):
    await loader()

    synchronizers_mocks.ClientsTableSynchronizer.assert_awaited_with(
        [
            (1, "login1", 11),
            (2, "login2", 22),
        ]
    )
    synchronizers_mocks.ClientsProgramsTableSynchronizer.assert_awaited_with(
        [
            (1, 1),
            (1, 2),
        ]
    )
    synchronizers_mocks.GainedClientBonusesTableSynchronizer.assert_awaited_with(
        [
            (1, 1, "222.33"),
            (1, 2, "333.44"),
        ]
    )
    synchronizers_mocks.SpentClientBonusesTableSynchronizer.assert_awaited_with(
        [
            (1, "555.66"),
            (2, "666.77"),
        ]
    )
