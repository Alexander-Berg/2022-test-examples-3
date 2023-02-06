import asyncio
from random import shuffle

import pytest

from sendr_settings import Config, HotSetting, PgSource
from sendr_settings.db import SettingsEntity
from sendr_settings.exceptions import HotSettingsUnsupportedTypeException


class TestSettings:
    @pytest.fixture
    def config(self):
        return Config()

    @pytest.fixture(params=(None, True, '1', 1))
    def value(self, request):
        return request.param

    def test_set_get_item(self, config, value):
        config['key'] = value
        assert config['key'] == value

    def test_set_get_attr(self, config, value):
        config.key = value
        assert config.key == value

    def test_set_attr_get_item(self, config, value):
        config.key = value
        assert config['key'] == value

    def test_set_item_get_attr(self, config, value):
        config['key'] = value
        assert config.key == value

    def test_set_item_del(self, config, value):
        config['key'] = value
        del config['key']
        with pytest.raises(KeyError):
            config['key']

    def test_del_not_found(self, config, value):
        with pytest.raises(KeyError):
            del config['key']

    def test_del_attr(self, config, value):
        config.key = value
        del config.key
        with pytest.raises(AttributeError):
            config.key

    def test_del_not_found_attr(self, config, value):
        with pytest.raises(AttributeError):
            del config.key

    def test_set_item_del_attr(self, config, value):
        config['key'] = value
        del config.key
        with pytest.raises(AttributeError):
            config.key

    def test_set_attr_del_item(self, config, value):
        config.key = value
        del config['key']
        with pytest.raises(AttributeError):
            config.key

    class TestGetConfigFiles:
        @pytest.fixture
        def env(self):
            return 'development'

        @pytest.fixture
        def list_files(self, env):
            files = [
                f'{n}.conf' + (f'.{file_env}' if file_env else '')
                for n in range(5)
                for file_env in (env, 'other', None)
            ]
            shuffle(files)
            return files

        @pytest.fixture(autouse=True)
        def mock_list_files(self, mocker, list_files):
            mocker.patch('sendr_settings.list_files', mocker.Mock(return_value=list_files))

        def test_get_config_files(self, config, env, list_files):
            returned_file_names = [
                readable.path
                for readable in config.get_config_files('', env)
            ]
            assert returned_file_names == sorted([
                file_name
                for file_name in list_files
                if file_name.endswith('.conf') or file_name.endswith(f'.conf.{env}')
            ])


class TestHotSettings:
    @pytest.fixture
    async def config(self, db_engine, logger_mock):
        config = Config()
        await config.add_pg_hot_source(
            db_engine=db_engine,
            logger=logger_mock,
        )
        return config

    @pytest.fixture
    def set_low_refresh_interval(self):
        PgSource.refresh_interval = 0.05
        yield
        PgSource.refresh_interval = 30

    @pytest.mark.asyncio
    async def test_should_return_fallback_value_if_not_inited(self):
        config = Config()
        config.TEST = HotSetting(fallback_value=10)

        assert config.TEST == 10

        config['TEST'] = HotSetting(fallback_value=20)

        assert config['TEST'] == 20

    @pytest.mark.asyncio
    async def test_should_raise_if_setting_type_is_unsupported(self, config):
        with pytest.raises(HotSettingsUnsupportedTypeException):
            config.TEST = HotSetting(fallback_value=dict())  # noqa

    @pytest.mark.asyncio
    async def test_should_instantly_refresh_on_add_hot_settings_source(self, storage, db_engine, logger_mock):
        config = Config()
        config.TEST = HotSetting(fallback_value=1)
        await storage.settings.create(SettingsEntity(
            key='TEST',
            value='5',
        ))

        await config.add_pg_hot_source(db_engine=db_engine, logger=logger_mock)

        from_db = config.TEST
        assert from_db == 5

    @pytest.mark.asyncio
    @pytest.mark.parametrize(('fallback_value', 'db_value', 'expected'), [
        (0.1, '0.5', 0.5),
        (1, '2', 2),
        (True, 'False', False),
        ('test', 'db_test', 'db_test'),
    ])
    async def test_supported_types_mapping(
        self,
        storage,
        db_engine,
        logger_mock,
        fallback_value,
        db_value,
        expected,
    ):
        config = Config()
        config.TEST = HotSetting(fallback_value=fallback_value)
        await storage.settings.create(SettingsEntity(
            key='TEST',
            value=db_value,
        ))

        await config.add_pg_hot_source(db_engine=db_engine, logger=logger_mock)

        from_db = config.TEST
        assert from_db == expected

    @pytest.mark.asyncio
    async def test_settings_are_cached(self, storage, db_engine, logger_mock):
        config = Config()
        config.TEST = HotSetting(fallback_value=1)
        setting_entity = SettingsEntity(
            key='TEST',
            value='2',
        )
        await storage.settings.create(setting_entity)

        await config.add_pg_hot_source(db_engine=db_engine, logger=logger_mock)

        setting_entity.value = '3'
        await storage.settings.save(setting_entity)

        assert config.TEST == 2

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('set_low_refresh_interval')
    async def test_should_refresh_settings_after_interval(self, storage, db_engine, logger_mock):
        config = Config()
        config.TEST = HotSetting(fallback_value=1)
        setting_entity = SettingsEntity(
            key='TEST',
            value='2',
        )
        await storage.settings.create(setting_entity)

        await config.add_pg_hot_source(db_engine=db_engine, logger=logger_mock)

        setting_entity.value = '3'
        await storage.settings.save(setting_entity)

        for i in range(10):
            if config.TEST == 3:
                return
            await asyncio.sleep(0.1)

        assert config.TEST == 3

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('set_low_refresh_interval')
    async def test_can_add_hot_setting_after_initialization(self, storage, config):
        setting_entity = SettingsEntity(
            key='NEW_SETTING',
            value='10',
        )
        await storage.settings.create(setting_entity)
        config.NEW_SETTING = HotSetting(fallback_value=1)

        for i in range(10):
            if config.NEW_SETTING == 10:
                return
            await asyncio.sleep(0.1)

        assert config.NEW_SETTING == 10

    @pytest.mark.asyncio
    @pytest.mark.parametrize(('db_value', 'expected'), [
        ('1', True),
        ('True', True),
        ('true', True),
        ('0', False),
        ('False', False),
        ('false', False),
        ('anything_else', True),
    ])
    async def test_expected_bool_mapping(
        self,
        storage,
        db_engine,
        logger_mock,
        db_value,
        expected,
    ):
        config = Config()
        config.TEST = HotSetting(fallback_value=True)
        await storage.settings.create(SettingsEntity(
            key='TEST',
            value=db_value,
        ))

        await config.add_pg_hot_source(db_engine=db_engine, logger=logger_mock)

        from_db = config.TEST
        assert from_db == expected

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('set_low_refresh_interval')
    async def test_should_return_last_stored_value_after_closing(self, storage, db_engine, logger_mock):
        config = Config()
        config.TEST = HotSetting(fallback_value=1)
        setting_entity = SettingsEntity(
            key='TEST',
            value='2',
        )
        await storage.settings.create(setting_entity)

        await config.add_pg_hot_source(db_engine=db_engine, logger=logger_mock)
        await asyncio.sleep(0.05)
        await config.close_hot_source()

        setting_entity.value = '3'
        await storage.settings.save(setting_entity)
        await asyncio.sleep(0.1)

        assert config.TEST == 2
