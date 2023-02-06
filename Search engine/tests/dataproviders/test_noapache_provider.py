# -*- coding: utf-8 -*-
import os

from extensions.configurations.noapache.provider import NoapacheDataProvider
from rtcc.core.common import ConfigurationID

CONFIG_ID = ConfigurationID.build(location="VLA", project="WEB", domain="RKUB", contour="HAMSTER",
                                  instance_type="NOAPACHE")


def test_noapache(tmpdir, data_dir):
    noapache_adapter = NoapacheDataProvider(data_path=os.path.join(data_dir, "search/element/"))
    config1 = noapache_adapter.get(config_id=CONFIG_ID)
    assert config1
