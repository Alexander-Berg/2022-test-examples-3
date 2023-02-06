# -*- coding: utf-8 -*-
import pytest

from extensions.configurations.noapache import NoapacheConfig
from rtcc.core.common import ConfigurationID
from rtcc.core.session import Session

SESSION = Session()


@pytest.mark.long
def test_complete():
    configuration_id = ConfigurationID.build(instance_type="NOAPACHE", location="MAN", project="IMGS", domain="RKUB",
                                             contour="PRODUCTION")
    config = NoapacheConfig(configuration_id, session=SESSION)
    assert config.view("complete")


@pytest.mark.long
def test_complete2():
    configuration_id = ConfigurationID.build(location="VLA", project="WEB", domain="RKUB",
                                             contour="HAMSTER", instance_type="NOAPACHE")
    config = NoapacheConfig(configuration_id, session=SESSION)
    assert config.view("complete")


def test_template():
    configuration_id = ConfigurationID.build(location="VLA", project="WEB", domain="RKUB",
                                             contour="HAMSTER", instance_type="NOAPACHE")
    config = NoapacheConfig(configuration_id, session=SESSION)
    assert config.view("template")
