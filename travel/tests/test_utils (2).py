# coding: utf8
from contextlib import contextmanager

import mock
import pytest
from django.conf import settings
from django.test.utils import override_settings
from hamcrest import assert_that, has_entries

import travel.rasp.library.python.common23.settings.utils as settings_utils

from travel.rasp.library.python.api_clients.mdb import MdbApiWrapper
from travel.rasp.library.python.common23.settings.utils import (
    use_master_only, get_setting,
    define_setting, bool_converter, merge_db_settings, apply_switch_workflow, get_fallback_conf_from_str,
    get_redis_caches_config
)
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


@pytest.yield_fixture
def resource_explorer_mock():
    with mock.patch.object(settings_utils, 'resource_explorer') as m_resource_explorer:
        m_resource_explorer.get_current_dc.return_value = 'ugr'
        yield m_resource_explorer


@contextmanager
def delete_setting(name):
    try:
        deleted_value = getattr(settings, name)
    except AttributeError:
        yield
    else:
        delattr(settings, name)
        try:
            yield
        finally:
            setattr(settings, name, deleted_value)


class TestGetSetting(object):
    def test_no_env(self):
        name = 'SETTING42'

        with pytest.raises(AttributeError):
            get_setting(name)

        with replace_setting('SETTING42', 'narnia42'):
            assert get_setting(name, default='my42') == 'narnia42'

        assert get_setting(name, default='my42') == 'my42'

    def test_env(self):
        name = 'SETTING42'

        # get from env
        with replace_setting('APPLIED_CONFIG', 'narnia'):
            assert get_setting(name, {
                'production': 'prod42',
                'narnia': 'narnia42',
            }, default='my42') == 'narnia42'

        # env doesn't exist - use default
        with replace_setting('APPLIED_CONFIG', 'testing'):
            assert get_setting(name, {
                'production': 'prod42',
                'narnia': 'narnia42',
            }, default='my42') == 'my42'

        # no APPLIED_CONFIG - use default
        with delete_setting('APPLIED_CONFIG'):
            assert get_setting(name, {
                'production': 'prod42',
                'narnia': 'narnia42',
            }, default='my42') == 'my42'

        # no env, no default
        with replace_setting('APPLIED_CONFIG', 'testing'):
            with pytest.raises(AttributeError):
                get_setting(name, {
                    'production': 'prod42',
                    'narnia': 'narnia42',
                })


class TestDefineSetting(object):
    name = 'SETTING42'

    @classmethod
    def setup_class(cls):
        assert not hasattr(settings, cls.name)

    def teardown_method(self, method):
        if hasattr(settings, self.name):
            delattr(settings, self.name)

    def test_no_default(self):
        with pytest.raises(AssertionError):
            define_setting(self.name)

    def test_no_default_but_override(self):
        setattr(settings, self.name, 'something')
        define_setting(self.name)
        assert getattr(settings, self.name) == 'something'

    def test_with_default_no_override(self):
        define_setting(self.name, default='some_default')
        assert getattr(settings, self.name) == 'some_default'

    def test_with_default_override(self):
        setattr(settings, self.name, 'something')
        define_setting(self.name, default='some_default')
        assert getattr(settings, self.name) == 'something'

    def test_env_match_no_override(self):
        with replace_setting('APPLIED_CONFIG', 'prod'):
            define_setting(self.name, {
                'prod': 'prod_value',
                'other': 'other_value',
            }, default='default_value')
        assert getattr(settings, self.name) == 'prod_value'

    def test_env_match_no_override_callable(self):
        def my_value_getter():
            return 'prod_callable_value'

        with replace_setting('APPLIED_CONFIG', 'prod'):
            define_setting(self.name, {
                'prod': lambda: my_value_getter(),
                'other': 'other_value',
            }, default='default_value')
        assert getattr(settings, self.name) == 'prod_callable_value'

    def test_env_not_match_no_override(self):
        with replace_setting('APPLIED_CONFIG', 'prod2'):
            define_setting(self.name, {
                'prod': 'prod_value',
                'other': 'other_value',
            }, default='default_value')
        assert getattr(settings, self.name) == 'default_value'

    def test_env_not_match_no_default_no_override(self):
        with pytest.raises(AssertionError):
            with replace_setting('APPLIED_CONFIG', 'prod2'):
                define_setting(self.name, {
                    'prod': 'prod_value',
                    'other': 'other_value',
                })

    def test_env_not_match_no_default_but_override(self):
        setattr(settings, self.name, 'override_value')
        with replace_setting('APPLIED_CONFIG', 'prod2'):
            define_setting(self.name, {
                'prod': 'prod_value',
                'other': 'other_value',
            })
        assert getattr(settings, self.name) == 'override_value'

    def test_env_match_but_override(self):
        setattr(settings, self.name, 'override_value')
        with replace_setting('APPLIED_CONFIG', 'prod'):
            define_setting(self.name, {
                'prod': 'prod_value',
                'other': 'other_value',
            })
        assert getattr(settings, self.name) == 'override_value'

    def test_with_django_override_setting(self):
        with override_settings(SETTING_43=42):
            with override_settings(SETTING_44=42):
                define_setting(self.name, default=1)

        assert getattr(settings, self.name) == 1

    def test_environ_override_no_settings(self):
        with mock.patch.dict('os.environ', {'RASP_{}'.format(self.name): 'newvalue'}):
            define_setting(self.name, default=1)

        assert getattr(settings, self.name) == 'newvalue'

    def test_environ_override_overrided_settings(self):
        setattr(settings, self.name, 'override_value')
        with mock.patch.dict('os.environ', {'RASP_{}'.format(self.name): 'newvalue'}):
            define_setting(self.name, default=1)

        assert getattr(settings, self.name) == 'newvalue'


class TestEnvironOverrideBoolConveter(object):
    name = 'SETTING42'

    @classmethod
    def setup_class(cls):
        assert not hasattr(settings, cls.name)

    def teardown_method(self, method):
        if hasattr(settings, self.name):
            delattr(settings, self.name)

    @pytest.mark.parametrize('value', ('t', '1', 'true', 'y', 'yes'))
    def test_yes(self, value):
        with mock.patch.dict('os.environ', {'RASP_{}'.format(self.name): value}):
            define_setting(self.name, default=object(), converter=bool_converter)
        assert getattr(settings, self.name) is True

    @pytest.mark.parametrize('value', ('f', '0', 'false', 'n', 'no', ''))
    def test_no(self, value):
        with mock.patch.dict('os.environ', {'RASP_{}'.format(self.name): value}):
            define_setting(self.name, default=object(), converter=bool_converter)
        assert getattr(settings, self.name) is False

    def test_error(self):
        with mock.patch.dict('os.environ', {'RASP_{}'.format(self.name): 'hzkz'}):
            with pytest.raises(Exception):
                define_setting(self.name, default=object(), converter=bool_converter)


def test_merge_db_settings():
    sett_base = {
        'NAME': 'dbname42',
        'USER': 'user123',
        'CLUSTER': {
            'CLUSTER_ID': 'someid',
            'USE_REPLICAS': False,
        }
    }

    sett_new = {
        'NAME': 'dbname_other',
        'PASWWORD': '4567',
        'CLUSTER': {
            'USE_REPLICAS': True,
            'USE_MASTER': False,
        }
    }

    merge_db_settings(sett_base, sett_new)
    assert sett_base == {
        'NAME': 'dbname_other',
        'PASWWORD': '4567',
        'USER': 'user123',
        'CLUSTER': {
            'CLUSTER_ID': 'someid',
            'USE_REPLICAS': True,
            'USE_MASTER': False,
        }
    }


def test_apply_switch_workflow():
    settings = {
        'DATABASES': {
            'default': {
                'USER': 'user42',
                'PASSWORD': 'pwd',
            },
            'some_alias': {'a': 1, 'CLUSTER': {}}
        },
        'MAINTENANCE_DB': 'maint_db42',
        'WORK_DB': 'work_db42',
        'SERVICE_DB': 'service_db42',
        'OTHER_SETTING': 123
    }

    apply_switch_workflow(
        settings,
        maint_conf={
            'USER': 'rasp',
            'PASSWORD': '1234567',
            'CLUSTER': {
                'CLUSTER_ID': 'rasp_mdb_maintenance_id_42',
            }

        },
        main0_conf={
            'USER': 'rasp',
            'PASSWORD': '12345678',
            'NAME': 'raspdbmdb0',
            'CLUSTER': {
                'CLUSTER_ID': 'rasp_mdb_main0_id_42',
                'MDB_API_CALL_ENABLED': True,
            }
        },
        main1_conf={
            'USER': 'rasp_other',
            'PASSWORD': '123456789',
            'CLUSTER': {
                'CLUSTER_ID': 'rasp_mdb_main1_id_42',
                'USE_MASTER': True,
                'CHECK_MASTER_ON_EACH_CONNECT': True,
            }

        },
    )

    assert settings == {
        'MAINTENANCE_DB': 'maint_db42',
        'WORK_DB': 'work_db42',
        'SERVICE_DB': 'service_db42',
        'OTHER_SETTING': 123,
        'MAINTENANCE_DB_ENABLED': True,
        'DATABASES': {
            'some_alias': {'a': 1, 'CLUSTER': {}},
            'default': {
                'ENGINE': 'travel.rasp.library.python.common23.db.backends.alias_proxy',
                'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.switcher.instance_role_alias',
            },
            'maint_db42': {
                'ENGINE': 'travel.rasp.library.python.common23.db.backends.cluster_mdb',
                'STORAGE_ENGINE': 'InnoDB',
                'NAME': 'rasp_maintenance',
                'USER': 'rasp',
                'PASSWORD': '1234567',
                'CLUSTER': {
                    'CLUSTER_ID': 'rasp_mdb_maintenance_id_42',
                    'MDB_API_CALL_ENABLED': False,
                    'CHECK_MASTER_ON_EACH_CONNECT': False,
                }
            },
            'service_db42': {
                'ENGINE': 'travel.rasp.library.python.common23.db.backends.alias_proxy',
                'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.switcher.service_alias',
            },
            'work_db42': {
                'ENGINE': 'travel.rasp.library.python.common23.db.backends.alias_proxy',
                'ALIAS_GETTER': 'travel.rasp.library.python.common23.db.switcher.work_alias',
            },
            'main0_rasp': {
                'ENGINE': 'travel.rasp.library.python.common23.db.backends.cluster_mdb',
                'STORAGE_ENGINE': 'InnoDB',
                'USER': 'rasp',
                'PASSWORD': '12345678',
                'NAME': 'raspdbmdb0',
                'CLUSTER': {
                    'CLUSTER_ID': 'rasp_mdb_main0_id_42',
                    'MDB_API_CALL_ENABLED': True,
                    'CHECK_MASTER_ON_EACH_CONNECT': False,
                },
            },
            'main1_rasp': {
                'ENGINE': 'travel.rasp.library.python.common23.db.backends.cluster_mdb',
                'STORAGE_ENGINE': 'InnoDB',
                'USER': 'rasp_other',
                'PASSWORD': '123456789',
                'NAME': 'rasp',
                'CLUSTER': {
                    'CLUSTER_ID': 'rasp_mdb_main1_id_42',
                    'MDB_API_CALL_ENABLED': False,
                    'USE_MASTER': True,
                    'CHECK_MASTER_ON_EACH_CONNECT': True,
                },
            },
        }
    }


def test_use_master_only():

    def create_settings():
        return {
            'S': 43,
            'DATABASES': {
                'default': {
                    'USER': 'user42',
                    'PASSWORD': 'pwd',
                },
                'maint': {
                    'a': 123,
                    'CLUSTER': {
                        'USE_REPLICAS': True,
                        'USE_MASTER': True,
                        'SOME': 42,
                    }
                },
                'rasp': {
                    'b': 42,
                    'CLUSTER': {
                        'SOME': 43,
                    }
                },
            },
        }

    settings = create_settings()
    use_master_only(settings, ['rasp'])

    assert settings == {
        'DATABASES': {
            'default': {
                'USER': 'user42',
                'PASSWORD': 'pwd',
            },
            'maint': {
                'a': 123,
                'CLUSTER': {
                    'USE_REPLICAS': True,
                    'USE_MASTER': True,
                    'SOME': 42,
                }
            },
            'rasp': {
                'b': 42,
                'CLUSTER': {
                    'SOME': 43,
                    'USE_REPLICAS': False,
                    'USE_MASTER': True,
                    'CHECK_MASTER_ON_EACH_CONNECT': True,
                }
            },
        },
        'S': 43,
    }

    settings = create_settings()
    use_master_only(settings)

    assert settings == {
        'DATABASES': {
            'default': {
                'USER': 'user42',
                'PASSWORD': 'pwd',
            },
            'maint': {
                'a': 123,
                'CLUSTER': {
                    'USE_REPLICAS': False,
                    'USE_MASTER': True,
                    'SOME': 42,
                    'CHECK_MASTER_ON_EACH_CONNECT': True,
                }
            },
            'rasp': {
                'b': 42,
                'CLUSTER': {
                    'SOME': 43,
                    'USE_REPLICAS': False,
                    'USE_MASTER': True,
                    'CHECK_MASTER_ON_EACH_CONNECT': True,
                }
            },
        },
        'S': 43,
    }


def test_fallback_conf_from_str():
    assert get_fallback_conf_from_str('') is None

    assert get_fallback_conf_from_str('host1, host2, host3') == {
        'MASTER': 'host1',
        'REPLICAS': ['host2', 'host3'],
    }

    assert get_fallback_conf_from_str('host3') == {
        'MASTER': 'host3',
        'REPLICAS': [],
    }


def test_get_redis_caches_config(resource_explorer_mock):
    cluster_info = mock.MagicMock()
    cluster_info.master = mock.MagicMock(dc='myt', hostname='myt-0')
    cluster_info.replicas = [mock.MagicMock(dc='sas', hostname='sas-1'), mock.MagicMock(dc='man', hostname='man-2')]
    cluster_info.instances = cluster_info.replicas + [cluster_info.master]

    def get_config():
        return get_redis_caches_config(
            cluster_id='yc_cluster_id',
            cluster_password='pass',
            cluster_name='yc_cluster_name',
            fallback_hosts=['fall-0', 'fall-1']
        )

    with mock.patch.object(MdbApiWrapper,  'get_cluster_info') as m_get_cluster_info:
        m_get_cluster_info.return_value = cluster_info
        resource_explorer_mock.get_current_dc.return_value = 'sas'
        config = get_config()
        assert config == {
            'default': {
                'OPTIONS': {
                    'SOCKET_TIMEOUT': 2,
                    'SENTINEL_HOSTS': [('myt-0', 26379), ('sas-1', 26379)],
                    'SENTINEL_SERVICE_NAME': 'yc_cluster_name',
                    'SENTINEL_SOCKET_TIMEOUT': 0.2,
                    'CLIENT_CLASS': 'travel.rasp.library.python.common23.db.redis.django_redis_sentinel_client.DjangoRedisSentinelClient',
                    'CURRENT_DC': 'sas',
                    'PASSWORD': 'pass'
                },
                'BACKEND': 'django_redis.cache.RedisCache',
                'LOCATION': ['redis://myt-0:6379', 'redis://sas-1:6379'],
                'LONG_TIMEOUT': 86400,
                'TIMEOUT': 120
            }
        }

        resource_explorer_mock.get_current_dc.return_value = 'man'
        config = get_config()
        assert_that(config['default'], has_entries({
            'LOCATION': ['redis://myt-0:6379', 'redis://man-2:6379'],
            'OPTIONS': has_entries({
                'CURRENT_DC': 'man',
                'SENTINEL_HOSTS': [('myt-0', 26379), ('man-2', 26379)],
            })
        }))

        resource_explorer_mock.get_current_dc.return_value = 'vla'
        config = get_config()
        assert_that(config['default'], has_entries({
            'LOCATION': ['redis://myt-0:6379', 'redis://sas-1:6379', 'redis://man-2:6379'],
            'OPTIONS': has_entries({
                'CURRENT_DC': 'vla',
                'SENTINEL_HOSTS': [('myt-0', 26379), ('sas-1', 26379), ('man-2', 26379)],
            })
        }))

        # fallback_hosts
        m_get_cluster_info.return_value = None
        resource_explorer_mock.get_current_dc.return_value = 'sas'
        config = get_config()
        assert_that(config['default'], has_entries({
            'LOCATION': ['redis://fall-0:6379', 'redis://fall-1:6379'],
            'OPTIONS': has_entries({
                'CURRENT_DC': 'sas',
                'SENTINEL_HOSTS': [('fall-0', 26379), ('fall-1', 26379)],
            })
        }))
