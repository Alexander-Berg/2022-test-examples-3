# -*- coding: utf-8 -*-
from contextlib import contextmanager

import py
import pytest
import mock
from yatest.common import source_path

from market.idx.marketindexer.marketindexer import miconfig
from market.idx.pylibrary.mindexer_core.publishers.publisher import FullPublisher

import market.idx.pylibrary.mindexer_core.zkmaster.zkmaster as zkmaster


@pytest.fixture()
def zk_master_mock():
    zkMaster = mock.create_autospec(zkmaster.ZkMaster)

    @contextmanager
    def make_zkmaster_mock():
        yield zkMaster
    make_zkmaster_mock.instance = zkMaster
    return make_zkmaster_mock


@pytest.yield_fixture()
def reductor_mock():
    with mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.Reductor._has_option', reuturn_value=True),\
            mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.Reductor._execute', reuturn_value=None) as execute,\
            mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.calc_download_timeout', reuturn_value=100):
        yield execute


@pytest.yield_fixture()
def miconfig_mock(tmpdir):
    icpath = source_path('market/idx/miconfigs/etc/feature/common.ini')
    dspath = source_path('market/idx/marketindexer/tests/datasources.conf')
    full_config = miconfig.MiConfig(icpath, dspath, prefix_dir=str(tmpdir))
    py.path.local(full_config.log_dir).ensure(dir=True)
    with mock.patch('market.idx.marketindexer.miconfig.force_full_mode', lambda: full_config),\
            mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.miconfig.force_full_mode', lambda: full_config):
        yield full_config


@pytest.yield_fixture()
def geninfo_mock():
    with mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.geninfo') as m:
        yield m


@pytest.yield_fixture()
def full_publisher(zk_master_mock, miconfig_mock):
    with FullPublisher(zkmastertype=zk_master_mock) as pub:
        pub.release_generation = mock.create_autospec(pub.release_generation)
        yield pub


def make_generation_stub(config, generation_name):
    py.path.local(config.working_dir)\
           .join(generation_name)\
           .join('input/qindex.generation')\
           .write(generation_name + '00', ensure=True)
    # just a dummy config file
    py.path.local(config.working_dir)\
           .join('publisher/config.json')\
           .write('{"dcgroups": {}}', ensure=True)


def make_full_generation_stub(config, generation_name):
    py.path.local(config.working_dir)\
           .join(generation_name)\
           .join('input/qindex.generation')\
           .write(generation_name + '00', ensure=True)
    # just a dummy config file
    py.path.local(config.working_dir)\
           .join(generation_name)\
           .join('config.json')\
           .write('{"dcgroups": {}}', ensure=True)


def test_publish_full_generation(full_publisher, miconfig_mock, reductor_mock, geninfo_mock):
    """
    Проверяем корректность вызова редуктора при публикации full.
    """
    generation_name = '20180101_1000'
    make_generation_stub(miconfig_mock, generation_name)

    full_publisher.publish_generation(generation_name)

    upload_call = mock.call('upload', generation_name, '--mode', 'full')
    switch_call = mock.call('switch', '--mode', 'full', '--full', generation_name)
    assert [upload_call, switch_call] in reductor_mock.mock_calls
