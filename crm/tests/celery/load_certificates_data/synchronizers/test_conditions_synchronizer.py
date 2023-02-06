from datetime import datetime

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.synchronizers import (
    DirectConditionsSynchronizer,
)
from crm.agency_cabinet.certificates.server.tests.factory import Factory
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return DirectConditionsSynchronizer(con)


@pytest.fixture
def conditions_rows():
    return [
        {
            "agency_id": 1234,
            "agency_active_clients_value": 1,
            "agency_active_clients_threshold": 2,
        },
        {
            "agency_id": 4321,
            "agency_metrika_sert_spec_value": 0,
            "agency_metrika_sert_spec_threshold": 0,
        },
    ]


async def test_conditions_synchronizer_inserts_data(
    synchronizer: DirectConditionsSynchronizer, factory: Factory, conditions_rows
):
    await synchronizer.process_data(conditions_rows)

    assert await factory.list_direct_conditions() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "name": "agency_active_clients",
            "value": "1",
            "threshold": "2",
            "is_met": False,
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "name": "agency_metrika_sert_spec",
            "value": "0",
            "threshold": "0",
            "is_met": True,
            "created_at": Any(datetime),
        },
    ]


async def test_removes_existing_data(
    synchronizer: DirectConditionsSynchronizer, factory: Factory, conditions_rows
):
    await factory.create_certificate_condition(
        agency_id=1234,
        name="SHOULD NOT BE PRESENTED IN RESULT",
        value="1.5",
        threshold="2",
        is_met=False,
    )

    await synchronizer.process_data(conditions_rows)

    assert await factory.list_direct_conditions() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "name": "agency_active_clients",
            "value": "1",
            "threshold": "2",
            "is_met": False,
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "name": "agency_metrika_sert_spec",
            "value": "0",
            "threshold": "0",
            "is_met": True,
            "created_at": Any(datetime),
        },
    ]
