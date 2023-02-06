# -*- coding: utf-8 -*-
import json
import os
import shutil
import unittest
from collections import defaultdict
from contextlib import contextmanager

import mock
import pytest
from market.pylibrary.mindexerlib import util
from sqlalchemy import select

import context
from market.idx.marketindexer.marketindexer import miconfig
from market.pylibrary.mindexerlib import sql
import market.idx.pylibrary.mindexer_core.file_switch.file_switch as file_switch
import market.idx.pylibrary.mindexer_core.publishers.publisher as publisher
import market.idx.pylibrary.mindexer_core.stats.stats as stats
import market.idx.pylibrary.mindexer_core.zkmaster.zkmaster as zkmaster


@contextmanager
def make_zk_mock():
    zkMaster = mock.create_autospec(zkmaster.ZkMaster)
    zkMaster.get_current_master_uploaded_index.return_value = zkmaster.IndexInfo('20131003_1919')
    yield zkMaster


def mock_it(publisher_object):
    publisher_object.start_generation = mock.Mock()
    publisher_object.rollback_pipeline_data = mock.Mock()
    return publisher_object


class TestPublisher(context.HbaseTestCase):
    generation = '20131003_1919'
    diff_generation = '20131003_1930'
    hosts = ['a.yandex.ru', 'b.yandex.ru']

    def setUp(self):
        self.CONFIG = miconfig.default()
        for host in self.hosts:
            context.touch(self.CONFIG.working_dir, self.generation, 'report-data', 'backends', host + '.cfg')
            context.touch(self.CONFIG.working_dir, self.diff_generation, 'report-data', 'backends', host + '.cfg')

        dd = lambda: defaultdict(dd)
        rconfig = dd()
        for host in self.hosts:
            hostconfig = {
                'name': host,
                'service': 'marketsearch3',
            }
            rconfig['dcgroups']['group@dc']['hosts'][host] = hostconfig
        rconfig['download_timeout'] = 5600

        config_path = miconfig.default().reductor_config_path
        context.touch(config_path)
        with open(config_path, 'w') as fp:
            json.dump(rconfig, fp, indent=2)

        ds = miconfig.default().datasources
        context.create_table_from_description(ds, 'super', sql.generations)

        stats.append_stats(self.generation, {'странная неиспользованная статистика': 5})

        self.psh = mock_it(publisher.FullPublisher(zkmastertype=context.Null))

        context.touch(self.CONFIG.working_dir, self.generation, 'input', 'feedlog.meta')
        context.touch(self.CONFIG.working_dir, self.generation, 'input', 'mb_snapshot.mbi.result.pbuf.sn')

        context.write(os.path.join(self.CONFIG.working_dir, self.generation, 'mistate', 'start_date'), '0')
        context.write(os.path.join(self.CONFIG.working_dir, self.generation, 'mistate', 'end_date'), '1')
        context.write(os.path.join(self.CONFIG.working_dir, self.generation, 'mistate', 'release_date'), '2')
        context.write(os.path.join(self.CONFIG.working_dir, self.generation, 'mistate', 'sc_version'), '3')
        context.write(os.path.join(self.CONFIG.working_dir, self.generation, 'input', 'qindex.generation'),
                      '20151111_211621')

        context.write(os.path.join(self.CONFIG.working_dir, self.diff_generation, 'mistate', 'start_date'), '0')
        context.write(os.path.join(self.CONFIG.working_dir, self.diff_generation, 'mistate', 'end_date'), '1')
        context.write(os.path.join(self.CONFIG.working_dir, self.diff_generation, 'mistate', 'release_date'), '2')
        context.write(os.path.join(self.CONFIG.working_dir, self.diff_generation, 'mistate', 'sc_version'), '3')
        context.write(os.path.join(self.CONFIG.working_dir, self.diff_generation, 'input', 'qindex.generation'),
                      '20151111_211621')

        with zkmaster.ZkMaster() as zk:
            zk.make_me_master()

    def tearDown(self):
        shutil.rmtree(self.CONFIG.working_dir, ignore_errors=True)

    @pytest.yield_fixture(scope='class', autouse=True)
    def do_not_send_data_to_graphite(self):
        with mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.measure_and_send'):
            yield

    def _reset_status(self):
        self.psh._status.clear()

    def _check_ok(self):
        self.assertTrue(self.psh._status.empty())  # no errors
        self._reset_status()

    def _check_fail(self):
        self.assertFalse(self.psh._status.empty())
        self._reset_status()

    def _check_status(self, monitor, config):
        self.assertEqual(monitor, publisher.check_publisher(config)[:len(monitor)])

    def _check_released(self, super_connection, generation_name):
        with super_connection.begin():
            result = super_connection.execute(
                select([sql.generations.c.released]).where(sql.generations.c.name == generation_name))

            released = result.fetchone()
            self.assertEqual(released[0], 1)

    def test_run_reductor(self):
        self.psh.run_reductor(self.generation, context.Null())
        self.psh.reload_marketsearch()

    def test_check_publisher(self):
        self._reset_status()
        config = miconfig.force_full_mode()

        self._check_status('1;no status file', config)
        util.touch(publisher.get_publisher_status_file())

        self._check_status('1;no success file', config)
        util.touch(publisher.get_publisher_success_file())

        self.assertRaises(publisher.GenerationNotExists, self.psh.publish_generation, 'bad_generation')
        self._check_fail()
        self._check_status('0; pub_status=publishing generation not found; age 0h 0m', config)

    def test_upload_full(self):
        config = miconfig.force_full_mode()
        self.psh.upload(self.generation, True)
        self._check_ok()
        self._check_status('0; pub_status=<empty>; age 0h 0m', config)

    def test_fail(self):
        working_dir = self.CONFIG.working_dir
        os.unlink(os.path.join(working_dir, self.generation, 'report-data', 'backends', self.hosts[0] + '.cfg'))
        psh = mock_it(publisher.FullPublisher())
        self.assertRaises(publisher.ReductorError, psh.run_reductor, self.generation, context.Null())


class TestReductor(unittest.TestCase):
    def test_ok(self):
        reductor = publisher.Reductor('true', config_path='/')
        self.assertEqual(None, reductor.upload('1', skip_check=True))
        self.assertEqual(None, reductor.switch())
        self.assertEqual(None, reductor.reload_marketsearch())

    def test_fail(self):
        reductor = publisher.Reductor('false', config_path='/')
        self.assertRaises(publisher.ReductorError, reductor.upload, '1', skip_check=True)
        self.assertRaises(publisher.ReductorError, reductor.switch)
        self.assertRaises(publisher.ReductorError, reductor.reload_marketsearch)


class TestFileSwitch(unittest.TestCase):
    def setUp(self):
        context.create_workdir_test_environment(context.switchdir)

    def test_fileswitch(self):
        self.assertFalse(file_switch.is_switch_on('disable_copybases'))
        self.assertRaises(Exception, file_switch.is_switch_on, 'this-is-unsupported-switch')

        file_switch.toggle_switch('disable_copybases')
        self.assertTrue(file_switch.is_switch_on('disable_copybases'))

        file_switch.toggle_switch()


if '__main__' == __name__:
    unittest.main()
