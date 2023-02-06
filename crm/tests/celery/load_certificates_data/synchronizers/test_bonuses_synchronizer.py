from datetime import datetime

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.synchronizers import (
    DirectBonusPointsSynchronizer,
)
from crm.agency_cabinet.certificates.server.tests.factory import Factory
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return DirectBonusPointsSynchronizer(con)


@pytest.fixture
def bonuses_rows():
    return [
        {
            "agency_id": 1234,
            "agency_case_value": 1.5,
            "agency_case_threshold": 2,
            "agency_case_score": 0,
        },
        {
            "agency_id": 4321,
            "media_general_value": "",
            "media_general_threshold": "",
            "media_general_score": 5,
        },
    ]


async def test_bonuses_synchronizer_inserts_data(
    synchronizer: DirectBonusPointsSynchronizer, factory: Factory, bonuses_rows
):
    await synchronizer.process_data(bonuses_rows)

    assert await factory.list_direct_bonus_points() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "name": "agency_case",
            "value": "1.5",
            "threshold": "2",
            "score": 0,
            "is_met": False,
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "name": "media_general",
            "value": "",
            "threshold": "",
            "score": 5,
            "is_met": True,
            "created_at": Any(datetime),
        },
    ]


async def test_removes_existing_data(
    synchronizer: DirectBonusPointsSynchronizer, factory: Factory, bonuses_rows
):
    await factory.create_direct_bonus_point(
        agency_id=1234,
        name="SHOULD NOT BE PRESENTED IN RESULT",
        value="1.5",
        threshold="2",
        score=0,
        is_met=False,
    )

    await synchronizer.process_data(
        bonuses_rows,
    )

    assert await factory.list_direct_bonus_points() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "name": "agency_case",
            "value": "1.5",
            "threshold": "2",
            "score": 0,
            "is_met": False,
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "name": "media_general",
            "value": "",
            "threshold": "",
            "score": 5,
            "is_met": True,
            "created_at": Any(datetime),
        },
    ]
