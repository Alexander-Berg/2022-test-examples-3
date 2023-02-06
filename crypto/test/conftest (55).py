import calendar
import datetime

import pytest
import pytz

from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python.ftp.client.ftp_client import FtpClient
from crypta.lib.python.ftp.testing_server import FtpTestingServer


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture
def frozen_time():
    dt = datetime.datetime(year=2020, month=1, day=7, hour=0, minute=1, second=1, tzinfo=pytz.timezone("Europe/Moscow"))
    return int(calendar.timegm(dt.astimezone(pytz.timezone("UTC")).timetuple()))


@pytest.fixture
def ftp_server():
    ftp = FtpTestingServer([("FTP_USER", "FTP_PASSWORD")])
    try:
        ftp.start()
        yield ftp
    finally:
        ftp.stop()


@pytest.fixture
def config(ftp_server, yt_stuff):
    return {
        config_fields.FTP_HOST: "localhost",
        config_fields.FTP_PORT: ftp_server.port,
        config_fields.FTP_USER: ftp_server.auths[0][0],
        config_fields.FTP_DIR: "/",
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "FAKE_POOL",
        config_fields.UPLOAD_REPORTS_TO_FTP: True,
        config_fields.DAILY_REPORTS_DIR: "//reports/daily",
        config_fields.MONTHLY_REPORTS_DIR: "//reports/monthly",
        config_fields.UPLOADED_DAILY_REPORTS_TRACK_TABLE: "//dmp/uploaded_daily_reports",
        config_fields.UPLOADED_MONTHLY_REPORTS_TRACK_TABLE: "//dmp/uploaded_monthly_reports",
        config_fields.OLDEST_DAILY_REPORT_TO_UPLOAD_DAYS: 7,
        config_fields.OLDEST_MONTHLY_REPORT_TO_UPLOAD_MONTHS: 1,
    }


@pytest.fixture
def ftp_client(ftp_server, config):
    user, password = ftp_server.auths[0]
    return FtpClient(
        host=config[config_fields.FTP_HOST],
        port=config[config_fields.FTP_PORT],
        user=user,
        password=password,
    )
