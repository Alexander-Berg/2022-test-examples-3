# -*- coding: utf-8 -*-
import os

import yatest
import pytest

from clusters_monitoring import (
    ActivationSettings,
    read_thresholds,
)

CONFIGS_DIR = yatest.common.source_path('market/pinger-report/etc/conf-available/')


@pytest.mark.parametrize('config_name', [
    c for c in os.listdir(CONFIGS_DIR)
    if c.endswith('.ini') and not c.startswith('.')
])
def test_read_thresholds_from_configs(config_name):
    '''
    validate that all configs can be read
    '''
    config_path = os.path.join(CONFIGS_DIR, config_name)
    read_thresholds(config_path)


def test_read_activation_settings_for_fresh():
    '''
    validate that activation settings are readable from production.fresh.ini
    and all monitorings should be disabled
    '''
    config_path = os.path.join(CONFIGS_DIR, 'production.fresh.ini')
    settings = ActivationSettings.from_config(config_path)
    assert not settings.is_black_clusters_enabled
    assert not settings.is_old_prep_report_version_enabled
    assert not settings.is_disaster_enabled


def test_read_activation_settings_for_generic_prod():
    '''
    validate that activation settings are readable from production.ini
    and all monitorings should be enabled (default behavior)
    '''
    config_path = os.path.join(CONFIGS_DIR, 'production.ini')
    settings = ActivationSettings.from_config(config_path)
    assert settings.is_black_clusters_enabled
    assert settings.is_old_prep_report_version_enabled
    assert settings.is_disaster_enabled
