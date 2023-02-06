import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.processors import (
    DirectProlongationScoreProcessor,
)

pytestmark = [pytest.mark.asyncio]


async def test_prolongation_scores_processor_returns_data(
    direct_synchronizer, data_row
):
    result = list(DirectProlongationScoreProcessor.process_row(data_row))

    assert result == [
        (1234, "direct", 6.5, 5, "general"),
        (1234, "direct", 1, 2, "rsya"),
        (1234, "direct", 3, 2, "search"),
    ]
