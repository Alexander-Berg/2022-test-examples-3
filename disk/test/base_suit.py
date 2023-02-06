# -*- coding: utf-8 -*-
"""
Базовый класс для всех тестов, здесь инициализируются все fixtures.
Каждый тестовый класс должен создавать окружение до выполнения всех
методов и уничтожать после.
"""
import copy
import hashlib
import os
import random
import string
import time
import traceback
import unittest
import datetime
import mock

from lxml import etree
from collections import defaultdict
from contextlib import contextmanager

from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.operations.filesystem.move import MoveOnDisk
from mpfs.core.operations.filesystem.store import StoreDisk
from mpfs.core.social.share import ShareProcessor
from mpfs.core.support.operations import GetFileChecksumsOperation
from mpfs.dao.session import Session
from mpfs.metastorage.postgres.query_executer import PGQueryExecuter
from mpfs.platform.utils import CaseInsensitiveDict

if 'MPFS_PACKAGE' not in os.environ:
    os.environ['MPFS_PACKAGE'] = 'disk'

from mpfs.engine.process import get_default_log
log = get_default_log()

import mpfs.engine.process
import mpfs.core
import mpfs.engine.http.client

from test.helpers.stubs.services import KladunStub, PreviewerStub
from test.fixtures.users import default_user, usr_1, usr_2, usr_3
from test.conftest import INIT_USER_IN_POSTGRES, run_delayed_async_tasks, delay_async_tasks

from mpfs.frontend.request import UserRequest
from mpfs.common.errors import NoShardForUidRoutingError
from mpfs.common.static.tags import COMMIT_FILE_INFO, COMMIT_FILE_UPLOAD, COMMIT_FINAL
from mpfs.common.util import from_json, to_json, wise_to_str
from mpfs.core import base as core
from mpfs.frontend.api.disk.billing import Billing
from mpfs.frontend.api.support_api import Support
from mpfs.frontend.api.disk.mail import Mail
from mpfs.frontend.api.disk.json import JSON
from mpfs.frontend.api.disk.service import Service
from mpfs.core.billing.api import service_delete
from mpfs.core.services import mulca_service
from mpfs.metastorage.mongo.mapper import POSTGRES_USER_INFO_ENTRY


class BaseTestCase(unittest.TestCase):
    def get_request(self, args=None):
        req = UserRequest({})
        if not args:
            args = {}
        req.set_args(args)
        return req

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def run(self, result=None):
        log.info('\n' + ' * ' * 15 + 'Test method: %s' % self._testMethodName + ' * ' * 15)
        unittest.TestCase.run(self, result=result)
        log.info('\n' + ' * ' * 40 + '\n\n')
        return result


class MpfsTestCase(BaseTestCase):
    def setup_method(self, method):
        self.run_000_user_check()


class FakeRequest(object):
    requestLine = ''
    http_resp = {}
    remote_addr = '127.0.0.1'
    headers = CaseInsensitiveDict()
    environ = {}
    json = None
    data = None


class FakeResponse(object):
    def __init__(self):
        self.status = 200
        self.headers = {}


class JsonApiTestCaseMixin(object):
    request = FakeRequest()
    response = FakeResponse()

    DELAY_ASYNC_TASKS = False

    def params(self, opts={}, fake_request_add_fields=None):
        opts = copy.deepcopy(opts)
        fake_request = FakeRequest()
        if fake_request_add_fields:
            for k, v in fake_request_add_fields:
                setattr(fake_request, k, v)
        opts['_request'] = self.request = fake_request
        opts['_response'] = self.response = FakeResponse()
        return opts

    def do_request(self, method, opts={}, api=None, json=None, request_line=None, headers=None, client_addr='127.0.0.1',
                   fake_request_add_fields=None):
        if request_line is None:
            request_line = '/json/'
        if api is None:
            api = JSON()

        # cast parameters to string to mimic real world request handling
        # https://st.yandex-team.ru/CHEMODAN-41611
        for opt in opts:
            value = opts[opt]
            if not isinstance(value, basestring):
                if value is None:
                    opts[opt] = ''
                else:
                    opts[opt] = str(value)

        params = self.params(opts, fake_request_add_fields=fake_request_add_fields)
        params['_request'].remote_addr = client_addr
        self.request.requestLine = request_line
        params['_request'].json = json
        params['_request'].data = to_json(json)
        self.request.environ['REQUEST_URI'] = request_line + method
        if headers:
            self.request.headers = CaseInsensitiveDict(headers)
        else:
            self.request.headers = {}
        api.setup(params)
        response = api.process(method)
        return api, response

    def json_ok(self, method, opts={}, result_type='none', api=None, json=None, request_line=None, headers=None,
                fake_request_add_fields=None, status=None, client_addr='127.0.0.1'):
        with delay_async_tasks(enable=self.DELAY_ASYNC_TASKS):
            api, response = self.do_request(method=method, opts=opts, api=api, json=json, request_line=request_line,
                                            headers=headers, fake_request_add_fields=fake_request_add_fields,
                                            client_addr=client_addr)

            if self.response.status == 500:
                self.fail("Internal Server Error: %s" % getattr(api.req, 'exc_traceback', ''))
            if response:
                result = from_json(response)
                if isinstance(result, dict) and ('code' in result and 'title' in result):
                    self.fail(result)
            else:
                result = response
            if result_type != 'none':
                self.assertEqual(type(result), result_type)

            if status is not None:
                assert self.response.status == status

        return result

    def json_error(self, method, opts={}, code=None, title=None, data=None, api=None, json=None,
                   request_line=None, headers=None, status=None, client_addr='127.0.0.1'):
        with delay_async_tasks(enable=self.DELAY_ASYNC_TASKS):
            api, response = self.do_request(method=method, opts=opts, api=api, json=json,
                                            request_line=request_line, headers=headers, client_addr=client_addr)
            if status is not None:
                error_text = "Wrong HTTP status code; expected status: %s, but actual status was: %s"\
                             % (status, self.response.status)

                if isinstance(api.formatter_obj.request.data['result'], Exception):
                    error_text += '\n Unexpected error: %s' % api.formatter_obj.request.data['result']

                self.assertEqual(
                    status,
                    self.response.status,
                    error_text
                )
            if response:
                result = from_json(response)
                self.assertEqual(type(result), dict, result)
                code_value = result.get('code')
                title_value = result.get('title')
                data_value = result.get('data')
                if code_value or title_value:
                    if code is not None:
                        self.assertEqual(code_value, int(code),
                                    "code: %s != %s, message: %s" % (code_value, code, title_value))
                    if title is not None:
                        self.assertEqual(title_value, str(title),
                                     "message: %s != %s, code: %s" % (title_value, title, code_value))
                    if data is not None:
                        self.assertEqual(data_value, data,
                                     "data: %s != %s, code: %s" % (data_value, data, code_value))
                    return result
                else:
                    self.fail(result)
            elif (code is not None or title is not None) and (response is None or response == ''):
                self.fail((code, title))

    def get_space(self, uid):
        return self.json_ok('user_info', {'uid': uid})['space']


class TrashTestCaseMixin(JsonApiTestCaseMixin):
    def get_trashed_item(self, name):
        trash = self.json_ok('list', {'uid': self.uid, 'path': '/trash/', 'meta': ''})
        return next(f for f in trash if f['path'].startswith(name))


class SupportApiTestCaseMixin(JsonApiTestCaseMixin):
    def support_ok(self, method, opts=None):
        if not opts:
            opts = {}
        support_api = Support()
        return self.json_ok(method, opts, api=support_api)

    def support_error(self, method, opts=None):
        if not opts:
            opts = {}
        support_api = Support()
        return self.json_error(method, opts, api=support_api)


class BillingApiTestCaseMixin(JsonApiTestCaseMixin):
    def billing_ok(self, method, opts={}, json=None, headers=None, status=None):
        billing_api = Billing()
        return self.json_ok(method, opts, api=billing_api, json=json, headers=headers, status=status)

    def billing_error(self, method, opts={}, json=None, code=None, status=None):
        billing_api = Billing()
        return self.json_error(method, opts, code=code, api=billing_api, status=status, json=json)

    def assertUserHasExactServices(self, uid, pids):
        services = self.billing_ok('service_list', {'uid': uid, 'ip': '127.0.0.1'})
        services = map(lambda j: j['name'], services)
        if set(services) != set(pids):
            raise self.failureException, "%s != %s" % (pids, services)

    def get_user_pids(self, uid):
        services = self.billing_ok('service_list', {'uid': uid, 'ip': '127.0.0.1'})
        return map(lambda j: j['name'], services)

    def assertUserHasServices(self, uid, pids):
        services = self.get_user_pids(uid)
        diff = set(pids) - set(services)
        if diff:
            raise self.failureException, "user %s hasn't following services %s" % (uid, list(diff))

    def assertUserNotHasServices(self, uid, pids):
        services = self.get_user_pids(uid)
        intersection = set(pids) & set(services)
        if len(intersection) > 0:
            raise self.failureException, "user %s has following services %s" % (uid, list(intersection))

    def unsubscribe_services(self, uid, services):
        """
        Unsubscribe user from services.
        :param uid:
        :param services: List of products names.
        :return:
        """
        args = {'uid': uid, 'ip': '127.0.0.1'}
        user_services = self.billing_ok('service_list', args)
        for service in user_services:
            s_sid = service['sid']
            s_name = service['name']
            if s_name in services:
                service_delete(uid, sid=s_sid, disable=True)


class UserOperationsTestCaseMixin(object):
    @staticmethod
    def make_random_string(length):
        return ''.join([random.choice(string.hexdigits) for _ in xrange(length)]).lower()

    def create_executing_move_operation(self, uid=default_user.uid):
        odata = {
            'force': 0,
            'target': '%s:/disk/file-%s.txt' % (uid, self.make_random_string(12)),
            'callback': '',
            'connection_id': '',
            'source': '%s:/disk/file.txt' % uid,
            'at_version': time.time(),
            'id': self.make_random_string(64)
        }
        operation = MoveOnDisk.Create(uid, odata)
        operation.set_executing()
        return operation.id

    def create_executing_store_operation(self, uid=default_user.uid):
        odata = {
            'set_public': None,
            'connection_id': '',
            'free_space': 10737418240,
            'path': '%s:/disk/file-%s.txt' % (uid, self.make_random_string(12)),
            'replace_md5': None,
            'id': self.make_random_string(64),
            'size': 8242923,
            'at_version': 1504614138197633,
            'callback': '',
            'client_type': 'json',
            'tld': 'ru',
            'sha256': '',
            'changes': {},
            'md5': 'af5c933769d9948b95c1ad2153e3c7bf'
        }
        operation = StoreDisk.Create(uid, odata)
        operation.set_executing()
        return operation.id

    def create_executing_support_operation(self, uid=default_user.uid):
        odata = {
            'id': self.make_random_string(64)
        }
        operation = GetFileChecksumsOperation.Create(uid, odata)
        operation.set_executing()
        return operation.id



class UserTestCaseMixin(object):
    """
    Common utilities to manipulate users in tests.
    """
    created_users_uids = set()
    uid = default_user.uid
    login = default_user.login
    display_name = default_user.display_name
    email = default_user.email
    user_1 = usr_1
    user_2 = usr_2
    user_3 = usr_3

    def create_user(self, uid, locale='ru', noemail=0, b2b_key=None, source=None, add_services=None, shard=None):
        """Create user."""
        req = UserRequest({})

        shard = POSTGRES_USER_INFO_ENTRY

        if isinstance(noemail, bool):
            noemail = int(noemail)
        req.set_args({
            'uid': str(uid),
            'locale': locale,
            'noemail': noemail,
            'shard': shard,
            'b2b_key': b2b_key,
            'source': source,
            'add_services': add_services,
        })
        core.user_init(req)
        self.created_users_uids.add(uid)

    def remove_created_users(self):
        """Clear storage from all users created via create_user."""

        while len(self.created_users_uids) > 0:
            uid = self.created_users_uids.pop()
            try:
                self.remove_user(uid)
            except NoShardForUidRoutingError:
                # Игнорируем, если пользователь уже удален
                pass

    def remove_user(self, uid):
        disk_db = CollectionRoutedDatabase()

        disk_db.group_links.remove({'uid': uid})
        disk_db.groups.remove({'owner': uid})
        disk_db.support_mpfs.remove({'uid': uid})

        usrctl = mpfs.engine.process.usrctl()
        if not usrctl.is_user_in_postgres(uid):
            disk_db.user_data.remove({'uid': uid})
            disk_db.changelog.remove({'uid': uid})
            disk_db.trash.remove({'uid': uid})
            disk_db.link_data.remove({'uid': uid})
            disk_db.hidden_data.remove({'uid': uid})
            disk_db.operations.remove({'uid': uid})
            disk_db.attach_data.remove({'uid': uid})
            disk_db.notes_data.remove({'uid': uid})
        usrctl.remove(uid, check_init=False)


class MailApiTestCaseMixin(object):
    def mail(self, method, opts={}):
        mail_api = Mail()
        params = self.params(opts)
        params['_request'].requestLine = '/mail/'
        mail_api.setup(params)
        return etree.fromstring(mail_api.process(method))

    def mail_ok(self, method, opts={}):
        resp = self.mail(method, opts)
        if resp.tag == 'response' and not int(resp.attrib['status']):
            self.fail('response status 0, expected 1\n' + etree.tostring(resp, pretty_print=True))
        else:
            return resp

    def mail_error(self, method, opts={}, code=None, title=None, status=None):
        resp = self.mail(method, opts)
        if resp.tag == 'response' and int(resp.attrib['status']):
            self.fail('response status 1, expected 0\n' + etree.tostring(resp, pretty_print=True))
        else:
            error_tag = resp.find('error')
            code_value = int(error_tag.find('code').text)
            message_value = str(error_tag.find('title').text)
            if code is not None:
                self.assertEqual(code_value, int(code),
                                "code: %s != %s, message: %s" % (code_value, code, message_value))
            if title is not None:
                self.assertEqual(message_value, str(title),
                                 "message: %s != %s, code: %s" % (message_value, title, code_value))
            if status is not None:
                self.assertEqual(self.response.status, status,
                                 "Wrong HTTP status code; expected status: %s, but actual status was: %s" % (
                                     status, self.response.status
                                 ))
            return resp


class ServiceApiTestCaseMixin(JsonApiTestCaseMixin):
    def service(self, method, opts={}, parse_resp=True, status=None):
        service_api = Service()
        service_api.setup(self.params(opts))
        response = service_api.process(method)
        received_status = service_api.formatter_obj.request.http_resp.status
        if status is not None and received_status not in status:
            self.fail(received_status)
        if response:
            result = from_json(response) if parse_resp else response
        else:
            result = False
        return result

    @staticmethod
    def _config_api(kwargs):
        kwargs = copy.deepcopy(kwargs)
        kwargs['api'] = Service()
        kwargs['request_line'] = '/service/'
        return kwargs

    def service_ok(self, *args, **kwargs):
        kwargs = self._config_api(kwargs)
        return self.json_ok(*args, **kwargs)

    def service_error(self, *args, **kwargs):
        kwargs = self._config_api(kwargs)
        return self.json_error(*args, **kwargs)


original_open_url = copy.deepcopy(mpfs.engine.http.client.open_url)
OLD_TESTS_XIVA_CALLBACK = 'http://localhost/service/echo?uid='
XIVA_PATCH = None


def set_up_open_url():
    requests = defaultdict(list)

    ###################
    # Дичь, чтобы не переписывать все тесты где проверяется пуши в формате xml
    # Перехватывает вызов `XivaSendService.send`, переконвертирует payload из
    # json в xml и кладет его в `xiva_requests`.
    def building_xml(data, parent_element=None):
        """
        Рекурсивная функция построения xml
        """
        def parse_data(data):
            """
            Разбор питоновой структуры
            """
            if not isinstance(data, dict):
                raise TypeError()

            parameters = data.get('parameters', {})
            if parameters:
                parameters = {str(k): wise_to_str(v) for k, v in parameters.iteritems()}
            value = data.get('value')
            if value:
                value = wise_to_str(value)
            return data['tag'], parameters, value, data.get('values')

        tag_name, parameters, text, children = parse_data(data)
        if parent_element is None:
            parent_element = etree.Element(tag_name, **parameters)
            current_element = parent_element
        else:
            current_element = etree.SubElement(parent_element, tag_name, **parameters)

        if children:
            for child in children:
                building_xml(child, current_element)
        elif text:
            current_element.text = text
        return parent_element

    def fake_xiva_send(*args, **kwargs):
        uid = args[1]
        json_payload = args[3]
        payload = from_json(json_payload)
        if 'root' in payload:
            root = payload.pop('root')
            payload.update(root)
        xml_payload = etree.tostring(building_xml(payload), pretty_print=False)
        requests[OLD_TESTS_XIVA_CALLBACK + uid].append({
            'args': [],
            'url_data': None,
            'cookie_data': None,
            'pure_data' : xml_payload,
        })
    XIVA_PATCH = mock.patch('mpfs.core.services.push_service.XivaSendService.send', new=fake_xiva_send)
    XIVA_PATCH.start()
    ###################

    def fake_open_url(*args, **kwargs):
        requests[args[0]].append({'args': args,
                                  'url_data': kwargs.get('url_data'),
                                  'cookie_data': kwargs.get('cookie_data'),
                                  'pure_data': kwargs.get('pure_data')})
        try:
            result = original_open_url(*args, **kwargs)
        except Exception:
            log.error(traceback.format_exc())
            result = {}
        return result or {}

    mpfs.engine.http.client.open_url = fake_open_url
    return requests


def tear_down_open_url():
    if XIVA_PATCH:
        XIVA_PATCH.stop()
    mpfs.engine.http.client.open_url = original_open_url


@contextmanager
def patch_http_client_open_url():
    """
    Патчим open url

    Возвращается словарь:
        ключи - урлы,
        значения - массив параметров запросов (один запрос - элемент массива)
    """
    try:
        yield set_up_open_url()
    finally:
        tear_down_open_url()


class UploadFileTestCaseMixin(MailApiTestCaseMixin, ServiceApiTestCaseMixin):
    uploaded_files = []
    default_latitude = 55.734243
    default_longitude = 37.589006

    def get_uploaded_files(self):
        return self.uploaded_files[::]

    def remove_uploaded_files(self):
        while len(self.uploaded_files) > 0:
            f = self.uploaded_files.pop()
            try:
                self.json_ok('rm', opts=f)
            except AssertionError:
                pass

    def hardlink_file(self, uid, original_path, hardlink_path, *args, **kwargs):
        """Создаёт хардлинк файла"""
        kwargs.pop('hardlink', None)
        original_info = self.json_ok('info', opts={'uid': uid, 'path': original_path, 'meta': ''})
        hardlink_data = dict([(k, original_info['meta'][k]) for k in ('md5', 'sha256', 'size')])
        hardlink_data.update(kwargs.pop('file_data', {}))
        return self.upload_file(uid, hardlink_path, file_data=hardlink_data, hardlink=True, *args, **kwargs)

    @contextmanager
    def patch_mulca_is_file_exist(self, func_resp=None):
        """
        Патчим проверку наличия файла в мульке
        """
        def tmp(*args):
            return func_resp
        original_func = mulca_service.Mulca.is_file_exist
        mulca_service.Mulca.is_file_exist = tmp
        try:
            yield
        finally:
            mulca_service.Mulca.is_file_exist = original_func

    def upload_file(self, uid, path, ok=True, connection_id='', open_url_data=None, force=1, file_data=None, opts=None,
                    media_type=None, hardlink=False, preview=True, callback_failed=False, live_photo_md5=None,
                    live_photo_sha256=None, live_photo_size=None, live_photo_type=None, live_photo_operation_id=None,
                    headers=None, nonexistent_stids_on_hardlink=(), regenerate_preview_result_on_hardlink=None,
                    internal_data=None, width=None, height=None, angle=None, json=None, photostream_destination=None,
                    return_result=False, error_on_store=False):
        try:
            path = path.decode('utf-8')
        except UnicodeEncodeError:
            pass
        if open_url_data is None:
            open_url_data = {}
        if file_data is None:
            file_data = {}
        if internal_data is None:
            internal_data = {}

        address = '%s:%s' % (uid, path)
        rand = str('%f' % time.time()).replace('.', '')[9:]  # FIXME: Для таймстемпа оканчивающегося на нули size будет 0 => упадет hardlink_copy с PreconditionsFailed.

        try:
            file_md5 = file_data['md5']
        except KeyError:
            file_md5 = hashlib.md5(rand).hexdigest()
        try:
            file_sha256 = file_data['sha256']
        except KeyError:
            file_sha256 = hashlib.sha256(rand).hexdigest()

        try:
            mimetype = file_data['mimetype']
        except KeyError:
            mimetype = 'application/x-www-form-urlencoded'

        try:
            drweb = file_data['drweb']
        except KeyError:
            drweb = 'true'

        try:
            size = file_data['size']
        except KeyError:
            size = int(rand)

        try:
            etime = file_data['etime']
        except KeyError:
            etime = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%dT%H:%M:%SZ')

        file_id = hashlib.sha256(file_md5 + ':' + file_sha256).hexdigest()
        try:
            mid_digest = file_data['digest_mid']
        except KeyError:
            mid_digest = '100000.yadisk:%s.%s' % (uid, int(file_md5[:16], 16))
        try:
            mid_file = file_data['file_mid']
        except KeyError:
            mid_file = '100000.yadisk:%s.%s' % (uid, int(file_md5[:16][::-1], 16))
        try:
            pmid = file_data['pmid']
        except KeyError:
            pmid = '100000.yadisk:%s.%s' % (uid, int(file_md5[:32][::2], 16))

        url_data = set_up_open_url()
        if opts is None:
            opts = {}
        opts.update({'uid': uid,
                     'path': address,
                     'force': force,
                     'md5': file_md5,
                     'sha256': file_sha256,
                     'size': str(size),
                     'callback': ''})
        if 'mtime' in file_data:
            opts['mtime'] = file_data['mtime']
        if connection_id:
            opts['connection_id'] = connection_id
        if live_photo_md5:
            opts['live_photo_md5'] = live_photo_md5
        if live_photo_sha256:
            opts['live_photo_sha256'] = live_photo_sha256
        if live_photo_size:
            opts['live_photo_size'] = live_photo_size
        if live_photo_type:
            opts['live_photo_type'] = live_photo_type
        if live_photo_operation_id is not None:
            opts['live_photo_operation_id'] = live_photo_operation_id
        if photostream_destination is not None:
            opts['photostream_destination'] = photostream_destination
        fos_app_version = opts.get('fos_app_version')
        fos_reply_email = opts.get('fos_reply_email')
        fos_recipient_type = opts.get('fos_recipient_type')
        fos_expire_seconds = opts.get('fos_expire_seconds')
        fos_os_version = opts.get('fos_os_version')
        fos_subject = opts.get('fos_subject')
        if not ok:
            self.mail_error('store', opts)
        else:
            if media_type == 'video':
                body_1 = etree.fromstring(open('fixtures/xml/kladun_video_1.xml').read())
                body_2 = etree.fromstring(open('fixtures/xml/kladun_video_2.xml').read())
                if preview:
                    body_2.find('stages').find('preview-video-mulca-upload').find('result').set('mulca-id', pmid)
                else:
                    elem = body_2.find('stages').find('preview-video-mulca-upload')
                    body_2.find('stages').remove(elem)
                body_3 = etree.fromstring(open('fixtures/xml/kladun_video_3.xml').read())
                if preview:
                    body_3.find('stages').find('preview-video-mulca-upload').find('result').set('mulca-id', pmid)
                else:
                    elem = body_3.find('stages').find('preview-video-mulca-upload')
                    body_3.find('stages').remove(elem)
            else:
                body_1 = etree.fromstring(open('fixtures/xml/kladun_store_1.xml').read())
                body_2 = etree.fromstring(open('fixtures/xml/kladun_store_2.xml').read())
                if preview:
                    body_2.find('stages').find('preview-image-mulca-upload').find('result').set('mulca-id', pmid)
                else:
                    elem = body_2.find('stages').find('preview-image-mulca-upload')
                    body_2.find('stages').remove(elem)
                body_3 = etree.fromstring(open('fixtures/xml/kladun_store_3.xml').read())
                if preview:
                    body_3.find('stages').find('preview-image-mulca-upload').find('result').set('mulca-id', pmid)
                else:
                    elem = body_3.find('stages').find('preview-image-mulca-upload')
                    body_3.find('stages').remove(elem)
                if etime:
                    body_3.find('stages').find('exif-info').find('result').set('creation-date', etime)
                    body_3.find('stages').find('media-info').find('result').set('creation-date', etime)
                else:
                    exif_info = body_3.find('stages').find('exif-info')
                    body_3.find('stages').remove(exif_info)
                    media_info = body_3.find('stages').find('media-info')
                    body_3.find('stages').remove(media_info)

                if width is not None:
                    body_3.find('stages').find('generate-image-one-preview').find('result').set('original-width', str(width))
                if height is not None:
                    body_3.find('stages').find('generate-image-one-preview').find('result').set('original-height', str(height))
                if angle is not None:
                    body_3.find('stages').find('generate-image-one-preview').find('result').set('rotate-angle', str(angle))

            for body in (body_1, body_2, body_3):
                body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
                body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
                body.find('request').find('chemodan-file-attributes').set('path', address)
                for arg in ('current', 'total'):
                    body.find('stages').find('incoming-http').find('progress').set(arg, str(size))
                for tag in ('incoming-http', 'incoming-file'):
                    body.find('stages').find(tag).find('result').set('content-length', str(size))
                    body.find('stages').find(tag).find('result').set('content-type', mimetype)
                body.find('stages').find('incoming-file').find('result').set('md5', file_md5)
                body.find('stages').find('incoming-file').find('result').set('sha256', file_sha256)
            for body in (body_2, body_3):
                body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file)
                body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest)
                body.find('stages').find('antivirus').find('result').set('result', drweb)
            if hardlink:
                def is_file_exist(stid):
                    if stid in set(nonexistent_stids_on_hardlink):
                        return False
                    else:
                        return True
                with mock.patch.object(mulca_service.Mulca, 'is_file_exist', side_effect=is_file_exist):
                    with KladunStub(status_values=(body_1,)):
                        with PreviewerStub(regenerate_preview_result=regenerate_preview_result_on_hardlink):
                            result = self.json_ok('store', opts, headers=headers)
            else:
                if error_on_store:
                    return self.json_error('store', opts, headers=headers, json=json)
                result = self.json_ok('store', opts, headers=headers, json=json)
            self.assertEqual(type(result), dict)
            if not (hardlink and result.get('status') == 'hardlinked'):
                self.assertTrue('oid' in result)
                oid = result['oid']
                self.assertTrue(oid)
                internal_data['oid'] = oid

                #===================================================================
                # Callback #1
                opts = {
                    'uid': uid,
                    'oid': oid,
                    'status_xml': etree.tostring(body_1),
                    'type': COMMIT_FILE_INFO,
                }
                if hardlink:

                    def is_file_exist(stid):
                        if stid in set(nonexistent_stids_on_hardlink):
                            return False
                        else:
                            return True

                    with mock.patch.object(mulca_service.Mulca, 'is_file_exist', side_effect=is_file_exist):
                        with KladunStub(status_values=(body_1,)):
                            with PreviewerStub(regenerate_preview_result=regenerate_preview_result_on_hardlink):
                                result = self.service_error('kladun_callback', opts, headers=headers)
                    self.assertEqual(result['code'], 94)
                else:
                    if callback_failed:
                        with KladunStub(status_values=(body_1,)):
                            self.service_error('kladun_callback', opts, headers=headers)
                    else:
                        with KladunStub(status_values=(body_1, body_2, body_3)):
                            self.service_ok('kladun_callback', opts, headers=headers)
                            # Callback #2
                            opts = {
                                'uid': uid,
                                'oid': oid,
                                'status_xml': etree.tostring(body_2),
                                'type': COMMIT_FILE_UPLOAD,
                            }
                            if path.startswith('/client/'):
                                opts['fos_reply_email'] = fos_reply_email
                                opts['fos_recipient_type'] = fos_recipient_type
                                opts['fos_app_version'] = fos_app_version
                                opts['fos_expire_seconds'] = fos_expire_seconds
                                opts['fos_os_version'] = fos_os_version
                                opts['fos_subject'] = fos_subject
                            self.service_ok('kladun_callback', opts, headers=headers)
                            # Callback #3
                            opts = {
                                'uid': uid,
                                'oid': oid,
                                'status_xml': etree.tostring(body_3),
                                'type': COMMIT_FINAL,
                            }
                            self.service_ok('kladun_callback', opts, headers=headers)

        tear_down_open_url()
        open_url_data.update(url_data)
        # сохраняем чтоб иметь возможность потом зачистить все созданные тестом файлы
        self.uploaded_files.append({'uid': uid, 'path': address})
        if return_result:
            return result
        return size

    def upload_file_with_coordinates(self, uid, path, latitude=default_latitude, longitude=default_longitude, **kwargs):
        result = self.upload_file(uid, path, **kwargs)
        if latitude and longitude:
            self.set_file_coordinates(uid, path, latitude, longitude)
        return result

    def set_file_coordinates(self, uid, path, latitude=default_latitude, longitude=default_longitude):
        session = Session.create_from_uid(uid)
        session.execute('''
            UPDATE disk.files
            SET ext_coordinates = POINT(:latitude, :longitude)
            WHERE
                uid = :uid AND
                fid = (SELECT fid FROM code.path_to_fid(:path, :uid))
        ''', {'uid': int(uid), 'path': path, 'latitude': latitude, 'longitude': longitude})

    def upload_video(self, *args, **kwargs):
        kwargs['media_type'] = 'video'
        return self.upload_file(*args, **kwargs)

    def dstore_file(self, uid, path, connection_id='', file_data=None, opts=None):
        if file_data is None:
            file_data = {}
        if opts is None:
            opts = {}
        address = '%s:%s' % (uid, path)
        rand = str('%f' % time.time()).replace('.', '')[9:]

        info_opts = {'uid': uid,
                     'path': path,
                     'meta': ''}
        file_info = self.json_ok('info', info_opts)
        original_file_md5 = file_info['meta']['md5']
        original_mid = file_info['meta']['file_mid']

        try:
            file_md5 = file_data['md5']
        except KeyError:
            file_md5 = hashlib.md5(rand).hexdigest()
        try:
            file_sha256 = file_data['sha256']
        except KeyError:
            file_sha256 = hashlib.sha256(rand).hexdigest()
        try:
            size = file_data['size']
        except KeyError:
            size = int(rand)

        file_id = hashlib.sha256(file_md5 + ':' + file_sha256).hexdigest()
        mid_digest = '100000.yadisk:138710986.%s' % int(file_md5[:16], 16)
        mid_file = '100000.yadisk:138710986.%s' % int(file_md5[:16][::-1], 16)
        dstore_opts = {'uid': uid,
                       'path': address,
                       'md5': original_file_md5,
                       'callback': ''}
        dstore_opts.update(opts)
        if connection_id:
            dstore_opts['connection_id'] = connection_id

        result = self.json_ok('dstore', dstore_opts)
        self.assertTrue('oid' in result)
        oid = result['oid']
        self.assertTrue(oid)
        body_1_dstore = etree.fromstring(open('fixtures/xml/kladun_dstore_1.xml').read())
        body_2_dstore = etree.fromstring(open('fixtures/xml/kladun_dstore_2.xml').read())
        body_3_dstore = etree.fromstring(open('fixtures/xml/kladun_dstore_3.xml').read())
        for body in (body_1_dstore, body_2_dstore, body_3_dstore):
            body.find('request').set('original-md5', original_file_md5)
            body.find('request').set('original-mulca-id', original_mid)
            body.find('request').find('chemodan-file-attributes').set('uid', str(uid))
            body.find('request').find('chemodan-file-attributes').set('file-id', file_id)
            body.find('request').find('chemodan-file-attributes').set('path', address)
            for arg in ('current', 'total'):
                body.find('stages').find('incoming-http').find('progress').set(arg, str(size))
            body.find('stages').find('expected-patched-md5').find('result').set('md5', file_md5)
            body.find('stages').find('patched-file').find('result').set('md5', file_md5)
            body.find('stages').find('patched-file').find('result').set('sha256', file_sha256)
            body.find('stages').find('patched-file').find('result').set('content-length', str(size))
        for body in (body_2_dstore, body_3_dstore):
            body.find('stages').find('mulca-file').find('result').set('mulca-id', mid_file)
            body.find('stages').find('mulca-digest').find('result').set('mulca-id', mid_digest)

        with KladunStub(status_values=(body_1_dstore, body_2_dstore, body_3_dstore)):
            # Callback #1 dstore
            opts = {
                'uid': uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1_dstore),
                'type': COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)

            # Callback #2 dstore
            opts = {
                'uid': uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2_dstore),
                'type': COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)

            # Callback #3 dstore
            opts = {
                'uid': uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3_dstore),
                'type': COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

        return size

class SharingTestCaseMixin(JsonApiTestCaseMixin):
    def create_shared_folder(self, owner_uid, path, shared_uids):
        self.json_ok('mkdir', {'uid': owner_uid, 'path': path})
        group = self.json_ok('share_create_group', opts={'uid': owner_uid, 'path': path})

        for uid in shared_uids:
            invite_hash = self.share_invite(group['gid'], uid)
            self.json_ok('share_activate_invite', opts={'uid': uid, 'hash': invite_hash})

        return group

    def check_uids_in_group(self, gid, owner_id, uids):
        actual_uids = []

        for i, x in enumerate(ShareProcessor.list_users_in_group(gid, owner_id)):
            actual_uids.append(x.owner if i == 0 else x.uid)

        assert sorted(actual_uids) == sorted(uids)

    def share_invite(self, gid, uid, rights=660, login='mpfs-test@yandex.ru'):
        """
        Создаёт приглашение для пользователя с идентификатором `uid` в группу с идентификатором `gid`.

        :param gid:
        :param uid:
        :param rights:
        :return: Возвращает идентификатор (хэш) приглашения.
        """
        db = CollectionRoutedDatabase()
        group = db.groups.find_one({'_id': gid})
        args = {
            'rights': rights,
            'universe_login': login,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': group['owner'],
            'path': group['path'],
        }
        result = self.json_ok('share_invite_user', args)
        invite_hash = result['hash']
        # дописываем в приглашение нужный uid.
        db.group_invites.update({'_id': invite_hash}, {'$set': {'uid': uid}})
        return invite_hash


class BaseApiTest(MpfsTestCase):
    request = FakeRequest()
    response = FakeResponse()

    def setUp(self):
        # вырубаем rate limiter
        from mpfs.core.services import rate_limiter_service
        rate_limiter_service.IS_RATE_LIMITER_ENABLED = False


if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()
