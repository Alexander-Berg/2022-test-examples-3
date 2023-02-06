# -*- coding: utf-8 -*-
from attrdict import AttrDict
from hamcrest import assert_that, is_, empty, has_entry, is_not, has_key
from mock import patch
from nose_parameterized import parameterized

from mpfs.common.errors import AuthorizationError
from mpfs.config import settings
from mpfs.core.locks.validators import FIELDS_LENGTH_LIMIT
from test.parallelly.json_api.base import CommonJsonApiTestCase


class LocksTestCase(CommonJsonApiTestCase):
    SERVICES_TVM_2_0_CLIENT_ID = settings.auth['clients']['disk_web_client_locks']['tvm_2_0']['client_ids'][0]

    def setup_method(self, method):
        super(LocksTestCase, self).setup_method(method)
        self.path = '/disk/New Folder'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.path})
        self.headers = {'X-Ya-Service-Ticket': 'secretenot'}
        self.auth_patcher = patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                                  return_value=AttrDict({'src': self.SERVICES_TVM_2_0_CLIENT_ID}))
        self.auth_patcher.start()

    def teardown_method(self, method):
        self.auth_patcher.stop()
        super(LocksTestCase, self).teardown_method(method)

    def test_locks_list(self):
        locks_list = self.json_ok('locks_list', {'uid': self.uid},
                                  headers=self.headers,
                                  client_addr='10.10.0.1')
        assert_that(locks_list, is_(empty()))

        op_type = 'copy_resource'
        self.json_ok('locks_set',
                     {'uid': self.uid, 'path': self.path},
                     json={'op_type': op_type},
                     headers=self.headers,
                     client_addr='10.10.0.1')

        locks_list = self.json_ok('locks_list', {'uid': self.uid},
                                  headers=self.headers,
                                  client_addr='10.10.0.1')

        assert_that(locks_list, has_entry(self.path,
                                          has_entry('data',
                                                    has_entry('op_type', op_type))))

    def test_locks_set(self):
        op_type = 'copy_resource'
        lock_info = self.json_ok('locks_set',
                                 {'uid': self.uid, 'path': self.path},
                                 json={'op_type': op_type},
                                 headers=self.headers,
                                 client_addr='10.10.0.1')

        assert_that(lock_info, has_entry('data',
                                         has_entry('op_type', op_type)))

    def test_locks_delete_on_non_existen_resource(self):
        """Должны выполнить удаление лока несуществующего ресурса без ошибок"""
        self.json_ok('locks_delete',
                     {'uid': self.uid, 'path': '/disk/non-existent.file'},
                     headers=self.headers,
                     client_addr='10.10.0.1')

    def test_locks_delete(self):
        self.json_ok('locks_set',
                     {'uid': self.uid, 'path': self.path},
                     json={'op_type': 'copy_resource'},
                     headers=self.headers,
                     client_addr='10.10.0.1')

        self.json_ok('locks_delete',
                     {'uid': self.uid, 'path': self.path},
                     headers=self.headers,
                     client_addr='10.10.0.1')

        locks_list = self.json_ok('locks_list', {'uid': self.uid},
                                  headers=self.headers,
                                  client_addr='10.10.0.1')
        assert_that(locks_list, is_(empty()))

    @parameterized.expand([('wrong_field_name', {'op_tipe': 'copy_resource'}),
                           ('too_long_value', {'oid': 'a' * (FIELDS_LENGTH_LIMIT + 1)})])
    def test_locks_delete(self, case_name, lock_data):
        self.json_error('locks_set',
                        {'uid': self.uid, 'path': self.path},
                        json=lock_data,
                        headers=self.headers,
                        client_addr='10.10.0.1')

    @parameterized.expand(['locks_delete',
                           'locks_set',
                           'locks_list'])
    def test_failed_auth_with_another_client(self, method):
        with patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                   return_value=AttrDict({'src': 9988770})):
            self.json_error(method,
                            {'uid': self.uid, 'path': '/disk/non-existent.file'},
                            json={'op_tipe': 'copy_resource'},
                            headers=self.headers,
                            code=AuthorizationError.code,
                            client_addr='10.10.0.2')

    @parameterized.expand(['locks_delete',
                           'locks_set',
                           'locks_list'])
    def test_failed_auth_without_service_ticket(self, method):
        self.json_error(method,
                        {'uid': self.uid, 'path': '/disk/non-existent.file'},
                        json={'op_tipe': 'copy_resource'},
                        code=AuthorizationError.code,
                        client_addr='10.10.0.2')
