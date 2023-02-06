# -*- coding: utf-8 -*-
import random

import dateutil.parser
import mock

from datetime import datetime
from dateutil.relativedelta import relativedelta
from hamcrest import assert_that, is_not, has_item, has_entry

from test.helpers.products import YANDEX_PLUS
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin, BillingApiTestCaseMixin
from test.fixtures.users import user_1
from mpfs.common import errors
from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.core.services.conductor_service import ConductorService


class PartnerServicesTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    other_uid = user_1.uid

    def test_space_after_fail(self):
        bug_uid = 111111111291580861
        uid = self.uid
        url = 'disk/partners/%s/services' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services'],
                                   uid=bug_uid):
            resp = self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'}, uid=bug_uid)

        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services'],
                                   uid=uid):
            resp = self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'}, uid=uid)
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 201)
            self.assertEqual(resp_content['product_id'], "rostelecom_2014_100gb")
            self.assertIn('expires', resp_content)

    def test_permissions(self):
        # для всех ручек
        for method, url in (('POST', 'disk/partners/%s/services'),
                            ('PUT', 'disk/partners/%s/services/prolongate_by_product'),
                            ('DELETE', 'disk/partners/%s/services/remove_by_product')):
            # не тот скоуп
            with self.specified_client(scopes=['cloud_api:disk.read']):
                resp = self.client.request(method, url % 'rostelecom', query={'product_id': 'test'})
                self.assertEqual(resp.status, 403)

            # скоуп другого пратнёра
            with self.specified_client(scopes=['cloud_api:disk.partners.TEST.manage_services']):
                resp = self.client.request(method,
                                           url % 'rostelecom',
                                           query={'product_id': 'rostelecom_2014_100gb'})
                resp_content = from_json(resp.content)
                self.assertEqual(resp.status, 403)
                self.assertEqual(resp_content['error'], 'ForbiddenError')

            # неизвестный партнер
            with self.specified_client(scopes=['cloud_api:disk.partners.TEST.manage_services']):
                resp = self.client.request(method, url % 'TEST', query={'product_id': 'test'})
                resp_content = from_json(resp.content)
                self.assertEqual(resp.status, 404)
                self.assertEqual(resp_content['error'], 'DiskProductNotFoundError')

            # левый ip
            with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']), \
                 mock.patch('mpfs.platform.common.PlatformRequest.get_real_remote_addr', return_value='6.6.6.6'):
                resp = self.client.request(method,
                                           url % 'rostelecom',
                                           query={'product_id': 'rostelecom_2014_100gb'})
                resp_content = from_json(resp.content)
                self.assertEqual(resp.status, 403)
                self.assertEqual(resp_content['error'], 'ForbiddenError')

    def test_handler_accepts_subnets(self):
        import mpfs.platform.v1.disk.rostelecom.handlers
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']), \
                mock.patch('mpfs.platform.common.PlatformRequest.get_real_remote_addr', return_value='87.226.160.5'), \
                mock.patch.object(mpfs.platform.v1.disk.rostelecom.handlers, 'ROSTELECOM_UNLIM_ENABLED_FOR_UIDS', new=[]):
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert resp.status_code == 200

    def test_create_service(self):
        url = 'disk/partners/%s/services' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            # нет такого продукта
            resp = self.client.post(url, query={'product_id': 'test'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 404)
            self.assertEqual(resp_content['error'], 'DiskProductNotFoundError')

            # создаем услугу
            resp = self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 201)
            self.assertEqual(resp_content['product_id'], "rostelecom_2014_100gb")
            self.assertIn('expires', resp_content)

            # проверяем, что создалась на 4 месяца
            expires_dt = dateutil.parser.parse(resp_content['expires'])
            assert relativedelta(expires_dt.replace(tzinfo=None), datetime.now()).months == 3

    def test_delete_service_by_service_id(self):
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            partner = 'rostelecom'

            url = 'disk/partners/%s/services' % partner
            resp = self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'})
            assert resp.status == 201
            service_id = from_json(resp.content)['service_id']

            service_url = 'disk/partners/%s/services/%s' % (partner, service_id)
            resp = self.client.delete(service_url)
            assert resp.status == 204

    def test_delete_service_of_another_partner(self):
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            valid_partner = 'rostelecom'
            invalid_partner = 'yandex_plus'

            url = 'disk/partners/%s/services' % valid_partner
            resp = self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'})
            assert resp.status == 201
            service_id = from_json(resp.content)['service_id']

            service_url = 'disk/partners/%s/services/%s' % (invalid_partner, service_id)
            resp = self.client.delete(service_url)
            assert resp.status == 404

    def test_delete_non_existent_service(self):
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            partner = 'rostelecom'

            url = 'disk/partners/%s/services' % partner
            resp = self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'})
            assert resp.status == 201

            service_url = 'disk/partners/%s/services/%s' % (partner, 'non_existent_service_id')
            resp = self.client.delete(service_url)
            assert resp.status == 404

    def test_create_service_without_expires(self):
        url = 'disk/partners/yandex_plus/services'
        with self.specified_client(scopes=['cloud_api:disk.partners.yandex_plus.manage_services']):
            resp = self.client.post(url, query={'product_id': 'yandex_plus_10gb'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 201)
            self.assertEqual(resp_content['product_id'], "yandex_plus_10gb")
            assert 'service_id' in resp_content
            assert 'expires' not in resp_content

    def test_prolongate_service(self):
        url = 'disk/partners/%s/services/prolongate_by_product' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            # услуга не найдена
            resp = self.client.put(url, query={'product_id': 'rostelecom_2014_100gb'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 404)
            self.assertEqual(resp_content['error'], "DiskServiceNotFoundError")

            # создаем услугу
            self.client.post('disk/partners/%s/services' % 'rostelecom',
                             query={'product_id': 'rostelecom_2014_100gb'})

            # продливаем
            resp = self.client.put(url, query={'product_id': 'rostelecom_2014_100gb'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 200)
            self.assertEqual(resp_content['product_id'], "rostelecom_2014_100gb")
            self.assertIn('expires', resp_content)

            # проверяем, что продлилась на 1 месяц и теперь будет действовать 5 месяцев
            expires_dt = dateutil.parser.parse(resp_content['expires'])
            assert relativedelta(expires_dt.replace(tzinfo=None), datetime.now()).months == 4

            # пробуем продлить второй раз
            resp = self.client.put(url, query={'product_id': 'rostelecom_2014_100gb'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 200)
            # проверяем, что повторни ничего не продлилось, дата окончания услуги осталась прежней
            new_expires_dt = dateutil.parser.parse(resp_content['expires'])
            assert expires_dt == new_expires_dt

    def test_service_info(self):
        url = 'disk/partners/%s/services' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            # создаем услугу
            self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'})
            # запрашиваем услуги пользователя
            resp = self.client.get(url)
            resp_content = from_json(resp.content)
            assert resp.status == 200
            assert 'partner' in resp_content
            assert resp_content['partner'] == 'rostelecom'
            assert 'items' in resp_content
            assert len(resp_content['items']) == 1

    def test_is_paid(self):
        url = 'disk/partners/%s/services' % 'yandex_directory'
        with self.specified_client(scopes=['cloud_api:disk.partners.yandex_directory.manage_services']):
            self.client.post(url, query={'product_id': 'yandex_directory_1tb'})
        with self.specified_client(scopes=['cloud_api:disk.info']):
            resp = self.client.get('disk')
            disk_info = from_json(resp.content)
            assert disk_info['is_paid'] is True

    def test_service_info_for_services_without_btime(self):
        url = 'disk/partners/%s/services' % 'yandex_directory'
        with self.specified_client(scopes=['cloud_api:disk.partners.yandex_directory.manage_services']):
            # создаем услугу
            self.client.post(url, query={'product_id': 'yandex_directory_1tb'})
            # запрашиваем услуги пользователя
            resp = self.client.get(url)
            resp_content = from_json(resp.content)
            assert resp.status == 200
            assert 'partner' in resp_content
            assert resp_content['partner'] == 'yandex_directory'
            assert 'items' in resp_content
            assert len(resp_content['items']) == 1
            assert 'expires' not in resp_content['items'][0]

    def test_service_info_with_several_non_singleton_services(self):
        url = 'disk/partners/%s/services' % 'yandex_directory'

        with self.specified_client(scopes=['cloud_api:disk.partners.yandex_directory.manage_services']):
            services_count = 5
            service_ids = set()
            for i in xrange(services_count):
                resp = self.client.post(url, query={'product_id': 'yandex_directory_1tb'})
                resp_content = from_json(resp.content)
                service_ids.add(resp_content['service_id'])
            assert len(service_ids) == services_count

            resp = self.client.get(url)
            resp_content = from_json(resp.content)
            assert resp.status == 200
            assert 'partner' in resp_content
            assert resp_content['partner'] == 'yandex_directory'
            assert 'items' in resp_content
            assert len(resp_content['items']) == services_count

            print resp_content['items']
            info_service_ids = set()
            for item in resp_content['items']:
                info_service_ids.add(item['service_id'])
            assert service_ids == info_service_ids

    def test_service_info_for_uninitialized_uid(self):
        url = 'disk/partners/%s/services' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services'], uid=self.other_uid):
            # запрашиваем услуги пользователя
            resp = self.client.get(url)
            resp_content = from_json(resp.content)
            assert resp.status == 200
            assert 'partner' in resp_content
            assert resp_content['partner'] == 'rostelecom'
            assert 'items' in resp_content
            assert len(resp_content['items']) == 0

            resp = self.json_ok('user_check', opts={'uid': self.other_uid})
            assert resp['need_init'] == '1'

    def test_service_info_for_unexistent_uid(self):
        url = 'disk/partners/%s/services' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services'], uid=None):
            # запрашиваем услуги пользователя
            resp = self.client.get(url)
            assert resp.status == 401

    def test_remove_service(self):
        url = 'disk/partners/%s/services/remove_by_product' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            # услуга не найдена
            resp = self.client.delete(url, query={'product_id': 'rostelecom_2014_100gb'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 404)
            self.assertEqual(resp_content['error'], "DiskServiceNotFoundError")

            # создаем услугу
            self.client.post('disk/partners/%s/services' % 'rostelecom',
                             query={'product_id': 'rostelecom_2014_100gb'})
            # удаляем услугу
            resp = self.client.delete(url, query={'product_id': 'rostelecom_2014_100gb'})
            self.assertEqual(resp.status, 204)
            # пробуем удалить еще раз
            resp = self.client.delete(url, query={'product_id': 'rostelecom_2014_100gb'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 404)
            self.assertEqual(resp_content['error'], "DiskServiceNotFoundError")

    def test_wrong_content_type_with_empty_body(self):
        """
        Проверяем что не проверяем Content-Type запроса при пустом теле запроса.
        https://st.yandex-team.ru/CHEMODAN-21805
        """
        path = 'disk/partners/rostelecom/services'
        query = {'product_id': 'rostelecom_2014_100gb'}
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            resp = self.client.post(path,
                                    query=query,
                                    headers={'Content-Type': 'application/asdfkj'})
            assert resp.status_code == 201

    def test_2_legged_auth(self):
        """Проверка двуногой авторизации"""
        url = 'disk/partners/%s/services' % 'rostelecom'
        user_email = 'qawsedrftgyhujikolzaxscdvfbgnhmjdfsefefefefefefesdvsvxzvvmorvj98424@yandex.ru'

        # user не установлен
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services'],
                                   uid=None):
            resp = self.client.get(url)
            assert resp.status_code == 401
            resp = self.client.get(url, query={'email': 'mpfs-test@yandex.ru'})
            assert resp.status_code == 200
            resp = self.client.get(url, query={'email': user_email})
            assert resp.status_code == 404

        # если передан email и токен с пользователем, то авторизуем по нему
        # токену, а не по параметру email
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            resp = self.client.get(url)
            assert resp.status_code == 200
            resp = self.client.get(url, query={'email': user_email})
            assert resp.status_code == 200

    def test_service_2015(self):
        url = 'disk/partners/%s/services' % 'rostelecom'

        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            resp = self.client.post(url, query={'product_id': 'rostelecom_2015_100gb_paid'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 201)
            self.assertEqual(resp_content['product_id'], "rostelecom_2015_100gb_paid")
            self.assertIn('expires', resp_content)

    def test_prolongate_service_2015(self):
        url = 'disk/partners/%s/services/prolongate_by_product' % 'rostelecom'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom.manage_services']):
            # услуга не найдена
            resp = self.client.put(url, query={'product_id': 'rostelecom_2015_100gb_paid'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 404)
            self.assertEqual(resp_content['error'], 'DiskServiceNotFoundError')

            check_time = datetime.utcnow().replace(microsecond=0)

            # создаем услугу
            resp = self.client.post('disk/partners/%s/services' % 'rostelecom',
                                    query={'product_id': 'rostelecom_2015_100gb_paid'})
            resp_content = from_json(resp.content)

            # проверяем, что услуга создана на 1 год
            expires_dt = dateutil.parser.parse(resp_content['expires'])
            delta = relativedelta(expires_dt.replace(tzinfo=None), check_time)
            assert delta.years == 1 and delta.months == 0

            # продливаем
            resp = self.client.put(url, query={'product_id': 'rostelecom_2015_100gb_paid'})
            resp_content = from_json(resp.content)
            self.assertEqual(resp.status, 200)
            self.assertEqual(resp_content['product_id'], 'rostelecom_2015_100gb_paid')
            self.assertIn('expires', resp_content)

            # проверяем, что продлилась на 12 месяцев и теперь будет действовать 2 года
            expires_dt = dateutil.parser.parse(resp_content['expires'])
            delta = relativedelta(expires_dt.replace(tzinfo=None), check_time)
            assert delta.years == 2 and delta.months == 0

            # пробуем продлить второй раз
            resp = self.client.put(url, query={'product_id': 'rostelecom_2015_100gb_paid'})
            self.assertEqual(resp.status, 409)

            # проверяем, что повторно ничего не продлилось, дата окончания услуги осталась прежней
            resp = self.client.get('disk/partners/%s/services' % 'rostelecom',
                                   query={'product_id': 'rostelecom_2015_100gb_paid'})
            resp_content = from_json(resp.content)
            new_expires_dt = dateutil.parser.parse(resp_content['items'][0]['expires'])
            assert expires_dt == new_expires_dt

    def test_return_proper_error_on_delete_product_for_not_existing_user(self):
        with self.specified_client(uid='fake_uid', scopes=['cloud_api:disk.partners.yandex_plus.manage_services']):
            resp = self.client.delete(
                'disk/partners/yandex_plus/services/remove_by_product',
                query={'product_id': 'yandex_plus_10gb'},
            )
            self.assertEqual(resp.status, 401)
            resp_content = from_json(resp.content)
            self.assertEqual(resp_content['error'], 'UnauthorizedError')


class PartnerServicesInternalTestCase(UserTestCaseMixin, BillingApiTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def test_stub_for_service_creation(self):
        self.create_user(self.uid)
        url = 'disk/partners/yandex_plus/services'

        with self.specified_client(scopes=['cloud_api:disk.partners.yandex_plus.manage_services']), \
             mock.patch.dict('mpfs.platform.v1.disk.billing.handlers.BILLING_PARTNERS_STUB_SERVICE_CREATION',
                             {'enabled': True,
                              'enable_for': {'yandex_plus': 'yandex_plus_10gb'}}):
            services = self.client.post(url, query={'product_id': 'yandex_plus_10gb'})
            resp_content = from_json(services.content)
            self.assertEqual(services.status, 201)
            self.assertEqual(resp_content['product_id'], "yandex_plus_10gb")
            assert 'service_id' not in resp_content
        # no real service is provided
        services = self.billing_ok('service_list', opts={'uid': self.uid, 'ip': 'localhost'})
        assert_that(services, is_not(has_item(has_entry('name', YANDEX_PLUS.id))))

    def test_stub_for_service_creation_with_not_initialized_user(self):
        url = 'disk/partners/yandex_plus/services'

        with self.specified_client(scopes=['cloud_api:disk.partners.yandex_plus.manage_services']), \
             mock.patch.dict('mpfs.platform.v1.disk.billing.handlers.BILLING_PARTNERS_STUB_SERVICE_CREATION',
                             {'enabled': True,
                              'enable_for': {'yandex_plus': 'yandex_plus_10gb'}}):
            services = self.client.post(url, query={'product_id': 'yandex_plus_10gb'})
            resp_content = from_json(services.content)
            self.assertEqual(services.status, 201)
            self.assertEqual(resp_content['product_id'], "yandex_plus_10gb")
            assert 'service_id' not in resp_content

        # user is still not initialized
        self.billing_error('service_list', opts={'uid': self.uid, 'ip': 'localhost'},
                           code=errors.StorageInitUser.code)

    def test_stub_for_service_creation_disabled(self):
        url = 'disk/partners/yandex_plus/services'

        with self.specified_client(scopes=['cloud_api:disk.partners.yandex_plus.manage_services']), \
             mock.patch.dict('mpfs.platform.v1.disk.billing.handlers.BILLING_PARTNERS_STUB_SERVICE_CREATION',
                             {'enabled': False,
                              'enable_for': {'yandex_plus': 'yandex_plus_10gb'}}):
            services = self.client.post(url, query={'product_id': 'yandex_plus_10gb'})
            resp_content = from_json(services.content)
            self.assertEqual(services.status, 201)
            self.assertEqual(resp_content['product_id'], "yandex_plus_10gb")
            assert 'service_id' in resp_content
        # user initialized and service is provided
        services = self.billing_ok('service_list', opts={'uid': self.uid, 'ip': 'localhost'})
        assert_that(services, has_item(has_entry('name', YANDEX_PLUS.id)))

    def test_permissions(self):
        # для всех ручек
        tmp_filename = '/tmp/mpfs/cache_%s' % hex(random.getrandbits(8*10))[2:-1]
        with mock.patch.object(ConductorService, 'cache_filepath', tmp_filename):
            for method, url, status in (('POST', 'disk/partners/%s/services', 201),
                                        ('PUT', 'disk/partners/%s/services/prolongate_by_product', 200),
                                        ('DELETE', 'disk/partners/%s/services/remove_by_product', 204)):
                resp = self.client.request(method, url % 'rostelecom',
                                           query={'product_id': 'rostelecom_2014_100gb'},
                                           uid=self.uid,
                                           ip='6.6.6.6')
                self.assertEqual(resp.status, status)

    def test_return_proper_error_on_delete_product_for_not_existing_user(self):
        with self.specified_client(uid='fake_uid', scopes=['cloud_api:disk.partners.yandex_plus.manage_services']):
            resp = self.client.delete(
                'disk/partners/yandex_plus/services/remove_by_product',
                query={'product_id': 'yandex_plus_10gb'},
            )
            self.assertEqual(resp.status, 401)
            resp_content = from_json(resp.content)
            self.assertEqual(resp_content['error'], 'UserNotFoundInPassportError')
