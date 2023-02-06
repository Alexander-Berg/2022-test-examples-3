# coding: utf-8

import mock
import pytest

from container import Container


CONTAINER_NAME = 'ISS-AGENT--17050/17050_test_report_market_vla_FY0gcyTvA5D/iss_hook_start'
CONTAINER_ENV = (
    'CONTAINER=porto;ISS=1;BSCONFIG_IHOST=iva1-0466;BSCONFIG_INAME=iva1-0466:17050;'
    'BSCONFIG_IPORT=17050;BSCONFIG_ITAGS=IVA_MARKET_TEST_REPORT_GENERAL_MARKET a_ctype_testing '
    'a_dc_iva a_geo_msk a_itype_marketreport a_line_iva-2 a_metaprj_market a_prj_report-general-market '
    'a_shard_0 a_tier_MarketMiniClusterTier1 cgset_memory_recharge_on_pgfault_1 itag_replica_0 '
    'use_hq_spec enable_hq_report enable_hq_poll;BSCONFIG_SHARDDIR=./;BSCONFIG_SHARDNAME=;'
    'HOSTNAME=iva1-0466-728-iva-market-test--d79-17050.gencfg-c.yandex.net;'
    'HQ_INSTANCE_SPEC_HASH=ec6fe2cfde7690fa41dea0e53401e567;JUGGLER_AGENT_PORT=31579;'
    'NANNY_SERVICE_ID=test_report_market_iva;NODE_NAME=iva1-0466.search.yandex.net;annotated_ports={};'
    'monitoringHostname=iva1-0466-728-iva-market-test--d79-17050.gencfg-c.yandex.net;monitoringInstanceName=;'
    'monitoringJugglerEndpoint=http://[2a02:6b8:c0c:1597:10b:1de7:0:429a]:31579/;'
    'monitoringYasmagentEndpoint=http://[2a02:6b8:c0c:1597:10b:1de7:0:429a]:11003/;'
    'nanny_container_access_url=http://nanny.yandex-team.ru/api/repo/CheckContainerAccess/;'
    'tags=IVA_MARKET_TEST_REPORT_GENERAL_MARKET a_ctype_testing a_dc_iva a_geo_msk a_itype_marketreport '
    'a_line_iva-2 a_metaprj_market a_prj_report-general-market a_shard_0 a_tier_MarketMiniClusterTier1 '
    'cgset_memory_recharge_on_pgfault_1 itag_replica_0 use_hq_spec enable_hq_report enable_hq_poll;'
    'yasmUnistatUrl=http://[2a02:6b8:c0d:780:10e:f8ef:0:429a]:17069/stat\\;'
    'http://[2a02:6b8:c0d:780:10e:f8ef:0:429a]:17067/stat'
)


@pytest.fixture(scope='function')
def mocked_execute(mocker):
    return mocker.patch('util.execute', autospec=True)


@pytest.mark.parametrize('hw_exit_code, hw_out', [
    (1, ''),
    (0, '/root'),
])
def test_root(mocked_execute, hw_exit_code, hw_out):
    mocked_execute.return_value = hw_exit_code, hw_out, ''
    container = Container(CONTAINER_NAME)
    result = container.root
    mocked_execute.assert_called_once_with(['/usr/sbin/portoctl', 'get', CONTAINER_NAME, 'cwd'])
    assert result == (hw_out if hw_exit_code == 0 else None)


@pytest.mark.parametrize('hw_exit_code, hw_out', [
    (1, ''),
    (0, CONTAINER_ENV),
])
def test_env(mocked_execute, hw_exit_code, hw_out):
    mocked_execute.return_value = hw_exit_code, hw_out, ''
    container = Container(CONTAINER_NAME)
    result = container.env
    mocked_execute.assert_called_once_with(['/usr/sbin/portoctl', 'get', CONTAINER_NAME, 'env'])
    assert bool(result) == (hw_exit_code == 0)
    default = 'unknown'
    assert container.itype == ('marketreport' if hw_exit_code == 0 else default)
    assert container.ctype == ('testing' if hw_exit_code == 0 else default)
    assert container.prj == ('report-general-market' if hw_exit_code == 0 else default)
    assert container.slot_name == ('iva1-0466:17050' if hw_exit_code == 0 else default)


def test_volume_path_by_root_path(mocked_execute):
    volume_path = '/root'
    mocked_execute.return_value = 0, volume_path, ''
    container = Container(CONTAINER_NAME)
    result = container.volume_path
    mocked_execute.assert_called_once_with(['/usr/sbin/portoctl', 'get', CONTAINER_NAME, 'root_path'])
    assert result == volume_path


def test_volume_path_by_root_path_failed(mocked_execute):
    name = CONTAINER_NAME.split('/')[0]
    mocked_execute.return_value = 1, '', ''
    container = Container(name)
    result = container.volume_path
    mocked_execute.assert_called_once_with(['/usr/sbin/portoctl', 'get', name, 'root_path'])
    assert result is None


def test_volume_path_by_parent(mocked_execute):
    volume_path = '/root'
    parent = '/'.join(CONTAINER_NAME.split('/')[:-1])
    mocked_execute.side_effect = [
        (1, '', ''),
        (0, volume_path, ''),
    ]
    container = Container(CONTAINER_NAME)
    result = container.volume_path
    mocked_execute.assert_has_calls([
        mock.call(['/usr/sbin/portoctl', 'get', CONTAINER_NAME, 'root_path']),
        mock.call(['/usr/sbin/portoctl', 'get', parent, 'root']),
    ])
    assert result == volume_path


def test_volume_path_all_failed(mocked_execute):
    mocked_execute.return_value = 1, '', ''
    container = Container(CONTAINER_NAME)
    result = container.volume_path
    assert result is None
