# -*- coding: utf-8 -*-

import datetime
import os

import pytest

import reanimator
import test_common


def assertOK(capture):
    assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-premature-exit;0;Ok\n'
    assert capture.get_stderr() == ''


def test_report_is_alive():
    with test_common.OutputCapture() as capture:
        reanimator.main()
        assertOK(capture)


def test_report_has_just_crashed(monitoring_dir):
    crash_time = datetime.datetime.now()

    with open(os.path.join(monitoring_dir, reanimator.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime("%Y-%m-%dT%H:%M:%S"))

    with test_common.OutputCapture() as capture:
        reanimator.main()
        assert capture.get_stdout() == 'PASSIVE-CHECK:market-report-premature-exit;2;Premature exit of Report detected\n'
        assert capture.get_stderr() == ''


def test_report_crashed_before(monitoring_dir):
    crash_time = datetime.datetime.now() - reanimator.MAX_TIME_DIFF

    with open(os.path.join(monitoring_dir, reanimator.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime("%Y-%m-%dT%H:%M:%S"))

    with test_common.OutputCapture() as capture:
        reanimator.main()
        assertOK(capture)


@pytest.mark.usefixtures('indigo_cluster')
def test_ignore_indigo_clusters(monitoring_dir):
    crash_time = datetime.datetime.now()

    with open(os.path.join(monitoring_dir, reanimator.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime("%Y-%m-%dT%H:%M:%S"))

    with test_common.OutputCapture() as capture:
        reanimator.main()
        assertOK(capture)


def test_skip_old_event_after_removing_indigo(monitoring_dir):
    crash_time = datetime.datetime.now()

    with open(os.path.join(monitoring_dir, reanimator.LAST_CRASH_INFO_PATH), 'w') as f:
        f.write(crash_time.strftime("%Y-%m-%dT%H:%M:%S"))

    with test_common.colorize_cluster_in_indigo():
        with test_common.OutputCapture() as capture:
            reanimator.main()
            assertOK(capture)

    with test_common.OutputCapture() as capture:
        reanimator.main()
        assertOK(capture)
