# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest
from hamcrest import assert_that, has_entries

from travel.rasp.library.python.common23.dynamic_settings.core import DynamicSetting, Storage, NOTHING, Settings, DEFAULT_CACHE_TIME
from travel.rasp.library.python.common23.tester.utils.datetime import replace_now


class InMemoryStorage(Storage):
    def __init__(self, **kwargs):
        super(InMemoryStorage, self).__init__()
        self.data = kwargs

    def get(self, setting_name):
        return self.data.get(setting_name, NOTHING)

    def set(self, setting_name, value):
        self.data[setting_name] = value

    def get_setting_info(self, setting_name):
        info = super(InMemoryStorage, self).get_setting_info(setting_name)
        info['cool'] = 'stuff'
        return info


class TestStorage(object):
    def test_get_setting_info(self):
        storage = InMemoryStorage(a=1)
        assert storage.get_setting_info('a') == {'value': 1, 'cool': 'stuff'}


class TestSettings(object):
    def test_settings_registrations(self):
        storage = InMemoryStorage(a=1)
        settings = Settings(storage=storage, a=DynamicSetting(2), b=DynamicSetting(3))

        assert settings.a == 1
        assert settings.b == 3

        with pytest.raises(KeyError):
            settings.c

        settings.register_setting('c', DynamicSetting(42))
        assert settings.c == 42
        settings.c = 43
        assert settings.c == 43

        settings.register_settings(d=DynamicSetting(5), e=DynamicSetting(6))
        settings.d = 7
        assert settings.d == 7
        assert settings.e == 6

    def test_get_settings(self):
        s1, s2 = DynamicSetting(2), DynamicSetting(3)
        settings = Settings(storage=InMemoryStorage(), a=s1, b=s2)
        sett_dict = settings.get_settings()
        assert sorted(list(sett_dict.items()), key=lambda kv: kv[0]) == [('a', s1), ('b', s2)]

        # проверяем, что изменение sett_dict не влияет на внутренности settings
        sett_dict['a'] = DynamicSetting(42)
        assert sorted(list(settings.get_settings().items()), key=lambda kv: kv[0]) == [('a', s1), ('b', s2)]

    def test_saving_context(self):
        storage = InMemoryStorage(a=1)
        settings = Settings(storage=storage, a=DynamicSetting(2))

        assert storage._saving_context == {}
        with settings.saving_context(user='admin'):
            assert storage._saving_context == {'user': 'admin'}
        assert storage._saving_context == {}

    def test_get_setting_info(self):
        storage = InMemoryStorage(a=1)
        settings = Settings(
            storage=storage,
            a=DynamicSetting(2, description='my super a'),
            b=DynamicSetting(3, cache_time=1)
        )

        assert_that(settings.get_setting_info('a'), has_entries({
            'default_value': 2,
            'cached_at': None,
            'cached_value': NOTHING,
            'cache_time': timedelta(seconds=DEFAULT_CACHE_TIME),
            'description': 'my super a',
            'stored_info': {'value': 1, 'cool': 'stuff'},
        }))

        assert_that(settings.get_setting_info('b'), has_entries({
            'default_value': 3,
            'cached_at': None,
            'cached_value': NOTHING,
            'cache_time': timedelta(seconds=1),
            'description': '',
            'stored_info': {'value': NOTHING, 'cool': 'stuff'},
        }))

        with replace_now('2018-11-02 13:15:01'):
            settings.a  # get value into cache

            assert_that(settings.get_setting_info('a'), has_entries({
                'default_value': 2,
                'cached_at': datetime(2018, 11, 2, 13, 15, 1),
                'cached_value': 1,
                'cache_time': timedelta(seconds=DEFAULT_CACHE_TIME),
                'description': 'my super a',
                'stored_info': {'value': 1, 'cool': 'stuff'},
            }))


class TestDynamicSetting(object):
    def test_get_without_caching(self):
        storage = InMemoryStorage()
        setting = DynamicSetting(2, storage=storage, name='a')
        assert setting.get() == setting.get_from_storage() == 2

        storage = InMemoryStorage(a=1)
        setting = DynamicSetting(2, storage=storage, name='a')
        assert setting.get() == setting.get_from_storage() == 1

        storage = InMemoryStorage(a='1')
        setting = DynamicSetting(2, storage=storage, name='a', converter=int)
        assert setting.get() == setting.get_from_storage() == 1

    def test_get_with_caching(self):
        storage = InMemoryStorage(a=1)
        setting = DynamicSetting(2, storage=storage, name='a', cache_time=10)

        now = datetime(2011, 10, 20, 12, 34, 40)
        with replace_now(now):
            assert setting.get() == 1
            storage.set('a', 2)  # меняем значение "в базе"
            assert setting.get() == 1
            assert setting.get_from_storage() == 2

        with replace_now(now + timedelta(seconds=8)):
            assert setting.get() == 1

        with replace_now(now + timedelta(seconds=11)):
            assert setting.get() == 2

    def test_set(self):
        storage = InMemoryStorage(a=1)
        setting = DynamicSetting(2, storage=storage, name='a', converter=str, cache_time=100500)
        assert setting.get() == '1'
        setting.set(3)
        assert setting.get() == '3'
