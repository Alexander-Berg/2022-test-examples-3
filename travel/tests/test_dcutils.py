# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

import mock
import pytest

from travel.rasp.library.python.common23.utils.dcutils import ResourceExplorer, is_qyp_hostname, get_qyp_dc


@pytest.fixture()
def qloud_env():
    env = {'QLOUD_PROJECT': 'rasp', 'QLOUD_DATACENTER': 'iva'}
    with mock.patch.dict('os.environ', env):
        yield env


@pytest.fixture()
def deploy_env():
    env = {'TRAVEL_DEPLOY_PROJECT': 'rasp'}
    with mock.patch.dict('os.environ', env):
        yield env


@pytest.fixture()
def sandbox_env():
    env = {'SANDBOX_CONFIG': '/home/zomb-sandbox/client/sandbox/etc/settings.yaml'}
    with mock.patch.dict('os.environ', env):
        yield env


class TestResourceExplorer(object):
    def test_is_not_run_in_qloud(self):
        explorer = ResourceExplorer()
        assert not explorer.is_run_in_qloud()

    def test_run_in_qloud(self, qloud_env):
        explorer = ResourceExplorer()
        assert explorer.is_run_in_qloud()

    def test_run_in_deploy(self, deploy_env):
        explorer = ResourceExplorer()
        assert explorer.is_run_in_deploy()

    def test_run_in_sandbox(self, sandbox_env):
        explorer = ResourceExplorer()
        assert explorer.is_run_in_sandbox()

    def test_get_current_dc_yadeploy(self, qloud_env):
        with mock.patch.dict(os.environ, {"DEPLOY_NODE_DC": "vla"}):
            assert ResourceExplorer().get_current_dc() == 'vla'

    def test_get_current_dc_qloud(self, qloud_env):
        assert ResourceExplorer().get_current_dc() == 'iva'

    @mock.patch('socket.gethostname', return_value='myhost.mydc.yp-c.yandex.net')
    def test_get_currnet_dc_qyp(self, qloud_env):
        assert ResourceExplorer().get_current_dc() == 'mydc'

    @mock.patch('socket.gethostname', return_value='localhost123')
    def test_get_currnet_dc_unknown(self, qloud_env):
        assert ResourceExplorer().get_current_dc() == 'xxx'


@pytest.mark.parametrize(
    'hostname, is_qyp', [('rasp-teamcity-trusty-1.sas.yp-c.yandex.net', True), ('wiki.yandex-team.ru', False)]
)
def test_is_qyp_hostname(hostname, is_qyp):
    assert is_qyp_hostname(hostname) == is_qyp


def test_get_qyp_dc():
    assert get_qyp_dc('rasp-teamcity-trusty-1.sas.yp-c.yandex.net') == 'sas'
