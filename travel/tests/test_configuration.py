# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.settings.configuration import Configuration
from travel.rasp.library.python.common23.settings.configuration.base import BaseConfigurator


class SomeConfigurator(BaseConfigurator):
    def apply_base(self, settings):
        settings['INDEPENDED'] = 'independed'

    def apply_development(self, settings):
        settings['DEPENDED'] = 'development_depended'

    def apply_production(self, settings):
        settings['DEPENDED'] = 'production_depended'

    def apply_stress(self, settings):
        settings['DEPENDED'] = 'stress_depended'

    def apply_testing(self, settings):
        settings['DEPENDED'] = 'testing_depended'

    def apply_after_all(self, settings):
        settings['DEPENDED_AFTER_ALL'] = settings['DEPENDED'] + '_after'


@pytest.mark.parametrize('env_type, env_name, applied_config, depended_setting', [
    ['production', 'any_name', 'production', 'production_depended'],
    ['testing', 'any_name', 'testing', 'testing_depended'],
    ['stress', 'any_name', 'stress', 'stress_depended'],
    ['development', 'any_name', 'development', 'development_depended'],

])
def test_valid_environments(env_type, env_name, applied_config, depended_setting):
    settings = {
        'YANDEX_ENVIRONMENT_TYPE': env_type,
        'YANDEX_ENVIRONMENT_NAME': env_name,
        'YANDEX_DATA_CENTER': 'xxx'
    }
    Configuration([SomeConfigurator]).apply(settings)

    assert applied_config == applied_config
    assert settings['DEPENDED'] == depended_setting
    assert settings['DEPENDED_AFTER_ALL'] == depended_setting + '_after'
    assert settings['INDEPENDED'] == 'independed'


def test_invalid_environments():
    settings = {
        'YANDEX_ENVIRONMENT_TYPE': 'bad_type',
        'YANDEX_ENVIRONMENT_NAME': 'bad_name',
        'YANDEX_DATA_CENTER': 'xxx'
    }

    with pytest.raises(Exception) as e:
        Configuration([SomeConfigurator]).apply(settings)

    assert e.value.args == ('Environment type {!r} not supported'.format('bad_type'),)


def test_call_order():
    class Configurator1(BaseConfigurator):
        def apply_base(self, settings):
            settings['DEPENDED'] = '1'

        def apply_production(self, settings):
            settings['DEPENDED'] += '2'

        def apply_after_all(self, settings):
            settings['DEPENDED'] += '3'

    class Configurator2(BaseConfigurator):
        def apply_base(self, settings):
            settings['DEPENDED'] += '4'

        def apply_production(self, settings):
            settings['DEPENDED'] += '5'

        def apply_after_all(self, settings):
            settings['DEPENDED'] += '6'

    settings = {
        'YANDEX_ENVIRONMENT_TYPE': 'production',
        'YANDEX_ENVIRONMENT_NAME': 'production_name',
        'YANDEX_DATA_CENTER': 'xxx'
    }
    Configuration([Configurator1, Configurator2]).apply(settings)
    assert settings['DEPENDED'] == '123456'
