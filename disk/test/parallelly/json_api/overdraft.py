# coding=utf-8
import datetime

import mock

from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.common.util.overdraft import OVERDRAFT_STATUS_FIELD, OVERDRAFT_DATE_FIELD
from mpfs.core.billing.api import service_delete
from mpfs.config import settings
from mpfs.core.user.base import User
from test.base import DiskTestCase, time_machine
USER_OVERDRAFT_RESTRICTIONS_THRESHOLD = settings.user['overdraft']['restrictions_threshold']


class TestOverdraft(DiskTestCase):

    def test_overdraft(self):
        with enable_experiment_for_uid('new_overdraft_strategy', self.uid):
            resp = self.billing_ok('service_create', {'uid': self.uid,
                                                      'line': 'partner',
                                                      'pid': 'yandex_disk_for_business',
                                                      'ip': '127.0.0.1',
                                                      'product.amount': 10})
            self.upload_file(self.uid, '/disk/1.txt', file_data={'size': 1024 ** 3 * 10 + 1})  # на один байт больше 10 ГБ (10ГБ выдаётся каждому пользователю)
            self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                                      'ip': '127.0.0.1', 'send_email': 0})
            user = User(self.uid)
            assert user.is_overdrawn()
            assert not user.is_in_overdraft_for_restrictions()
            assert not user.get_overdraft_info()

    def test_overdraft_restriction(self):
        with enable_experiment_for_uid('new_overdraft_strategy', self.uid), \
             mock.patch('mpfs.core.services.passport_service.Passport.userinfo',
                        return_value={'country': 'ru', 'email': 'email@example.com', 'language': 'ru'}):
            resp = self.billing_ok('service_create', {'uid': self.uid,
                                                      'line': 'partner',
                                                      'pid': 'yandex_disk_for_business',
                                                      'ip': '127.0.0.1',
                                                      'product.amount': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 2})
            self.upload_file(self.uid, '/disk/1.txt', file_data={'size': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 1024 ** 3 * 10 + 1})
            self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                               'ip': '127.0.0.1', 'send_email': 0})
            user = User(self.uid)
            assert user.is_overdrawn()
            assert user.is_in_overdraft_for_restrictions()
            overdrawn_info = user.get_overdraft_info()
            assert overdrawn_info
            assert overdrawn_info[OVERDRAFT_STATUS_FIELD] == 1
            assert overdrawn_info[OVERDRAFT_DATE_FIELD]
            old_date = overdrawn_info[OVERDRAFT_DATE_FIELD]
            resp = self.billing_ok('service_create', {'uid': self.uid,
                                                      'line': 'partner',
                                                      'pid': 'yandex_disk_for_business',
                                                      'ip': '127.0.0.1',
                                                      'product.amount': 0})
            with time_machine(datetime.date.today() + datetime.timedelta(1)):
                self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                                   'ip': '127.0.0.1', 'send_email': 0})
                overdrawn_info = user.get_overdraft_info()
                assert overdrawn_info[OVERDRAFT_DATE_FIELD] == old_date

            resp = self.billing_ok('service_create', {'uid': self.uid,
                                                      'line': 'partner',
                                                      'pid': 'yandex_disk_for_business',
                                                      'ip': '127.0.0.1',
                                                      'product.amount': 1})
            with time_machine(datetime.date.today() + datetime.timedelta(1)):
                self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                                   'ip': '127.0.0.1', 'send_email': 0})
                overdrawn_info = user.get_overdraft_info()
                assert overdrawn_info[OVERDRAFT_DATE_FIELD] == old_date

            resp = self.billing_ok('service_create', {'uid': self.uid,
                                                      'line': 'partner',
                                                      'pid': 'yandex_disk_for_business',
                                                      'ip': '127.0.0.1',
                                                      'product.amount': 3})
            with time_machine(datetime.date.today() + datetime.timedelta(1)):
                self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                                   'ip': '127.0.0.1', 'send_email': 0})
                overdrawn_info = user.get_overdraft_info()
                assert overdrawn_info[OVERDRAFT_DATE_FIELD] > old_date

    def test_set_user_overdraft_date(self):
        with enable_experiment_for_uid('new_overdraft_strategy', self.uid):
            resp = self.billing_ok('service_create', {'uid': self.uid,
                                                      'line': 'partner',
                                                      'pid': 'yandex_disk_for_business',
                                                      'ip': '127.0.0.1',
                                                      'product.amount': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 2})
            self.upload_file(self.uid, '/disk/1.txt', file_data={'size': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 1024 ** 3 * 10 + 1})  # на один байт больше 10 ГБ + USER_OVERDRAFT_RESTRICTIONS_THRESHOLD (10ГБ выдаётся каждому пользователю)
            self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                                      'ip': '127.0.0.1', 'send_email': 0})
            user = User(self.uid)
            overdraft_date = '2021-05-28'
            self.json_ok('set_user_overdraft_date', {'uid': self.uid, 'overdraft_date': overdraft_date, 'force': 1})
            overdrawn_info = user.get_overdraft_info()
            assert overdrawn_info[OVERDRAFT_DATE_FIELD] == overdraft_date

    def test_overdraft_push(self):
        with enable_experiment_for_uid('new_overdraft_strategy', self.uid), \
             mock.patch('mpfs.core.pushnotifier.queue.PushQueue.put') as put:
            resp = self.billing_ok('service_create', {'uid': self.uid,
                                                      'line': 'partner',
                                                      'pid': 'yandex_disk_for_business',
                                                      'ip': '127.0.0.1',
                                                      'product.amount': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 2})
            self.upload_file(self.uid, '/disk/1.txt', file_data={'size': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 1024 ** 3 * 10 + 1})
            self.billing_ok('service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True,
                                               'ip': '127.0.0.1', 'send_email': 0})
            assert put.called
