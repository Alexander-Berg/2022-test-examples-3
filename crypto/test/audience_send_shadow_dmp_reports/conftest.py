import calendar
import datetime

import pytest
import pytz
import yatest.common

from crypta.lib.python import templater


pytest_plugins = [
    "crypta.lib.python.smtp.test_helpers.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture
def frozen_time():
    dt = datetime.datetime(year=2020, month=1, day=4, hour=0, minute=1, second=1, tzinfo=pytz.timezone("Europe/Moscow"))
    return int(calendar.timegm(dt.astimezone(pytz.timezone("UTC")).timetuple()))


@pytest.fixture(scope="function")
def config_file(yt_stuff, local_smtp_server):
    config_file_path = yatest.common.test_output_path("config.yaml")

    smtp_host, smtp_port = local_smtp_server.local_address

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_send_shadow_dmp_reports/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "smtp_host": smtp_host,
            "smtp_port": smtp_port,
            "oldest_daily_report_to_send_days": 4,
            "oldest_monthly_report_to_send_months": 2,
        },
    )
    return config_file_path
