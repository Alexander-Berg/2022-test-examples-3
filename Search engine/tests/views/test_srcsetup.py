# -*- coding: utf-8 -*-
import pytest

from extensions.configurations.srcsetup import SrcSetupConfig
from extensions.configurations.srcsetup import SrcSetupGenerator
from rtcc.core.dataobjects import ConfigurationId
from rtcc.core.dataobjects import ConfigurationInfo
from rtcc.core.dataobjects import PatchInfo
from rtcc.core.generator import ExtendedConfigGenerator


@pytest.mark.long
def test_srcsetup_simple():
    generator = SrcSetupGenerator(local_path="srcsetup-generator")
    generator.install()

    data = generator.generate('WEB', 'MSK_WEB_AH_SRC_SETUP_RKUB')
    assert data


@pytest.mark.long
def test_srcsetup_config():
    config_info = ConfigurationInfo(ConfigurationId("production", "web_ah_src_setup_rkub", "msk"), "")
    generator = ExtendedConfigGenerator(SrcSetupConfig)
    result = generator.view_config(config_info, "complete")
    assert result


@pytest.mark.long
def test_srcsetup_config_patch():
    config_info = ConfigurationInfo(
        ConfigurationId("priemka_test", "web_ah_src_setup_rkub", "msk"),
        "",

        patch=PatchInfo(
            ConfigurationId("production", "web_ah_src_setup_rkub", "msk"),
            {"RTMR": {
                "graph": "atom",
                "path": "/path",
                "port": 12345,
                "slb": "hamster.yandex.ru"
            }}
        ))
    generator = ExtendedConfigGenerator(SrcSetupConfig)
    result = generator.view_config(config_info, "complete")
    assert result
