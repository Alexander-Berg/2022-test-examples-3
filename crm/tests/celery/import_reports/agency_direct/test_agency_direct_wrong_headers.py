import pytest


@pytest.fixture
def fixture_wrong_headers(fixture_celery_agency_direct_common_headers, fixture_celery_agency_direct_headers):
    return [
        fixture_celery_agency_direct_common_headers,
        fixture_celery_agency_direct_headers[:10]
    ]


async def test_validate_headers(
    agency_direct_report_importer,
    fixture_celery_report_import,
    fixture_celery_client_rows,
    fixture_wrong_headers,
):
    assert agency_direct_report_importer._validate_headers(fixture_wrong_headers) is False
