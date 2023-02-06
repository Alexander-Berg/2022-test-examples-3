import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.processors import (
    DirectConditionProcessor,
)

pytestmark = [pytest.mark.asyncio]


async def test_conditions_processor_returns_data(direct_synchronizer, data_row):
    result = list(DirectConditionProcessor.process_row(data_row))

    assert result == [
        (1234, "agency_active_clients", "1", "1", True),
        (1234, "agency_metrika_sert_spec", "0", "0", True),
        (1234, "agency_direct_sert_spec", "18.7", "14.88", True),
    ]


async def test_conditions_processor_does_not_raise_if_kpi_not_presented_in_data(
    direct_synchronizer, data_row
):
    data_row.pop("agency_active_clients_value")

    result = list(DirectConditionProcessor.process_row(data_row))

    assert result == [
        (1234, "agency_metrika_sert_spec", "0", "0", True),
        (1234, "agency_direct_sert_spec", "18.7", "14.88", True),
    ]
