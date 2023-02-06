# coding: utf-8

import pytest
import tempfile

from market.idx.marketindexer.miconfig import MiConfig

import yatest


CONFIG_DATA = """
[general]
working_dir = /

[external_data]
data_limit_per_table_source_name1 = 1000
data_limit_per_offer_source_name1 = 100
source_path_source_name1 = //home/some/src_path/yt/path1
destination_path_source_name1 = //home/some/dst_path/yt/path1

data_limit_per_table_abc = 2000
data_limit_per_offer_abc = 200
source_path_abc = //home/some/src_path/yt/abc
destination_path_abc = //home/some/dst_path/yt/abc
"""


@pytest.yield_fixture
def miconfig_path():
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write(CONFIG_DATA)
        f.flush()
        yield f.name


@pytest.yield_fixture
def miconfig(miconfig_path):
    ds_config_path = yatest.common.source_path('market/idx/marketindexer/tests/datasources.conf')
    yield MiConfig([miconfig_path], ds_config_path)


def test_external_data_setction(miconfig):
    assert miconfig.external_data_data_limit_per_table_source_name1 == 1000
    assert miconfig.external_data_data_limit_per_offer_source_name1 == 100
    assert miconfig.external_data_source_path_source_name1 == '//home/some/src_path/yt/path1'
    assert miconfig.external_data_destination_path_source_name1 == '//home/some/dst_path/yt/path1'

    assert miconfig.external_data_data_limit_per_table_abc == 2000
    assert miconfig.external_data_data_limit_per_offer_abc == 200
    assert miconfig.external_data_source_path_abc == '//home/some/src_path/yt/abc'
    assert miconfig.external_data_destination_path_abc == '//home/some/dst_path/yt/abc'
