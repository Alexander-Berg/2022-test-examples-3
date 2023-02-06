# -*- coding: utf-8 -*-
import json
import mock
import pytest

from mpfs.core.billing import Market
from mpfs.core.billing.processing.pushthelimits import can_pushto_10gb
from test.base import DiskTestCase
from nose_parameterized import parameterized
from hamcrest import assert_that, contains_inanyorder, is_, empty, equal_to

import mpfs.engine.process
from test.helpers.products import INITIAL_5GB, APP_INSTALL
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import PassportStub, PassportResponseMock

from mpfs.core import base as core
from mpfs.core.metastorage.control import disk_info
from mpfs.common import errors
from mpfs.core.billing.api import service_delete
from mpfs.core.billing.processing import pushthelimits
from mpfs.core.billing.processing.common import simple_create_service
from mpfs.core.billing.client import Client
from mpfs.core.billing.product import Product
from mpfs.core.job_handlers.user import handle_post_modify_user_settings
from mpfs.core.queue import mpfs_queue
from mpfs.common.util import hashed
from mpfs.core.user.constants import PREDEFINED_CONTENT_FOLDERS_2014
from test.fixtures.users import (
    user_4,
    turkish_user,
    user_1,
    user_3,
    user_with_plus,
    test_user,
)
from test.helpers import products, locale
from test.helpers.size_units import GB
from test.conftest import capture_queue_errors
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class MiscSpaceBonusUserTestCase(DiskTestCase):
    belarus_user = '12'
    turkey_user = '13'
    localhost = 'http://localhost/service/echo'

    def teardown_method(self, method):
        self.remove_created_users()
        super(MiscSpaceBonusUserTestCase, self).teardown_method(method)

    def test_create_turkey_user(self):
        args = {'uid': self.turkey_user,
                'locale': locale.TR_TR,
                'noemail': 0,
                'shard': None}
        core.user_init(self.get_request(args))
        args = {'uid' : self.turkey_user,
                'path': self.turkey_user + ':/disk',
                'meta': {}}
        content = map(lambda x: x.get('id'),
                      core.content(self.get_request(args))['list'])

        assert_that(content,
                    contains_inanyorder(*PREDEFINED_CONTENT_FOLDERS_2014[locale.TR_TR]))

    def test_create_user_with_plus(self):
        """Проверяем наличие услуги и общий объем места у пользователя с подпиской Яндекс.Плюс"""
        with mock.patch('mpfs.core.user.standart.FEATURE_TOGGLES_ADD_YANDEX_PLUS_SERVICE_ON_USER_INIT', True):
            core.user_init(self.get_request({'uid': user_with_plus.uid,
                                             'locale': locale.RU_RU,
                                             'noemail': 0,
                                             'shard': None}))

        service_list = self.billing_ok('service_list', {'uid': user_with_plus.uid, 'ip': self.localhost})
        assert products.YANDEX_PLUS.id in {x['name'] for x in service_list}

        actual_limit = self.space_limit(user_with_plus.uid)
        assert actual_limit == products.INITIAL_10GB.amount + products.YANDEX_PLUS.amount

    def test_does_not_create_user_with_plus_when_feature_is_disabled(self):
        with mock.patch('mpfs.core.user.standart.FEATURE_TOGGLES_ADD_YANDEX_PLUS_SERVICE_ON_USER_INIT', False):
            core.user_init(self.get_request({'uid': user_with_plus.uid,
                                             'locale': locale.RU_RU,
                                             'noemail': 0,
                                             'shard': None}))

        service_list = self.billing_ok('service_list', {'uid': user_with_plus.uid, 'ip': self.localhost})
        assert products.YANDEX_PLUS.id not in {x['name'] for x in service_list}

        actual_limit = self.space_limit(user_with_plus.uid)
        assert actual_limit == products.INITIAL_10GB.amount


    def test_create_belarus_user(self):
        args = {'uid': self.belarus_user,
                'locale': 'by',
                'noemail': 0,
                'shard': None}
        core.user_init(self.get_request(args))
        args = {'uid': self.belarus_user,
                'path': self.belarus_user + ':/disk',
                'meta': None}
        content = map(lambda x: x.get('id'),
                      core.content(self.get_request(args))['list'])

        assert_that(content,
                    contains_inanyorder(*PREDEFINED_CONTENT_FOLDERS_2014[locale.RU_RU]))

    def test_turkish_user_before_may_2012(self):
        # Регистрируем
        args = {'uid': turkish_user.uid,
                'locale': locale.TR_TR,
                'noemail': 0,
                'shard': None}
        core.user_init(self.get_request(args))

        # Проверяем
        limit = self.space_limit(turkish_user.uid)
        self.assertEqual(products.INITIAL_10GB.amount + products.TURKISH_USER.amount, limit)

    @parameterized.expand([(products.YANDEX_STAFF.id, products.YANDEX_STAFF.amount),
                           (products.PASSPORT_SPLIT.id, products.PASSPORT_SPLIT.amount),
                           (products.TURKEY_PROJE_Y.id, products.TURKEY_PROJE_Y.amount),
                           (products.YANDEX_BROWSER.id, products.YANDEX_BROWSER.amount),
                           (products.YANDEX_SHAD.id, products.YANDEX_SHAD.amount),
                           (products.TURKEY_PANORAMA.id, products.TURKEY_PANORAMA.amount)])
    def test_yandex_product(self, bonus_id, bonus_amount):
        request = self.get_request({'uid': self.uid, 'key': bonus_id,
                                    'project': 'disk', 'value': '1',
                                    'type': 'states', 'namespace': None})
        old_limit = self.space_limit(self.uid)
        core.set_user_var(request)
        new_limit = self.space_limit(self.uid)
        self.assertEqual(old_limit + bonus_amount, new_limit)

        # Убираем бонус
        old_limit = self.space_limit(self.uid)
        core.remove_user_var(request)
        new_limit = self.space_limit(self.uid)
        self.assertEqual(old_limit - bonus_amount, new_limit)

        # Проверяем, что повторная попытка убрать бонус окажется неудачной
        self.assertRaises(errors.SettingStateNotFound,
                          core.remove_user_var, request)

    @parameterized.expand([(products.YANDEX_EGE.id, products.YANDEX_EGE.amount),
                           (products.BLAT_250.id, products.BLAT_250.amount)])
    def test_additional_yandex_product(self, bonus_id, bonus_amount):
        user_info = self.json_ok('user_info', {'uid': self.uid})
        space_before = user_info['space']['limit']
        if 'global' in user_info['states']:
            self.assertFalse(bonus_id in user_info['states']['global'])

        opts = {'uid': self.uid,
                'key': bonus_id,
                'value': '1'}
        self.json_ok('state_set', opts)
        user_info = self.json_ok('user_info', {'uid' : self.uid})
        space_after = user_info['space']['limit']

        self.assertEqual(space_before + bonus_amount, space_after)
        self.assertTrue(bonus_id in user_info['states']['global'])
        self.assertEqual(user_info['states']['global'][bonus_id], '1')

        opts = {'uid': self.uid,
                'ip': self.localhost}
        service_list = self.billing_ok('service_list', opts)
        found = False
        for service in service_list:
            if service['name'] == bonus_id:
                found = True
                self.assertEqual(service['size'], bonus_amount)
                self.assertTrue('expires' in service)

        self.assertTrue(found)

    def test_initial_5gb(self):
        uid = test_user.uid

        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True):
            self.json_ok('user_init', {'uid': uid})

        limit = self.space_limit(uid)
        assert_that(limit, equal_to(products.INITIAL_5GB.amount))

    def test_cant_install_get_services_with_initial_5gb(self):
        uid = test_user.uid

        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True):
            self.json_ok('user_init', {'uid': uid})

        assert_that(can_pushto_10gb(uid, APP_INSTALL.id), equal_to(False))


class UserCheckTestCase(DiskTestCase):
    stubs_manager = StubsManager(
        class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {PassportStub}
    )
    uid = test_user.uid

    def test_check_user_init_for_standard_user(self):
        with PassportResponseMock():
            resp = self.json_ok('can_init_user', {'uid': self.uid})
        assert resp['can_init'] == '1'

    @parameterized.expand([('mailish', {'12': 'fluffy.enot.from.gmail'}, False, '1'),
                           ('without_portal', {}, False, '0'),
                           ('pdd', {}, True, '1'),
                           ('with_portal', {'1': 'yandex.login.for.norma.user'}, False, '1'),])
    def test_check_user_init_for_user_without_portal_login(self, case_name, aliases, is_pdd, expected_result):
        from mpfs.core.services.passport_service import Passport
        Passport.reset()

        def render_response_func(template):
            response = json.loads(template.render())
            user = response['users'][0]
            user['aliases'] = aliases
            user['uid']['hosted'] = is_pdd
            return json.dumps(response)

        with PassportResponseMock(render_response_func=render_response_func):
            resp = self.json_ok('can_init_user', {'uid': self.uid})

        assert resp['can_init'] == expected_result


class UserStatesTestCase(DiskTestCase):
    def test_first_file(self):
        opts = {'uid': self.uid}
        user_info = self.json_ok('user_info', opts)
        self.assertEqual(user_info['space']['used'], 0)
        self.assertTrue('first_file' not in user_info.get('states', {}).get('global', {}))
        size = self.upload_file(self.uid, '/disk/readme.txt')
        user_info = self.json_ok('user_info', opts)
        self.assertEqual(user_info['space']['used'], size)
        self.assertEqual(user_info['states']['global']['first_file'], 1)

    def test_set_clid(self):
        clid = hashed(self.uid)
        opts = {'uid': self.uid,
                'key': 'clid',
                'value': clid,
                'namespace': 'app'}
        self.json_ok('state_set', opts)
        result = self.json_ok('user_info', {'uid' : self.uid})
        self.assertEqual(result['states']['app']['clid'], clid)
        status = db.disk_info.find_one({'uid': self.uid, 'key': '/states/disk/app'})
        self.assertEqual(dict(status['data'])['clid'], clid)
        new_clid = hashed(0)
        opts = {'uid': self.uid,
                'key': 'clid',
                'value': new_clid,
                'namespace': 'app'}
        self.json_ok('state_set', opts)
        result = self.json_ok('user_info', {'uid' : self.uid})
        self.assertEqual(result['states']['app']['clid'], new_clid)
        status = db.disk_info.find_one({'uid' : self.uid, 'key' : '/states/disk/app'})
        self.assertEqual(dict(status['data'])['clid'], new_clid)


class PushLimitTestCase(DiskTestCase):
    def run_000_user_check(self, uid=None):
        """Не создаем юзера"""
        pass

    def check_pushthelimitsto_10gb(self):
        # проверяем все ли нужные сервисы подключились
        self.assertUserHasServices(self.uid, pushthelimits.PRODUCTS_TO_PUSH_THE_LIMITS_TO_10GB)
        # проверяем размер диска пользователя
        user_limit = disk_info.show_single(self.uid, '/limit', None).get('data')
        self.assertEqual(user_limit, products.INITIAL_10GB.amount)


class PushUserLimitTo10GBTestCase(PushLimitTestCase):
    uid_1 = user_1.uid
    uid_3 = user_3.uid

    def check_user_init(self, uid, pids=None, limit=None):
        if pids:
            # проверяем подкючился ли правильный сервис
            self.assertUserHasExactServices(uid, pids)
        if limit:
            # проверяем размер диска пользователя
            user_limit = disk_info.show_single(uid, '/limit', None).get('data')
            self.assertEqual(user_limit, limit)

    def test_pushthelimitsto_10gb_on_user_with_init10gb(self):
        self.create_user(self.uid_1)
        self.json_ok('pushthelimitsto_10gb', {'uid': self.uid_1})
        user_limit = disk_info.show_single(self.uid_1, '/limit', None).get('data')
        self.assertEqual(user_limit, products.INITIAL_10GB.amount)

    def test_user_init_with_initial_10gb_service(self):
        self.create_user(self.uid_3)
        self.check_user_init(self.uid_3,
                             pids=[products.INITIAL_10GB.id], limit=products.INITIAL_10GB.amount)
        # проверяем не подключится ли ещё один сервис
        # если user_init дёрнуть ещё раз для существующего пользователя
        self.create_user(self.uid_3)
        self.check_user_init(self.uid_3,
                             pids=[products.INITIAL_10GB.id], limit=products.INITIAL_10GB.amount)


@pytest.mark.skipif(True, reason='No more users without initial_10gb')
class CanonicalUserTestCase(PushLimitTestCase):
    CANONICAL_USER_STORAGE_LIMIT = 3 * GB
    """Лимит для пользователя без подключенных услуг начальных гигабайт"""

    uid = user_4.uid

    def run_000_user_check(self, uid=None):
        """Создаем пользователя с 3GB"""
        self.create_canonical_user(self.uid)

    def make_user_canonical(self, uid):
        # default 10GB service будет у всех в выдаче
        # но у старых "каноничных" пользователей лимит будет 3GB
        disk_info.put(uid, '/limit', self.CANONICAL_USER_STORAGE_LIMIT)

    def create_canonical_user(self, uid, locale='ru', noemail=0):
        self.create_user(uid, locale=locale, noemail=noemail)
        self.make_user_canonical(uid)

    def test_pushthelimitsto_10gb_on_canonical_user(self):
        self.json_ok('pushthelimitsto_10gb', {'uid': self.uid})
        self.check_pushthelimitsto_10gb()
        # проверяем не добавятся ли сервисы ещё раз если они у нас уже есть, если дёрнуть ручку
        self.json_ok('pushthelimitsto_10gb', {'uid': self.uid})
        self.check_pushthelimitsto_10gb()

    def test_denny_bonus_for_desktop_installed_manually(self):
        """Бонус не должен быть выдан при установке ПО посредствам set_state.

        Детали: https://st.yandex-team.ru/CHEMODAN-11680
        """
        args = {'uid': self.uid,
                'key': 'desktop_installed',
                'project': 'disk',
                'value': '1',
                'type': 'states',
                'namespace': None}
        old_limit = self.space_limit(self.uid)
        self.assertNotEqual(old_limit, products.INITIAL_10GB.amount)

        core.set_user_var(self.get_request(args))
        new_limit = self.space_limit(self.uid)

        self.assertEqual(old_limit, new_limit)

    def test_device_installation(self):
        args = {'uid': self.uid, 'id': 'WANNA 3G RIGHT NOW', 'project': 'disk',
                'type': 'desktop', 'info': {'os': 'os/2'}}
        old_limit_db = db.disk_info.find_one({'uid': str(self.uid), 'key': '/limit'})['data']
        old_limit = self.space_limit(self.uid)
        self.assertEqual(old_limit, old_limit_db)

        core.user_install_device(self.get_request(args))
        new_limit = self.space_limit(self.uid)
        new_limit_db = db.disk_info.find_one({'uid': str(self.uid), 'key': '/limit'})['data']
        self.assertEqual(new_limit, new_limit_db)
        self.assertEqual(old_limit_db + products.CLIENT.amount, new_limit_db)
        self.assertEqual(old_limit + products.CLIENT.amount, new_limit)

    def test_webdav_used(self):
        args = {'uid': self.uid,
                'key': 'webdav_used',
                'project': 'disk',
                'value': '1',
                'type': 'states',
                'namespace': None}
        old_limit = self.space_limit(self.uid)
        core.set_user_var(self.get_request(args))
        new_limit = self.space_limit(self.uid)

        self.assertEqual(old_limit + products.CLIENT.amount, new_limit)

    def test_promo_sharing(self):
        old_limit = self.json_ok('user_info', {'uid': self.uid})['space']['limit']
        self.json_ok('state_set', {'uid': self.uid,
                'key': products.PROMO_SHARED.id,
                'project': 'disk',
                'value': '1'})
        new_limit = self.json_ok('user_info', {'uid': self.uid})['space']['limit']

        self.assertEqual(old_limit + products.PROMO_SHARED.amount, new_limit)

    def test_mobile_installed(self):
        args = {'uid': self.uid,
                'key': 'mobile_installed',
                'project': 'disk',
                'value': '1',
                'type': 'states',
                'namespace': None}
        old_limit = self.space_limit(self.uid)
        core.set_user_var(self.get_request(args))
        new_limit = self.space_limit(self.uid)

        self.assertEqual(old_limit + products.CLIENT.amount, new_limit)

    def test_file_uploading(self):
        args = {'uid': self.uid,
                'key': products.FILE_UPLOADED.id,
                'project': 'disk',
                'value': '1',
                'type': 'states',
                'namespace': None}
        old_limit = self.space_limit(self.uid)
        core.set_user_var(self.get_request(args))
        new_limit = self.space_limit(self.uid)

        self.assertEqual(old_limit + products.FILE_UPLOADED.amount, new_limit)

    def test_user_install_device_does_not_lead_to_error_when_user_has_app_install(self):
        """
            Проверяем, что таск handle_post_modify_user_settings не падает с ошибкой
            после вызова user_install_device, если у пользователя уже есть услуга app_install

            https://st.yandex-team.ru/CHEMODAN-33756
        """
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        for service in services:
            service_delete(self.uid, sid=service['sid'], disable=True)

        client = Client(self.uid)
        for p in ('app_install', 'initial_3gb'):
            simple_create_service(client, Product(p))

        with capture_queue_errors() as errors:
            self.json_ok('user_install_device', {'uid': self.uid, 'type': 'mobile', 'value': 1, 'id': '1'})

        assert_that(errors, is_(empty()))

    def test_handle_post_modify_user_settings_does_not_fail_when_removing_nonexistent_state(self):
        """
            Проверяем, что таск handle_post_modify_user_settings не падает с ошибкой при удалении состояния,
            которого у пользователя нет

            https://st.yandex-team.ru/CHEMODAN-33956
        """
        nonexistent_state = 'mobile_installed'
        handle_post_modify_user_settings.apply_async((self.uid, nonexistent_state, 'states', 'remove', 'disk',
                                                      None, 'global'))

    def test_autobind_market_on_user_init(self):
        args = {'uid': self.uid,
                'locale': 'ru',
                'noemail': 0,
                'shard': None}
        core.user_init(self.get_request(args))
        client = Client(self.uid)
        # Маркет должен быть задан
        assert client.attributes.market == Market.DEFAULT
