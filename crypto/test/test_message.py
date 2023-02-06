import os

import pytest

from crypta.dmp.yandex.bin.send_metrics_mail.lib import message
from crypta.lib.python import time_utils


CRYPTA_FROZEN_TIME = 1545973200  # 2018-12-28 08:00:00 MSK
COVERAGE_DAYS = [0, 1]
METRICS = {
    "segments_count.enabled": 111,
    "coverage.total.ext_id": 1000,
    "coverage.total.yuid": 400,
    "matching_rate.total.user": 0.4,
    "matching_rate.1d.user": 0.2,
    "small_segments": [1, 2, 3],
}
METRICS_WITHOUT_SOME_COVERAGE = {
    "segments_count.enabled": 111,
    "coverage.total.ext_id": 1000,
    "coverage.total.yuid": 400,
    "matching_rate.total.user": 0.4,
    "small_segments": [1],
}
METRICS_WITHOUT_COVERAGE = {
    "segments_count.enabled": 111,
    "coverage.total.ext_id": 0,
    "coverage.total.yuid": 0,
    "small_segments": [],
}


def setup_function():
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = str(CRYPTA_FROZEN_TIME)


@pytest.mark.parametrize("metrics", [
    METRICS, METRICS_WITHOUT_SOME_COVERAGE, METRICS_WITHOUT_COVERAGE
], ids=["FullMetrics", "MetricsWithoutSomeCoverage", "MetricsWithoutCoverage"])
def test_get_mail_content(metrics):
    return message.get_stats_mail_content("dmp-xxx", metrics, COVERAGE_DAYS)
