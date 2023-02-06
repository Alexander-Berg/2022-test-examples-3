import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.processors import (
    DirectKPIProcessor,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def meta_data():
    return [
        [
            ["label", ["some_kpi_rate"]],
            ["value_type", ["Поиск"]],
            ["max_score", ["2"]],
        ]
    ]


async def test_kpi_processor_returns_data(direct_synchronizer, data_row, meta_data):
    data_row["meta_data"] = meta_data
    result = list(DirectKPIProcessor.process_row(data_row))

    assert result == [(1234, "some_kpi_rate", 1.5, 2, "Поиск")]


async def test_kpis_processor_does_not_raise_if_kpi_not_presented_in_data(
    direct_synchronizer, data_row, meta_data
):
    meta_data.append(
        [
            ["label", ["another_kpi_rate"]],
            ["value_type", ["Поиск"]],
            ["max_score", ["2"]],
        ]
    )
    data_row["meta_data"] = meta_data

    result = list(DirectKPIProcessor.process_row(data_row))

    assert result == [(1234, "some_kpi_rate", 1.5, 2, "Поиск")]
