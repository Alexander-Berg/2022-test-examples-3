import datetime
import logging
import os

import mock
from yt.wrapper import ypath

from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.s2s.lib import schemas
from crypta.s2s.lib.proto.config_pb2 import TConfig
from crypta.s2s.services.transfer_conversions_to_yt.lib import parsers
from crypta.s2s.services.transfer_conversions_to_yt.lib.default_csv_column_names import DefaultCsvColumnNames
from crypta.s2s.services.transfer_conversions_to_yt.lib.google_sheets_provider_runner import GoogleSheetsProviderRunner


def test_google_sheets_provider_runner(yt_stuff, frozen_time):
    provider = TConfig.TGoogleSheetsProvider(Key='{"x": 1}', Url="__URL__")
    fresh_dir = "//xxx/fresh"
    backup_config = TConfig.TDirWithTtl(Dir="//xxx/backup", TtlDays=30)
    errors_config = TConfig.TDirWithTtl(Dir="//xxx/errors", TtlDays=7)
    parser = parsers.ConversionParser(timestamp_range=(1628000000, 1629000000), static_goal_id=1111)
    logger = logging.getLogger()

    diff_test = tests.Diff()

    yt_client = yt_stuff.get_yt_client()
    yt_client.create("map_node", fresh_dir, recursive=True)
    yt_client.create("map_node", backup_config.Dir, recursive=True)
    yt_client.create("map_node", errors_config.Dir, recursive=True)

    with mock.patch("gspread.service_account_from_dict", mock_service_account_from_dict):
        runner = GoogleSheetsProviderRunner(
            yt_client=yt_client,
            provider=provider,
            fresh_dir=fresh_dir,
            backup_config=backup_config,
            errors_config=errors_config,
            parser=parser,
            logger=logger,
        )
        output_files = tests.yt_test_func(
            yt_client=yt_client,
            func=runner.run,
            output_tables=[(
                tables.YsonTable("fresh.yson", ypath.ypath_join(fresh_dir, frozen_time), yson_format="pretty"),
                [
                    diff_test,
                    tests.SchemaEquals(schemas.get_conversion_schema()),
                ],
            ), (
                files.YtFile("backup.json", ypath.ypath_join(backup_config.Dir, "{}.json".format(frozen_time))),
                [
                    diff_test,
                    tests.ExpirationTime(ttl=datetime.timedelta(days=backup_config.TtlDays)),
                ],
            ), (
                tables.YsonTable("errors.yson", ypath.ypath_join(errors_config.Dir, frozen_time), yson_format="pretty"),
                [
                    diff_test,
                    tests.SchemaEquals(schemas.get_raw_conversion_parsing_error_schema()),
                    tests.ExpirationTime(ttl=datetime.timedelta(days=errors_config.TtlDays)),
                ],
            )]
        )
        return {os.path.basename(output_file["file"]["uri"]): output_file for output_file in output_files}


class MockWorksheet:
    def get_all_records(self):
        return [{
            DefaultCsvColumnNames.yclid: 111,
            DefaultCsvColumnNames.conversion_time: "Aug 7, 2021 5:01:54 PM",
        }, {
            DefaultCsvColumnNames.yclid: 222,
            DefaultCsvColumnNames.conversion_time: "Aug 14, 2021 5:01:54 PM",
        }, {
            DefaultCsvColumnNames.yclid: 333,
        }]


class MockSpreadsheet:
    def get_worksheet(self, n):
        return MockWorksheet()


class MockClient:
    def open_by_url(self, url):
        return MockSpreadsheet()


def mock_service_account_from_dict(d, scopes):
    return MockClient()
