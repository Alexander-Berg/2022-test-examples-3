# -*- coding: utf-8 -*-
from django.test import TestCase
from hamcrest import assert_that, equal_to, contains_string, has_entry, has_item, has_property, all_of
from mock import patch
from nose_parameterized import parameterized

from api_admin.api_auth.forms import AuthClientAddForm


class AuthClientAddFormTestCase(TestCase):
    def test_mutually_exclusive_auth(self):
        form = AuthClientAddForm({'name': 'enot',
                                  'oauth_client_name': 'enot',
                                  'oauth_client_id': 'enotid',
                                  'auth_methods': [AuthClientAddForm.METHOD_YATEAM_TVM,
                                                   AuthClientAddForm.METHOD_TVM],
                                  'tvm_client_ids': '12345',
                                  'tvm_2_0_client_ids': ''})
        auth_labels = dict(AuthClientAddForm.METHODS_CHOICES)
        # Патчим, чтобы не проверять уникальность имени через zookeeper
        with patch('api_admin.api_auth.forms.AuthClientAddForm.clean_name', return_value='enot'), \
             patch('api_admin.api_auth.forms.AuthClientAddForm.clean_tvm_2_0_client_ids', return_value=[]):
            assert_that(form.is_valid(), equal_to(False))
            form.clean()
            assert_that(form.errors.as_data(),
                        has_entry('auth_methods',
                                  has_item(has_property('message',
                                                        all_of(
                                                            contains_string(
                                                                auth_labels[AuthClientAddForm.METHOD_YATEAM_TVM]
                                                            ),
                                                            contains_string(auth_labels[AuthClientAddForm.METHOD_TVM])
                                                        )))))

    def test_tvm_2_0_client_ids_duplicate_check(self):
        form = AuthClientAddForm({
            'name': 'enot',
            'oauth_client_name': 'enot',
            'oauth_client_id': 'enotid',
            'auth_methods': [AuthClientAddForm.METHOD_TVM_2_0],
            'tvm_2_0_client_ids': AuthClientAddForm.base_fields['tvm_2_0_client_ids'].separator.join(
                ['1000', '1000', '2000', '5000']
            ),
            'tvm_client_ids': ''
        })

        # Фейковый zookeeper
        fake_zk_auth_settings = [
            {'name': 'alice', 'tvm_2_0_client_ids': [2000, 3000]},
            {'name': 'bob', 'tvm_2_0_client_ids': [4000, 5000]},
        ]
        with patch('api_admin.api_auth.forms.AuthClientAddForm._zk_auth_settings', fake_zk_auth_settings):
            assert_that(form.is_valid(), equal_to(False))
            form.clean()
            assert_that(
                form.errors.as_data(),
                has_entry(
                    'tvm_2_0_client_ids',
                    has_item(
                        has_property(
                            'message',
                            all_of(
                                contains_string(AuthClientAddForm.base_fields['tvm_2_0_client_ids'].label),
                                contains_string('2000'),
                                contains_string('5000'),
                            )
                        )
                    )
                )
            )

    @parameterized.expand([
        ('test=1', True),
        ('with_underline=1', True),
        ('with_underline-and-dash=1', True),
        ('test=123\r\nmulti=3\r\nstring=15', True),
        ('test_double=1.5', False),
        ('test name with space=1', False),
        ('test_name_without_number=', False),
        ('=1', False),
        ('1', False),
        ('=', False),
        ('test_only_name', False),
    ])
    def test_limit_groups_validator(self, limit_groups, validation_result):
        form = AuthClientAddForm({'name': 'enot',
                                  'oauth_client_name': 'enot',
                                  'oauth_client_id': 'enotid',
                                  'auth_methods': [AuthClientAddForm.METHOD_TVM_2_0],
                                  'limit_groups': limit_groups,
                                  'tvm_client_ids': '12345',
                                  'tvm_2_0_client_ids': ''})
        with patch('api_admin.api_auth.forms.AuthClientAddForm.clean_name', return_value='enot'), \
             patch('api_admin.api_auth.forms.AuthClientAddForm.clean_tvm_2_0_client_ids', return_value=[12345]):
            assert_that(form.is_valid(), equal_to(validation_result))
            form.clean()
            if not validation_result:
                assert_that(form.errors.as_data(), has_item('limit_groups'))
