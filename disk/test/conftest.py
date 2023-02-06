# -*- coding: utf-8 -*-
from functools import wraps

import pytest
import copy
import logging
import multiprocessing
import os
import warnings
import mongomock
import time
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
warnings.simplefilter('ignore', UserWarning)

from distutils import dir_util
from collections import namedtuple
from contextlib import contextmanager
import mock
from _pytest.tmpdir import TempdirFactory

import mpfs.engine.process
from test.fixtures.users import default_user, user_7

UID = default_user.uid
_original_queue_put = None
collected_data = {}
REAL_MONGO = True
INIT_USER_IN_POSTGRES = True
COMMON_DB_IN_POSTGRES = False


_QueueError = namedtuple('_QueueError', ['error', 'job'])
_queue_global_error_list = None
_queue_global_params_list = None


_delayed_async_tasks = None


@contextmanager
def delay_async_tasks(enable=True):
    global _delayed_async_tasks

    try:
        if enable:
            _delayed_async_tasks = []

        yield _delayed_async_tasks

        if enable:
            run_delayed_async_tasks()
    finally:
        _delayed_async_tasks = None


@contextmanager
def capture_queue_errors():
    global _queue_global_error_list
    _queue_global_error_list = []
    yield _queue_global_error_list
    _queue_global_error_list = None


@contextmanager
def capture_queue_parameters():
    global _queue_global_params_list
    _queue_global_params_list = []
    yield _queue_global_params_list
    _queue_global_params_list = None


def pop_unsupported_keys(func):
    """Удаляет неподдерживаемые mongomock'ом ключи"""

    def wrapped(self, *args, **kwargs):
        # Удаляем параметры, которые не реализованы в mongomock
        for key in ['read_preference',
                    'fsync',
                    'w',
                    'shard_name',
                    'timeout']:
            if key in kwargs:
                del kwargs[key]

        if 'fields' in kwargs:
            args = list(args) + [kwargs.pop('fields')]

        return func(self, *args, **kwargs)

    return wrapped


def mongomock_insert_decorator(f):
    """
    Да, можно написать лучше, сделано, как workaround для конкретного кейса
    """
    @wraps(f)
    def wrapper(*args, **kwargs):
        doc = args[1]
        for k, v in doc.items():
            if k == '$or':
                del doc[k]
                continue
            if isinstance(v, dict):
                if '$exists' in v:
                    del v['$exists']
                    if not v:
                        del doc[k]
        return f(*args, **kwargs)
    return wrapper


def mongomock_update_decorator(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        if kwargs.get('upsert'):
            original_insert = mongomock.collection.Collection._insert
            mongomock.collection.Collection._insert = mongomock_insert_decorator(original_insert)
            try:
                return f(*args, **kwargs)
            finally:
                mongomock.collection.Collection._insert = original_insert
        else:
            return f(*args, **kwargs)
    return wrapper


def _patch_remote_db():
    real_mongo = os.environ.get('MPFS_REAL_MONGO', 'FALSE').lower() == 'true'
    if not real_mongo:
        global REAL_MONGO
        REAL_MONGO = False

        mongomock.collection.Collection.update = mongomock_update_decorator(mongomock.collection.Collection.update)

        mongomock.collection.Collection.find = pop_unsupported_keys(mongomock.collection.Collection.find)

        fake_connection_pool = {}

        def fake_connection_source(obj, conn_name, **kwargs):
            # подразумевается, что разные имена ссылаются на разные репликасеты, на деле это не так.
            if conn_name not in fake_connection_pool:
                fake_connection_pool[conn_name] = mongomock.MongoClient()
            return fake_connection_pool[conn_name]

        mongo_client_patches = [
            mock.patch('mpfs.metastorage.mongo.source.MongoSourceController.connection', new=fake_connection_source),
            mock.patch('mpfs.metastorage.mongo.source.MongoSourceController.checkdb', return_value=True),
            mock.patch('mpfs.metastorage.mongo.pool.MPFSMongoReplicaSetPool.get_connection_for_rs_name', new=fake_connection_source),
            mock.patch('mpfs.metastorage.mongo.pool.MPFSMongoReplicaSetPool.is_shard_writeable', return_value=True),
        ]
        for mongo_client_patch in mongo_client_patches:
            mongo_client_patch.start()


@pytest.mark.trylast
def pytest_configure(config):
    prepare_config_files(TempdirFactory(config))
    setup_process()


def prepare_config_files(tmpdir_factory):
    tmp = tmpdir_factory.mktemp("config")
    tmp_path = str(tmp.dirpath())
    configs_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../apps"))

    for config_path in ("common/conf/mpfs",
                        "disk/conf/mpfs",
                        "browser/conf/mpfs",
                        "api/conf/mpfs"):
        cur_configs = os.path.join(configs_dir, config_path)
        dir_util.copy_tree(cur_configs, tmp_path)

    os.environ['MPFS_CONFIG_PATH'] = tmp_path

    from mpfs.config.constants import DISK_ANDROID_VIDEOUNLIM_ALERT_FILE_PATH
    with open(DISK_ANDROID_VIDEOUNLIM_ALERT_FILE_PATH, 'w') as fh:
        fh.write('%s\n' % user_7.uid)


def setup_process():
    """Подготавливает процесс, схоже с подготовками в `uwsgi_*.py` скриптах."""
    os.environ.pop('QLOUD_DATACENTER', None)
    from mpfs.common.util.logger import redirect_logs, configure_logging
    from mpfs.config import settings

    TEST_LOGS_FILENAME = settings.logger['test_logs_filename']

    configure_logging(settings.logger['dict_config'], app_name="test", signum=None)
    redirect_logs(lambda: logging.FileHandler(filename=TEST_LOGS_FILENAME))

    # Импортируем после настройки логгера, для корректной инициализации логгера в модулях
    from mpfs.platform.auth import ClientNetworks, TVMAuth
    from mpfs.core.services.tvm_service import tvm
    from mpfs.core.services.tvm_2_0_service import tvm2

    # Костыль для тестов, ибо в них setup зовется дважды из разных мест
    if mpfs.engine.process.get_register_after_fork_impl() is None:
        mpfs.engine.process.set_register_after_fork_impl(multiprocessing.util.register_after_fork)

    setup_queue2()
    _patch_remote_db()

    global INIT_USER_IN_POSTGRES
    init_user_in_postgres = os.environ.get('MPFS_INIT_IN_POSTGRES', 'TRUE').lower() == 'true'
    INIT_USER_IN_POSTGRES = init_user_in_postgres
    global COMMON_DB_IN_POSTGRES
    common_db_in_postgres = os.environ.get('MPFS_COMMON_DB_IN_POSTGRES', 'FALSE').lower() == 'true'
    COMMON_DB_IN_POSTGRES = common_db_in_postgres

    mock.patch.dict(
        'mpfs.config.settings.common_pg',
        {c: {'use_pg': COMMON_DB_IN_POSTGRES, 'mongo_ro': COMMON_DB_IN_POSTGRES} for c in settings.common_pg.keys()}
    ).start()

    mpfs.engine.process.setup(setuid=False)
    mpfs.engine.process.setup_tvm_2_0_clients(settings.auth['clients'])
    mpfs.engine.process.setup_authorization_networks()
    mpfs.engine.process.setup_handlers_groups()
    ClientNetworks()

    # заполнение публичных ключей TVM при старте сервиса
    TVMAuth.register_tvm_clients()
    tvm.collect_tvm_pub_keys()

    tvm2.update_public_keys()
    tvm2.update_service_tickets()

    mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MKDIR_ENABLED', False).start()


def setup_queue2():
    def fake_get_shuffled_rabbitmq_hosts():
        return ['localhost']

    import mpfs.engine.queue2.utils
    mpfs.engine.queue2.utils.get_rabbitmq_hosts = fake_get_shuffled_rabbitmq_hosts

    from mpfs.engine.queue2.celery import app
    app.conf.CELERY_ALWAYS_EAGER = True
    app.conf.CELERY_EAGER_PROPAGATES_EXCEPTIONS = True
    import mpfs.core.queue

    def fake_queue_put(*a, **kw):
        a = copy.deepcopy(a)
        kw = copy.deepcopy(kw)

        jtype = a[1]

        try:
            command, jtype = a[0], a[1]
            current_time = int(time.time())

            command['context'] = {
                'ycrid': mpfs.engine.process.get_cloud_req_id(),  # эмулируем работу queller'а в тестах
            }

            data = {
                'ctime': current_time,
                'mtime': current_time,
                'state': 0,
                'stime': None,
                'data': command,
                'name': '',
                'type': jtype,
                'priority': None,
                'try_count': 0,
                'cloud_request_id': mpfs.engine.process.get_cloud_req_id(),
            }

            collected_data['queue_put'] = {'a': (mpfs.engine.process.hostname(), data), 'kw': {}}

            global _delayed_async_tasks
            global _queue_global_params_list
            if _queue_global_params_list is not None:
                _queue_global_params_list.append(a)

            if _delayed_async_tasks is None:
                # выполняем таск синхронно
                mpfs.core.queue.QueueDispatcher().put(*a, **kw)
            else:
                # складываем таск в список, чтобы выполнить после того, как отработает ручка
                _delayed_async_tasks.append((a, kw))
        except Exception as e:
            global _queue_global_error_list
            if _queue_global_error_list is not None:
                _queue_global_error_list.append(_QueueError(error=e, job=jtype))

    global _original_queue_put
    _original_queue_put = mpfs.core.queue.mpfs_queue.put
    mpfs.core.queue.mpfs_queue.put = fake_queue_put


def run_delayed_async_tasks():
    import mpfs.core.queue

    global _delayed_async_tasks
    if _delayed_async_tasks is not None:
        mpfs.engine.process.reset_cached()  # эмулируем разный кеш у мпфс и воркера, который исполняет таск

    while _delayed_async_tasks:
        a, kw = _delayed_async_tasks.pop()
        mpfs.core.queue.QueueDispatcher().put(*a, **kw)


def teardown_queue():
    import mpfs.core.queue
    from mpfs.core.queue import mpfs_queue

    for job_queue in mpfs_queue.job_routes.itervalues():
        if isinstance(job_queue.control, FakeQueueCollection):
            job_queue.control = job_queue.control.original_collection
    for control_name, control_instance in mpfs_queue.controls.iteritems():
        if isinstance(control_instance, FakeQueueCollection):
            mpfs_queue.controls[control_name] = control_instance.original_collection

    mpfs.core.queue.mpfs_queue.put = _original_queue_put


class FakeQueueCollection(object):

    def __init__(self, collection):
        self.original_collection = collection
        from mpfs.engine.process import get_default_log
        self.log = get_default_log()

    def __getattribute__(self, *args, **kwargs):
        try:
            return object.__getattribute__(self, *args, **kwargs)
        except AttributeError:
            return object.__getattribute__(self.original_collection, *args, **kwargs)

    def put(self, *a, **kw):
        collected_data['queue_put'] = {'a': a, 'kw': kw}
        self.log.info('JOB_PUT: %s %s' % (a, kw))
        return self.original_collection.put(*a, **kw)

    def remove(self, *a, **kw):
        collected_data['queue_remove'] = {'a': a, 'kw': kw}
        self.log.info('JOB_REMOVE: %s %s' % (a, kw))
        return self.original_collection.remove(*a, **kw)

    def move(self, *a, **kw):
        collected_data['queue_move'] = {'a': a, 'kw': kw}
        self.log.info('JOB_MOVE: %s %s' % (a, kw))
        return self.original_collection.move(*a, **kw)
