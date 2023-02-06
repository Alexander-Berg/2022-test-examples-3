# -*- coding: utf-8 -*-
import os

from extensions.configurations.sfront.provider import RequestInfoDataProvider
from rtcc.core.common import ConfigurationID
from rtcc.core.session import Session

CONFIG_ID = ConfigurationID.build(location="MSK", project="WEB", domain="RKUB", contour="HAMSTER", instance_type="SFRONT")


def test_request(tmpdir, test_data_dir):
    session1 = Session(tmpdir.strpath)
    config1 = session1.register(RequestInfoDataProvider(
            db_file=os.path.join(test_data_dir, "configurations/request.db"))).get(config_id=CONFIG_ID)
    config2 = session1.register(RequestInfoDataProvider(
            db_file=os.path.join(test_data_dir, "configurations/request.db"))).get(config_id=CONFIG_ID)
    assert len(config1.sources) == len(config2.sources)


def test_request_save_load(tmpdir, test_data_dir):
    session1 = Session(tmpdir.strpath)
    config1 = session1.register(RequestInfoDataProvider(
            db_file=os.path.join(test_data_dir, "configurations/request.db"))).get(config_id=CONFIG_ID)
    session1.save()
    session2 = Session(tmpdir.strpath)
    config2 = session2.register(RequestInfoDataProvider(
            db_file=os.path.join(test_data_dir, "configurations/request.db"))).get(config_id=CONFIG_ID)
    assert len(config1.sources) == len(config2.sources)
