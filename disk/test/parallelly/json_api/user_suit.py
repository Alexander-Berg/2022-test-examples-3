# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime
import json
from copy import deepcopy

import pytest
import mock
import time
from nose_parameterized import parameterized

from mpfs.common.util import ctimestamp
from mpfs.common.util.video_unlim import COUNTRY_KEY, COUNTRY_FIELD
from mpfs.config import settings
from mpfs.core.albums.static import FacesIndexingState
from mpfs.core.metastorage.control import disk_info
from mpfs.core.user.base import User
from mpfs.metastorage.mongo.collections.filesystem import UserDataCollection
from test.base_suit import SharingTestCaseMixin
from test.helpers import products
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.helpers.size_units import GB

import mpfs.engine.process

from test.conftest import INIT_USER_IN_POSTGRES
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PassportStub

from mpfs.dao.session import Session
from mpfs.core import base
from mpfs.core.user import constants
from mpfs.core.services.passport_service import Passport, blackbox
from mpfs.core.filesystem.quota import Quota
from mpfs.core.billing.processing.common import simple_create_service, simple_delete_service
from mpfs.core.billing.client import Client
from mpfs.core.billing import Product, Service
from mpfs.core.billing.service import ServiceList
from mpfs.common import errors
from mpfs.common.static import SPACE_1GB
from mpfs.common.static import codes
from mpfs.common.static.tags.billing import CLIENT
from mpfs.frontend import api
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.metastorage.mongo.collections.system import DiskInfoCollection
from mpfs.core.user.standart import StandartUser
from mpfs.core.user_activity_info.dao import UserActivityDAOItem
from test.base import time_machine


db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class UserJsonApiTestCase(CommonJsonApiTestCase):
    def test_user_info(self):
        """
        Тестируем user_info
        """
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert 'db' in user_info
        assert 'space' in user_info
        assert 'version' in user_info
        assert 'locale' in user_info
        assert user_info['paid'] == 0
        assert 'trash_autoclean_period' in user_info

        version_from_user_info = user_info['version']
        version_from_db = db.user_index.find_one({'_id': self.uid})['version']

        assert version_from_user_info == version_from_db

        #проверяем работу признака платности пользователя
        opts = {'uid': self.uid, 'line': 'primary_2014', 'pid': '10gb_1m_2014', 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1

        #проверяем, что user_info работает для аттачевых
        self.remove_user(self.uid)
        self.upload_file(self.uid, "/attach/file")

        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert 'db' in user_info

    @parameterized.expand([
        (True, True, 'ru', FacesIndexingState.REINDEXED),
        (True, True, 'en', FacesIndexingState.COUNTRY_RESTRICTION),
        (True, False, 'ru', None),
        (False, True, 'ru', FacesIndexingState.REINDEXED),
        (False, True, 'en', FacesIndexingState.COUNTRY_RESTRICTION),
        (False, False, 'ru', None)
    ])
    def test_faces_enabled_on_user_init(self, reg_with_lock, exp_enabled, country, faces_indexing_state):
        uid = self.user_1.uid
        userinfo = DEFAULT_USERS_INFO[uid]
        userinfo['country'] = country

        with mock.patch('mpfs.common.util.experiments.logic.experiment_manager.is_feature_active', return_value=exp_enabled), \
             mock.patch.dict('mpfs.config.settings.feature_toggles', {'user_init_lock_percentage': 100 if reg_with_lock else 0}), \
             PassportStub(userinfo=userinfo):

            db_info = usrctl.check(uid)
            assert db_info is None
            self.json_ok('user_init', {'uid': uid})
            db_info = usrctl.check(uid)
            assert db_info['faces_indexing_state'] == faces_indexing_state

            if faces_indexing_state == FacesIndexingState.REINDEXED:
                # там должна быть текущая дата, проверяем, что она возле текущей
                assert db_info['faces_indexing_state_time'] >= ctimestamp() - 5
            else:
                assert db_info['faces_indexing_state_time'] is None

    def test_add_space_for_sso(self):
        uid = self.user_1.uid
        self.json_ok('user_init', {'uid': uid})
        userinfo = self.json_ok('user_info', {'uid': uid})
        assert userinfo['space']['limit'] == SPACE_1GB * 10

    @parameterized.expand([
        ('simple pdd user', '567999', None, 10, 1010),
        ('sso user', '567213', '567213/login', 110, 1010),
        ('alien sso user', '567888', '567888/login', 10, 1010),
    ])
    def test_add_space_for_sso(self, casename, domain_id, sso_user, expected_space_gb, expected_space_gb_after_psbilling):
        uid = self.user_1.uid

        PassportStub.update_info_by_uid(uid, domain_id=domain_id, sso_user=sso_user)
        new_config = deepcopy(settings.billing)
        new_config['sso_organization_space'] = ['567213:107374182400']
        with mock.patch('mpfs.config.settings.billing', new=new_config):
            self.json_ok('user_init', {'uid': uid})
            assert self.space_limit_gb(uid) == expected_space_gb

        # проверяем, что после выдачи услуги из пс-биллинга мы перезапишем место
        sid = self.billing_ok(
            'service_create_for_ps_billing',
            {'uid': uid, 'line': 'partner', 'pid': 'yandex_b2b_mail_pro', 'product.amount': GB * 1000,
             'auto_init_user': 1, 'ip': '127.0.0.1'},
        )['sid']
        self.billing_ok(
            'service_set_attribute',
            {'uid': uid, 'sid': sid, 'key': 'product.amount', 'value': GB * 1000, 'ip': '127.0.0.1'},
        )
        assert self.space_limit_gb(uid) == expected_space_gb_after_psbilling

    @parameterized.expand([
        ('567213:107374182400', 110),
        ('567213notanumber:107374182400', 10),
        ('567213:107374182400notanumber', 10),
        ('567213', 10),
        ('567213:123:321', 10),
        ('random string', 10),
    ])
    def test_add_space_for_sso_incorrect_config(self, config, expected_space_gb):
        uid = self.user_1.uid

        PassportStub.update_info_by_uid(uid, domain_id='567213', sso_user='567213/login')
        new_config = deepcopy(settings.billing)
        new_config['sso_organization_space'] = [config]
        with mock.patch('mpfs.config.settings.billing', new=new_config):
            self.json_ok('user_init', {'uid': uid})
            assert self.space_limit_gb(uid) == expected_space_gb

    @mock.patch.dict(settings.services['disk'], {'default_limit': 0,
                                                 'filesize_limit': 10737418240,
                                                 'paid_filesize_limit': 53687091200,
                                                 'timeout': 1.0,
                                                 'unique_items': True})
    def test_is_overdrawn_in_user_info(self):
        """
        Тестируем значение атрибута is_overdrawn в user_info
        """
        Quota().set_limit(SPACE_1GB * 10, uid=self.uid)
        file_data = {'size': int(SPACE_1GB)*int(9.9)}
        path = '/disk/file.dat'
        self.upload_file(self.uid, path, file_data=file_data)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        self.assertEqual(user_info['is_overdrawn'], 0)
        Quota().set_limit(SPACE_1GB * 5, uid=self.uid)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        self.assertEqual(user_info['is_overdrawn'], 1)

    def test_init_saves_locale(self):
        """
        Убеждаемся, что user_init для существующего пользователя не меняет локаль
        """
        user_info = self.json_ok('user_info', {'uid': self.uid})
        # локаль дефолтного юзера должна быть 'ru'
        assert user_info['locale'] == 'ru'
        opts = {
            'uid': self.uid,
            'locale': 'en'
        }
        self.json_ok('user_init', opts)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['locale'] == 'ru'

    def test_user_init_saves_country(self):
        self.json_ok('user_info', {'uid': self.uid})
        country_info = disk_info.find_one_by_field(self.uid, {'key': COUNTRY_KEY})
        assert country_info['data'][COUNTRY_FIELD]


    def test_init_does_not_update_reg_time(self):
        """
        Убеждаемся, что user_init для существующего пользователя не меняет время регистрации
        """
        uid = self.user_1.uid
        create_dt = datetime.datetime(2018, 3, 12, 18, 34, 46, 98201)
        update_dt = create_dt + datetime.timedelta(seconds=100)
        create_time = time.mktime(create_dt.timetuple())
        with time_machine(create_dt):
            self.create_user(uid)
        user_info = self.json_ok('user_info', {'uid': uid})
        # проверяем, что при создании пользователя время регистрации проставляется правильно
        assert create_time == user_info['reg_time']
        opts = {
            'uid': uid,
        }
        with time_machine(update_dt):
            self.json_ok('user_init', opts)
        user_info = self.json_ok('user_info', {'uid': uid})
        # проверяем, что при повторном вызове user_init время регистрации не изменилось
        assert create_time == user_info['reg_time']

    def test_awaps_user_info(self):
        opts = {
            'uid': self.uid,
        }
        result = self.json_ok('awaps_user_info', opts)
        assert 'shared_folders' in result
        assert 'photostream_used' in result
        assert 'screenshot' in result
        assert 'paid_services' in result
        assert 'all_invites_activated' in result
        assert 'public_links' in result

    def test_default_tld(self):
        """Проверить, что дефолтный параметр tld устанавливается в атрибуты JSONRequest.
        """
        with mock.patch.object(base, 'info', return_value={}) as mocked_info:
            # Отвалится из-за неправильного return_value, но нам все равно
            self.json_error('info', {'uid': self.uid})
            args, kwargs = mocked_info.call_args
            json_request, = args
            assert hasattr(json_request, 'tld')
            assert json_request.tld == api.USER_DEFAULT_TLD

    def test_user_init_for_social_user(self):
        """Проверить запрос `user_init` для социально-авторизованного пользователя.

        Должен возвращать {"code":113,"title":"account has no password"}.
        """
        with PassportStub() as stub:
            # для соц.аккаунта при подписке паспорт вернет ошибку
            stub.subscribe.side_effect = Passport.errors_map['accountwithpasswordrequired']
            self.json_error(
                'user_init',
                {
                    'uid': 444328444,
                    'locale': 'ru',
                    'source': 'rest_api_other'
                },
                code=codes.PASSPORT_PASSWORD_NEEDED
            )

    def test_user_init_without_lock(self):
        with mock.patch.dict('mpfs.config.settings.feature_toggles', {'user_init_lock_percentage': 0}):
            self.json_ok('user_init', {'uid': self.uid_1})

    def test_user_init_with_unknown_in_passport_uid(self):
        with PassportStub() as stub:
            stub.subscribe.side_effect = Passport.errors_map['unknownuid']
            self.json_error('user_init', {'uid': self.uid_1}, status=422)

    @parameterized.expand([
        (True,),
        (False,)
    ])
    def test_always_photoslice_albums_enabled_flag(self, is_new_user):
        if not is_new_user:
            session = Session.create_from_uid(self.uid)
            session.execute(
                'UPDATE disk.user_index SET photoslice_albums_enabled = NULL WHERE uid=:uid', {'uid': self.uid}
            )
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert 'photoslice_albums_enabled' in user_info
        assert user_info['photoslice_albums_enabled'] == 1

    def test_default_folders(self):
        resp = self.json_ok('user_info', {'uid': self.uid})
        assert 'default_folders' in resp
        # есть выкаченный на 100% эксп - его надо убрать и тогда убрать эту строку
        resp['default_folders'].pop('attach')
        assert resp['default_folders'] == self.json_ok('default_folders', {'uid': self.uid})


class DefaultFoldersEndpointTestCase(CommonJsonApiTestCase):
    """Набор тестов на ручку `default_folders`."""

    endpoint = 'default_folders'

    def __init__(self, *args, **kwargs):
        super(DefaultFoldersEndpointTestCase, self).__init__(*args, **kwargs)
        self.not_inited_uid = self.user_3.uid

    @parameterized.expand([
        (
            'ru',
            {
                'yaslovariarchive': '/attach/yaslovariarchive',
                'yaruarchive': '/attach/yaruarchive',
                'archive': '/attach/archive',
                'google': '/disk/Социальные сети/Google+',
                'instagram': '/disk/Социальные сети/Instagram',
                'vkontakte': '/disk/Социальные сети/ВКонтакте',
                'yabooks': '/disk/Яндекс.Книги',
                'screenshots': '/disk/Скриншоты/',
                'downloads': '/disk/Загрузки/',
                'odnoklassniki': '/disk/Социальные сети/Одноклассники',
                'applications': '/disk/Приложения',
                'yalivelettersarchive': '/attach/yalivelettersarchive',
                'facebook': '/disk/Социальные сети/Facebook',
                'social': '/disk/Социальные сети/',
                'mailru': '/disk/Социальные сети/Мой Мир',
                'fotki': '/disk/Яндекс.Фотки/',
                'photostream': '/disk/Фотокамера/',
                'yateamnda': '/disk/Yandex Team (NDA)',
                'yafotki': '/attach/YaFotki',
                'scans': '/disk/Сканы'
            }
        ),
        (
            'en',
            {
                'yaslovariarchive': '/attach/yaslovariarchive',
                'yaruarchive': '/attach/yaruarchive',
                'archive': '/attach/archive',
                'google': '/disk/Social networks/Google+',
                'instagram': '/disk/Social networks/Instagram',
                'vkontakte': '/disk/Social networks/VK',
                'yabooks': '/disk/Yandex.Books',
                'screenshots': '/disk/Screenshots/',
                'downloads': '/disk/Downloads/',
                'odnoklassniki': '/disk/Social networks/Одноклассники',
                'applications': '/disk/Applications',
                'yalivelettersarchive': '/attach/yalivelettersarchive',
                'facebook': '/disk/Social networks/Facebook',
                'social': '/disk/Social networks/',
                'mailru': '/disk/Social networks/Мой Мир',
                'fotki': '/disk/Yandex.Fotki/',
                'photostream': '/disk/Camera Uploads/',
                'yateamnda': '/disk/Yandex Team (NDA)',
                'yafotki': '/attach/YaFotki',
                'scans': '/disk/Scans'
            }
        ),
        (
            'tr',
            {
                'yaslovariarchive': '/attach/yaslovariarchive',
                'yaruarchive': '/attach/yaruarchive',
                'archive': '/attach/archive',
                'google': '/disk/Sosyal ağlar/Google+',
                'instagram': '/disk/Sosyal ağlar/Instagram',
                'vkontakte': '/disk/Sosyal ağlar/VK',
                'yabooks': '/disk/Yandex.Kitaplar',
                'screenshots': '/disk/Ekran görüntüleri/',
                'downloads': '/disk/Downloads/',
                'odnoklassniki': '/disk/Sosyal ağlar/Одноклассники',
                'applications': '/disk/Uygulamalar',
                'yalivelettersarchive': '/attach/yalivelettersarchive',
                'facebook': '/disk/Sosyal ağlar/Facebook',
                'social': '/disk/Sosyal ağlar/',
                'mailru': '/disk/Sosyal ağlar/Мой Мир',
                'fotki': '/disk/Yandex.Foto/',
                'photostream': '/disk/Kameradan yüklenenler/',
                'yateamnda': '/disk/Yandex Team (NDA)',
                'yafotki': '/attach/YaFotki',
                'scans': '/disk/Tarananlar'
            }
        ),
        (
            'uk',
            {
                'yaslovariarchive': '/attach/yaslovariarchive',
                'yaruarchive': '/attach/yaruarchive',
                'archive': '/attach/archive',
                'google': '/disk/Соціальні мережі/Google+',
                'instagram': '/disk/Соціальні мережі/Instagram',
                'vkontakte': '/disk/Соціальні мережі/ВКонтакте',
                'yabooks': '/disk/Яндекс.Книжки',
                'screenshots': '/disk/Скриншоти/',
                'downloads': '/disk/Завантаження/',
                'odnoklassniki': '/disk/Соціальні мережі/Одноклассники',
                'applications': '/disk/Додатки',
                'yalivelettersarchive': '/attach/yalivelettersarchive',
                'facebook': '/disk/Соціальні мережі/Facebook',
                'social': '/disk/Соціальні мережі/',
                'mailru': '/disk/Соціальні мережі/Мой Мир',
                'fotki': '/disk/Яндекс.Фотки/',
                'photostream': '/disk/Фотокамера/',
                'yateamnda': '/disk/Yandex Team (NDA)',
                'yafotki': '/attach/YaFotki',
                'scans': '/disk/Скани'
            }
        ),
        (
            'ua',
            {
                'yaslovariarchive': '/attach/yaslovariarchive',
                'yaruarchive': '/attach/yaruarchive',
                'archive': '/attach/archive',
                'google': '/disk/Социальные сети/Google+',
                'instagram': '/disk/Социальные сети/Instagram',
                'vkontakte': '/disk/Социальные сети/ВКонтакте',
                'yabooks': '/disk/Яндекс.Книги',
                'screenshots': '/disk/Скриншоты/',
                'downloads': '/disk/Загрузки/',
                'odnoklassniki': '/disk/Социальные сети/Одноклассники',
                'applications': '/disk/Приложения',
                'yalivelettersarchive': '/attach/yalivelettersarchive',
                'facebook': '/disk/Социальные сети/Facebook',
                'social': '/disk/Социальные сети/',
                'mailru': '/disk/Социальные сети/Мой Мир',
                'fotki': '/disk/Яндекс.Фотки/',
                'photostream': '/disk/Фотокамера/',
                'yateamnda': '/disk/Yandex Team (NDA)',
                'yafotki': '/attach/YaFotki',
                'scans': '/disk/Сканы'
            }
        )
    ])
    def test_default_folders_exist_false(self, locale, expected_result):
        uid = self.not_inited_uid

        # ATTENTION: Локаль `ua` не входит в список `SUPPORTED_LOCALES`, поэтому будет
        # выбрана дефолтная локаль, а именно `ru`.
        assert 'ua' not in constants.SUPPORTED_LOCALES

        self.json_ok('user_init', {
            'uid': uid,
            'locale': locale
        })

        result = self.json_ok(self.endpoint, {
            'uid': uid,
            'locale': locale
        })
        assert result == expected_result


class MksysdirEndpointTestCase(CommonJsonApiTestCase):
    endpoint = 'mksysdir'

    def __init__(self, *args, **kwargs):
        super(MksysdirEndpointTestCase, self).__init__(*args, **kwargs)
        self.not_inited_uid = self.user_3.uid

    @parameterized.expand([
        # ru
        (
            'vkontakte',
            'ru',
            '/disk/Социальные сети',
            '/disk/Социальные сети/ВКонтакте'
        ),
        (
            'facebook',
            'ru',
            '/disk/Социальные сети',
            '/disk/Социальные сети/Facebook'
        ),
        (
            'mailru',
            'ru',
            '/disk/Социальные сети',
            '/disk/Социальные сети/Мой Мир'
        ),
        (
            'odnoklassniki',
            'ru',
            '/disk/Социальные сети',
            '/disk/Социальные сети/Одноклассники'
        ),
        (
            'google',
            'ru',
            '/disk/Социальные сети',
            '/disk/Социальные сети/Google+'
        ),
        (
            'instagram',
            'ru',
            '/disk/Социальные сети',
            '/disk/Социальные сети/Instagram'
        ),

        # en
        (
            'vkontakte',
            'en',
            '/disk/Social networks',
            '/disk/Social networks/VK'
        ),
        (
            'facebook',
            'en',
            '/disk/Social networks',
            '/disk/Social networks/Facebook'
        ),
        (
            'mailru',
            'en',
            '/disk/Social networks',
            '/disk/Social networks/Мой Мир'
        ),
        (
            'odnoklassniki',
            'en',
            '/disk/Social networks',
            '/disk/Social networks/Одноклассники'
        ),
        (
            'google',
            'en',
            '/disk/Social networks',
            '/disk/Social networks/Google+'
        ),
        (
            'instagram',
            'en',
            '/disk/Social networks',
            '/disk/Social networks/Instagram'
        ),

        # tr
        (
            'vkontakte',
            'tr',
            '/disk/Sosyal ağlar',
            '/disk/Sosyal ağlar/VK'
        ),
        (
            'facebook',
            'tr',
            '/disk/Sosyal ağlar',
            '/disk/Sosyal ağlar/Facebook'
        ),
        (
            'mailru',
            'tr',
            '/disk/Sosyal ağlar',
            '/disk/Sosyal ağlar/Мой Мир'
        ),
        (
            'odnoklassniki',
            'tr',
            '/disk/Sosyal ağlar',
            '/disk/Sosyal ağlar/Одноклассники'
        ),
        (
            'google',
            'tr',
            '/disk/Sosyal ağlar',
            '/disk/Sosyal ağlar/Google+'
        ),
        (
            'instagram',
            'tr',
            '/disk/Sosyal ağlar',
            '/disk/Sosyal ağlar/Instagram'
        ),

        # uk
        (
            'vkontakte',
            'uk',
            '/disk/Соціальні мережі',
            '/disk/Соціальні мережі/ВКонтакте'
        ),
        (
            'facebook',
            'uk',
            '/disk/Соціальні мережі',
            '/disk/Соціальні мережі/Facebook'
        ),
        (
            'mailru',
            'uk',
            '/disk/Соціальні мережі',
            '/disk/Соціальні мережі/Мой Мир'
        ),
        (
            'odnoklassniki',
            'uk',
            '/disk/Соціальні мережі',
            '/disk/Соціальні мережі/Одноклассники'
        ),
        (
            'google',
            'uk',
            '/disk/Соціальні мережі',
            '/disk/Соціальні мережі/Google+'
        ),
        (
            'instagram',
            'uk',
            '/disk/Соціальні мережі',
            '/disk/Соціальні мережі/Instagram'
        ),
    ])
    def test_mksysdir_social_network(self, folder_type, locale, social_network_base_path, social_network_special_path):
        u"""Проверить создание системной директории для социальных сетей."""

        # Создаем системную папку для конкретной социальной сети и с конкретной локалью,
        # убеждаемся что в ответе `mksysdir` вернулись корректные данные, делаем
        # листинг по директории социальных сетей, убеждаемся что созданная только что
        # папка отдается в листинге и у нее проставлен корректный folder_type.

        uid = self.not_inited_uid

        self.json_ok('user_init', {
            'uid': uid,
            'locale': locale
        })

        result = self.json_ok(self.endpoint, {
            'uid': uid,
            'type': folder_type,
        })

        self.assertEqual(result['id'], social_network_special_path + '/')

        # проверяем, что проставлен корректный folder_type в meta и папки действительно создались
        result = self.json_ok('list', {
            'uid': uid,
            'path': social_network_base_path,
            'meta': 'folder_type'
        })

        listing_paths = [resource['path'] for resource in result]
        assert social_network_base_path in listing_paths
        assert social_network_special_path in listing_paths

        for resource in result:
            if resource['path'] == social_network_base_path:
                assert resource['meta']['folder_type'] == 'social'
            elif resource['path'] == social_network_special_path:
                assert resource['meta']['folder_type'] == folder_type

    @parameterized.expand([
        ('scans', 'ru', '/disk/Сканы'),
        ('scans', 'en', '/disk/Scans'),
        ('scans', 'tr', '/disk/Tarananlar'),
        ('scans', 'uk', '/disk/Скани'),
    ])
    def test_mksysdir(self, folder_type, locale, response_path):
        uid = self.not_inited_uid

        self.json_ok('user_init', {
            'uid': uid,
            'locale': locale
        })

        response = self.json_ok(self.endpoint, {
            'uid': uid,
            'type': folder_type,
        })

        assert response['path'] == response_path


class PassportCallbackEndpointTestCase(CommonJsonApiTestCase, SharingTestCaseMixin):
    endpoint = 'passport_callback'

    def test_passport_callback_user_deleted_event_calls_async_task(self):
        """Протестировать что при колбэке от паспорта об удаленном пользователе
        мы вызываем асинхронную задачу на выполнение."""
        uid = self.uid

        with PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']):
            with mock.patch(
                'mpfs.core.job_handlers.user.handle_passport_user_deleted_event.apply_async',
                return_value=None
            ) as mocked_async_task:
                self.service_ok('passport_callback', {
                    'uid': uid,
                    'event': 'account.changed',
                    'v': '1',
                    'timestamp': str(int(time.time()))
                })
                mocked_async_task.assert_called_once()
                args, kwargs = mocked_async_task.call_args
                assert 'uid' in kwargs['kwargs']
                assert kwargs['kwargs']['uid'] == uid

    def test_passport_callback_user_deleted_event_for_non_inited_user(self):
        """Протестировать работу ручки для юзера, который у нас не инициализирован."""
        not_inited_uid = self.user_3.uid

        self.service_error('passport_callback', {
            'uid': not_inited_uid,
            'event': 'account.changed',
            'v': '1',
            'timestamp': str(int(time.time()))
        }, code=codes.WH_USER_NEED_INIT)

    def test_passport_callback_public_links_removed_after_user_deleted_event(self):
        """Протестировать что после колбэка об удалении пользователя у него удаляются публичные ссылки."""

        uid = self.uid

        dir_path = '/disk/dir'
        self.json_ok('mkdir', {'uid': uid, 'path': dir_path})
        self.json_ok('set_public', {'uid': uid, 'path': dir_path})

        file_path = '/disk/file.jpg'
        self.upload_file(uid, file_path)
        self.json_ok('set_public', {'uid': uid, 'path': file_path})

        response = self.json_ok('info', {'uid': uid, 'path': dir_path, 'meta': ''})
        dir_public_hash = response['meta']['public_hash']

        response = self.json_ok('info', {'uid': uid, 'path': file_path, 'meta': ''})
        file_public_hash = response['meta']['public_hash']

        # публичная папка не заблокирована
        self.json_ok('public_info', {
            'private_hash': dir_public_hash
        })

        # публичный файл не заблокирован
        self.json_ok('public_info', {
            'private_hash': file_public_hash
        })

        with PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']):
            self.service_ok('passport_callback', {
                'uid': uid,
                'event': 'account.changed',
                'v': '1',
                'timestamp': str(int(time.time()))
            })

        self.json_error('public_info', {
            'private_hash': dir_public_hash
        }, code=codes.RESOURCE_NOT_FOUND)

        self.json_error('public_info', {
            'private_hash': file_public_hash
        }, code=codes.RESOURCE_NOT_FOUND)

    def test_passport_callback_user_left_groups_after_user_deleted_event(self):
        """Проверить что после колбэка от паспорта об удалении пользователя он покидает все
        группы, в которых был участником."""

        self.create_user(self.user_1.uid, noemail=True)  # владелец группы
        self.create_user(self.user_2.uid, noemail=True)  # приглашенный

        shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': shared_folder_path})
        result = self.json_ok('share_create_group', {'uid': self.user_1.uid, 'path': shared_folder_path})
        gid = result['gid']

        invite_hash = self.share_invite(gid, uid=self.user_2.uid)
        self.json_ok('share_activate_invite', {'hash': invite_hash, 'uid': self.user_2.uid})

        result = self.json_ok('share_users_in_group', {'uid': self.user_1.uid, 'gid': gid})
        assert filter(lambda u: u['status'] == 'approved' and u['uid'] == self.user_2.uid, result['users'])

        with PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']):
            self.service_ok('passport_callback', {
                'uid': self.user_2.uid,
                'event': 'account.changed',
                'v': '1',
                'timestamp': str(int(time.time()))
            })

        result = self.json_ok('share_users_in_group', {'uid': self.user_1.uid, 'gid': gid})
        assert not filter(lambda u: u['status'] == 'approved' and u['uid'] == self.user_2.uid, result['users'])

    def test_passport_callback_user_kicked_all_users_in_own_groups_after_user_deleted_event(self):
        """Проверить что после колбэка от паспорта об удалении пользователя он кикает всех
        пользователей в группах, где он владелец."""

        self.create_user(self.user_1.uid, noemail=True)  # владелец группы
        self.create_user(self.user_2.uid, noemail=True)  # приглашенный

        shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.user_1.uid, 'path': shared_folder_path})
        result = self.json_ok('share_create_group', {'uid': self.user_1.uid, 'path': shared_folder_path})
        gid = result['gid']

        invite_hash = self.share_invite(gid, uid=self.user_2.uid)
        self.json_ok('share_activate_invite', {'hash': invite_hash, 'uid': self.user_2.uid})

        result = self.json_ok('share_users_in_group', {'uid': self.user_1.uid, 'gid': gid})
        assert len(result['users']) == 2
        assert filter(lambda u: u['status'] == 'approved' and u['uid'] == self.user_2.uid, result['users'])

        with PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']):
            self.service_ok('passport_callback', {
                'uid': self.user_1.uid,
                'event': 'account.changed',
                'v': '1',
                'timestamp': str(int(time.time()))
            })

        result = self.json_ok('share_users_in_group', {'uid': self.user_1.uid, 'gid': gid})
        assert len(result['users']) == 1
        assert not filter(lambda u: u['uid'] == self.user_2.uid, result['users'])
        assert filter(lambda u: u['uid'] == self.user_1.uid and u['status'] == 'owner', result['users'])


class UserInitFailoverTestCase(CommonJsonApiTestCase):
    def test_initialization_if_usermap_entry_exists(self):
        # берем какой-нибудь существующий шард
        user_info = self.json_ok('user_info', {'uid': self.uid})
        shard = user_info['db']['shard']

        # создаем запись в usermap с этим шардом для какого-нибудь неинициализированного пользователя
        dbctl = mpfs.engine.process.dbctl()
        dbctl.mapper.add_route(self.uid_1, shard=shard)

        # зовем user_init, который должен отработать корректно
        self.json_ok('user_init', {'uid': self.uid_1})

        user_info = self.json_ok('user_info', {'uid': self.uid_1})
        assert shard == user_info['db']['shard']

    def test_initialization_for_empty_disk_info(self):
        # зовем user_init, который упал при создании disk_info
        with mock.patch.object(DiskInfoCollection, 'create', side_effect=RuntimeError()):
            self.json_error('user_init', {'uid': self.uid_1})

        # а теперь пытаемся еще раз проинициализировать этого пользователя
        self.json_ok('user_init', {'uid': self.uid_1})

        user_info = self.json_ok('user_info', {'uid': self.uid_1})
        assert user_info['db']['shard']

    @parameterized.expand([
        (42 * GB, ['32_gb_autoupload'], ['initial_10gb', '32_gb_autoupload'], 42 * GB),
        (3 * GB, ['app_install'], ['initial_10gb', 'app_install'],
         products.INITIAL_10GB.amount + products.APP_INSTALL.amount),
        (5 * GB, ['initial_3gb', 'app_install'], ['initial_10gb', 'initial_3gb', 'app_install'],
         products.INITIAL_10GB.amount + products.INITIAL_3GB.amount + products.APP_INSTALL.amount),
        (100500 * GB, ['initial_3gb'], ['initial_3gb', products.INITIAL_10GB.id],
         products.INITIAL_10GB.amount + products.INITIAL_3GB.amount),
        (100500 * GB, [], [products.INITIAL_10GB.id], products.INITIAL_10GB.amount),
        (100500 * GB, ['app_install'], ['app_install', products.INITIAL_10GB.id],
         products.INITIAL_10GB.amount + products.APP_INSTALL.amount),
    ])
    def test_simultaneous_initialization_and_service_create(
            self, initial_quota_size, services_before_init, services_after_init, quota_size_after_init):
        # https://st.yandex-team.ru/CHEMODAN-42996
        #
        # Шаги, воспроизводящие баг
        # Дергается user_init
        # Создается запись в usermap
        # Дергается service_create pid=x
        # Выдается услуга x
        # инициализация юзера доходит до момента выдачи init_10gb,
        # из-за наличия услуги х уже у юзера не выдается место

        self.json_ok('user_init', {'uid': self.uid_1})

        client = Client(self.uid_1)
        for service_info in ServiceList(**{CLIENT: client}):
            if service_info['pid'] == products.INITIAL_10GB.id:
                continue
            simple_delete_service(client, Service(service_info['sid']))

        for pid in services_before_init:
            client = Client(self.uid_1)
            simple_create_service(client, Product(pid))

        Quota().set_limit(initial_quota_size, uid=self.uid_1)

        # а теперь пытаемся проинициализировать этого пользователя
        self.json_ok('user_init', {'uid': self.uid_1})

        user_info = self.json_ok('user_info', {'uid': self.uid_1})
        assert user_info['db']['shard']

        services = self.billing_ok('service_list', {'uid': self.uid_1, 'ip': '127.0.0.1'})
        assert len(services) == len(services_after_init)
        for pid in services_after_init:
            assert any(service['name'] == pid for service in services)

        assert Quota().limit(uid=self.uid_1) == quota_size_after_init

    @parameterized.expand([(domain.name, domain) for domain in StandartUser.required_user_domains])
    def test_user_init_with_partially_initialized_domains(self, _, domain):
        '''
        https://st.yandex-team.ru/CHEMODAN-43158
        Тестируем, что пользователь с частично созданными доменами нормально доинициализируется
        '''
        with mock.patch.object(domain, 'check', return_value=False):
            # инициализируем пользователя с одним доменом
            # disk_info должен существовать всегда для инициализации статистики
            required_user_domains = [domain]
            if domain.name != 'disk_info':
                required_user_domains.append(DiskInfoCollection())
            # user_data должен существовать для создания /disk
            if domain.name != 'user_data':
                required_user_domains.append(UserDataCollection())

            with mock.patch.object(StandartUser, 'required_user_domains', new=required_user_domains):
                self.json_ok('user_init', {'uid': self.uid_1})
            # повторно запускаем инициализацию
            self.json_ok('user_init', {'uid': self.uid_1})

    @parameterized.expand(StandartUser.required_folders)
    def test_user_init_with_partially_initialized_folders(self, folder, domain):
        '''
        https://st.yandex-team.ru/CHEMODAN-43158
        Тестируем, что пользователь с частично созданными папками нормально доинициализируется
        '''
        with mock.patch.object(domain, 'show_single', side_effect=errors.StorageNotFound()):
            # инициализируем пользователя с одной папкой
            with mock.patch.object(StandartUser, 'required_folders', new=[(folder, domain)]):
                self.json_ok('user_init', {'uid': self.uid_1})

            # повторно запускаем инициализацию
            self.json_ok('user_init', {'uid': self.uid_1})

    def test_user_init_is_successful_after_retry(self):
        # зовем user_init, который упадет не дойдя до конца
        with mock.patch.object(DiskInfoCollection, 'create', side_effect=RuntimeError()):
            self.json_error('user_init', {'uid': self.uid_1})

        # проверяем, что пользователь определяется как находящийся в процессе инициализации, т.е. установлен лок
        assert usrctl.user_init_in_progress(self.uid_1)

        # проверяем, что пользователь определяется, как непроинициализированный
        self.json_error('user_info', {'uid': self.uid_1}, code=codes.WH_USER_NEED_INIT)

        # инициализируем пользователя снова
        self.json_ok('user_init', {'uid': self.uid_1})

        self.json_ok('user_info', {'uid': self.uid_1})

        # проверяем, что лок снялся
        assert not usrctl.user_init_in_progress(self.uid_1)

    def test_user_check_works_while_user_init_in_progress(self):

        res = self.json_ok('user_check', {'uid': self.uid_1})
        assert int(res['need_init']) == 1

        with mock.patch.object(DiskInfoCollection, 'create', side_effect=RuntimeError()):
            self.json_error('user_init', {'uid': self.uid_1})

        assert usrctl.user_init_in_progress(self.uid_1)

        res = self.json_ok('user_check', {'uid': self.uid_1})
        assert int(res['need_init']) == 1

        self.json_ok('user_init', {'uid': self.uid_1})

        res = self.json_ok('user_check', {'uid': self.uid_1})
        assert int(res['need_init']) == 0


class UserActivityInfoTestCase(CommonJsonApiTestCase):
    def test_user_activity_info(self):
        pg_data = {
            'platform_type': 'web',
            'first_activity': datetime.date(2019, 1, 26),
            'last_activity': datetime.date(2019, 1, 26),
        }
        with mock.patch(
                'mpfs.core.user_activity_info.dao.UserActivityInfoDAO.find_by_uid',
                return_value=[UserActivityDAOItem.create_from_raw_pg_dict(pg_data)]):
            activity_info = self.json_ok('user_activity_info', {'uid': self.uid})
        assert activity_info == {'web': {'first_activity': '2019-01-26', 'last_activity': '2019-01-26'}}

    def test_user_activity_info_empty_answer(self):
        with mock.patch('mpfs.core.user_activity_info.dao.UserActivityInfoDAO.find_by_uid', return_value=[]):
            activity_info = self.json_ok('user_activity_info', {'uid': self.uid})
        assert activity_info == {}

    @parameterized.expand([
        ('rest_andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'android', True),
        ('andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'android', True),
        ('rest_ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'ios', True),
        ('ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'ios', True),
        ('rest_win-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'windows', False),
        ('win-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'windows', False),
        ('rest_mac-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'mac', False),
        ('mac-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'mac', False),
        ('dav-ece8527244bbd2d5535a0ba5f189d0c4-api03v', None, False),
        ('rest_lnx-ece8527244bbd2d5535a0ba5f189d0c4-api03v', None, False),
        ('lnx-ece8527244bbd2d5535a0ba5f189d0c4-api03v', None, False),
    ])
    def test_update_activity_info_on_user_info_call_from_mobile(self, ycrid, platform, should_be_updated):
        activity_info = self.json_ok('user_activity_info', {'uid': self.uid})
        assert activity_info == {}
        current_datetime = datetime.datetime(2019, 2, 11, 12, 0, 0)
        with time_machine(current_datetime), mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid):
            self.json_ok('user_info', {'uid': self.uid})
        activity_info = self.json_ok('user_activity_info', {'uid': self.uid})
        if INIT_USER_IN_POSTGRES and should_be_updated:
            assert activity_info == {platform: {'first_activity': u'2019-02-11', 'last_activity': u'2019-02-11'}}
        else:
            assert activity_info == {}

        current_datetime = datetime.datetime(2019, 2, 12, 12, 0, 0)
        with time_machine(current_datetime), mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid):
            self.json_ok('user_info', {'uid': self.uid})
        activity_info = self.json_ok('user_activity_info', {'uid': self.uid})
        if INIT_USER_IN_POSTGRES and should_be_updated:
            assert activity_info == {platform: {'first_activity': u'2019-02-11', 'last_activity': u'2019-02-12'}}
        else:
            assert activity_info == {}

    @parameterized.expand([
        ('web-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'web', True),
        ('rest_andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'android', True),
        ('andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'android', True),
        ('rest_ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'ios', True),
        ('ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'ios', True),
        ('rest_win-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'windows', True),
        ('win-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'windows', True),
        ('rest_mac-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'mac', True),
        ('mac-ece8527244bbd2d5535a0ba5f189d0c4-api03v', 'mac', True),
        ('dav-ece8527244bbd2d5535a0ba5f189d0c4-api03v', None, False),
        ('rest_lnx-ece8527244bbd2d5535a0ba5f189d0c4-api03v', None, False),
        ('lnx-ece8527244bbd2d5535a0ba5f189d0c4-api03v', None, False),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='PG implementation only')
    def test_update_activity_info_on_user_init_call_from_mobile(self, ycrid, platform, should_be_updated):
        current_datetime = datetime.datetime(2019, 2, 11, 12, 0, 0)
        with time_machine(current_datetime), mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid):
            self.json_ok('user_init', {'uid': self.uid_1, 'shard': 'pg'})
        activity_info = self.json_ok('user_activity_info', {'uid': self.uid_1})
        if INIT_USER_IN_POSTGRES and should_be_updated:
            assert activity_info == {platform: {'first_activity': u'2019-02-11', 'last_activity': u'2019-02-11'}}
        else:
            assert activity_info == {}

    def test_not_call_update_same_date(self):
        activity_info = self.json_ok('user_activity_info', {'uid': self.uid})
        assert activity_info == {}

        current_datetime = datetime.datetime(2019, 2, 12, 12, 0, 0)
        with time_machine(current_datetime), mock.patch(
                'mpfs.engine.process.get_cloud_req_id',
                return_value='rest_andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v'):
            self.json_ok('user_info', {'uid': self.uid})

        activity_info = self.json_ok('user_activity_info', {'uid': self.uid})
        if INIT_USER_IN_POSTGRES:
            assert activity_info == {'android': {'first_activity': u'2019-02-12', 'last_activity': u'2019-02-12'}}
        else:
            assert activity_info == {}

        with time_machine(current_datetime), mock.patch('mpfs.engine.process.get_cloud_req_id',
                        return_value='rest_andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v'), \
             mock.patch('mpfs.core.user.common.user_activity_info_dao.update_activity_dates',
                        return_value=Exception) as update_mock:
            self.json_ok('user_info', {'uid': self.uid})
            if INIT_USER_IN_POSTGRES:
                update_mock.assert_not_called()
            else:
                update_mock.assert_called_once()

    def test_ignore_errors_in_activity_info_update(self):
        with mock.patch('mpfs.engine.process.get_cloud_req_id',
                        return_value='rest_andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v'), \
             mock.patch('mpfs.core.user.common.user_activity_info_dao.update_activity_dates',
                        return_value=Exception) as update_mock:
            self.json_ok('user_info', {'uid': self.uid})
            update_mock.assert_called_once()
