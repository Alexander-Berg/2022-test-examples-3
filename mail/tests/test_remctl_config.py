import pytest
import yaml
import mail.sendbernar.package.scripts.remctl as remctl

from mail.webmail_config.lib.make_config import make_config
from library.python import resource


@pytest.fixture(scope='module', autouse=True)
def gen_config():
    pytest.base = resource.find('sendbernar/sendbernar.yml')
    pytest.service_yaml = yaml.safe_load(resource.find('sendbernar/service.yaml'))
    pytest.common_yaml = yaml.safe_load(resource.find('webmail_config/common.yaml'))
    pytest.db_yaml = yaml.safe_load(resource.find('ymod_cachedb/service.yaml'))


@pytest.mark.parametrize('env', [
    'production',
    'development',
    'testing',
])
def test_parse_callmeback_tvm_id(env):
    cfg = make_config(env, pytest.base, pytest.service_yaml, pytest.common_yaml, pytest.db_yaml, silent=True)
    assert remctl.get_callmebacks_tvm_id_from_config(cfg) is not None


@pytest.mark.parametrize('env', [
    'production',
    'development',
    'testing',
])
def test_parse_default_callmeback_url(env):
    cfg = make_config(env, pytest.base, pytest.service_yaml, pytest.common_yaml, pytest.db_yaml, silent=True)
    assert remctl.get_callmebacks_url_from_config(cfg, remctl.CALLMEBACK_CONFIG_PATH, 'sendbernar') is not None
