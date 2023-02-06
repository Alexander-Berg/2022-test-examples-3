import datetime
import os
import time

import exts.tmp
import pytest
import yatest.common

FRESH_HPROF_FILES_BIN = yatest.common.binary_path(
    "market/sre/juggler/bundles/checks/fresh-hprof-files/fresh-hprof-files"
)


@pytest.fixture(scope="module")
def temp_dir():
    with exts.tmp.temp_dir() as temp_dir:
        yield temp_dir


@pytest.fixture(scope="module")
def prepare_logs(temp_dir):
    log_dir_with_hprof = os.path.join(temp_dir, "logs/with-hprof")
    log_dir_without_hprof = os.path.join(temp_dir, "logs/without-hprof")

    app_log_dir_with_hprof = os.path.join(log_dir_with_hprof, "app")
    app_log_dir_without_hprof = os.path.join(log_dir_without_hprof, "app")

    log_dirs = [app_log_dir_with_hprof, app_log_dir_without_hprof]
    hprof_dirs = [os.path.join(app_log_dir_with_hprof, "hprof")]

    now = datetime.datetime.now()
    second_before = time.mktime((now - datetime.timedelta(seconds=1)).timetuple())
    hour_before = time.mktime((now - datetime.timedelta(hours=1)).timetuple())
    day_before = time.mktime((now - datetime.timedelta(hours=24)).timetuple())

    log_files = (("test1.log", second_before), ("test2.log", hour_before), ("test2.log", day_before))
    hprof_files = (("test1.hprof", second_before), ("test2.hprof", hour_before), ("test3.hprof", day_before))

    create_files_in_dirs(log_dirs, log_files)
    create_files_in_dirs(hprof_dirs, hprof_files)

    return log_dir_with_hprof, log_dir_without_hprof


def create_files_in_dirs(dirs, files):
    for d in dirs:
        if not os.path.exists(d):
            os.makedirs(d)

        for f, m_time in files:
            p = os.path.join(d, f)

            with open(p, "w") as fp:
                fp.write("")

            os.utime(p, (m_time, m_time))


def test_no_hprof_ok(prepare_logs):
    _, log_dir_without_hprof = prepare_logs
    res = yatest.common.execute([FRESH_HPROF_FILES_BIN, "-log-dir", log_dir_without_hprof])

    assert "0;OK" in res.std_out
    assert res.std_err == ""


def test_aged_hprof_ok(prepare_logs):
    log_dir_wit_hprof, _ = prepare_logs
    res = yatest.common.execute([FRESH_HPROF_FILES_BIN, "-log-dir", log_dir_wit_hprof, "-max-age", "1s"])

    assert "0;OK" in res.std_out
    assert res.std_err == ""


def test_no_log_dir_warn():
    res = yatest.common.execute([FRESH_HPROF_FILES_BIN, "-log-dir", "log/dir/not/exists"])

    assert "1;log directory not found" in res.std_out and "no such file or directory" in res.std_out
    assert res.std_err == ""


def test_fresh_hprof_crit(prepare_logs):
    log_dir_wit_hprof, _ = prepare_logs
    res = yatest.common.execute([FRESH_HPROF_FILES_BIN, "-log-dir", log_dir_wit_hprof])

    assert res.std_out.endswith("2;Found fresh (younger than 24h0m0s) hprof files: test1.hprof, test2.hprof\n")
    assert res.std_err == ""
