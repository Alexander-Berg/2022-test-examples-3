# coding: utf-8

import os
import shutil
import tempfile
import textwrap
import unittest

import market.idx.pylibrary.mindexer_core.configure.configure as configure
from market.idx.marketindexer.marketindexer import miconfig
from market.pylibrary.yatestwrap.yatestwrap import source_path
import context


class TestConfig(unittest.TestCase):
    def do_test_cfg(self, cfg):
        self.assertEqual(cfg.get('general', 'working_dir'), '/var/lib/yandex/indexer/market')
        self.assertEqual(cfg.get('general', 'not_exists'), None)
        self.assertEqual(cfg.get('not_exists', 'not_exists', 'bla'), 'bla')
        self.assertRaises(Exception, cfg.get, 'not_exists', 'not_exists', safe=False)

        self.assertEqual(cfg.envtype, 'testing')
        self.assertEqual(cfg.mitype, 'stratocaster')
        self.assertEqual(cfg.yt_genlog_gendir, '//home/market/testing/indexer/stratocaster/offers')

    def test_config(self):
        cfg = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH)
        self.do_test_cfg(cfg)

    def test_default(self):
        os.environ['IC_CONFIG_PATH'] = context.MI_CONFIG_PATH
        os.environ['IL_CONFIG_PATH'] = context.MI_CONFIG_PATH
        os.environ['DS_CONFIG_PATH'] = context.DS_CONFIG_PATH
        os.environ['ZK_CONFIG_PATH'] = context.ZK_CONFIG_PATH
        cfg = miconfig.default()
        self.do_test_cfg(cfg)
        self.assertEqual(miconfig.default(), cfg)

    def test_resolve(self):
        cfg = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH)
        tmpl = '{working_dir}/{stuff}'
        self.assertEqual(
            cfg.resolve(tmpl, stuff='mystuff'),
            '/var/lib/yandex/indexer/market/mystuff')

    def test_fs_output_prefix(self):
        cfg = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH)
        cfg._fs_output_prefix = '/home/user/'
        tmpl = '{working_dir}/{stuff}'
        self.assertEqual(
            cfg.output_fs_path(cfg.resolve(tmpl, stuff='mystuff')),
            '/home/user/var/lib/yandex/indexer/market/mystuff')

    def test_yt_output_prefix(self):
        cfg = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH, envtype='outtaspace')
        cfg._yt_output_prefix = '//tmp/user/'
        tmpl = '{yt_market_workdir}/{stuff}'
        self.assertEqual(cfg.envtype, 'outtaspace')
        self.assertEqual(
            cfg.output_yt_path(cfg.resolve(tmpl, stuff='mystuff')),
            '//tmp/user/home/market/outtaspace/mystuff')

    def test_override(self):
        try:
            tmp_dir = tempfile.mkdtemp()
            local_ini_path = os.path.join(tmp_dir, 'local.ini')
            os.symlink(source_path('market/idx/miconfigs/etc/feature/common.ini'), local_ini_path)
            # set the IL_CONFIG_PATH environment variable to ensure
            # proper work of the get_local_conf() function
            os.environ['IL_CONFIG_PATH'] = local_ini_path
            override_file = source_path('market/idx/marketindexer/tests/data/test_override.ini')
            self.assertEqual(miconfig.is_config_overridden(), False)
            miconfig.override_local_ini(override_file)
            paths = [context.MI_CONFIG_PATH, local_ini_path]
            cfg = miconfig.MiConfig(paths, context.DS_CONFIG_PATH)
            self.assertEqual(cfg.get('general', 'does_not_exist'), 'now_it_does')
            self.assertEqual(cfg.get('indexknn', 'build_knn_for_offers'), 'false')
            self.assertEqual(miconfig.is_config_overridden(), True)
        finally:
            shutil.rmtree(tmp_dir)

    def test_custom_env(self):
        cfg = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH, envtype='production', mitype='gibson')
        self.assertEqual(cfg.yt_genlog_gendir, '//home/market/production/indexer/gibson/offers')

    def test_masterconfig_model_options(self):
        cfg = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH, envtype='production', mitype='gibson')
        cfg.master_config_path = source_path('market/idx/marketindexer/tests/data/test_masterconfig.ini')

        options = configure.get_masterconfig_options(cfg, '20190303_4242', configure.MODEL_VCLUSTERS_SECTION)

        assert options == {
            'PortionDocCount': '20000',
            'PortionMaxMemory': '100000',
            'Threads': '1',
            'IndexDir': 'workindex',
            'PartsCount': '8',
            'HTMLParserConfig': '/etc/yandex/marketindexer/PARSER_CONFIG',
            'UseNewIndexer': 'true',
            'UseWadForC2N': 'true',
            'NeedBuildAnnIndex': 'true'
        }

    def test_masterconfig_offer_options(self):
        cfg = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH, envtype='production', mitype='gibson')
        cfg.master_config_path = source_path('market/idx/marketindexer/tests/data/test_masterconfig.ini')

        options = configure.get_masterconfig_options(cfg, '20190303_4242', configure.OFFERS_SECTION)

        assert options == {
            'group': 'mi_worker-stable-gibson',
            'nparts': '16',
            'threads_per_worker': '8',
            'build_blue_shard': 'true',
            'use_wad_for_c2n': 'true',
            'UseNewIndexer': 'true',
            'UseWadForC2N': 'true'
        }

    def test_getlist(self):
        dpath = source_path('test_getlist.ini')
        config_data = textwrap.dedent('''
            [general]
            working_dir = /var/lib/yandex/indexer/market
            [lists]
            # no_list =
            empty_list =
            one_element = one
            two_elements = two, three
            int_elements = 3, 1, 2
        ''')
        with open(dpath, 'w') as f:
            f.write(config_data)
        cfg = miconfig.MiConfig(dpath, context.DS_CONFIG_PATH)
        self.assertEqual(cfg.getlist('lists', 'no_list'), None)
        self.assertEqual(cfg.getlist('lists', 'empty_list'), [])
        self.assertEqual(cfg.getlist('lists', 'one_element'), ['one'])
        self.assertEqual(cfg.getlist('lists', 'two_elements'), ['two', 'three'])
        self.assertEqual(cfg.getlist('lists', 'int_elements', etype=int), [3, 1, 2])

    def test_two_phase_reload_meta(self):
        dpath = source_path('test_two_phase_reload_meta.ini')
        config_data = textwrap.dedent('''
            [general]
            working_dir = /indexer/market
            [two_phase_reload_meta:market_report_meta@atlantis]
            enabled = true
            base_group = market_report_exp1@atlantis
            dc_allowed_for_reload = vla,man,sas
        ''')
        with open(dpath, 'w') as f:
            f.write(config_data)
        cfg = miconfig.MiConfig(dpath, context.DS_CONFIG_PATH)
        self.assertEqual(len(cfg.two_phase_reload_meta), 1)
        self.assertTrue('market_report_meta@atlantis' in cfg.two_phase_reload_meta)

        group_cfg = cfg.two_phase_reload_meta['market_report_meta@atlantis']
        self.assertTrue(group_cfg.enabled)
        self.assertEqual(group_cfg.base_group, 'market_report_exp1@atlantis')
        self.assertEqual(group_cfg.service, 'marketsearch3')
        self.assertEqual(len(group_cfg.dc_allowed_for_reload), 3)
        self.assertEqual(group_cfg.num_dc_for_reload, 1)
        self.assertEqual(group_cfg.first_phase_cluster_num, 1)
        self.assertEqual(group_cfg.run_integration_test, False)
        self.assertEqual(group_cfg.check_report_errors, False)
        self.assertEqual(group_cfg.check_report_timings, False)
        self.assertEqual(group_cfg.integration_test_timeout, 600)
        self.assertEqual(group_cfg.stats_wait_time, 300)
        self.assertEqual(group_cfg.timings_check_size_per_host, 100)
