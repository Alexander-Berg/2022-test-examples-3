# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from textwrap import dedent

import pytest

import common.utils.subprocess as utils_subprocess
from common.utils.subprocess import ProcessResult, run_process_with_timeout, process_job, ProcessError


@pytest.fixture(autouse=True, scope='module')
def set_timeouts():
    old_sleep_interval = utils_subprocess.SLEEP_INTERVAL
    old_terminating_timeout = utils_subprocess.TERMINATING_TIMEOUT
    old_before_kill_timeout = utils_subprocess.BEFORE_KILL_TIMEOUT

    utils_subprocess.SLEEP_INTERVAL = 0.01
    utils_subprocess.TERMINATING_TIMEOUT = 0.1
    utils_subprocess.BEFORE_KILL_TIMEOUT = 0.1

    yield

    utils_subprocess.SLEEP_INTERVAL = old_sleep_interval
    utils_subprocess.TERMINATING_TIMEOUT = old_terminating_timeout
    utils_subprocess.BEFORE_KILL_TIMEOUT = old_before_kill_timeout


class TestRunProcessWithTimeout(object):
    def test_exit_success(self):
        result = run_process_with_timeout('true')
        assert result.is_success()
        assert result.result_type == ProcessResult.FINISHED
        assert result.status_code == 0

    def test_exit_success_timeout(self):
        result = run_process_with_timeout('sleep', '0.1')
        assert result.is_success()
        assert result.result_type == ProcessResult.FINISHED
        assert result.status_code == 0

    def test_exit_fail(self):
        result = run_process_with_timeout('false')
        assert not result.is_success()
        assert result.result_type == ProcessResult.FINISHED
        assert result.status_code != 0

    def test_exit_timeout(self):
        result = run_process_with_timeout('sleep', '1', timeout=0.1)
        assert not result.is_success()
        assert result.result_type == ProcessResult.KILLED
        assert result.status_code is None
        assert result.reason == 'Timed out killing with SIGTERM. Killed with SIGTERM'

    def test_external_kill(self, tmpdir):
        script = tmpdir.join('script.py')
        script.write(dedent('''
        import time, os, signal
        os.kill(os.getpid(), signal.SIGABRT)
        time.sleep(10)
        '''))
        result = run_process_with_timeout('python', str(script), timeout=5)
        assert not result.is_success()
        assert result.result_type == ProcessResult.KILLED
        assert result.status_code is None
        assert result.reason == 'Killed with SIGABRT'

    def test_catching_sigterm(self, tmpdir):
        script = tmpdir.join('script.py')
        script.write(dedent('''
        import time, signal
        def handler(*args, **kwargs):
            time.sleep(10)
        signal.signal(signal.SIGTERM, handler)
        time.sleep(10)
        '''))
        result = run_process_with_timeout('python', str(script), timeout=0.1)
        assert not result.is_success()
        assert result.result_type == ProcessResult.KILLED
        assert result.status_code is None
        assert result.reason == 'Timed out, didnt die from SIGTERM, killing with SIGKILL. Killed with SIGKILL'

    def test_catching_sigterm_exit_normal(self, tmpdir):
        script = tmpdir.join('script.py')
        script.write(dedent('''
        import time, signal, sys
        def handler(*args, **kwargs):
            sys.exit(0)
        signal.signal(signal.SIGTERM, handler)
        time.sleep(10)
        '''))
        result = run_process_with_timeout('python', str(script), timeout=0.1)
        assert not result.is_success()
        assert result.result_type == ProcessResult.FINISHED
        assert result.status_code == 0
        assert result.reason == 'Timed out killing with SIGTERM. Finished with 0'


class TestProcessJob(object):
    def test_exit_success(self):
        process_job('true')

    def test_exit_fail(self):
        with pytest.raises(ProcessError):
            process_job('false')
