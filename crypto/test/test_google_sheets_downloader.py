import logging

from grut.libs.proto.objects import conversion_source_pb2
import mock
import yatest.common

from crypta.s2s.services.conversions_downloader.lib import google_sheets_downloader


def test_google_sheets_downloader():
    destination_path = yatest.common.test_output_path("output.csv")
    settings = conversion_source_pb2.TConversionSourceSpec.TGoogleSheetsSettings(url="xxx")

    with mock.patch("gspread.service_account_from_dict", mock_service_account_from_dict):
        downloader = google_sheets_downloader.GoogleSheetsDownloader(settings, "__GOOGLE_API_KEY__", logging.getLogger(__name__))
        downloader.download(destination_path)

    return yatest.common.canonical_file(destination_path, local=True)


class MockWorksheet:
    def get_all_records(self):
        return [{
            "id": 1,
            "client_uniq_id": "client-1",
            "client_ids": 1,
            "emails": "local@local.local",
            "emails_md5": "==xxx==",
        }, {
            "id": 2,
            "phones": "+79998887766",
            "phones_md5": "===xxx===",
            "create_date_time": "21.04.20 11:59",
            "order_status": "PAID",
            "revenue": 22,
        }, {
            "id": 3,
            "cost": 33,
        }]


class MockSpreadsheet:
    def get_worksheet(self, n):
        return MockWorksheet()


class MockClient:
    def open_by_url(self, url):
        return MockSpreadsheet()


def mock_service_account_from_dict(d, scopes):
    return MockClient()
