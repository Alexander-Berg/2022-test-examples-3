# -*- coding: utf-8 -*-
import pytest

from rtcc.core.dataobjects import ConfigurationId
from rtcc.core.dataobjects import ConfigurationInfo
from rtcc.core.processor import Processor


@pytest.mark.long
def test_generate():
    processor = Processor()
    result = processor.generate_all(
        [ConfigurationInfo(ConfigurationId("production", "noapache_web_rkub", "msk"), "HEAD"),
         ConfigurationInfo(ConfigurationId("production", "noapache_web_rkub", "sas"), "HEAD")])
    assert len(result.list) == 2


@pytest.mark.long
def test_generate_single():
    processor = Processor()
    result = processor.generate(ConfigurationInfo(ConfigurationId("production", "noapache_web_rkub", "msk"), "HEAD"))
    assert len(result.generation_result.list) == 1
