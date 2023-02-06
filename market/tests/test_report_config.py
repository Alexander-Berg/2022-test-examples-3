# -*- coding: utf-8 -*-
import os

import pytest
import pyb.plugin.marketsearch as marketsearch

from market.pylibrary.yatestwrap.yatestwrap import source_path
from pyb.report_config import ServantConfig, read_servant_config, servant_list
from context import MARKETSEARCH_DATA_DIR


@pytest.fixture(scope='module')
def report_config():
    with open(source_path('market/backctld/tests/marketsearch_data/market-report.cfg'), 'r') as config_file:
        config = ServantConfig(config_file)
    return config


def test_read_report_config_path_from_backctld_config(report_config):
    backctld_config = marketsearch.Config(os.path.join(MARKETSEARCH_DATA_DIR, 'marketsearch3.conf'), '/dev/null', 'user')
    backctld_config.report_config_path = os.path.join(MARKETSEARCH_DATA_DIR, backctld_config.report_config_path)

    actual_config = read_servant_config(backctld_config, 'xxx')
    assert report_config.server['Port'] == actual_config.server['Port'] == '17051'


def test_read_servant_list_from_config():
    from mock import patch
    os_patch = patch('os.path.exists', return_value=False)
    os_patch.start()

    try:
        backctld_config = marketsearch.Config(os.path.join(MARKETSEARCH_DATA_DIR, 'marketsearch3.conf'), '/dev/null', 'user')
        servant_list_result = servant_list(backctld_config)
    finally:
        os_patch.stop()

    assert servant_list_result == ['my-market-report']


def test_port(report_config):
    assert report_config.server['Port'] == '17051'


def test_collection_dirs(report_config):
    assert 'OfferBasedCollectionDirs' in report_config.market_report
    assert report_config.market_report['OfferBasedCollectionDirs'] == \
        '/var/lib/search/index/part-0,/var/lib/search/index/part-8'


def test_subsection(report_config):
    assert 'Authorization' in report_config.server
    assert 'UserName' in report_config.server['Authorization']
    assert report_config.server['Authorization']['UserName'] == 'backctld'


def test_collections(report_config):
    # Теги Collection в конфиге репрта парсятся и записываются в
    # dict report_config.collections, в качестве ключей используются их
    # аттрибуты id
    assert 'yandsearch' in report_config.collections


def test_collection_withou_id(report_config):
    # Коллекции без аттрибута id не приваодят к падению,
    # они просто игнорируются
    assert len(report_config.collections) == 6


def test_parse_key_value_pairs(report_config):
    # У репорта в конфиге 3 разных спосба записывать ключ/значение
    pairs_text = '''\
    one 1
    two: 2
    three = 3
    '''
    pairs_dict = {
        'one': '1',
        'two': '2',
        'three': '3'
    }
    parsed_dict = report_config._parse_key_value_pairs(pairs_text)
    assert pairs_dict == parsed_dict


def test_subsection_array(report_config):
    # Если в секции есть множество повторяющихся тегов,
    # то они превращаются в список
    assert 'SearchSource' in report_config.collections['yandsearch']
    assert isinstance(report_config.collections['yandsearch']['SearchSource'], list)
    assert len(report_config.collections['yandsearch']['SearchSource']) == 34
