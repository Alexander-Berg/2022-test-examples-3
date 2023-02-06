import pytest

from hamcrest import assert_that, has_entries


class TestReportImport:
    @pytest.fixture(autouse=True)
    def setup(self, setup_report_data):
        pass

    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/import/{org_id}/report/')

    @pytest.fixture
    async def response_body(self, response):
        return await response.text()

    def test_response_status(self, response):
        assert response.status == 200

    def test_response_body(self, response_body, expected_report_response):
        assert response_body == expected_report_response

    def test_response_headers(self, response):
        assert_that(
            response.headers,
            has_entries({
                'Content-Type': 'text/csv; charset=utf-8',
                'Content-Disposition': 'attachment; filename="report.csv"',
            })
        )
