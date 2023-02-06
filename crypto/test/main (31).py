import calendar
import datetime
import os

import pytest
import pytz
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common import report_generator
from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import time_utils
from crypta.lib.python.smtp.test_helpers import mail_canonizers
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)


DAILY_REPORTS_DIR = "//dmp-xxx/reports/daily"
DAILY_REPORTS_TRACK_TABLE = "//dmp-xxx/daily_track_table"
MONTHLY_REPORTS_DIR = "//dmp-xxx/reports/monthly"
MONTHLY_REPORTS_TRACK_TABLE = "//dmp-xxx/monthly_track_table"

EMPTY_REPORTS = ["2020-01-02.xlsx"]


def get_frozen_time():
    dt = datetime.datetime(year=2020, month=1, day=7, hour=0, minute=1, second=1, tzinfo=pytz.timezone("Europe/Moscow"))
    return int(calendar.timegm(dt.astimezone(pytz.timezone("UTC")).timetuple()))


def get_input_report(directory, filename):
    cypress_path = ypath.ypath_join(directory, filename)
    on_write = files.OnWrite(attributes={
        report_generator.EMPTY_REPORT_ATTRIBUTE: filename in EMPTY_REPORTS,
    })
    return files.YtFile(filename, cypress_path, on_write=on_write)


@pytest.fixture
def config(yt_stuff, local_smtp_server):
    smtp_host, smtp_port = local_smtp_server.local_address
    return {
        config_fields.DAILY_REPORTS_DIR: DAILY_REPORTS_DIR,
        config_fields.DMP_LOGIN: "dmp-xxx",
        config_fields.MONTHLY_REPORTS_DIR: MONTHLY_REPORTS_DIR,
        config_fields.OLDEST_DAILY_REPORT_TO_SEND_DAYS: 7,
        config_fields.OLDEST_MONTHLY_REPORT_TO_SEND_MONTHS: 1,
        config_fields.REPORT_EMAILS_BCC: ["crypta-dmp@yandex-team.ru"],
        config_fields.REPORT_EMAILS_CC: ["data-partners@yandex-team.ru"],
        config_fields.SEND_REPORT_EMAILS: True,
        config_fields.SENT_DAILY_REPORTS_TRACK_TABLE: DAILY_REPORTS_TRACK_TABLE,
        config_fields.SENT_MONTHLY_REPORTS_TRACK_TABLE: MONTHLY_REPORTS_TRACK_TABLE,
        config_fields.SMTP_EMAIL_FROM: "data-partners@yandex-team.ru",
        config_fields.SMTP_HOST: smtp_host,
        config_fields.SMTP_PORT: smtp_port,
        config_fields.STATISTICS_EMAILS: ["xxx@local.local", "zzz@local.local"],
        config_fields.YT_POOL: "pool",
        config_fields.YT_PROXY: yt_stuff.get_server(),
    }


def test_report_mail(yt_stuff, local_smtp_server, config):
    with local_smtp_server:
        yt_output_files = tests.yt_test(
            yt_client=yt_stuff.get_yt_client(),
            binary=yatest.common.binary_path("crypta/dmp/yandex/bin/send_report_mail/bin/crypta-dmp-yandex-send-report-mail"),
            args=["--config", yaml_config.dump(config)],
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (get_input_report(DAILY_REPORTS_DIR, filename), [tests.Exists()])
                for filename in ("2019-12-31.xlsx", "2020-01-01.xlsx", "2020-01-02.xlsx")
            ] + [
                (get_input_report(MONTHLY_REPORTS_DIR, filename), [tests.Exists()])
                for filename in ("2019-12.xlsx", "2019-11.xlsx")
            ],
            output_tables=[
                (tables.YsonTable("daily_track_table.yson", DAILY_REPORTS_TRACK_TABLE, yson_format="pretty"), [tests.Diff()]),
                (tables.YsonTable("monthly_track_table.yson", MONTHLY_REPORTS_TRACK_TABLE, yson_format="pretty"), [tests.Diff()]),
            ],
            env={
                "YT_TOKEN": "FAKE_YT_TOKEN",
                time_utils.CRYPTA_FROZEN_TIME_ENV: str(get_frozen_time()),
            },
        )

    output_files = ["mail_0.txt", "mail_1.txt", "mail_2.txt"]
    assert not os.path.exists("mail_3.txt")

    result = mail_canonizers.canonize_mails_by_subject(output_files)

    for yt_output_file in yt_output_files:
        result[os.path.basename(yt_output_file["file"]["uri"])] = yt_output_file

    return result
