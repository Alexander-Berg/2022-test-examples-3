# -*- coding: utf-8 -*-
import pytest

from extensions.configurations.sfront import RequestConfig
from rtcc.core.common import ConfigurationID
from rtcc.core.session import Session

SESSION = Session()


@pytest.mark.long
def test_complete(test_dir):
    configuration_id = ConfigurationID.build(location="VLA", project="WEB", domain="RKUB", contour="HAMSTER",
                                             instance_type="SFRONT")
    config_view = RequestConfig(configuration_id, session=SESSION).view("complete")
    assert config_view


def test_template(test_dir):
    configuration_id = ConfigurationID.build(location="VLA", project="WEB", domain="RKUB", contour="HAMSTER",
                                             instance_type="SFRONT")
    config_view = RequestConfig(configuration_id, session=SESSION).view("template")
    assert config_view
