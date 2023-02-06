from kazoo import exceptions
from mock_zookeeper import Node, ZookeperMock
from mock_queue import MockQueue
from taas_lib.exceptions import TaasException
from taas_lib.graph import ExecutionGraph
from taas_lib.paths import YtAvatarsConsumerPath, YtThumbnailerConsumerPaths, YtBigThumbnailerConsumerPaths, YtPaths, ZookeeperPaths, generate_state
from taas_lib.queue import KazooQueue, KazooLockingQueue
from taas_lib.registry import TaasRegistry
from taas_lib.taas_retry import taas_retry
from taas_lib.zk_task import AvatarsBackendDeployTask, BaseTask, ThumbdaemonBackendDeployTask, ThumbTask, UrlTask, LockTask

import json
import os
import pytest

from datetime import timedelta


class TestNode(object):
    def test_create(self):
        node = Node('name', 5)
        assert node.get_value() == 5
        assert len(node.get_children()) == 0

    def test_set_value(self):
        node = Node('/')
        node.set_value(5)
        assert node.get_value() == 5
        node.set_value('string')
        assert node.get_value() == 'string'

    def test_add_child(self):
        node = Node('/')
        node.add_child('subpath_0')
        node.add_child('subpath_1', 1)
        node.add_child('subpath_2', 'string')

        assert len(node.get_children()) == 3
        assert node.get_child('subpath_0').get_value() == ''
        assert node.get_child('subpath_1').get_value() == 1
        assert node.get_child('subpath_2').get_value() == 'string'

        node = node.get_child('subpath_2')
        node.add_child('subsubpath_2', 3)
        assert node.get_child('subsubpath_2').get_value() == 3


class TestQueueMock(object):
    def test_create(self):
        queue = MockQueue()
        assert 0 == queue.size()

    def test_put_single(self):
        queue = MockQueue()

        item_to_put_0 = {
            'name': 'test_item_0',
            'value': 0
        }

        queue.put(item_to_put_0)
        assert queue.size() == 1

        got_item = queue.get()
        assert got_item == item_to_put_0
        assert queue.size() == 0

    def test_put_multiple(self):
        queue = MockQueue()

        item_to_put_0 = {
            'name': 'test_item_0',
            'value': 0
        }

        item_to_put_1 = {
            'name': 'test_item_1',
            'value': 1
        }

        queue.put(item_to_put_0)
        assert queue.size() == 1
        queue.put(item_to_put_1)
        assert queue.size() == 2

        got_item = queue.get()
        assert got_item == item_to_put_0
        assert queue.size() == 1

        got_item = queue.get()
        assert got_item == item_to_put_1
        assert queue.size() == 0


@pytest.fixture()
def zk_client():
    zk = ZookeperMock()
    zk.start()
    return zk


@pytest.mark.usefixtures('zk_client')
class TestKazooQueue(object):
    def test_create(self, zk_client):
        queue = KazooQueue(zk_client, '/queue')
        assert queue.size() == 0

    def test_put(self, zk_client):
        queue = KazooQueue(zk_client, '/queue')

        test_data = ['item_0', 'item_1', 3]

        for test_sample in test_data:
            queue.put(test_sample)

        assert queue.size() == 3

    def test_get(self, zk_client):
        queue = KazooQueue(zk_client, '/queue')
        test_data = ['item_0', 'item_1', 3]
        for test_sample in test_data:
            queue.put(test_sample)

        for test_sample in test_data:
            assert str(test_sample) == queue.get()

        assert queue.size() == 0


@pytest.mark.usefixtures('zk_client')
class TestKazooLockingQueue(object):
    def test_create(self, zk_client):
        queue = KazooLockingQueue(zk_client, '/queue')
        assert queue.size() == 0

    def test_put(self, zk_client):
        queue = KazooLockingQueue(zk_client, '/queue')

        test_data = ['item_0', 'item_1', 3]

        for test_sample in test_data:
            queue.put(test_sample)

        assert queue.size() == 3


@pytest.mark.usefixtures('zk_client')
class TestZookeeperMock(object):
    def test_raise_not_started(self):
        zk = ZookeperMock()
        with pytest.raises(exceptions.ConnectionLoss):
            zk.create(path='/1/2/3/4/5', value='value', makepath=True)

        zk.start()
        zk.create(path='/1/2/3/4/5', value='value', makepath=True)
        zk.stop()
        zk.close()

    def test_create(self, zk_client):
        test_data = [
            {'path': '/1/2/3/4/5', 'value': 15},
            {'path': '/root', 'value': 'zabaka'},
            {
                'path': '/media-services/images/taas_test/consumers_tasks/consumer_name/7118a847-49ea-42d8-a0eb-fbf2eef8085b/state',
                'value': 'enqueued'}
        ]

        for test_sample in test_data:
            zk_client.create(path=test_sample['path'], value=test_sample['value'], makepath=True)

        for test_sample in test_data:
            assert zk_client.get(test_sample['path'])[0] == test_sample['value']

    def test_exists(self, zk_client):
        test_data = [
            {'path': '/1/2/3/4/5', 'value': 15},
            {'path': '/root', 'value': 'zabaka'},
        ]

        for test_sample in test_data:
            zk_client.create(path=test_sample['path'], value=test_sample['value'], makepath=True)

        for test_sample in test_data:
            assert zk_client.exists(path=test_sample['path'])

        assert not zk_client.exists(path='/unbelievable/you/could/imagine/same/path')

    def test_get_children(self, zk_client):
        test_data = [
            {'path': '/1/2/3/4/5', 'children': ['node0', 'node1', 'node2', 'node3']},
            {'path': '/root', 'children': ['node0', 'node1', 'node2']},
        ]

        for test_sample in test_data:
            for child in test_sample['children']:
                zk_client.create(path=os.path.join(test_sample['path'], child), makepath=True)

        for test_sample in test_data:
            assert sorted(zk_client.get_children(test_sample['path'])) == sorted(test_sample['children'])

    def test_delete(self, zk_client):
        test_data = [
            {'path': '/1/2/3/4/5', 'value': 15},
            {'path': '/root', 'value': 'zabaka'},
        ]

        for test_sample in test_data:
            zk_client.create(path=test_sample['path'], value=test_sample['value'], makepath=True)

        for test_sample in test_data:
            assert zk_client.exists(path=test_sample['path'])

        for test_sample in test_data:
            zk_client.delete(path=test_sample['path'])

        for test_sample in test_data:
            assert not zk_client.exists(path=test_sample['path'])

    def test_set(self, zk_client):
        test_data_0 = [
            {'path': '/1/2/3/4/5', 'value': 15},
            {'path': '/root', 'value': 'zabaka'},
        ]

        test_data_1 = [
            {'path': '/1/2/3/4/5', 'value': 5},
            {'path': '/root', 'value': 'nezabaka'},
        ]

        for test_sample in test_data_0:
            zk_client.create(path=test_sample['path'], value=test_sample['value'], makepath=True)

        for test_sample in test_data_1:
            zk_client.set(path=test_sample['path'], value=test_sample['value'])

        for test_sample in test_data_1:
            assert zk_client.get(path=test_sample['path'])[0] == test_sample['value']

        with pytest.raises(exceptions.NoNodeError):
            zk_client.set(path='/some/invalid/path', value=2)


@pytest.fixture()
def base_task():
    return BaseTask('banach.yt.yandex.net', 'consumer_name', 'type', description='test base task')


@pytest.fixture()
def thumbs_task():
    return ThumbTask('banach.yt.yandex.net', 'consumer_name', 'task_type', 'thumb_ids', 'thumbs', 'thumb_type',
                     description='test add task')


@pytest.fixture()
def lock_task():
    return LockTask(
        src_server='banach.yt.yandex.net',
        consumer='consumer_name',
        thumb_ids='thumb_ids',
        thumbs='thumbs',
        missing_ids='missing_ids',
        relaxed_alive=True,
        alive='alive')


@pytest.mark.usefixtures('zk_client', 'base_task')
class TestBaseTask(object):
    def test_init(self):
        BaseTask('banach.yt.yandex.net', 'consumer_name', 'type', description='test base task')

    def test_init_with_id(self):
        correct_id = 'a5c76511-18fe-4a98-a85a-8b7974c9c2f7'

        task0 = BaseTask('banach.yt.yandex.net', 'consumer_name', 'type', description='test base task',
                         task_id=correct_id)
        assert task0.id() == correct_id
        with pytest.raises(TaasException):
            BaseTask('banach.yt.yandex.net', 'consumer_name', 'type', description='test base task',
                     task_id='11 a5c761231511')

    def test_init_with_timestamp(self):
        correct_ts = 1481120291
        incorrect_ts = 148112029

        BaseTask('banach.yt.yandex.net', 'consumer_name', 'type', description='test base task',
                 production_timestamp=correct_ts)
        with pytest.raises(TaasException):
            BaseTask('banach.yt.yandex.net', 'consumer_name', 'type', description='test base task',
                     production_timestamp=incorrect_ts)

    def test_getters(self):
        src_server = 'banach.yt.yandex.net'
        production_timestamp = 1481120291
        description = 'test base task'
        consumer = 'consumer_name'
        task_type = 'type'

        task = BaseTask(src_server=src_server,
                        consumer=consumer,
                        task_type=task_type,
                        description=description,
                        production_timestamp=production_timestamp)

        assert task.src_server() == src_server
        assert task.id()
        assert task.timestamp()
        assert task.task_type() == task_type
        assert task.consumer() == consumer
        assert task.description() == description
        assert task.production_timestamp() == production_timestamp
        assert len(task.params()) == 0

    def test_add_param(self, base_task):
        test_data = {
            'task_param0': 'task_value_0',
            'task_param1': 0,
            'task_param2': False,
            'task_param3': True,
            'task_param4': 10,
        }

        for key, value in test_data.iteritems():
            base_task.add_param(key, value)

        task_params = base_task.params()
        assert len(task_params) == len(test_data)
        for key, value in test_data.iteritems():
            assert base_task.has_param(key)
            assert value == base_task.get_param(key)

        with pytest.raises(TaasException):
            base_task.get_param('not_existing_param')

    def test_task_record(self, base_task):
        test_data = {
            'task_param0': 'task_value_0',
            'task_param1': 0,
            'task_param2': False,
            'task_param3': True,
            'task_param4': 10,
        }

        for key, value in test_data.iteritems():
            base_task.add_param(key, value)

        task_record = base_task.get_task_record()

        for key, value in test_data.iteritems():
            assert key in task_record
            assert value == task_record[key]

        additional_keys = [
            'type',
            'task_uuid',
            'timestamp',
            'description',
            'production_timestamp',
            'consumer',
            'src_server',
        ]

        for key in additional_keys:
            assert key in task_record


@pytest.mark.usefixtures('zk_client', 'thumbs_task')
class TestThumbTask(object):
    def test_create(self):
        task = ThumbTask('banach.yt.yandex.net', 'consumer_name', 'task_type', 'thumb_ids', 'thumbs', 'thumb_type',
                         'alive')
        assert task.thumbs() == 'thumbs'
        assert task.thumb_type() == 'thumb_type'
        assert task.thumb_ids() == 'thumb_ids'
        assert task.thumbs() == 'thumbs'
        assert task.thumbs() == 'thumbs'
        assert task.alive() == 'alive'

    def test_task_publish(self, zk_client, thumbs_task):
        test_data = {
            'task_param0': 'task_value_0',
            'task_param1': 0,
            'task_param2': False,
            'task_param3': True,
            'task_param4': 10,
        }

        for key, value in test_data.iteritems():
            thumbs_task.add_param(key, value)

        thumbs_task.publish(zk_client)
        with pytest.raises(TaasException):
            thumbs_task.publish(zk_client)

    def test_get_state(self, zk_client, thumbs_task):
        task_id = thumbs_task.publish(zk_client)
        task = thumbs_task.get_task(zk_client, task_id)
        state = task.get_state(zk_client)
        assert state['task_id'] == task_id
        assert state['consumer'] == 'consumer_name'
        assert state['state'] == 'enqueued'

    def test_get_task(self, zk_client, thumbs_task):
        test_data = {
            'task_param0': 'task_value_0',
            'task_param1': 0,
            'task_param2': False,
            'task_param3': True,
            'task_param4': 10,
        }

        for key, value in test_data.iteritems():
            thumbs_task.add_param(key, value)

        task_id = thumbs_task.publish(zk_client)

        task = BaseTask.get_task(zk_client, task_id)
        task_params = task.params()
        for key, value in test_data.iteritems():
            assert key in task_params
            assert value == task_params[key]

        unpublished_task = ThumbTask('banach.yt.yandex.net', 'consumer_name', 'task_type', 'thumb_ids', 'thumbs',
                                     'thumb_type', description='unpublished add task')
        task_id = unpublished_task.id()

        with pytest.raises(TaasException):
            ThumbTask.get_task(zk_client, task_id)

    def test_failed(self, zk_client, thumbs_task):
        thumbs_task.publish(zk_client)
        assert not thumbs_task.failed(zk_client)

    def test_get_error_message(self, zk_client, thumbs_task):
        thumbs_task.publish(zk_client)
        assert not thumbs_task.get_error_message(zk_client)

    def test_get_history(self, zk_client, thumbs_task):
        thumbs_task.publish(zk_client)
        history = thumbs_task.get_history(zk_client)
        assert history[0][0] == thumbs_task._get_task_initial_state()

    def test_remove(self, zk_client, thumbs_task):
        thumbs_task.publish(zk_client)
        task_id = thumbs_task.id()
        ThumbTask.get_task(zk_client, task_id)

        thumbs_task.remove(zk_client)

        with pytest.raises(TaasException):
            ThumbTask.get_task(zk_client, task_id)

    def test_from_task_record(self):
        spawning_task = ThumbTask('banach.yt.yandex.net', 'consumer_name', 'task_type', 'thumb_ids', 'thumbs', 'thumb_type', 'alive', 'desc', production_timestamp=1481120291)
        task_record = spawning_task.get_task_record()

        spawned_task = ThumbTask.from_task_record(task_record)
        assert spawned_task.thumbs() == spawning_task.thumbs()
        assert spawned_task.thumb_ids() == spawning_task.thumb_ids()
        assert spawned_task.thumb_type() == spawning_task.thumb_type()
        assert spawned_task.alive() == spawning_task.alive()
        assert spawned_task.src_server() == spawning_task.src_server()
        assert spawned_task.id() == spawning_task.id()
        assert spawned_task.task_type() == spawning_task.task_type()
        assert spawned_task.consumer() == spawning_task.consumer()
        assert spawned_task.description() == spawning_task.description()
        assert spawned_task.production_timestamp() == spawning_task.production_timestamp()

        for key, value in spawning_task.params().iteritems():
            assert spawned_task.params()[key] == value


class TestUrlTask(object):
    def test_create(self):
        task = UrlTask('banach.yt.yandex.net', 'consumer_name', 'thumb_ids', 'urls')
        assert task.urls() == 'urls'
        assert task.thumb_ids() == 'thumb_ids'

    def test_from_task_record(self):
        spawning_task = UrlTask('banach.yt.yandex.net', 'consumer_name', 'thumb_ids', 'urls', 'desc', production_timestamp=1481120291)
        task_record = spawning_task.get_task_record()

        spawned_task = UrlTask.from_task_record(task_record)
        assert spawned_task.urls() == spawning_task.urls()
        assert spawned_task.thumb_ids() == spawning_task.thumb_ids()
        assert spawned_task.src_server() == spawning_task.src_server()
        assert spawned_task.id() == spawning_task.id()
        assert spawned_task.task_type() == spawning_task.task_type()
        assert spawned_task.consumer() == spawning_task.consumer()
        assert spawned_task.description() == spawning_task.description()
        assert spawned_task.production_timestamp() == spawning_task.production_timestamp()

        for key, value in spawning_task.params().iteritems():
            assert spawned_task.params()[key] == value


@pytest.mark.usefixtures('zk_client', 'lock_task')
class TestLockTask(object):
    def test_create(self):
        task = LockTask(
            src_server='banach.yt.yandex.net',
            consumer='consumer_name',
            thumb_ids='thumb_ids',
            thumbs='thumbs',
            missing_ids='missing_ids',
            relaxed_alive=True,
            alive='alive')

        assert task.thumbs() == 'thumbs'
        assert task.thumb_ids() == 'thumb_ids'
        assert task.thumbs() == 'thumbs'
        assert task.alive() == 'alive'
        assert task.relaxed_alive() is True
        assert task.missing_ids() == 'missing_ids'
        assert LockTask.get_type() == 'lock'


class TestThumbdaemonBackendDeployTask(object):
    def test_create(self):
        ThumbdaemonBackendDeployTask('banach.yt.yandex.net', 'consumer_name', 'thumb_ids', 'thumbs', 'thumb_type',
                                     'alive', description='unpublished add task')


class TestAvatarsBackendDeployTask(object):
    def test_create(self):
        AvatarsBackendDeployTask('banach.yt.yandex.net', 'consumer_name', 'thumb_ids', 'thumbs', 'thumb_type', 'alive',
                                 description='unpublished add task')


@pytest.fixture()
def zookeeper_paths():
    return ZookeeperPaths()


@pytest.mark.usefixtures('zookeeper_paths')
class TestZookeeperPaths(object):
    def test_default(self):
        paths = ZookeeperPaths()
        assert paths.thumbnailer_backend_task_queue() == '/media-services/images/taas/backoffice/task_queue'

    def test_queue_path(self):
        paths = ZookeeperPaths(home='/home')
        assert paths.thumbnailer_backend_task_queue() == '/home/backoffice/task_queue'
        assert paths.big_thumbnailer_backend_task_queue() == '/home/big_backoffice/task_queue'
        assert paths.crawler_task_queue() == '/home/crawler/task_queue'
        assert paths.avatars_backend_task_queue() == '/home/avatars/task_queue'

    def test_task_paths(self, zookeeper_paths):
        task_id = 'acb34-acdfge73b-34095fa-234234'
        assert zookeeper_paths.task('consumer', task_id) == os.path.join(
            '/media-services/images/taas/consumers_tasks/consumer', task_id)
        assert zookeeper_paths.task_state('consumer', task_id) == os.path.join(
            '/media-services/images/taas/consumers_tasks/consumer', task_id, 'state')
        assert zookeeper_paths.task_description('consumer', task_id) == os.path.join(
            '/media-services/images/taas/consumers_tasks/consumer', task_id, 'description')
        assert zookeeper_paths.task_history('consumer', task_id) == os.path.join(
            '/media-services/images/taas/consumers_tasks/consumer', task_id, 'history')
        assert zookeeper_paths.task_error_message('consumer', task_id) == os.path.join(
            '/media-services/images/taas/consumers_tasks/consumer', task_id, 'error_message')

    def test_consumer_home(self, zookeeper_paths):
        assert zookeeper_paths.consumers_home() == '/media-services/images/taas/consumers_tasks'
        assert zookeeper_paths.consumer_home(consumer='consumer') == os.path.join(
            '/media-services/images/taas/consumers_tasks', 'consumer')

    def test_registry(self, zookeeper_paths):
        assert zookeeper_paths.registry() == '/media-services/images/taas/registry'

    def test_avatars_jobs(self, zookeeper_paths):
        assert zookeeper_paths.avatars_jobs() == '/media-services/images/taas/avatars/jobs'

    def test_leader_election(self, zookeeper_paths):
        assert zookeeper_paths.leader_election() == '/media-services/images/taas/avatars/election'


@pytest.fixture()
def registry():
    zk = ZookeperMock()
    zk.start()
    return TaasRegistry(zk_client=zk, path='/')


@pytest.mark.usefixtures('zk_client', 'registry')
class TestTaasRegistry(object):
    def test_create(self, zk_client):
        registry = TaasRegistry(zk_client=zk_client, path='/')
        assert not registry.has_consumer('consumer')

    def test_add_consumer(self):
        test_consumers = [
            {
                'consumer': 'consumer_0',
                'thumbnailer_quota': 0,
                'avatars_quota': 1,
                'avatars_queue': 'test_queue',
                'avatars_namespace': 'test_namespace',
                'avatars_ttl_mode': False,
                'avatars_upload_rps': 100,
                'avatars_remove_rps': 200
            },
            {
                'consumer': 'consumer_1',
                'thumbnailer_quota': None,
                'avatars_quota': 0,
                'avatars_queue': 'consumer_1_queue',
                'avatars_namespace': 'consumer_1_namespace',
                'avatars_ttl_mode': True,
                'avatars_ttl': 100
            },
            {
                'consumer': 'consumer_2',
                'thumbnailer_quota': 0,
                'avatars_quota': None,
                'avatars_queue': None,
                'avatars_namespace': None,
                'avatars_ttl_mode': False,
                'avatars_upload_rps': 100,
                'avatars_remove_rps': 200
            }
        ]

        zk = ZookeperMock()
        zk.start()
        registry = TaasRegistry(zk_client=zk, path='/')

        for consumer in test_consumers:
            registry.add_consumer(**consumer)

        for consumer in test_consumers:
            with pytest.raises(TaasException):
                registry.add_consumer(consumer=consumer['consumer'],
                                      thumbnailer_quota=consumer['thumbnailer_quota'],
                                      avatars_quota=consumer['avatars_quota'],
                                      avatars_queue=consumer['avatars_queue'],
                                      avatars_namespace=consumer['avatars_namespace'])

        registry = TaasRegistry(zk_client=zk, path='/')

        for consumer in test_consumers:
            assert registry.has_consumer(consumer=consumer['consumer'])
            assert registry.thumbnailer_quota(consumer=consumer['consumer']) == consumer['thumbnailer_quota']
            assert registry.avatars_quota(consumer=consumer['consumer']) == consumer['avatars_quota']
            assert registry.avatars_queue(consumer=consumer['consumer']) == consumer['avatars_queue']
            assert registry.avatars_namespace(consumer=consumer['consumer']) == consumer['avatars_namespace']

        with pytest.raises(TaasException):
            registry.add_consumer(consumer='consumer')

    def test_setters(self):
        test_consumer_init = {
            'consumer': 'consumer_0',
            'thumbnailer_quota': 0,
            'avatars_quota': 1,
            'avatars_queue': 'test_queue',
            'avatars_namespace': 'test_namespace',
            'avatars_upload_rps': 100,
            'avatars_remove_rps': 250
        }

        test_consumer_set = {
            'consumer': 'consumer_0',
            'thumbnailer_quota': 0,
            'avatars_quota': 1,
            'avatars_queue': 'test_queue',
            'avatars_namespace': 'test_namespace'
        }

        zk = ZookeperMock()
        zk.start()
        registry = TaasRegistry(zk_client=zk, path='/')
        registry.add_consumer(**test_consumer_init)

        registry.set_thumbnailer_quota(test_consumer_set['consumer'], test_consumer_set['thumbnailer_quota'])
        registry.set_avatars_quota(test_consumer_set['consumer'], test_consumer_set['avatars_quota'])
        registry.set_avatars_queue(test_consumer_set['consumer'], test_consumer_set['avatars_queue'])
        registry.set_avatars_namespace(test_consumer_set['consumer'], test_consumer_set['avatars_namespace'])

        with pytest.raises(TaasException):
            registry.set_thumbnailer_quota(consumer='unknown_consumer', quota=0)

        assert registry.thumbnailer_quota(consumer=test_consumer_set['consumer']) == test_consumer_set['thumbnailer_quota']
        assert registry.avatars_quota(consumer=test_consumer_set['consumer']) == test_consumer_set['avatars_quota']
        assert registry.avatars_queue(consumer=test_consumer_set['consumer']) == test_consumer_set['avatars_queue']
        assert registry.avatars_namespace(consumer=test_consumer_set['consumer']) == test_consumer_set['avatars_namespace']

        registry = TaasRegistry(zk_client=zk, path='/')
        assert registry.thumbnailer_quota(consumer=test_consumer_set['consumer']) == test_consumer_set['thumbnailer_quota']
        assert registry.avatars_quota(consumer=test_consumer_set['consumer']) == test_consumer_set['avatars_quota']
        assert registry.avatars_queue(consumer=test_consumer_set['consumer']) == test_consumer_set['avatars_queue']
        assert registry.avatars_namespace(consumer=test_consumer_set['consumer']) == test_consumer_set['avatars_namespace']

    def test_remove_consumer(self):
        test_consumer = {
            'consumer': 'consumer_0',
            'thumbnailer_quota': 0,
            'avatars_quota': 1,
            'avatars_queue': 'test_queue',
            'avatars_namespace': 'test_namespace',
            'avatars_upload_rps': 100,
            'avatars_remove_rps': 250
        }

        zk = ZookeperMock()
        zk.start()
        registry = TaasRegistry(zk_client=zk, path='/')
        registry.add_consumer(**test_consumer)
        assert registry.has_consumer(consumer=test_consumer['consumer'])

        registry.remove_consumer(consumer=test_consumer['consumer'])
        assert not registry.has_consumer(consumer=test_consumer['consumer'])

        registry = TaasRegistry(zk_client=zk, path='/')
        assert not registry.has_consumer(consumer=test_consumer['consumer'])


@pytest.fixture()
def yt_paths():
    return YtPaths()


@pytest.mark.usefixtures('yt_paths')
class TestYtPaths(object):
    def test_init_default(self):
        paths = YtPaths()
        assert paths.home() == '//home/images/taas'

    def test_init(self):
        home_test = '//1/2/3/4/5'
        paths = YtPaths(home=home_test)
        assert paths.home() == home_test

    def test_subpaths(self, yt_paths):
        assert yt_paths.consumers_path() == '//home/images/taas/consumers'
        assert yt_paths.avatars_path() == '//home/images/taas/avatars'
        assert yt_paths.crawler_path() == '//home/images/taas/crawler'
        assert yt_paths.imtub_path() == '//home/images/taas/imtubs'
        assert yt_paths.thumbdb_path() == '//home/images/taas/thumbdb'
        assert yt_paths.tmp_thumbs_table() == '//home/images/taas/tmp/thumbs'
        assert yt_paths.tmp_id_table() == '//home/images/taas/tmp/ids'


class TestYtThumbnailerConsumerPaths(object):
    def test_init_default(self):
        paths = YtThumbnailerConsumerPaths(consumer='test_name')
        assert paths.home() == '//home/images/taas/consumers/test_name'

    def test_init(self):
        test_home = '//1/2/3/4/5/'
        test_name = 'test_name'
        paths = YtThumbnailerConsumerPaths(consumer=test_name, home=test_home)
        assert paths.home() == os.path.join(test_home, 'consumers', test_name)

    def test_subpaths(self):
        consumer_name = 'test_name'
        task_id = 'acb34-acdfge73b-34095fa-234234'
        paths = YtThumbnailerConsumerPaths(consumer=consumer_name)
        assert paths.ids_table() == os.path.join('//home/images/taas/consumers', consumer_name, 'ids')
        assert paths.consumer_ids_table() == os.path.join('//home/images/taas/consumers',
                                                          consumer_name, 'consumers_ids')
        assert paths.production_timestamp() == os.path.join('//home/images/taas/consumers',
                                                            consumer_name, 'production_timestamp')
        assert paths.thumbs_for_task(task_id) == os.path.join('//home/images/taas/consumers',
                                                              consumer_name, 'thumbs_{}'.format(task_id))
        assert paths.alive_for_task(task_id) == os.path.join('//home/images/taas/consumers',
                                                             consumer_name, 'alive_{}'.format(task_id))
        assert paths.task_path(task_id) == os.path.join('//home/images/taas/consumers', consumer_name,
                                                        'backoffice_task_queue', task_id)
        assert paths.export_ids_table() == os.path.join('//home/images/taas/consumers', consumer_name, 'export_ids')


class TestYtBigThumbnailerConsumerPaths(object):
    def test_init_default(self):
        paths = YtBigThumbnailerConsumerPaths(consumer='test_name')
        assert paths.home() == '//home/images/taas_big/consumers/test_name'

    def test_init(self):
        test_home = '//1/2/3/4/5/'
        test_name = 'test_name'
        paths = YtBigThumbnailerConsumerPaths(consumer=test_name, home=test_home)
        assert paths.home() == os.path.join(test_home, 'consumers', test_name)

    def test_subpaths(self):
        consumer_name = 'test_name'
        task_id = 'acb34-acdfge73b-34095fa-234234'
        paths = YtBigThumbnailerConsumerPaths(consumer=consumer_name)
        assert paths.ids_table() == os.path.join('//home/images/taas_big/consumers', consumer_name, 'ids')
        assert paths.consumer_ids_table() == os.path.join('//home/images/taas_big/consumers',
                                                          consumer_name, 'consumers_ids')
        assert paths.production_timestamp() == os.path.join('//home/images/taas_big/consumers',
                                                            consumer_name, 'production_timestamp')
        assert paths.thumbs_for_task(task_id) == os.path.join('//home/images/taas_big/consumers',
                                                              consumer_name, 'thumbs_{}'.format(task_id))
        assert paths.alive_for_task(task_id) == os.path.join('//home/images/taas_big/consumers',
                                                             consumer_name, 'alive_{}'.format(task_id))
        assert paths.task_path(task_id) == os.path.join('//home/images/taas_big/consumers', consumer_name,
                                                        'backoffice_task_queue', task_id)
        assert paths.export_ids_table() == os.path.join('//home/images/taas_big/consumers', consumer_name, 'export_ids')


class TestYtAvatarsConsumerPath(object):
    def test_paths(self):
        consumer_name = 'consumer'
        home = 'home'
        state = generate_state()
        id = 'acb34-acdfge73b-34095fa-234234'
        state_with_id = '-'.join([state, id])

        paths = YtAvatarsConsumerPath(consumer=consumer_name, state=state, id=id, home=home)
        assert paths.alive() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'alive')
        assert paths.thumbs() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'thumbs')
        assert paths.ids() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'ids')
        assert paths.task() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'task')
        assert paths.errors() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'errors')
        assert paths.unknown_errors() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'unknown_errors')
        assert paths.stats() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'stats')
        assert paths.state_gendb() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'gendb')
        assert paths.state_min_generation() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'min_generation')
        assert paths.state_generation() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'generation')
        assert paths.upload_portion() == os.path.join(home, 'avatars', consumer_name, state_with_id, 'upload_portion')
        assert paths.state() == os.path.join(home, 'avatars', consumer_name, state_with_id)
        assert paths.gendb() == os.path.join(home, 'avatars', consumer_name, 'gendb')
        assert paths.min_generation() == os.path.join(home, 'avatars', consumer_name, 'min_generation')
        assert paths.generation() == os.path.join(home, 'avatars', consumer_name, 'generation')
        assert paths.removal_feed_portion() == os.path.join(home, 'avatars', consumer_name, 'removal_feed/removal_portion-{}'.format(state_with_id))


class TestRetry(object):
    @staticmethod
    @taas_retry(tries=2, timeout=timedelta(seconds=1).total_seconds())
    def do_with_failure(success_after_tries):
        if success_after_tries[0]:
            success_after_tries[0] -= 1
            raise Exception('something wrong')

    def test_retry_success(self):
        tries_till_success = 1
        TestRetry.do_with_failure([tries_till_success])

    def test_retry_failure(self):
        tries_till_success = 10  # 10 >> 2 tries specified in decorator
        with pytest.raises(Exception):
            TestRetry.do_with_failure([tries_till_success])


class TestExecutionGraph(object):
    def test_invalid(self):
        invalid_graph = {
            'A': {'requirements': ['B'], 'executor': None, 'params': None},
            'B': {'requirements': ['A'], 'executor': None, 'params': None}
        }

        with pytest.raises(Exception):
            ExecutionGraph(invalid_graph)

        invalid_graph = {
            'A': {'requirements': ['C'], 'executor': None, 'params': None},
            'B': {'requirements': ['A'], 'executor': None, 'params': None},
            'C': {'requirements': ['B'], 'executor': None, 'params': None}
        }

        with pytest.raises(Exception):
            ExecutionGraph(invalid_graph)

    def test_valid(self):
        valid_graph = {
            'A': {'requirements': [], 'executor': None, 'params': None},
            'B': {'requirements': ['A'], 'executor': None, 'params': None},
            'C': {'requirements': ['B'], 'executor': None, 'params': None}
        }

        ExecutionGraph(valid_graph)

        valid_graph = {
            'A': {'requirements': [], 'executor': None, 'params': None},
        }

        ExecutionGraph(valid_graph)

        valid_graph = {
            'A': {'requirements': [], 'executor': None, 'params': None},
            'B': {'requirements': ['A'], 'executor': None, 'params': None},
            'C': {'requirements': ['B'], 'executor': None, 'params': None},
            'D': {'requirements': ['B'], 'executor': None, 'params': None}
        }

        ExecutionGraph(valid_graph)

        valid_graph = {
            'A': {'requirements': [], 'executor': None, 'params': None},
            'B': {'requirements': [], 'executor': None, 'params': None},
            'C': {'requirements': ['B', 'A'], 'executor': None, 'params': None},
            'D': {'requirements': ['A', 'B'], 'executor': None, 'params': None}
        }

        ExecutionGraph(valid_graph)

    def test_topological(self):
        graph = {
            'A': {'requirements': [], 'executor': None, 'params': None},
        }

        ex_graph = ExecutionGraph(graph)
        topological = ex_graph.topological()
        assert topological == ['A']

        graph = {
            'A': {'requirements': [], 'executor': None, 'params': None},
            'B': {'requirements': ['A'], 'executor': None, 'params': None},
            'C': {'requirements': ['A', 'B'], 'executor': None, 'params': None},
            'D': {'requirements': ['B', 'C'], 'executor': None, 'params': None}
        }

        ex_graph = ExecutionGraph(graph)
        topological = ex_graph.topological()
        assert topological == ['A', 'B', 'C', 'D']

    def test_dependencides(self):
        graph = {
            'A': {'requirements': [], 'executor': None, 'params': None},
            'B': {'requirements': [], 'executor': None, 'params': None},
            'C': {'requirements': ['A', 'B'], 'executor': None, 'params': None},
            'D': {'requirements': ['B', 'C'], 'executor': None, 'params': None}
        }

        ex_graph = ExecutionGraph(graph)
        deps = ex_graph.dependencies()

        assert json.loads(deps.dump()) == {
            'count': {
                'A': 0,
                'B': 0,
                'C': 2,
                'D': 2},
            'dependencies': {
                'A': ['C'],
                'B': ['C', 'D'],
                'C': ['D']
            }}

        assert deps.has_ready_targets()
        assert not deps.all_done()
        with pytest.raises(Exception):
            deps.target_is_done('C')

        next_target = deps.next_ready_target()
        deps.target_is_done(next_target)
        next_target = deps.next_ready_target()
        deps.target_is_done(next_target)
        assert deps.has_ready_targets()
        assert deps.next_ready_target() == 'C'
        deps.target_is_done('C')
        assert deps.next_ready_target() == 'D'
        deps.target_is_done('D')
        assert not deps.has_ready_targets()
        assert not deps.next_ready_target()
        assert deps.all_done()
