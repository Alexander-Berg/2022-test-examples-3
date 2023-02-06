import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.processors import (
    DirectBonusPointProcessor,
)

pytestmark = [pytest.mark.asyncio]


async def test_bonuses_processor_returns_data(direct_synchronizer, data_row):
    result = list(DirectBonusPointProcessor.process_row(data_row))

    assert result == [
        (1234, "agency_case", "1.5", "2", 0, False),
        (1234, "media_general", "", "", 5, True),
        (1234, "media_revenue_bonus", "1000", "500", 0, True),
        (1234, "agency_media_sert", "1000", "3.4", 0, True),
        (1234, "pdz", "0", "отсутствует", 0, False),
    ]


async def test_bonus_processor_does_not_raise_if_bonus_not_presented_in_data(
    direct_synchronizer, data_row
):
    data_row.pop("agency_case_value")

    result = list(DirectBonusPointProcessor.process_row(data_row))

    assert result == [
        (1234, "media_general", "", "", 5, True),
        (1234, "media_revenue_bonus", "1000", "500", 0, True),
        (1234, "agency_media_sert", "1000", "3.4", 0, True),
        (1234, "pdz", "0", "отсутствует", 0, False),
    ]
