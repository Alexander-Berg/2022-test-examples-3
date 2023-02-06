import os

import yatest.common
from yt.wrapper import ypath

from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.dmp.yandex.bin.upload_report_to_ftp.lib import uploader
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
    utils,
)


def test_basic(ftp_client, yt_stuff, config, frozen_time):
    daily_track_table = config[config_fields.UPLOADED_DAILY_REPORTS_TRACK_TABLE]
    monthly_track_table = config[config_fields.UPLOADED_MONTHLY_REPORTS_TRACK_TABLE]
    utils.create_yt_dirs(yt_stuff, [ypath.ypath_dirname(daily_track_table)])

    yt_output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/upload_report_to_ftp/bin/crypta-dmp-yandex-upload-report-to-ftp"),
        args=["--config", yaml_config.dump(config)],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (files.YtFile(filename, ypath.ypath_join(config[config_fields.DAILY_REPORTS_DIR], filename)), [tests.Exists()])
            for filename in ("2019-12-31.xlsx", "2020-01-01.xlsx", "2020-01-02.xlsx")
        ] + [
            (files.YtFile(filename, ypath.ypath_join(config[config_fields.MONTHLY_REPORTS_DIR], filename)), [tests.Exists()])
            for filename in ("2019-12.xlsx", "2019-11.xlsx")
        ],
        output_tables=[
            (tables.YsonTable("daily_track_table.yson", daily_track_table, yson_format="pretty"), [tests.Diff()]),
            (tables.YsonTable("monthly_track_table.yson", monthly_track_table, yson_format="pretty"), [tests.Diff()]),
        ],
        env={
            "YT_TOKEN": "FAKE_YT_TOKEN",
            "CRYPTA_DMP_FTP_PASSWORD": ftp_client.password,
            time_utils.CRYPTA_FROZEN_TIME_ENV: str(frozen_time),
        },
    )

    result = {os.path.basename(yt_output_file["file"]["uri"]): yt_output_file for yt_output_file in yt_output_files}
    result["daily"] = sorted(ftp_client.nlst(os.path.join(uploader.REPORTS_DIR, uploader.DailySettings.output_dir)))
    result["monthly"] = sorted(ftp_client.nlst(os.path.join(uploader.REPORTS_DIR, uploader.MonthlySettings.output_dir)))

    return result
