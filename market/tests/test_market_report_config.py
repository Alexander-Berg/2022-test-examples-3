#!/usr/bin/env python

import unittest
import os
import common

class TestMarketReportConfig(unittest.TestCase):

    def test_no_alternarive_sources(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh33e.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh33e.market.yandex.net',
                    ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        self.assertTrue(yandsearch is not None)
        remote = yandsearch.find_section('SearchSource', lambda sec: 'basesearch16-1' in sec['CgiSearchPrefix'])
        self.assertTrue(remote is not None)
        self.assertEqual(len(remote['CgiSearchPrefix'].split()), 1)
        self.assertTrue('://localhost:' in remote['CgiSnippetPrefix'])
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('OfferBasedCollectionDirs /var/lib/search/index/part-0,/var/lib/search/index/part-8' in out)

    def test_support_of_only_one_search_instance_in_testing(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.market.yandex.net',
                    ENV_TYPE='testing')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('parallel' in mr['ReportRoles'])

    def test_sharded_model_index_on_search_backends(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.market.yandex.net',
                    ENV_TYPE='testing')
        self.assertTrue('IndexDir /var/lib/search/index/model/part-0' in out)
        self.assertTrue('CgiSearchPrefix: http://localhost:17051/basesearch-model-0' in out)
        self.assertTrue('CgiSnippetPrefix http://msh-off01ht.market.yandex.net:17088/basesearch-model-0' in out)
        self.assertTrue('CgiSearchPrefix http://msh08d.market.yandex.net:17051/basesearch-model-7' in out)
        self.assertTrue('CgiSnippetPrefix http://msh-off01ht.market.yandex.net:17088/basesearch-book-0' in out)
        self.assertTrue('CgiSearchPrefix http://msh08d.market.yandex.net:17051/basesearch-book-7' in out)

    def test_report_roles(self):
        data=[('market-report', 'somewhere.net', ['main']),
              ('market-parallel-report', 'somewhere.net', ['parallel']),
              ('market-ppcshop-report', 'somewhere.net', ['bidsearch']),
              ('market-report', 'msh01ht.market.yandex.net', ['main','parallel']),
              ('market-parallel-report', 'msh01ht.market.yandex.net', ['parallel']),
              ('market-ppcshop-report', 'msh01ht.market.yandex.net', ['bidsearch', 'parallel'])]

        for report_type, host_name, expected_roles in data:
            out = common.generate_config(report_type, 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                        TARGET_HOST_NAME=host_name,
                        ENV_TYPE='testing')
            conf = common.parse_config(out)
            mr = conf.get_sections('MarketReport')[0]

            for role in expected_roles:
                self.assertTrue(role in mr['ReportRoles'])

    def test_cards_collection(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.market.yandex.net',
                    ENV_TYPE='testing')
        conf = common.parse_config(out)
        base = common.get_cardsearch(conf)
        yandsearchs = [s for s in common.get_yandsearch(conf).get_sections('SearchSource') if ':17051/cardsearch' in s['CgiSearchPrefix']][0]
        self.assertEqual('/var/lib/search/index/cards', base['IndexDir'])
        self.assertTrue(not base['IndexArchiveMode'])
        self.assertEqual('CARD', yandsearchs['ServerDescr'])
        self.assertTrue(not yandsearchs['CgiSnippetPrefix'])

    def test_wizards_collection(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.market.yandex.net',
                    ENV_TYPE='testing')
        conf = common.parse_config(out)
        base = common.get_wizards_base_collection(conf)
        yandsearchs = [s for s in common.get_yandsearch(conf).get_sections('SearchSource') if ':17051/catalogsearch' in s['CgiSearchPrefix']][0]
        self.assertEqual('/var/lib/search/index/wizard', base['IndexDir'])
        self.assertTrue(not base['IndexArchiveMode'])
        self.assertEqual('CATALOG', yandsearchs['ServerDescr'])
        self.assertTrue(not yandsearchs['CgiSnippetPrefix'])

    def test_diff_collection(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.diff.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.diff.market.yandex.net',
                    ENV_TYPE='testing')
        conf = common.parse_config(out)
        mr = common.get_marketreport(conf)
        self.assertEqual(mr['OfferBasedCollectionDirs'], '/var/lib/search/index/part-0,/var/lib/search/index/part-8,/var/lib/search/index/diff-part-0')
        bsd = common.get_basesearch_diff(conf, '16-0')
        self.assertNotIn('IndexArchiveMode', bsd)
        self.assertEqual(bsd['IndexDir'], '/var/lib/search/index/diff-part-0')
        yandsearch = common.get_yandsearch(conf)
        remote = yandsearch.find_section('SearchSource', lambda sec: 'basesearch-diff16-1' in sec['CgiSearchPrefix'])
        self.assertTrue(remote is not None)
        self.assertNotIn('CgiSnippetPrefix', remote)

    def test_collection_based_dirs(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.market.yandex.net',
                    ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['OfferBasedCollectionDirs'], '/var/lib/search/index/part-0,/var/lib/search/index/part-8')
        self.assertEqual(mr['ModelBasedCollectionDirs'], '/var/lib/search/index/book/part-0,/var/lib/search/index/model/part-0')

    def test_testing_features_option_enabled_in_testing(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.market.yandex.net',
                    ENV_TYPE='testing')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('EnableTestingFeatures' in mr)
        self.assertEqual(mr['EnableTestingFeatures'], '1')

    def test_testing_features_option_disabled_in_production(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh33e.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh33e.market.yandex.net',
                    ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('EnableTestingFeatures' not in mr)

    def test_sandbox_firing_mode(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh33e.market.yandex.net.cfg',
                TARGET_HOST_NAME='msh33e.market.yandex.net',
                    ENV_TYPE='production', DISABLE_RANDOM='1')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['DisableRandom'], '1')

    def test_non_sandbox_firing_mode(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh33e.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh33e.market.yandex.net',
                    ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('DisableRandom' not in mr)


    def test_config_for_et_cluster(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01et.market.yandex.net',
                    ENV_TYPE='testing')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['LogLevel'], 'Debug')


    def test_request_threads_experiment(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh12v.market.yandex.net')
        conf = common.parse_config(out)
        for section in conf.get_sections('Collection'):
            if 'meta="yes"' in section.decl:
                self.assertEqual(section['RequestThreads'], '56')
            else:
                self.assertEqual(section['RequestThreads'], '42')
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh01v.market.yandex.net')
        conf = common.parse_config(out)
        for section in conf.get_sections('Collection'):
            if 'meta="yes"' in section.decl:
                self.assertEqual(section['RequestThreads'], '32')
            else:
                self.assertEqual(section['RequestThreads'], '24')


    def test_local_sources(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh33e.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh33e.market.yandex.net', ENV_TYPE='production')
        conf = common.parse_config(out)
        yandsearch = common.get_yandsearch(conf)
        self.assertTrue(yandsearch is not None)
        search_source_index = 0
        local_source_list = []
        for search_source in yandsearch.get_sections('SearchSource'):
            if 'localhost' in search_source['CgiSearchPrefix']:
                local_source_list.append(str(search_source_index))
            search_source_index += 1
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['LocalSourceList'], ','.join(local_source_list))

    def test_ichwill(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh33e.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh33e.market.yandex.net',
                    ENV_TYPE='production')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('IchWillHostAndPort' in mr)
        self.assertEqual(mr['IchWillHostAndPort'], 'ichwill.vs.market.yandex.net:81')

        out = common.generate_config('market-report', 'template.cfg', 'msh01hp.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01hp.market.yandex.net',
                    ENV_TYPE='prestable')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('IchWillHostAndPort' in mr)
        self.assertEqual(mr['IchWillHostAndPort'], 'frontwill01hp.supermarket.yandex.net:81')

        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                    TARGET_HOST_NAME='msh01ht.market.yandex.net',
                    ENV_TYPE='testing')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertTrue('IchWillHostAndPort' in mr)
        self.assertEqual(mr['IchWillHostAndPort'], 'ichwill.vs.market.yandex.net:81')


    def test_exec_stats(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh12e.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['EnableExecutionStats'], '1')

        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh24e.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['EnableExecutionStats'], '1')

        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh07e.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertFalse('EnableExecutionStats' in mr)


    def test_install_crash_handler(self):
        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh01e.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertFalse('InstallCrashHandler' in mr)

        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh-off01e.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertFalse('InstallCrashHandler' in mr)

        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh09e.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['InstallCrashHandler'], '1')

        out = common.generate_config('market-report', 'template.cfg', 'msh01ht.market.yandex.net.cfg',
                                     TARGET_HOST_NAME='msh-off02e.market.yandex.net')
        conf = common.parse_config(out)
        mr = conf.get_sections('MarketReport')[0]
        self.assertEqual(mr['InstallCrashHandler'], '1')
