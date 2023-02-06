#!/usr/bin/python
# -*- coding: utf-8 -*-
"""
Проверка, что конфиг загружается для всех возможных окружений и не райзятся ошибки
"""
import os
import mock

from unittest import TestCase
from mpfs.config import builder
from mpfs.config.builder import YAMLConfigProvider
from mpfs.config.builder import SECRET_KEYS_SETTINGS_PATH, GLOBAL_SETTINGS_PATH


class ConfigTestCase(TestCase):
    ALLOWED_ENVIRONMENTS = ('development', 'testing', 'prestable', 'production')
    ALLOWED_PACKAGES = ('disk', 'platform')

    @classmethod
    def setup_class(cls):
        cls.original_mpfs_pkg = os.environ.get('MPFS_PACKAGE', 'disk')
        cls.original_mpfs_env = os.environ.get('MPFS_ENVIRONMENT', 'development')

    @classmethod
    def teardown_class(cls):
        os.environ['MPFS_PACKAGE'] = cls.original_mpfs_pkg
        os.environ['MPFS_ENVIRONMENT'] = cls.original_mpfs_env
        import mpfs.config
        reload(mpfs.config)

    def test_config_overrides(self):
        for env in self.ALLOWED_ENVIRONMENTS:
            for pkg in self.ALLOWED_PACKAGES:
                os.environ['MPFS_PACKAGE'] = pkg
                os.environ['MPFS_ENVIRONMENT'] = env
                try:
                    import mpfs.config
                    reload(mpfs.config)
                except Exception:
                    print 'Failed at: PACKAGE: "%s". ENV: "%s"' % (pkg, env)
                    raise

    def test_secret_keys(self):
        FAKE_SECRET_KEYS_MAPPER = {
            'zaberun url': 'services zaberun secret_key',
            'zaberun stid_aes': 'services zaberun stid_secret'
        }

        class FakeYAMLConfigProvider(YAMLConfigProvider):
            def load_as_dict(self):
                if self.config_path == GLOBAL_SETTINGS_PATH:
                    return {
                        'services': {
                            'zaberun': {
                                'secret_key': 'secret_key',
                                'stid_secret': 'stid_secret',
                            },
                            'Passport': {
                                'secret_key': 'secret_key',
                                'stid_secret': 'stid_secret',
                            }
                        }
                    }
                elif self.config_path == SECRET_KEYS_SETTINGS_PATH:
                    return {
                        'zaberun': {
                            'url': '123',
                            'stid_aes': '456',
                        },
                    }

                return {}

        with mock.patch.object(builder, 'SECRET_KEYS_MAPPER', FAKE_SECRET_KEYS_MAPPER):
            with mock.patch.object(builder, 'YAMLConfigProvider', FakeYAMLConfigProvider):
                import mpfs.config
                reload(mpfs.config)

                from mpfs.config import settings
                assert settings.services['zaberun']['secret_key'] == '123'
                assert settings.services['zaberun']['stid_secret'] == '456'
                assert settings.services['Passport']['secret_key'] == 'secret_key'
                assert settings.services['Passport']['stid_secret'] == 'stid_secret'
