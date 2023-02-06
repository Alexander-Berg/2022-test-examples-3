import json
import os
import time
from logging import DEBUG, ERROR

import common
import mock_common
import pyserver
import webapi
from common import find_first_status, percentile
from sharddownload import ShardBuilder
from status import Status, create_status, app_status
from tests import mock_object
from tornado import testing
from tornado import wsgi

_SHARD_BUILDER_SCRIPT = 'shard_builder.sh'

webapi.init()


def test_find_first_status():
    assert find_first_status([], 'status1') is None
    assert find_first_status([['status1', 'payload'], ['status2', 'payload']], 'status1') == ['status1', 'payload']
    assert find_first_status([['status1', 'payload']], 'status2') is None

    assert find_first_status([
        ['status1', 'payload1'],
        ['status1', 'payload2'],
        ['status2', 'payload']
    ], 'status1') == ['status1', 'payload1']

    assert find_first_status([
        ['status1', 'payload1'],
        ['status1', 'payload2'],
        ['status2', 'payload1'],
        ['status2', 'payload2']
    ], 'status2') == ['status2', 'payload1']

    assert find_first_status([
        ['status1', 'payload1'],
        ['status1', 'payload2'],
        ['status2', 'payload1']
    ], 'status2') == ['status2', 'payload1']


def test_status():
    status = Status(mock_object.Config(), '/dev/null')
    status.set_status(Status.STARTING)
    assert status.get_status() == Status.STARTING

    status = Status(mock_object.Config(), '/dev/null')
    statuses = [st for st in Status.Statuses]
    for st in statuses:
        status.set_status(st)

    assert [st for st, _ in status.get_history()] == statuses


def test_percentile():
    assert percentile(None, 0) == 0
    assert percentile(None, 100) == 0
    assert percentile([], 0) == 0
    assert percentile([], 100) == 0
    assert percentile([1], 0) == 1
    assert percentile([1], 100) == 1
    assert percentile([1], 50) == 1

    assert percentile([1, 2, 3, 4, 5], 50) == 3
    assert percentile([1, 2, 3, 4], 50) == 3
    assert percentile([1, 2, 3, 4], 40) == 2

    assert percentile([1, 2, 3, 4, 5], 0) == 1
    assert percentile([1, 2, 3, 4, 5], 1) == 1

    assert percentile([1, 2, 3, 4, 5], 100) == 5
    assert percentile([1, 2, 3, 4, 5], 100500) == 5

    assert percentile([1, 2, 3, 4, 5], -100500) == 0


def test_zookeeper_mock():
    zookeeper = mock_object.Zookeeper()
    with zookeeper.host_semaphore:
        pass
    assert zookeeper.host_semaphore.acquire_count == zookeeper.host_semaphore.release_count == 1

    with zookeeper.shard_semaphore:
        pass
    assert zookeeper.shard_semaphore.acquire_count == zookeeper.shard_semaphore.release_count == 1

    with zookeeper.global_semaphore:
        pass
    assert zookeeper.global_semaphore.acquire_count == zookeeper.global_semaphore.release_count == 1

    value = 'value'
    path = '1/2/3/4/5'
    zookeeper.create(path=path, value=value, makepath=True)
    assert zookeeper.get(path) == value

    root = '1/2/3/8'
    zookeeper.create(path=os.path.join(root, '1'), value=value, makepath=True)
    zookeeper.create(path=os.path.join(root, '2'), value=value, makepath=True)
    zookeeper.create(path=os.path.join(root, '3'), value=value, makepath=True)
    assert sorted(zookeeper.get_children(root)) == sorted(['1', '2', '3'])


def mock_all_common():
    mock_common.init()

    common.execute = mock_common.execute
    common.sky_download = mock_common.sky_download
    common.get_instance_properties = mock_common.get_instance_properties
    common.get_instance_tags = mock_common.get_instance_tags
    common.create_logger = mock_common.create_logger
    common.iss_register_shard = mock_common.iss_register_shard
    common.get_shard_info = mock_common.get_shard_info_empty
    common.bs_register_shard = mock_common.bs_register_shard


def assert_sem_invariant(semaphore):
    assert semaphore.acquire_count == semaphore.release_count


def assert_shard_registration(registration, config):
    assert registration.shard_dir == config.shard_dir
    assert registration.required_shard_dir == config.required_shard_dir
    assert registration.registration_count == 1


def test_shardbuilder_usual_run():
    config = mock_object.Config()
    zookeeper = mock_object.Zookeeper()
    create_status(config, persistent=False)
    builder = ShardBuilder(config, zookeeper, None, None, _SHARD_BUILDER_SCRIPT)

    mock_all_common()

    builder.start(spawn_thread=False)

    assert_shard_registration(mock_common.iss_shard_registration, config)
    assert_shard_registration(mock_common.bs_shard_registration, config)

    assert app_status().get_status() == Status.DONE
    assert_sem_invariant(zookeeper.global_semaphore)
    assert_sem_invariant(zookeeper.shard_semaphore)
    assert_sem_invariant(zookeeper.host_semaphore)

    assert zookeeper.shard_semaphore.acquire_count > 0
    assert zookeeper.global_semaphore.acquire_count > 0
    assert zookeeper.host_semaphore.acquire_count > 0

    assert zookeeper.get(config.get_statistic_path(config.shard_dir)) is not None

    assert app_status().get_http_status_summary()[1] == 200


class TestServer(testing.AsyncHTTPTestCase):
    def get_app(self):
        return wsgi.WSGIContainer(pyserver.flask_app)

    def mock_all(self):
        self.config = mock_object.Config()
        self.zookeeper = mock_object.Zookeeper()
        create_status(self.config, persistent=False)
        self.builder = ShardBuilder(self.config, self.zookeeper, None, None, _SHARD_BUILDER_SCRIPT, exit_on_failure=False)

        mock_all_common()

    def mock_busy_shard_builder(self):
        self.shard_builder_ready = False

        def execute(cmd, logger, stdout_log_level=DEBUG, stderr_log_level=ERROR, **kwargs):
            if cmd[0] == _SHARD_BUILDER_SCRIPT:
                while not self.shard_builder_ready:
                    time.sleep(1)
            return 0

        common.execute = execute

    def mock_failed_shard_builder(self):
        self.shard_builder_ready = False

        def execute(cmd, logger, stdout_log_level=DEBUG, stderr_log_level=ERROR, **kwargs):
            if cmd[0] == _SHARD_BUILDER_SCRIPT:
                return 1
            return 0

        common.execute = execute

    def test_status_code_on_start(self):
        self.mock_all()
        self.builder.start(spawn_thread=True)

        time.sleep(1)

        response = self.fetch('/status_code')
        self.assertEqual(response.code, 200)

    def test_status_code_on_processing(self):
        self.mock_all()
        self.mock_busy_shard_builder()
        self.builder.start(spawn_thread=True)

        response = self.fetch('/status_code')
        self.assertEqual(response.code, 202)
        self.shard_builder_ready = True
        time.sleep(1)
        response = self.fetch('/status_code')
        self.assertEqual(response.code, 200)

    def test_status_on_fail(self):
        self.mock_all()
        self.mock_failed_shard_builder()
        self.builder.start(spawn_thread=True)

        time.sleep(1)
        assert app_status().get_status() == Status.FAILED

        response = self.fetch('/status_code')
        self.assertEqual(response.code, 500)

        response = self.fetch('/admin?action=stat')
        stats = json.loads(response.body)
        value = None
        for signal, signal_value in stats:
            if signal.startswith('failed'):
                value = signal_value
                break
        assert value == 1

    def test_stats(self):
        self.mock_all()
        self.mock_busy_shard_builder()
        self.builder.start(spawn_thread=True)

        time.sleep(1)
        self.shard_builder_ready = True

        while app_status().get_status() != Status.DONE:
            pass

        response = self.fetch('/admin?action=stat')
        self.assertEqual(response.code, 200)

        stats = json.loads(response.body)
        value = None
        for signal, signal_value in stats:
            if signal.startswith('shard_builder'):
                value = signal_value
                break
        assert value > 0
