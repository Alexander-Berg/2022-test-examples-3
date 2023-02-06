# coding: utf-8

import datetime
import logging
import os
import time

import freezegun
import mock
import pytest

from config import config as cfg
from container import Container
from core import Core
from core_watcher import CoreWatcher
import util


COREDUMP = 'ISS-AGENT--17050%17050_test%iss%report.782010.S11.20180420T153848.core'


@pytest.fixture(scope='function')
def mocked_file(mocker):
    return mocker.patch.object(util.File, 'mtime', new_callable=mock.PropertyMock, return_value=0)


@pytest.fixture(scope='function')
def mocked_container_root(mocker):
    return mocker.patch.object(Container, 'root', new_callable=mock.PropertyMock, return_value='mocked_container_root')


@pytest.fixture(scope='function')
def mocked_container_env(mocker):
    return mocker.patch.object(Container, 'env', new_callable=mock.PropertyMock, return_value='')


@pytest.fixture(scope='function')
def mocked_core_run_debugger(mocker):
    return mocker.patch.object(Core, '_run_debugger', return_value=None)


@pytest.fixture(scope='function')
def mocked_core_actual_size(mocker):
    return mocker.patch.object(Core, 'actual_size', new_callable=mock.PropertyMock, return_value=0)


@pytest.fixture(scope='function')
def mocked_core_expected_size(mocker):
    return mocker.patch.object(Core, 'expected_size', new_callable=mock.PropertyMock, return_value=0)


@pytest.fixture(scope='function')
def mocked_corewatcher_process_core(mocker):
    return mocker.patch.object(CoreWatcher, 'process_core')


@pytest.fixture(scope='function')
def mocked_corewatcher_save_state(mocker):
    return mocker.patch.object(CoreWatcher, '_save_state')


@pytest.fixture(scope='function')
def config_paths(tmpdir):
    root_path = str(tmpdir)
    cfg.update(
        core_dumps_path=os.path.join(root_path, 'coredumps'),
        output_path=os.path.join(root_path, 'output')
    )
    os.makedirs(cfg.core_dumps_path, exist_ok=True)
    os.makedirs(cfg.output_path, exist_ok=True)


def make_core_name(file_info):
    templ = file_info[0]
    mtime = file_info[1]
    dt = datetime.datetime.fromtimestamp(mtime)
    return templ.format(dt.strftime('%Y%m%dT%H%M%S'))


def make_cores(files):
    core_paths = []
    for info in files:
        # info: templ, mtime, contetn
        mtime = info[1]
        content = info[2] if len(info) == 3 else ''
        name = make_core_name(info)
        path = os.path.join(cfg.core_dumps_path, name)
        with open(path, 'w') as f:
            f.write(content)
        os.utime(path, (mtime, mtime))
        core_paths.append(path)
    return core_paths


def make_bin(path):
    bin_path = os.path.join(str(path), 'bin', 'report')
    bin_dir = os.path.dirname(bin_path)
    if not os.path.exists(bin_dir):
        os.makedirs(bin_dir)
    with open(bin_path, 'w') as f:
        f.write('')


def sort_processed_cores(cores):
    return [
        item[0]
        for item in sorted(cores.items(), key=lambda x: x[1])
    ]


def test_find_recent_cores(config_paths):
    now = int(time.time())
    day_ago = now - cfg.core_max_age
    cores = [
        ('ISS-AGENT--17050%17050_test%iss%report.78.S11.20180420T153841.core', day_ago),  # too old
        ('JSS-AGENT--17050%17050_test%iss%report.78.S11.20180420T153841.core', now),  # wrong prefix
    ]
    make_cores(cores)
    watcher = CoreWatcher()
    assert len(watcher._processed_cores) == 0
    cores = watcher.find_cores()
    assert len(cores) == 0
    assert len(watcher._processed_cores) == 0


def test_process_cores(config_paths, mocked_corewatcher_process_core):
    """Check main scenario of processing of cores"""
    now = int(time.time())
    cores = [
        ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', now - 1),
        ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', now),
        ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', now + 1)
    ]
    core_paths = make_cores(cores)
    watcher = CoreWatcher()

    watcher.process_cores()
    assert len(watcher._processed_cores) == 3
    # check process order
    for index, core_path in enumerate(sort_processed_cores(watcher._processed_cores)):
        assert core_path == core_paths[index]
    assert mocked_corewatcher_process_core.call_count == 3
    mocked_corewatcher_process_core.reset_mock()

    watcher.process_cores()
    assert len(watcher._processed_cores) == 3
    assert mocked_corewatcher_process_core.call_count == 0
    mocked_corewatcher_process_core.reset_mock()

    # add one more core
    new_core = ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', now + 3)
    core_paths = make_cores([new_core])
    watcher.process_cores()
    assert len(watcher._processed_cores) == 4
    processed_core = sort_processed_cores(watcher._processed_cores)[-1]
    assert processed_core == core_paths[0]
    assert mocked_corewatcher_process_core.call_count == 1


def test_process_cores_with_same_ts(config_paths, mocked_corewatcher_process_core):
    now = int(time.time())
    cores = [
        ('ISS-AGENT--17050%17050_test%iss%mini-tank.78.S11.{}.core', now + 1),
        ('ISS-AGENT--17050%17050_test%iss%reanimator.78.S11.{}.core', now + 1)
    ]
    make_cores(cores)
    watcher = CoreWatcher()

    watcher.process_cores()
    assert len(watcher._processed_cores) == 2
    assert mocked_corewatcher_process_core.call_count == 2
    mocked_corewatcher_process_core.reset_mock()

    watcher.process_cores()
    assert len(watcher._processed_cores) == 2
    assert mocked_corewatcher_process_core.call_count == 0


def test_process_core_with_unexpected_name(config_paths, mocked_corewatcher_process_core):
    now = int(time.time())
    cores = [
        ('ISS-AGENT--17050%17050_test%iss%report.XXXX0420T153848.core', now),  # wrong name
        ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', now + 1)
    ]
    make_cores(cores)
    watcher = CoreWatcher()

    watcher.process_cores()
    assert len(watcher._processed_cores) == 2
    mocked_corewatcher_process_core.assert_called_once()
    mocked_corewatcher_process_core.reset_mock()

    watcher.process_cores()
    assert len(watcher._processed_cores) == 2
    assert mocked_corewatcher_process_core.call_count == 0


def test_process_truncated_core(
    tmpdir,
    config_paths,
    mocked_container_root,
    mocked_container_env,
    mocked_core_expected_size,
    mocked_core_run_debugger,
    mocked_corewatcher_save_state
):
    make_bin(tmpdir)
    mocked_container_root.return_value = str(tmpdir)
    mocked_container_env.return_value = 'env'
    mocked_core_expected_size.return_value = 2
    watcher = CoreWatcher()

    core_paths = None
    now = datetime.datetime.utcnow()
    freezed_dt = now - datetime.timedelta(seconds=cfg.core_waiting_timeout + 1)
    with freezegun.freeze_time(freezed_dt, tz_offset=3):
        freezed_now = int(time.time())
        cores = [
            ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', freezed_now, 'A'),  # truncated
            ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', freezed_now + 1, 'AA')  # full dumped
        ]
        core_paths = make_cores(cores)
        watcher.process_cores()
        assert len(watcher._processed_cores) == 1
        processed_core = list(watcher._processed_cores.keys())[0]
        # full dumped core processed first
        assert processed_core == core_paths[1]
        assert mocked_core_run_debugger.call_count == 1
        mocked_core_run_debugger.reset_mock()

    watcher.process_cores()
    assert len(watcher._processed_cores) == 2
    processed_core = sort_processed_cores(watcher._processed_cores)[0]  # sort by mtime
    # truncated core processed after timeout
    assert processed_core == core_paths[0]
    assert mocked_core_run_debugger.call_count == 1


def test_clear_corewatcher_state(config_paths, mocked_corewatcher_process_core):
    now = datetime.datetime.utcnow()
    freezed_dt = now - datetime.timedelta(seconds=cfg.core_max_age * 2)
    with freezegun.freeze_time(freezed_dt, tz_offset=3):
        freezed_now = int(time.time())
        cores = [
            ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', freezed_now),
            ('ISS-AGENT--17050%17050_test%iss%report.78.S11.{}.core', freezed_now + 1)
        ]
        make_cores(cores)
        watcher = CoreWatcher()
        watcher.process_cores()
        assert len(watcher._processed_cores) == 2

    # Clear state here after process cores
    watcher = CoreWatcher()
    watcher.process_cores()
    assert len(watcher._processed_cores) == 2
    # Check state
    watcher = CoreWatcher()
    watcher.process_cores()
    assert len(watcher._processed_cores) == 1


def test_skip_core(
    caplog,
    tmpdir,
    mocked_file,
    mocked_container_root,
    mocked_container_env,
    mocked_core_actual_size,
    mocked_core_expected_size,
    mocked_core_run_debugger,
    mocked_corewatcher_save_state
):
    core = Core(COREDUMP)
    watcher = CoreWatcher()
    with caplog.at_level(logging.DEBUG):
        # binary not exists
        mocked_file.return_value = 1
        watcher.process_core(core)
        assert 'Skip core for binary outside' in caplog.text

        # binary not exists, container env is empty
        mocked_container_root.return_value = str(tmpdir)
        make_bin(tmpdir)
        core = Core(COREDUMP)
        watcher.process_core(core)
        assert 'Failed to load container info' in caplog.text

        # stack from core failed
        mocked_container_env.return_value = 'env'
        watcher.process_core(core)
        assert 'Failed to take stack trace' in caplog.text


def test_skip_core_from_black_list(caplog, mocked_file):
    mocked_file.return_value = 1
    for bin_name in cfg.bin_black_list:
        core_name = COREDUMP.replace('report', bin_name)
        core = Core(core_name)
        watcher = CoreWatcher()
        with caplog.at_level(logging.DEBUG):
            watcher.process_core(core)
        assert 'Skip core for binary from black list' in caplog.text
