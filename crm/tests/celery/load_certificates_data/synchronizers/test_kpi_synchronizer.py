from datetime import datetime
from decimal import Decimal

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.synchronizers import (
    DirectKPISynchronizer,
)
from crm.agency_cabinet.certificates.server.tests.factory import Factory
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return DirectKPISynchronizer(con)


@pytest.fixture
def kpi_rows():
    return [{"agency_id": 1234}, {"agency_id": 4321}]


async def test_kpis_synchronizer_inserts_data(
    synchronizer: DirectKPISynchronizer, factory: Factory, data_rows
):
    await synchronizer.process_data(data_rows)

    assert await factory.list_direct_kpis() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "name": "some_kpi_rate",
            "value": Decimal("1.500000"),
            "max_value": Decimal("3.000000"),
            "group_name": "Поиск",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "name": "some_kpi_rate",
            "value": Decimal("1.500000"),
            "max_value": Decimal("3.000000"),
            "group_name": "Поиск",
            "created_at": Any(datetime),
        },
    ]


async def test_removes_existing_data(
    synchronizer: DirectKPISynchronizer, factory: Factory, data_rows
):
    await factory.create_direct_kpi(
        agency_id=1234,
        name="SHOULD NOT BE PRESENTED IN RESULT",
        value=1.5,
        max_value=2,
        group="Поиск",
    )
    await synchronizer.process_data(data_rows)

    assert await factory.list_direct_kpis() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "name": "some_kpi_rate",
            "value": Decimal("1.500000"),
            "max_value": Decimal("3.000000"),
            "group_name": "Поиск",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "name": "some_kpi_rate",
            "value": Decimal("1.500000"),
            "max_value": Decimal("3.000000"),
            "group_name": "Поиск",
            "created_at": Any(datetime),
        },
    ]
