# -*- coding: utf-8 -*-
import pytest

from extensions.configurations.apphost import ApphostConfig
from extensions.configurations.apphost import ApphostGenerator
from rtcc.core.dataobjects import ConfigurationId
from rtcc.core.dataobjects import ConfigurationInfo
from rtcc.core.dataobjects import PatchInfo
from rtcc.core.generator import ExtendedConfigGenerator


@pytest.mark.long
def test_apphost_simple():
    generator = ApphostGenerator(local_path="apphost-generator")
    generator.install()

    generator.generate('WEB', 'MSK_WEB_APP_HOST_RKUB')
    assert generator


@pytest.mark.long
def test_apphost_config():
    config_info = ConfigurationInfo(ConfigurationId("production", "web_app_host_rkub", "msk"), "")
    generator = ExtendedConfigGenerator(ApphostConfig)
    result = generator.view_config(config_info, "complete")
    assert result


@pytest.mark.long
def test_apphost_config_patch():
    config_info = ConfigurationInfo(
        ConfigurationId("priemka_test", "web_app_host_rkub", "msk"),
        "",

        patch=PatchInfo(
            ConfigurationId("production", "web_app_host_rkub", "msk"),
            {"RTMR": {
                "graph": "atom",
                "path": "/path",
                "port": 12345,
                "slb": "hamster.yandex.ru"
            }}
        ))
    generator = ExtendedConfigGenerator(ApphostConfig)
    result = generator.view_config(config_info, "complete")
    assert result
