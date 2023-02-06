# -*- coding: utf-8 -*-

import datetime
import json
import os
import re
import shutil
import time
import unittest
from collections import namedtuple
from six.moves.configparser import ConfigParser

import context
from context import get_plugin_path, get_plugins_dir_path
from pyb.plugin import marketsearch
from pyb.taskrunner import TaskRunner
from pyb import report_config
from .common import DummyTaskRunner, symlink, dist_names_test1_test2  # noqa

from market.pylibrary.mi_util import util
from async_publishing.group_config import GroupConfig

from mock import Mock, patch
import pytest


def assert_not_called_with(self, *args, **kwargs):
    try:
        self.assert_called_with(*args, **kwargs)
    except AssertionError:
        return
    raise AssertionError('Expected %s to not have been called.' % self._format_mock_call_signature(args, kwargs))


Mock.assert_not_called_with = assert_not_called_with


class TimeoutException(Exception):
    pass


class Test(unittest.TestCase):
    _genlogs = ['20130920_1300', '20130920_1400', '20130920_1500', '20130920_1600', '20130920_1700']
    _dists = ['test1', 'test2', 'report-data']

    @pytest.fixture(scope='class', autouse=True)
    def patch_async_publishing_client(self):
        class Client:
            @property
            def my_group_config(self):
                json_config = {
                    'simultaneous_restart': None,
                    'failures_threshold': None,
                    'hosts': {
                        "1": [
                            {'key': 'rtc-123', 'fqdn': 'rtc-123.market.yandex.net', 'port': 17051, 'datacenter': None},
                        ],
                        "2": [
                            {'key': 'rtc-321', 'fqdn': 'rtc-321.market.yandex.net', 'port': 17051, 'datacenter': None},
                        ]
                    }
                }
                return GroupConfig.from_str(json.dumps(json_config))

        with patch('pyb.plugin.marketsearch.AsyncPublishingClient', return_value=Client()):
            yield

    @pytest.fixture(scope='class', autouse=True)
    def patch_zk_client(self):
        with patch('pyb.zk.create_zk_client'):
            yield

    @pytest.fixture(scope='class', autouse=True)
    def patch_reload_lock_path(self):
        with patch('pyb.plugin.marketsearch.MarketSearch.get_reload_lock_path', return_value='reload.lock'):
            yield

    def _list_generations(self, directory):
        return [gen for gen in os.listdir(directory) if gen in self._genlogs]

    def setUp(self):
        self._src_dir = context.MARKETSEARCH_DATA_DIR
        self._work_dir = os.path.join(os.getcwd(), context.TMP_DIR, 'base_test')
        self._stub_dir = os.path.join(context.MARKETSEARCH_DATA_DIR, 'stub.py')
        self._clean()

    def tearDown(self):
        self._clean()

    @property
    def _dists_dir(self):
        return os.path.join(self._work_dir, 'dists')

    @property
    def _genlogs_dir(self):
        return os.path.join(self._work_dir, 'marketsearch')

    def _clean(self):
        shutil.rmtree(self._work_dir, ignore_errors=True)

    def _make_stubs(self):
        paths = [
            '/usr/bin/torrent_client_clt',
            '/usr/bin/sky_downloader',
        ]
        for path in paths:
            symlink(self._stub_dir, self._work_dir + path)

    def _prepare(self):
        self._clean()

        util.makedirs(self._work_dir)
        util.makedirs(self._dists_dir)
        util.makedirs(self._genlogs_dir)

        confs = ['marketsearch2.conf', 'marketsearch2_1.conf']
        for conf in confs:
            ptpl = os.path.join(self._src_dir, conf + '.tpl')
            with open(ptpl) as f:
                content = f.read()
            pconf = os.path.join(self._work_dir, conf)
            with open(pconf, 'w') as f:
                f.write(content.replace('%%WORKDIR%%', self._work_dir))

        files = ['httpsearch', 'checksearch', 'checksearch_fail', 'generate',
                 'timetail', 'preport.log', 'httpsearch_list']
        for file in files:
            src = os.path.join(self._src_dir, file)
            dst = os.path.join(self._work_dir, file)
            shutil.copy(src, dst)

        for genlog in self._genlogs:
            for dist in self._dists:
                path = os.path.join(self._work_dir, 'marketsearch', genlog, dist)
                util.makedirs(path)
                with open(os.path.join(path, 'completed'), 'w'):
                    pass

        self._make_stubs()

    def _get_log_lines(self):
        with open(os.path.join(self._work_dir, 'marketsearch.log')) as f:
            return [v.strip() for v in f.readlines()]

    def _create(self, p_config, reports, experimental_unpack=False):
        dists = {}

        for dist in self._dists:
            dists[dist] = os.path.join(self._dists_dir, dist)

        class Context(object):
            def __init__(self, root_dir):
                self.name = os.path.basename(p_config)[:-len('.conf')]
                self.root_dir = root_dir
                self.task_runner = TaskRunner(root_dir)
                self.aria2_user = context.user_name()
                self.prefix_dir = None
                self.torrent_client = root_dir + '/usr/bin/torrent_client_clt'
                self.torrent_client_config = None
                self.sky_downloader_client = root_dir + '/usr/bin/sky_downloader'
                self.sky_downloader_client_config = None
                self.plugins_dir = os.path.dirname(p_config)
                self.backctld_port = None
                self.dists = dists
                self.experiment_flags_reader = None

            def get_path(self, path):
                return path

        ms = marketsearch.MarketSearch(Context(self._work_dir), plugin_config_path=p_config)
        ms._trunner = DummyTaskRunner()
        ms._config.httpsearch_list_path = os.path.join(self._work_dir, 'httpsearch_list')
        ms._r_supported = set(reports)
        ms._config.experimental_unpack = experimental_unpack
        return ms

    def _wait(self, ms, time_to_wait=30):
        start = time.time()
        retval = ms.check()
        while retval == '! in progress':
            # wait fot 30 seconds
            if time.time() > start + time_to_wait:
                raise TimeoutException()
            time.sleep(0.1)
            retval = ms.check()

        return retval

    def _check_structure(self, gen):
        dirs = os.listdir(self._dists_dir)
        self.assertEqual(set(dirs), set(self._dists))
        for dist in self._dists:
            src = os.path.join(self._dists_dir, dist)
            self.assertTrue(os.path.islink(src))
            dstreal = os.readlink(src)
            dstexp = os.path.join(self._work_dir, 'marketsearch', gen, dist)
            self.assertEqual(dstreal, dstexp)

    def test_all(self, experimental_unpack=False):
        self._prepare()

        p_config = os.path.join(self._work_dir, 'marketsearch2.conf')
        gen = '20130920_1600'
        saved_gens = [gen, '20130920_1700']
        reports = ['test1', 'test2', 'test3']

        # reload + check test
        # check if reload creates right dirs/links structure
        # ! there is no test when we put dists to shared memory
        ms = self._create(p_config, reports, experimental_unpack=experimental_unpack)
        ms.reload(gen)

        self.assertEquals(self._wait(ms), 'ok')
        self.assertEquals('! false', ms.is_reloading())

        self._check_structure(gen)

        dirs = self._list_generations(self._genlogs_dir)
        self.assertEqual(sorted(dirs), sorted(saved_gens))

        # test if ReloadLaucher generates report config
        with open('generated.cfg') as f:
            lines = [v.strip() for v in f.readlines()]
        self.assertEqual(lines, reports)

        # test if stop, start and check are called in right order and for all reports
        lines = self._get_log_lines()
        self.assertTrue(len(lines) >= 9)

        def check_lines(beg, end, irep, ok):
            processed = set()
            for i in range(beg, end):
                args = lines[i].split(' ')
                self.assertTrue(ok(args))
                processed.add(args[irep])
            if irep >= 0:
                self.assertEqual(set(reports), processed)

        check_lines(0, 3, 2, lambda a: len(a) == 3 and a[0] == 'httpsearch' and a[1] == 'close-for-load')
        check_lines(3, 6, 2, lambda a: len(a) == 3 and a[0] == 'httpsearch' and a[1] == 'stop')
        check_lines(6, 9, 2, lambda a: len(a) == 3 and a[0] == 'httpsearch' and a[1] == 'start')
        check_lines(9, 12, -1, lambda a: len(a) == 3 and a[0] == 'httpsearch' and a[1] == 'status')
        check_lines(12, 15, 1, lambda a: len(a) >= 2 and a[0] == 'checksearch')

        # rm_inactive_gens test
        src = os.path.join(self._work_dir, 'marketsearch', gen)
        dst = os.path.join(self._work_dir, 'report_data')
        os.symlink(src, dst)
        for genlog in self._genlogs:
            path = os.path.join(self._work_dir, 'marketsearch', genlog)
            if not os.path.exists(path):
                util.makedirs(path)

        retval = ms.rm_inactive_gens()
        self.assertEqual(retval, 'ok')
        generations = self._list_generations(self._genlogs_dir)
        self.assertEqual(sorted(generations), sorted(saved_gens))

    def test_all_experimental_unpack(self):
        return self.test_all(experimental_unpack=True)

    @pytest.mark.usefixtures('dist_names_test1_test2')
    def test_get_downloaded_generations(self):
        self._prepare()

        p_config = os.path.join(self._work_dir, 'marketsearch2.conf')
        ms = self._create(p_config, ['test1'])

        # make 20130920_1500 generation incomplete (no "completed" file)
        os.remove(os.path.join(self._work_dir, 'marketsearch', '20130920_1500', 'test2', 'completed'))
        self.assertEqual(ms.get_downloaded_generations(), '20130920_1700,20130920_1600,20130920_1400,20130920_1300')

        # make 20130920_1400 generation incomplete (missing directory for a dist)
        shutil.rmtree(os.path.join(self._work_dir, 'marketsearch', '20130920_1400', 'test2'))
        self.assertEqual(ms.get_downloaded_generations(), '20130920_1700,20130920_1600,20130920_1300')

    def test_reload_choose_gen(self):
        self._prepare()

        p_config = os.path.join(self._work_dir, 'marketsearch2.conf')
        ms = self._create(p_config, ['test1'])
        ms.reload()
        self.assertEquals(self._wait(ms), 'ok')
        self._check_structure(self._genlogs[-1])

    def test_reload_error(self):
        self._prepare()
        for genlog in self._genlogs:
            dir = os.path.join(self._genlogs_dir, genlog)
            shutil.rmtree(dir, ignore_errors=True)
        p_config = os.path.join(self._work_dir, 'marketsearch2.conf')
        ms = self._create(p_config, ['test1'])
        ms.reload()
        self.assertTrue(self._wait(ms).startswith('! failed'))
        self.assertEqual(ms._trunner.get(ms._name).result, "can't get generation for reload")

    def test_unpack_reload(self):
        self._prepare()
        p_config = os.path.join(self._work_dir, 'marketsearch2.conf')
        ms = self._create(p_config, ['test1'])
        ms.unpack_reload(None, None)
        self.assertEquals(self._wait(ms), 'ok')

    def test_generation_fqdn_config(self):
        self._prepare()
        p_config = os.path.join(self._work_dir, 'marketsearch2.conf')
        ms = self._create(p_config, ['test1'])
        ms.restart()
        self.assertEquals(self._wait(ms), 'ok')
        fqdn_config_path = os.path.join(self._work_dir, 'fqdn.json')
        with open(fqdn_config_path) as fqdn_config_file:
            fqdn_json = json.load(fqdn_config_file)
        self.assertEqual(fqdn_json, {'rtc-123': 'rtc-123.market.yandex.net', 'rtc-321': 'rtc-321.market.yandex.net'})


class SimpleTest(unittest.TestCase):
    def test_calc_gens_to_save(self):
        calc = marketsearch.calc_gens_to_save

        self.assertEquals(calc([3, 2, 1], [], num_to_save=1), [3])
        self.assertEquals(calc([3, 2, 1], [], num_to_save=2), [3, 2])

        self.assertEquals(calc([3, 2, 1], [1], num_to_save=1), [1])
        self.assertEquals(calc([3, 2, 1], [1], num_to_save=2), [1, 3])
        self.assertEquals(calc([3, 2, 1], [1], num_to_save=3), [1, 3, 2])
        self.assertEquals(calc([3, 2, 1], [1], num_to_save=4), [1, 3, 2])

        self.assertEquals(calc([3, 2, 1], [1, 2], num_to_save=1), [1, 2])

        self.assertEquals(calc([3, 2, 1], white_list=[1, 1], num_to_save=2), [1, 3])

    def test_stamp_to_date(self):
        date_short = util.stamp_to_datetime('20161230_1300')
        self.assertEqual(date_short, datetime.datetime(2016, 12, 30, 13, 0))
        date_long = util.stamp_to_datetime('20161230_130015')
        self.assertEqual(date_long, datetime.datetime(2016, 12, 30, 13, 0, 15))


class ConfigsConsistencyTest(unittest.TestCase):
    def test_marketsearch_snippet(self):
        def get_dists_count(pattern, config):
            return len([name for name, _ in config.items('dists') if re.match(pattern, name) is not None])

        marketsearch = ConfigParser()
        marketsearch.read(get_plugin_path('marketsearch3.conf'))

        snippet = ConfigParser()
        snippet.read(get_plugin_path('marketsearchsnippet.conf'))

        self.assertEqual(get_dists_count(r'search-part-\d+', marketsearch), get_dists_count(r'search-snippet-\d+', snippet))
        self.assertEqual(1, get_dists_count('search-snippet-data', snippet))


class IptrulerTest(unittest.TestCase):
    def test(self):
        """Проверяем Report
        close_iptruler
        open_iptruler
        """
        def check(reportname):
            class Iptruler(object):
                port_down = None
                port_up = None

                def down(self, port):
                    self.port_down = port

                def up(self, port):
                    self.port_up = port

            report = marketsearch.create_report(reportname)
            iptruler = Iptruler()

            logger_mock = Mock()
            system_ex_mock = Mock(return_value=0)
            with patch('market.pylibrary.mi_util.util.watching_system_ex', system_ex_mock):
                with patch('logging.getLogger', Mock(return_value=logger_mock)):
                    report.close_iptruler('/etc/init.d/mockhttpsearch', iptruler)

                    system_ex_mock.assert_called_with(['/etc/init.d/mockhttpsearch', 'close-for-load', reportname])
                    self.assertEquals(logger_mock.info.called, 1)
                    self.assertEquals(logger_mock.error.called, 0)

                    report.open_iptruler('/etc/init.d/mockhttpsearch', iptruler)

                    system_ex_mock.assert_called_with(['/etc/init.d/mockhttpsearch', 'open-for-load', reportname])
                    self.assertEquals(logger_mock.info.called, 1)
                    self.assertEquals(logger_mock.error.called, 0)

            self.assertEqual(iptruler.port_down, iptruler.port_up)

        check('market-report')
        check('market-parallel-report')
        check('market-ppcshop-report')
        check('market-snippet-report')

    def test_fail_to_change_report_state(self):
        """
        Проверяем, что при неудачной попытке поменять состояние репорта:
         1. Iptruler все равно делает свою работу
         2. Сообщение об этой неудачной попытке попадает в error log
        """
        report = marketsearch.create_report('market-report')

        iptruler_mock = Mock()
        logger_mock = Mock()
        system_ex_mock = Mock(return_value=1)
        with patch('market.pylibrary.mi_util.util.watching_system_ex', system_ex_mock):
            with patch('logging.getLogger', Mock(return_value=logger_mock)):
                report.close_iptruler('/etc/init.d/mockhttpsearch', iptruler_mock)

                iptruler_mock.down.assert_called_with(17051)
                system_ex_mock.assert_called_with(['/etc/init.d/mockhttpsearch', 'close-for-load', 'market-report'])
                self.assertEquals(logger_mock.error.called, 1)
                self.assertEquals(logger_mock.info.called, 0)

                report.open_iptruler('/etc/init.d/mockhttpsearch', iptruler_mock)

                iptruler_mock.up.assert_called_with(17051)
                system_ex_mock.assert_called_with(['/etc/init.d/mockhttpsearch', 'open-for-load', 'market-report'])
                self.assertEquals(logger_mock.error.called, 1)
                self.assertEquals(logger_mock.info.called, 0)


class ReloadTest(unittest.TestCase):
    def setUp(self, ):
        self._work_dir = os.path.join(os.getcwd(), context.TMP_DIR, 'reload_test')
        util.makedirs(self._work_dir)

        marketsearch3 = marketsearch.Config(get_plugin_path('marketsearch3.conf'), self._work_dir, 'user')
        marketsearch3.reportconf_gen = 'reportconf_gen'
        marketsearch3.mini_tank = 'mini_tank'
        self.marketsearch3 = marketsearch3

        plugins_dir = get_plugins_dir_path()
        prefix_dir = str(self._work_dir)

        DummyContext = namedtuple('DummyContext', 'config, full_config, plugins_dir, prefix_dir')
        self.context = DummyContext(marketsearch3, marketsearch3, plugins_dir, prefix_dir)

        report_config.SERVANTS_FILE = os.path.join(context.MARKETSEARCH_DATA_DIR, 'httpsearch_list')
        DummyServantConfig = namedtuple('ServantConfig', ['market_report', 'server', 'collections'])
        self.dummy_servant_config = DummyServantConfig(
            market_report={},
            server={'Port': '17051'},
            collections={
                'basesearch16-0': {},
                'basesearch16-8': {},
            }
        )
        self.servant_config_patcher = patch(
            'pyb.report_config.read_servant_config',
            return_value=self.dummy_servant_config
        )
        self.servant_config_patcher.start()
        Responce = namedtuple('Responce', ['status_code', 'text'])
        self.requests_get_patcher = patch(
            'requests.get',
            return_value=Responce(200, '<admin-action>ok</admin-action>')
        )
        self.get_mock = self.requests_get_patcher.start()
        self.watched_system_ex_patcher = patch('market.pylibrary.mi_util.util.watching_system_ex', return_value=0)
        self.watched_system_ex_mock = self.watched_system_ex_patcher.start()

    def tearDown(self):
        shutil.rmtree(self._work_dir, ignore_errors=True)
        self.watched_system_ex_patcher.stop()
        self.servant_config_patcher.stop()
        self.requests_get_patcher.stop()

    def _launcher(self, name, context=None, downloading_generations=None, open_for_load=True):
        if context is None:
            context = self.context

        if downloading_generations is None:
            downloading_generations = []

        launcher = marketsearch.DefaultReloadLauncher(
            self.context,
            name,
            downloading_generations,
            ['market-report'],
            open_for_load)
        # не пытаемся стартовать репорт на самом деле
        launcher._start_httpsearch_sync = lambda t: None
        launcher._update_quick_data = lambda: None
        return launcher

    def _set_current_generation(self, config, name):
        root_dir = os.path.join(self._work_dir, 'var/lib/search/')
        download_dir = os.path.join(root_dir, config.download_dir)
        util.makedirs(download_dir)
        util.atomic_write(os.path.join(download_dir, 'current.generation'), name)

    def _make_base_gen(self, name, published=False, additional_files=None, creation_time=None, meta=False):
        additional_files = additional_files or []
        root_dir = os.path.join(self._work_dir, 'var/lib/search/')
        for dist_name, dist_dir in self._dists(root_dir, self.marketsearch3, name, published=published):
            if (not meta and '-meta-' in dist_name) or (meta and '-meta-' not in dist_name):
                continue
            util.makedirs(dist_dir)
            self._create_file(os.path.join(dist_dir, 'content'), 'full ' + name, creation_time)

            if dist_name in ('search-report-data', 'search-meta-report-data'):
                backends_dir = os.path.join(dist_dir, 'backends')
                util.makedirs(backends_dir)
                self._create_file(os.path.join(backends_dir, 'content'), 'full ' + name, creation_time)
            if dist_name.startswith('search-part-'):
                qbids_generatioin = os.path.join(dist_dir, 'market_qbids.generation')
                self._create_file(qbids_generatioin, name + '00', creation_time)
        for file in additional_files:
            self._create_file(os.path.join(root_dir, file), 'full ' + name, creation_time)
        if published:
            self._set_current_generation(self.marketsearch3, name)
        if not published and creation_time is not None:
            generation_path = os.path.join(root_dir, self.marketsearch3.download_dir, name)
            self._set_mtime(generation_path, creation_time)

    def _dists(self, root_dir, config, generation_name, published=False):
        if published:
            for name, rel_path in config.dists:
                dist_path = os.path.join(root_dir, rel_path)
                yield name, dist_path
        else:
            for name, rel_path in config.dists:
                download_dir = os.path.join(root_dir, config.download_dir)
                yield name, os.path.join(download_dir, generation_name, name, '{}-{}'.format(name, generation_name))

    @staticmethod
    def _set_mtime(path, ts):
        os.utime(path, (ts, ts))

    @classmethod
    def _create_file(cls, path, content, creation_time=None):
        util.atomic_write(path, content)
        if creation_time is not None:
            cls._set_mtime(path, creation_time)

    def _get_generation_path(self, generation_name):
        root_dir = os.path.join(self._work_dir, 'var/lib/search/')
        return os.path.join(root_dir, self.marketsearch3.download_dir, generation_name)

    @property
    def _current_generation(self):
        root_dir = os.path.join(self._work_dir, 'var/lib/search/')
        current_full_path = os.path.join(root_dir, self.marketsearch3.download_dir, 'current.generation')
        return util.get_file_value(current_full_path)

    def test_not_published_gen(self):
        self._make_base_gen('20170320_1000')
        self._launcher('20170320_1000')
        self.assertEqual('', self._current_generation)

    def test_reload_full(self):
        old_gen_path = self._get_generation_path('20100101_0001')

        self._make_base_gen('20100101_0001', creation_time=1262329200)
        self._make_base_gen('20170320_0900')
        self._make_base_gen('20170320_1000', published=True)
        self._make_base_gen('20170320_1100')
        self.assertEqual('20170320_1000', self._current_generation)
        self.assertFalse(self.get_mock.called)
        self.assertTrue(os.path.exists(old_gen_path))

        launcher = self._launcher('20170320_1100', downloading_generations=['20100101_0001'])
        launcher.launch(0)

        self.assertEqual('20170320_1100', self._current_generation)
        # проверяем, что очень старое так и не скачавшееся поколение 20100101_0001 было удалено
        self.assertFalse(os.path.exists(old_gen_path))
        # открываемся от балансера после релода full
        httpsearch_cmd = os.path.join(self._work_dir, 'etc/init.d/httpsearch')
        self.watched_system_ex_mock.assert_any_call([httpsearch_cmd, 'open-for-load', 'market-report'])

    def test_reload_closed(self):

        self._make_base_gen('20100101_0001', creation_time=1262329200)
        self._make_base_gen('20170320_1000', published=True)
        self._make_base_gen('20170320_1100')
        self.assertEqual('20170320_1000', self._current_generation)
        self.assertFalse(self.get_mock.called)

        launcher = self._launcher('20170320_1100', downloading_generations=['20100101_0001'], open_for_load=False)
        launcher.launch(0)

        self.assertEqual('20170320_1100', self._current_generation)
        # не открываемся от балансера, если не указано обратного
        httpsearch_cmd = os.path.join(self._work_dir, 'etc/init.d/httpsearch')
        self.watched_system_ex_mock.assert_not_called_with([httpsearch_cmd, 'open-for-load', 'market-report'])

    def test_reload_meta(self):
        old_gen_path = self._get_generation_path('20200101_0001')

        self._make_base_gen('20200101_0001', creation_time=1262329200, meta=True)
        self._make_base_gen('20270320_0900', meta=True)
        self._make_base_gen('20270320_1000', published=True, meta=True)
        self._make_base_gen('20270320_1100', meta=True)
        self.assertEqual('20270320_1000', self._current_generation)
        self.assertFalse(self.get_mock.called)
        self.assertTrue(os.path.exists(old_gen_path))

        launcher = self._launcher('20270320_1100', downloading_generations=['20200101_0001'])
        launcher.launch(0)

        self.assertEqual('20270320_1100', self._current_generation)
        # проверяем, что очень старое так и не скачавшееся поколение 20200101_0001 было удалено
        self.assertFalse(os.path.exists(old_gen_path))
        # открываемся от балансера после релода full
        httpsearch_cmd = os.path.join(self._work_dir, 'etc/init.d/httpsearch')
        self.watched_system_ex_mock.assert_any_call([httpsearch_cmd, 'open-for-load', 'market-report'])

    def test_keep_very_old_generations(self):
        """ В тесте проверяем, что старые поколения не будут удалены, если лимит
        по количеству хранимых поколений не превышен.

        * old_gen – это очень старое поколение, оно сейчас используется. А еще оно в 10 раз старше,
          чем позволяет трешолд TOO_OLD_DOWNLOADING_GENERATION_S.
        * new_gen – новое поколение, которое только скачалось, на него будет выполнен релоад.
        """

        now_ts = time.time()
        old_ts = now_ts - 10 * marketsearch.DefaultReloadLauncher.TOO_OLD_DOWNLOADING_GENERATION_S
        old_gen = '20210101_0100'
        new_gen = '20210301_0200'
        old_gen_path = self._get_generation_path(old_gen)
        new_gen_path = self._get_generation_path(new_gen)

        self._make_base_gen(old_gen, creation_time=old_ts)
        self._make_base_gen(old_gen, published=True)
        self._make_base_gen(new_gen, creation_time=now_ts)

        # оба поколения присутствуют на диске
        self.assertTrue(os.path.exists(old_gen_path))
        self.assertTrue(os.path.exists(new_gen_path))

        # релоад на new_gen
        launcher = self._launcher(new_gen)
        launcher.launch(0)

        # после релоада старое поколение не было удалено
        self.assertTrue(os.path.exists(old_gen_path))
        self.assertTrue(os.path.exists(new_gen_path))


if __name__ == '__main__':
    unittest.main()
