#!/usr/bin/env python

import unittest
import common


class TestMboPreivewReportConfig(unittest.TestCase):

    def test_no_local_parts_except_preview(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['LocalSourceList'], '0')
        self.assertEqual(mr['OfferBasedCollectionDirs'], '')
        self.assertEqual(mr['ModelBasedCollectionDirs'], '/var/lib/search/mbo-preview')

    def test_preview_model_and_yandsearch_collections(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        common.get_wizards_base_collection(conf)
        preview_model = conf.find_section('Collection', lambda col: 'id="preview-model-0"' in col.decl)
        self.assertTrue(preview_model is not None)
        yandsearch = common.get_yandsearch(conf)
        self.assertTrue(yandsearch is not None)
        self.assertNotIn('IndexArchiveMode', preview_model)

    def test_remote_catalog(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        catalog_source = yandsearch.find_section('SearchSource', lambda sec: 'CATALOG' == sec['ServerDescr'])
        self.assertIn('http://msh01hp.market.yandex.net:17051/catalogsearch', catalog_source['CgiSearchPrefix'])

    def test_remote_cards(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        card_source = yandsearch.find_section('SearchSource', lambda sec: 'CARD' == sec['ServerDescr'])
        self.assertIn('http://msh01hp.market.yandex.net:17051/cardsearch', card_source['CgiSearchPrefix'])

    def test_remote_models(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        model_sources = [m for m in yandsearch.get_sections('SearchSource') if m['ServerDescr'] == 'MODEL']
        for source in model_sources:
            self.assertNotIn('localhost', source['CgiSearchPrefix'])

    def test_remote_offers(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        offer_sources = [m for m in yandsearch.get_sections('SearchSource') if m['ServerDescr'] == 'SHOP']
        for source in offer_sources:
            self.assertNotIn('localhost', source['CgiSearchPrefix'])

    def test_preview_model(self):
        out = common.generate_config('market-mbo-preview-report', 'template.cfg',
                                     'mbo-preview01et.supermarket.yandex.net.cfg',
                                     TARGET_HOST_NAME='mbo-preview01et.supermarket.yandex.net',
                                     ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        preview_model_sources = [m for m in yandsearch.get_sections('SearchSource') if m['ServerDescr'] == 'PREVIEW_MODEL']
        self.assertEqual(1, len(preview_model_sources))
        self.assertIn('localhost', preview_model_sources[0]['CgiSearchPrefix'])
