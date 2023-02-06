# -*- coding: utf-8 -*-

import fcntl
import os
import time

import pytest

from market.pylibrary.putil.protector import timeout, TimeoutError


def tmp_path(name):
    try:
        import yatest
        return yatest.common.test_output_path(name)
    except ImportError:
        return os.path.join('/tmp', name)


@pytest.yield_fixture()
def lock_path():
    path = tmp_path('protector-test-lock')
    try:
        open(path, 'w').close()
        yield path
    finally:
        try:
            os.remove(path)
        except:
            pass


def test_timeout_success():
    """When timeout doesn't happen, the function should run as normal.
    """
    @timeout(1)
    def foo():
        return 123

    assert 123 == foo()


def test_timeout_raise():
    """When the function wrapped by timeout raises,
    the wrapper should re-raise.
    """
    class BarError(Exception):
        pass

    @timeout(1)
    def foo():
        raise BarError()

    with pytest.raises(BarError):
        foo()


def test_timeout_fail():
    """When the function timeouts, the wrapper should raise the
    timeout exception.
    """
    @timeout(1)
    def foo():
        time.sleep(5)

    with pytest.raises(TimeoutError):
        foo()


def test_timeout_kill(lock_path):
    """Whatever child processes are created by the wrapped function
    must be killed when it timeouts.
    """
    @timeout(1)
    def foo():
        # the child process will hold an exclusive lock to lock_path for 5 seconds
        import subprocess
        child = subprocess.Popen(['/usr/bin/flock', '-x', '-n', lock_path, 'sleep', '5'])
        child.communicate()
        child.wait()

    with pytest.raises(TimeoutError):
        foo()

    # after 1 second timeout, we try to acquire an exclusive lock to lock_path.
    # if the child process is still alive at this point, IOError will be raised.
    lock_file = os.open(lock_path, os.O_RDWR)
    try:
        fcntl.lockf(lock_file, fcntl.LOCK_EX | fcntl.LOCK_NB)
    finally:
        os.close(lock_file)


def test_none_timeout():
    """
      If timeout is None, function work normaly
    """
    @timeout(None)
    def foo():
        return 239

    assert 239 == foo()
