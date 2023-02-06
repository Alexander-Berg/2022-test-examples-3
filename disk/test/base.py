# -*- coding: utf-8 -*-
import os
import random

from mpfs.common.static import SPACE_1GB
from mpfs.core.filesystem.helpers.counter import Counter

if 'MPFS_PACKAGE' not in os.environ:
    os.environ['MPFS_PACKAGE'] = 'disk'

import string
import contextlib
import json
import hashlib
import urlparse
import mock

from jsonschema import validate
from jsonschema.exceptions import ValidationError
from lxml import etree
from smtplib import SMTP
from unittest import TestCase


from test.fixtures.users import default_user
from test.base_suit import (UploadFileTestCaseMixin, BillingApiTestCaseMixin, UserTestCaseMixin,
                            FakeResponse)
from test.conftest import setup_queue2
from test.helpers.utils import ignored, without_querystring
from test.helpers.stubs.db import MpfsDbHelper
from test.helpers.stubs.postgres import get_postgres_connection_stub_impl
from test.helpers.stubs.globals import UWSGIStub
from test.helpers.stubs.manager import StubsManager, StubScope
from test.helpers.stubs.services import PassportStub
from mpfs.engine.process import get_global_request, set_global_request, get_default_log
from mpfs.frontend.api import Default
from mpfs.frontend.api.browser import Browser
from mpfs.frontend.api.disk.desktop import Desktop
from mpfs.frontend.api.support_api import Support
from mpfs.metastorage.mongo.mapper import POSTGRES_USER_INFO_ENTRY
from mpfs.metastorage.mongo.collections.base import DirectRouteCollection
from mpfs.frontend.request import UserRequest
from mpfs.common.errors import StorageInitUser
from mpfs.common.util import from_json, to_json
from mpfs.config import settings
from mpfs.core.base import user_info
from mpfs.core.user.base import User
from mpfs.core.metastorage import control
from mpfs.dao.session import Session


def parse_open_url_call(mock_obj, call_num=-1):
    """
    Функция для получения параметров вызова `open_url`

    :param mock_obj: объект mock.MagicMock
    :param call_num: индекс вызова open_url, который хотим рассмотреть. -1 - последний

    Работаем так:
        1. Мокаем метод open_url.
        2. Проставляем ожидаемый ответ сервиса.
        3. Дергаем тестируемый метод(тот, который дергает open_url).
        4. Передаем этой функции mock_obj и получаем с какими параметрами был вызван open_url.

    # П.1.
    with mock.patch.object(object_with_open_url_method, 'open_url') as mock_obj:
        # П.2.
        mock_obj.return_value = 'This is serivice expected response'
        # П.3.
        object_with_open_url_method.tested_method(*args, *kwargs)
        # П.4.
        url_info = parse_open_url_call(mock_obj)
    """
    name, args, kwargs = mock_obj.mock_calls[call_num]
    url = args[0]
    parsed_url = urlparse.urlparse(url)
    pure_data = kwargs.get('pure_data')
    json_body = None
    if pure_data:
        try:
            json_body = json.loads(pure_data)
        except Exception:
            pass

    return {
        'page': parsed_url.path,
        'params': urlparse.parse_qs(parsed_url.query),
        'pure_data': pure_data,
        'json_body': json_body
    }


class MpfsBaseTestCase(TestCase):
    """Базовый класс теста MPFS."""
    log = get_default_log()

    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) | {UWSGIStub})

    def disable_rate_limiter(self):
        """Вырубаем rate limiter"""
        from mpfs.core.services import rate_limiter_service
        rate_limiter_service.IS_RATE_LIMITER_ENABLED = False

    @classmethod
    def setup_class(cls):
        cls.stubs_manager.enable_stubs(scope=StubScope.CLASS)

    @classmethod
    def teardown_class(cls):
        cls.stubs_manager.disable_stubs(scope=StubScope.CLASS)

    @staticmethod
    def truncate_existed_tables(connection):
        tables_str = ''
        for table_name in iter_existed_table_names(connection):
            tables_str += '%s,' % table_name
        connection.execute('TRUNCATE TABLE %s CASCADE;' % tables_str.rstrip(','))

    @classmethod
    def remove_postgres_data(cls):
        from mpfs.metastorage.postgres.query_executer import ReadPreference
        for unit in (PostgresUnits.COMMON, PostgresUnits.UNIT1, PostgresUnits.UNIT2):
            session = Session.create_from_shard_id(unit, read_preference=ReadPreference.primary)
            with session.begin():
                cls.truncate_existed_tables(session)

    def setup_method(self, method):
        import mpfs.engine.process

        test_id = '%s.py/%s::%s' % (method.im_class.__module__, method.im_class.__name__, method.im_func.func_name)
        mpfs.engine.process.set_app_name(test_id)

        # делаем имя базы достаточно уникальным, чтобы ни с чем не пересечься
        # и подгоняем под требования к названиям баз в монге
        test_dbname = "%s_%s_%s_%s" % (
            mpfs.engine.process.hostname(),
            method.im_class.__module__,
            method.im_class.__name__,
            method.im_func.func_name,
        )
        test_dbname = test_dbname.replace('.', '_')
        test_dbname = hashlib.md5(test_dbname).hexdigest()[:10] + test_dbname[-49:]

        self.db_helper = MpfsDbHelper(test_dbname)
        self.db_helper.start()

        self.postgres_connections_stub = get_postgres_connection_stub_impl(PostgresUnits.COMMON)
        self.postgres_connections_stub.start()

        from mpfs.metastorage.postgres.query_executer import PGQueryExecuter
        PGQueryExecuter().reset_cache()

        mpfs.engine.process.reset_cached()

        # https://st.yandex-team.ru/CHEMODAN-38878
        for name in dir(control):
            if name.startswith('__'):
                continue
            else:
                var = getattr(control, name)
                if not isinstance(var, DirectRouteCollection):
                    continue

                var.__init__()  # перезатирает закешированные _collection, _connection, _database

        self.remove_postgres_data()

        self.disable_rate_limiter()
        self.stubs_manager.enable_stubs(scope=StubScope.FUNCTION)
        PassportStub.reset_users_info()

        self.log.info('\n\n\n' + ' * ' * 15 + 'Begin test method log output: %s' % self._testMethodName + ' * ' * 15)

    def teardown_method(self, method):
        import mpfs.engine.process
        self.db_helper.stop()
        mpfs.engine.process.reset_cached()

        Counter().reset()

        self.postgres_connections_stub.stop()
        self.stubs_manager.disable_stubs(scope=StubScope.FUNCTION)

        self.log.info('\n' + ' * ' * 15 + 'End test method log output: %s' % self._testMethodName + ' * ' * 15 + '\n\n')
        mpfs.engine.process.set_app_name("test")

    def assertHasValidSchema(self, item, schema):
        """Проверяет соответствует ли переданное значение JSON схеме."""
        try:
            validate(item, schema)
        except ValidationError as exc:
            self.fail(exc)


def iter_existed_table_names(connection):
    schema_name = 'disk'
    cursor = connection.execute("SELECT table_name "
                                "FROM information_schema.tables "
                                "WHERE table_schema='%s';" % schema_name)
    for table_name in cursor:
        yield schema_name + '.' + table_name[0]


class PostgresUnits(object):
    COMMON = '0'
    UNIT1 = '1'
    UNIT2 = '2'


class CommonDiskTestCase(UserTestCaseMixin, BillingApiTestCaseMixin,
                         UploadFileTestCaseMixin, MpfsBaseTestCase):
    """Базовый класс дисковых тестов."""
    uid = default_user.uid

    response = FakeResponse()

    search_url = without_querystring(settings.services['search_indexer']['base_url'])

    @property
    def request(self):
        return get_global_request()

    @request.setter
    def request(self, value):
        set_global_request(value)

    @property
    def request(self):
        return get_global_request()

    @request.setter
    def request(self, value):
        set_global_request(value)

    def setup_method(self, method):
        super(CommonDiskTestCase, self).setup_method(method)
        self.run_000_user_check(uid=self.uid)

    def get_request(self, args=None):
        req = UserRequest({})
        if not args:
            args = {}
        req.set_args(args)

        req.user = None
        if 'uid' in args:
            with ignored(StorageInitUser):
                req.user = User(args['uid'])

        required_attributes = ('add_services', 'source', 'b2b_key')
        for attribute in required_attributes:
            if not hasattr(req, attribute):
                req.set_args({attribute: None})

        return req

    def space_limit(self, uid):
        """Возвращает лимит пространства на Диске пользователя"""
        request = self.get_request({'uid' : uid, 'project': 'disk'})
        return user_info(request)['space']['limit']

    def space_limit_gb(self, uid):
        return self.space_limit(uid) / SPACE_1GB

    def run_000_user_check(self, uid=None):
        # Просто создаёт пользователя если его нет.
        if not uid:
            uid = self.uid
        opts = {'uid': uid}
        result = self.json_ok('user_check', opts)
        if result['need_init'] == '1':
            shard = POSTGRES_USER_INFO_ENTRY
            self.create_user(uid, shard=shard)
        self.xiva_subscribe(uid)

    def xiva_subscribe(self, uid):
        url = 'http://localhost/service/echo'
        opts = {'uid': uid, 'callback': url}
        self.service('xiva_subscribe', opts)

    def xiva_unsubscribe(self, uid):
        url = 'http://localhost/service/echo'
        opts = {'uid': uid, 'callback': url}
        self.service('xiva_unsubscribe', opts)

    def run_ZZZZ(self):
        pass

    def async_ok(self, method, *args, **kwargs):
        try:
            uid = args[0]['uid']
        except IndexError:
            uid = kwargs['opts']['uid']
        result = self.json_ok(method, *args, **kwargs)
        self.assertTrue('oid' in result)
        opts = {
                'uid' : uid,
                'oid' : result['oid'],
                }
        status_result = self.json_ok('status', opts)
        self.assertEqual(status_result['status'], 'DONE')
        return status_result

    def support_ok(self, method, opts={}):
        support_api = Support()
        return self.json_ok(method, opts, api=support_api, request_line='/support/')

    def support_error(self, method, opts={}, code=None):
        support_api = Support()
        return self.json_error(method, opts, code=code, api=support_api)

    def default(self, method, opts={}):
        default_api = Default()
        default_api.setup(self.params(opts))
        result = default_api.process(method)
        if result:
            return from_json(result)
        else:
            return False

    def desktop(self, method, opts={}):
        desktop_api = Desktop()
        desktop_api.setup(self.params(opts))
        result = desktop_api.process(method)
        if result:
            return from_json(result)

    def browser(self, method, opts={}, json=None):
        browser_api = Browser()
        params = self.params(opts)
        params['_request'].json = json
        params['_request'].data = to_json(json)
        browser_api.setup(params)
        return from_json(browser_api.process(method))

    def check_push(self, open_url_data, xiva_data=None):
        for k, v in open_url_data.iteritems():
            if xiva_data and k.startswith('http://localhost/service/echo'):
                for item in v:
                    pure_data = item.get('pure_data')
                    et = etree.fromstring(pure_data)
                    for op in et.iterfind('op'):
                        data = dict(op.items())
                        if data['key'] in xiva_data:
                            item_data = xiva_data[data['key']]
                            for k, v in item_data.iteritems():
                                self.assertEqual(str(data[k]), str(v))
                            item_data['notified'] = True
        if xiva_data:
            for k, v in xiva_data.iteritems():
                self.assertTrue(v.get('notified'), msg="no xiva push for %s" % k)


class DiskTestCase(CommonDiskTestCase):

    @classmethod
    def setup_class(cls):
        super(DiskTestCase, cls).setup_class()
        setup_queue2()

    @classmethod
    def teardown_class(cls):
        super(DiskTestCase, cls).teardown_class()

    def create_subtree(self, uid, root_folder, max_depth=3, max_files_count=5):
        """Создать поддерево ресурсов из ``max_depth`` папок и ``max_files_count`` файлов на
        каждом уровне. Начиная с ``root_folder``.

        :type uid: str
        :type root_folder: str
        :type max_depth: int
        :type max_files_count: int
        :rtype: list[str]
        """
        folder_names = [''.join(random.choice(string.ascii_uppercase) for _ in xrange(64)) for _ in xrange(max_depth)]
        subtree = []
        for folder in folder_names:
            root_folder = os.path.join(root_folder, folder)
            subtree.append(root_folder)
            self.json_ok('mkdir', {'uid': uid, 'path': root_folder})
            for i in range(max_files_count):
                file_ = os.path.join(root_folder, 'test_%d.txt' % i)
                self.upload_file(uid, path=file_)
                subtree.append(file_)
        return sorted(subtree)


class DiskTestCaseWithoutQueue(CommonDiskTestCase):
    pass


@contextlib.contextmanager
def patch_open_url(body):
    import mpfs.engine.http.client
    original_open_url = mpfs.engine.http.client.open_url

    def new_open_url(*args, **kwargs):
        return body
    mpfs.engine.http.client.open_url = new_open_url
    try:
        yield
    finally:
        mpfs.engine.http.client.open_url = original_open_url


@contextlib.contextmanager
def time_machine(new_date):
    """
    Контекстный менеджер для тестирования работы с разными датами
    Патчит метод datetime.now и time.time
    """
    import datetime
    original_datetime = datetime.datetime

    class DatetimeSubclassMeta(type):
        @classmethod
        def __instancecheck__(mcs, obj):
            return isinstance(obj, original_datetime)

    class NewDatetime(datetime.datetime):
        __metaclass__ = DatetimeSubclassMeta

        @classmethod
        def now(cls, tz=None):
            if isinstance(new_date, original_datetime):
                return new_date
            elif isinstance(new_date, datetime.date):
                return datetime.datetime.combine(new_date, datetime.datetime.min.time())
            else:
                raise RuntimeError('time_machine expects datetime.date or datetime.datetime objects (received: %s)' %
                                   type(new_date))

    datetime.datetime = NewDatetime

    import time
    original_time = time.time
    new_timestamp = time.mktime(new_date.timetuple())

    def new_time():
        return new_timestamp

    time.time = new_time

    try:
        yield
    finally:
        datetime.datetime = original_datetime
        time.time = original_time


def get_search_item_response(file_path, query):
    """Возвращает результат запроса ``query`` в поиск для файла ``file_name``

    :type file_path: str
    :type query: str
    :rtype: str
    """
    with open('fixtures/json/search_item_response.json') as fd:
        response = fd.read() % {'file_path': file_path, 'query': query}
        return json.dumps(json.loads(response))
