from unittest.mock import AsyncMock, Mock

import pytest

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_cashback_programs import (  # noqa: E501
    CashbackProgramsLoader,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2021-06-05"),
]


@pytest.fixture
def yql_client():
    class YqlTableMock:
        def __init__(self, data):
            self.__data = data

        def get_iterator(self):
            return self.__data

    class YqlRequestResultsMock:
        status = "COMPLETED"

        fake_cashback_programs_table = YqlTableMock(
            [
                (1, 1, True, False, "Программа 1", "Program 1", "Программа кэшбека #1", "Cashback program #1"),
                (2, 1, False, True, "Программа 1", "Program 1", "Программа кэшбека #1", "Cashback program #1"),
            ]
        )
        table = fake_cashback_programs_table

        def __iter__(self):
            return iter(
                [
                    self.fake_cashback_programs_table,
                ]
            )

    class YqlRequestMock:
        run = Mock()
        get_results = Mock(return_value=YqlRequestResultsMock())

    class YqlClientMock:
        query = Mock(return_value=YqlRequestMock())

    return YqlClientMock()


@pytest.fixture(autouse=True)
async def synchronizers_mocks(mocker):
    mock_manager = Mock()

    for sync in (
        "CashbackProgramsSynchronizer",
    ):
        mock_manager.attach_mock(
            mocker.patch(
                "crm.agency_cabinet.client_bonuses.server.lib."
                f"celery.tasks.load_cashback_programs.{sync}.process_data",
                AsyncMock(),
            ),
            sync,
        )

    return mock_manager


@pytest.fixture
def loader(db, yql_client):
    return CashbackProgramsLoader(db, yql_client)


async def test_calls_table_synchronizers(loader, synchronizers_mocks):
    await loader()

    synchronizers_mocks.CashbackProgramsSynchronizer.assert_awaited_with(
        [
            (1, 1, True, False, "Программа 1", "Program 1", "Программа кэшбека #1", "Cashback program #1"),
            (2, 1, False, True, "Программа 1", "Program 1", "Программа кэшбека #1", "Cashback program #1"),
        ]
    )
