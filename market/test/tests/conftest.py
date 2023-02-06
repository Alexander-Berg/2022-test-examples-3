# coding: utf8
import yatest
import pytest
from ruamel.yaml import YAML

from config import Config
from config import get_template_dir, get_temp_dir, get_cert_dir, get_common_dir   # noqa


ENV = 'testing'


def get_value_dirs():
    return [
        yatest.common.source_path('market/sre/conf/fslb/common/src/balancer/values-available'),
        yatest.common.source_path('market/sre/conf/fslb/test/values-available'),
        yatest.common.source_path('market/sre/conf/fslb/test/values-static'),
    ]


@pytest.fixture
def ip_service_map():
    """ Возвращает IP сервисов сконфигурированные в файле 00-ip-service-map.yaml """
    yaml = YAML(typ='safe')
    with open(yatest.common.source_path('market/sre/conf/fslb/test/values-static/00-ip-service-map.yaml')) as fd:
        return yaml.load(fd)


config = Config(get_value_dirs)
