# -*- coding: utf-8 -*-

import os
import subprocess
import time
import unittest

from datetime import datetime

import context
from market.pylibrary.mi_util.util import check_call_pipefail
from market.pylibrary.mi_util.util import to_iso
from market.pylibrary.putil.protector import TimeoutError


BUILDROOT = os.environ.get('ARCADIA_BUILD_ROOT')

READ_WRITE_BIN = os.path.join(os.path.dirname(__file__), "read_write_bin/__main__.py")
if BUILDROOT:  # we are in the Arcadia!
    READ_WRITE_BIN = os.path.join(BUILDROOT, "market/pylibrary/mi_util/tests/read_write_bin/read_write_bin")


class TestDateTime(unittest.TestCase):
    def test_to_iso(self):
        dt = datetime.utcfromtimestamp(1222333444.555666)
        self.assertEqual('2008-09-25T09:04:04', to_iso(dt))


class TestCheckCallPipeFail(unittest.TestCase):
    def test_simple_run(self):
        check_call_pipefail(["true", "true"])

    def test_simple_run_3(self):
        check_call_pipefail(["true", "true", "true"])

    def test_simple_fail(self):
        with self.assertRaises(subprocess.CalledProcessError):
            check_call_pipefail(["false", "false"])

    def test_simple_first_fail(self):
        with self.assertRaises(subprocess.CalledProcessError):
            check_call_pipefail(["false", "true"])

    def test_simple_last_fail(self):
        with self.assertRaises(subprocess.CalledProcessError):
            check_call_pipefail(["true", "false"])

    def test_simple_middle_fail(self):
        with self.assertRaises(subprocess.CalledProcessError):
            check_call_pipefail(["true", "false", "true"])

    def test_simple_timeout_fail(self):
        with self.assertRaises(TimeoutError):
            check_call_pipefail([["sleep", "2"], "true"], timeout=1)

    def test_simple_timeout_fail_retry(self):
        with self.assertRaises(TimeoutError):
            start = time.time()
            check_call_pipefail([["sleep", "2"], "true"], timeout=1, retry_count=3)
            stop = time.time()
            self.assertLess(2, stop - start)

    def test_simple_fail_retry(self):
        with self.assertRaises(subprocess.CalledProcessError):
            check_call_pipefail(["false", "true"], timeout=1, retry_count=2)

    def test_simple_timeout_fail_retry_sleep(self):
        with self.assertRaises(TimeoutError):
            check_call_pipefail([["sleep", "2"], "true"], timeout=1, retry_count=2, sleep_between_retries=1)

    def test_timeout_fail_retry_with_sigterm_ignored(self):
        with self.assertRaises(TimeoutError):
            check_call_pipefail(["true", [READ_WRITE_BIN, "ignore_signals"]], timeout=1, retry_count=2,
                                sleep_between_retries=1)

    def test_simple_fail_retry_sleep(self):
        with self.assertRaises(subprocess.CalledProcessError):
            check_call_pipefail(["false", "true"], timeout=1, retry_count=2, sleep_between_retries=1)

    def test_pipefail_closefd(self):
        with self.assertRaises(subprocess.CalledProcessError):
            check_call_pipefail([[READ_WRITE_BIN, "write"], [READ_WRITE_BIN, "read"]], timeout=5)
            # first command write to pipe many many bytes, but second reads only 4 bytes, so "Broken pipe" will happen
            # and we expect CalledProcessError exception here

    def test_simple_timeout_fail_retry_sleep__str_params(self):
        with self.assertRaises(TimeoutError):
            check_call_pipefail([["sleep", "2"], "true"], timeout="1", retry_count="2", sleep_between_retries="1")


if __name__ == '__main__':
    context.main()
