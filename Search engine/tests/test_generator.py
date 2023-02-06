# -*- coding: utf-8 -*-
import pytest

from extensions.configurations.noapache import NoapacheConfig
from rtcc.core.dataobjects import ConfigurationId
from rtcc.core.dataobjects import ConfigurationInfo
from rtcc.core.generator import ExtendedConfigGenerator


@pytest.mark.long
def test_generator_complete():
    config_info = ConfigurationInfo(ConfigurationId("production", "noapache_web_rkub", "msk"), "")
    generator = ExtendedConfigGenerator(NoapacheConfig)
    result = generator.view_config(config_info, "complete")
    assert len(result.list) == 1
    assert result.list[0].name == "apache.ywsearch.cfg"
    assert result.list[0].content.startswith("# Noapache configuration file")


@pytest.mark.long
def test_generator_template():
    config_info = ConfigurationInfo(ConfigurationId("production", "noapache_web_rkub", "msk"), "")
    generator = ExtendedConfigGenerator(NoapacheConfig)
    result = generator.view_config(config_info, "complete")
    assert len(result.list) == 1
    assert result.list[0].name == "apache.ywsearch.cfg"
    assert result.list[0].content.startswith("# Noapache configuration file")
