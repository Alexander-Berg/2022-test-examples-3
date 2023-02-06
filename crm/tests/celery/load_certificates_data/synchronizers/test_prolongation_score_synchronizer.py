from datetime import datetime
from decimal import Decimal

import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.synchronizers import (
    DirectProlongationScoreSynchronizer,
)
from crm.agency_cabinet.certificates.server.tests.factory import Factory
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return DirectProlongationScoreSynchronizer(con)


async def test_bonuses_synchronizer_inserts_data(
    synchronizer: DirectProlongationScoreSynchronizer, factory: Factory, data_rows
):
    await synchronizer.process_data(data_rows)

    assert await factory.list_prolongation_scores() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "project": "direct",
            "current_score": Decimal("6.500000"),
            "target_score": Decimal("5.000000"),
            "score_group": "general",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 1234,
            "project": "direct",
            "current_score": Decimal("1.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "rsya",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 1234,
            "project": "direct",
            "current_score": Decimal("3.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "search",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "project": "direct",
            "current_score": Decimal("6.500000"),
            "target_score": Decimal("5.000000"),
            "score_group": "general",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "project": "direct",
            "current_score": Decimal("1.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "rsya",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "project": "direct",
            "current_score": Decimal("3.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "search",
            "created_at": Any(datetime),
        },
    ]


async def test_removes_existing_data(
    synchronizer: DirectProlongationScoreSynchronizer, factory: Factory, data_rows
):
    await factory.create_prolongation_score(
        agency_id=8888,
        project="SHOULD NOT BE PRESENTED IN RESULT",
    )

    await synchronizer.process_data(
        data_rows,
    )

    assert await factory.list_prolongation_scores() == [
        {
            "id": Any(int),
            "agency_id": 1234,
            "project": "direct",
            "current_score": Decimal("6.500000"),
            "target_score": Decimal("5.000000"),
            "score_group": "general",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 1234,
            "project": "direct",
            "current_score": Decimal("1.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "rsya",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 1234,
            "project": "direct",
            "current_score": Decimal("3.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "search",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "project": "direct",
            "current_score": Decimal("6.500000"),
            "target_score": Decimal("5.000000"),
            "score_group": "general",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "project": "direct",
            "current_score": Decimal("1.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "rsya",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "agency_id": 4321,
            "project": "direct",
            "current_score": Decimal("3.000000"),
            "target_score": Decimal("2.000000"),
            "score_group": "search",
            "created_at": Any(datetime),
        },
    ]
