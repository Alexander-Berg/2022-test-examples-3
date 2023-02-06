import datetime
import os

import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.s2s.lib import (
    schemas,
    test_helpers,
)
from crypta.s2s.services.transfer_conversions_to_yt.lib.default_csv_column_names import DefaultCsvColumnNames


def test_url_provider(yt_stuff, mock_file_server, custom_column_names, conversion_name_to_goal_ids, frozen_time):
    provider = {
        "type": "url_provider",
        "url": mock_file_server.get_file_url(),
    }
    config_file = test_helpers.render_config_file(yt_proxy=yt_stuff.get_server(), provider=provider, conversion_name_to_goal_ids=conversion_name_to_goal_ids, column_names=custom_column_names)
    config = test_helpers.read_config(config_file)

    output_name = str(frozen_time)

    return _run_test(
        yt_stuff=yt_stuff,
        config_file=config_file,
        input_tables=[],
        output_tables=[
            _fresh_with_tests(config, output_name),
            _errors_with_tests(config, output_name),
            (_backup_file(config, output_name), _backup_tests(config)),
        ],
    )


def test_yt_provider(yt_stuff, conversion_name_to_goal_ids, frozen_time):
    provider = {
        "type": "yt_provider",
        "source_dir": "//input",
    }
    config_file = test_helpers.render_config_file(yt_proxy=yt_stuff.get_server(), provider=provider, conversion_name_to_goal_ids=conversion_name_to_goal_ids)
    config = test_helpers.read_config(config_file)

    date = "2021-11-11"
    output_name = "{}_{}".format(date, frozen_time)

    return _run_test(
        yt_stuff=yt_stuff,
        config_file=config_file,
        input_tables=[
            (
                tables.YsonTable("conversions.yson", ypath.ypath_join(config.YtProvider.TrackedSource.SourceDir, date)),
                [tests.TableIsNotChanged()],
            )
        ],
        output_tables=[
            _fresh_with_tests(config, output_name),
            _errors_with_tests(config, output_name),
            (_backup_table(config, output_name), _backup_tests(config)),
            _track_with_tests(config.YtProvider.TrackedSource.TrackTable),
        ],
    )


def test_sftp_provider(yt_stuff, sftp_server, sftp_client, sftp_username, sftp_source_dir, sftp_files_basenames):
    column_names = {
        DefaultCsvColumnNames.yclid: "yclid",
        DefaultCsvColumnNames.conversion_time: "date",
    }
    static_goal_id = 1111

    provider = {
        "type": "sftp_provider",
        "sftp_config": {
            "host": sftp_server.host,
            "port": sftp_server.port,
            "username": sftp_username,
        },
        "source_dir": sftp_source_dir,
    }
    config_file = test_helpers.render_config_file(yt_proxy=yt_stuff.get_server(), provider=provider, static_goal_id=static_goal_id, column_names=column_names)
    config = test_helpers.read_config(config_file)

    return _run_test(
        yt_stuff=yt_stuff,
        config_file=config_file,
        input_tables=[],
        output_tables=[
            _fresh_with_tests(config, basename)
            for basename in sftp_files_basenames
        ] + [
            _errors_with_tests(config, basename)
            for basename in sftp_files_basenames
        ] + [
            (_backup_file(config, basename), _backup_tests(config))
            for basename in sftp_files_basenames
        ] + [_track_with_tests(config.SftpProvider.TrackTable)]
    )


def _fresh_with_tests(config, basename):
    table = tables.YsonTable("fresh_{}.yson".format(basename), ypath.ypath_join(config.FreshConversionsDir, basename), yson_format="pretty")
    all_tests = [
        tests.Diff(),
        tests.SchemaEquals(schemas.get_conversion_schema()),
    ]
    return (table, all_tests)


def _errors_with_tests(config, basename):
    table = tables.YsonTable("errors_{}.yson".format(basename), ypath.ypath_join(config.ParsingErrors.Dir, basename), yson_format="pretty")
    all_tests = [
        tests.Diff(),
        tests.SchemaEquals(schemas.get_raw_conversion_parsing_error_schema()),
        tests.ExpirationTime(ttl=datetime.timedelta(days=config.ParsingErrors.TtlDays)),
    ]
    return (table, all_tests)


def _backup_table(config, basename):
    return tables.YsonTable("backup_{}.yson".format(basename), ypath.ypath_join(config.RawBackup.Dir, basename), yson_format="pretty")


def _backup_file(config, basename):
    return files.YtFile("backup_{}.csv".format(basename), ypath.ypath_join(config.RawBackup.Dir, "{}.csv".format(basename)))


def _backup_tests(config):
    return [
        tests.Diff(),
        tests.ExpirationTime(ttl=datetime.timedelta(days=config.RawBackup.TtlDays)),
    ]


def _track_with_tests(cypress_path):
    table = tables.YsonTable("track_table.yson", cypress_path, yson_format="pretty")
    return (table, [tests.Diff()])


def _run_test(yt_stuff, config_file, input_tables, output_tables):
    output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/s2s/services/transfer_conversions_to_yt/bin/crypta-s2s-transfer-conversions-to-yt"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=input_tables,
        output_tables=output_tables,
    )

    return {os.path.basename(output_file["file"]["uri"]): output_file for output_file in output_files}
