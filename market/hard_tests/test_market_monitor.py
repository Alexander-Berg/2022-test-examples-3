# -*- coding: utf-8 -*-

import os
import subprocess
import time
import unittest
from collections import namedtuple
from datetime import datetime, timedelta

import market.pylibrary.database as database
import market.pylibrary.filelock as filelock
from market.pylibrary.mindexerlib.proc import is_process_alive
from sqlalchemy.sql.functions import now

import context
from market.idx.marketindexer.marketindexer import miconfig
import market.idx.pylibrary.mindexer_core.market_monitorings.market_monitorings as market_monitorings
from market.pylibrary.mindexerlib import sql, util

RUNDIR = context.rundir
LOGDIR = context.logdir

SECONDS_IN_MINUTE = 60
SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE
SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR

GenerationTypeContext = namedtuple('GenerationTypeContext', ['my_type',
                                                             'other_type',
                                                             'miname',
                                                             'config',
                                                             'gen_age_interval_hours',
                                                             'log_age_interval_minutes',
                                                             'table', ])


class TestMonitorings(context.MysqlTestCase):
    def setUp(self):
        context.create_workdir_test_environment(RUNDIR)
        context.create_workdir_test_environment(LOGDIR)
        self.HOSTNAME = sql.HOSTNAME
        self.DS = miconfig.default().datasources

    def _get_gentype_context(self, gentype):
        if gentype == 'blue':
            return GenerationTypeContext(
                my_type='blue',
                other_type='full',
                miname='blue-massindexer',
                config=miconfig.force_full_mode(),
                gen_age_interval_hours=6,
                log_age_interval_minutes=100,
                table=sql.blue_generations,
            )
        else:
            return GenerationTypeContext(
                my_type='market',
                other_type='blue',
                miname='massindexer',
                config=miconfig.force_full_mode(),
                gen_age_interval_hours=6,
                log_age_interval_minutes=100,
                table=sql.generations,
            )

    def _prepare_db(self, gentype):
        gentype_ctx = self._get_gentype_context(gentype)

        super_connection = database.connect(**self.DS['super'])
        super_connection.drop_table(gentype_ctx.table, sql.metadata)
        context.create_table_from_description(self.DS, 'super', gentype_ctx.table)
        with super_connection.begin():
            super_connection.execute(gentype_ctx.table.insert(values=[
                {'name': '20131101_0800', 'type': gentype_ctx.my_type, 'hostname': self.HOSTNAME, 'state': 'complete',
                 'end_date': datetime.now() - timedelta(hours=1), 'fail_reason': ''},
                {'name': '20131101_0900', 'type': gentype_ctx.my_type, 'hostname': self.HOSTNAME, 'state': 'failed',
                 'end_date': now(), 'fail_reason': 'test'},
                {'name': '20131101_0930', 'type': gentype_ctx.my_type, 'hostname': self.HOSTNAME, 'state': 'cancelled',
                 'end_date': now(), 'fail_reason': 'cancelled'},
                {'name': '20131101_1000', 'type': gentype_ctx.other_type, 'hostname': self.HOSTNAME,
                 'state': 'complete',
                 'end_date': datetime.now() - timedelta(hours=1), 'fail_reason': ''},
                {'name': '20131101_1100', 'type': gentype_ctx.other_type, 'hostname': self.HOSTNAME, 'state': 'failed',
                 'end_date': now(), 'fail_reason': 'test'},
            ]))
        return super_connection

    def _prepare_mi(self, miname, create_log=True):
        lock_file = os.path.join(RUNDIR, '{}.lock'.format(miname))
        lockobj = filelock.FileLock(lock_file)
        lockobj.lock()

        logfile = None
        if create_log:
            logfile = os.path.join(context.logdir, 'massindexer.log')
            context.touch(logfile)

        proc = subprocess.Popen(['echo', 'MI'])
        pid_file = os.path.join(RUNDIR, '{}.pid'.format(miname))
        with open(pid_file, 'w') as f:
            f.write(str(proc.pid))

        return proc, logfile

    def _check_monitoring(self, config, return_code, message):
        answer = market_monitorings.MiMonitoring(config).check_marketindexer_ok()
        self.assertTrue(answer.startswith('{};'.format(return_code)), answer)
        self.assertTrue(-1 != answer.find(message), answer)

    def _do_check_marketindexer_ok(self, gentype):
        '''
        This is test for general big massindexer
        '''

        super_connection = self._prepare_db(gentype)
        gentype_ctx = self._get_gentype_context(gentype)
        proc, logfile = self._prepare_mi(gentype_ctx.miname)
        self._check_monitoring(gentype_ctx.config, 0, 'indexer is ok')

        mi_sleep_time_seconds = (time.time() - gentype_ctx.log_age_interval_minutes * SECONDS_IN_MINUTE - 10)
        os.utime(logfile, (mi_sleep_time_seconds, mi_sleep_time_seconds))
        self._check_monitoring(gentype_ctx.config, 2, 'indexer has been sleeping more than')

        with super_connection.begin():
            super_connection.execute(gentype_ctx.table.insert(values={
                'name': '20131101_1700', 'type': gentype_ctx.my_type, 'hostname': self.HOSTNAME, 'state': 'failed',
                'end_date': datetime.now() - timedelta(minutes=5)
            }))
        self._check_monitoring(gentype_ctx.config, 2, 'Too many failed generations')

        with super_connection.begin():
            super_connection.execute(gentype_ctx.table.update().values(
                end_date=datetime.now() - timedelta(hours=gentype_ctx.gen_age_interval_hours)).where(
                gentype_ctx.table.c.state == 'complete'))
        self._check_monitoring(gentype_ctx.config, 2, 'passed since last complete generation')

        self.assertEqual(0, proc.wait())
        self._check_monitoring(gentype_ctx.config, 2, 'marketindexer is down')

    def _do_check_marketindexer_sleep_after_successfull_indexing(self, gentype):
        '''
        Assume that massindexer has already completed, massindexer.log file is archived,
        we have massindexer_{generation}.log file in /var/log/marketindexer folder only
        (in that time massindexer is sleeping (about 15-40 minutes)
        '''
        super_connection = self._prepare_db(gentype)
        gentype_ctx = self._get_gentype_context(gentype)
        proc, _ = self._prepare_mi(gentype_ctx.miname, create_log=False)

        generation = util.datetime2generation(sql.get_id_name_from_database(config=None, connection=super_connection))
        logfile = os.path.join(context.logdir, 'massindexer_{}.log'.format(generation))
        context.touch(logfile)

        generation = util.datetime2generation(sql.get_id_name_from_database(config=None, connection=super_connection, interval_seconds=-SECONDS_IN_DAY))
        logfile = os.path.join(context.logdir, 'massindexer_{}.log'.format(generation))
        context.touch(logfile)
        mi_sleep_time_seconds = (time.time() - SECONDS_IN_DAY)
        os.utime(logfile, (mi_sleep_time_seconds, mi_sleep_time_seconds))

        self._check_monitoring(gentype_ctx.config, 0, 'indexer is ok')
        self.assertEqual(0, proc.wait())

    def test_is_process_alive(self):
        lock_file = os.path.join(RUNDIR, 'sleep.lock')
        lockobj = filelock.FileLock(lock_file)
        lockobj.lock()

        proc = subprocess.Popen(['echo', 'MI'])
        pid_file = os.path.join(RUNDIR, 'sleep.pid')
        with open(pid_file, 'w') as f:
            f.write(str(proc.pid))

        self.assertTrue(is_process_alive(pid_file, lock_file))

        lockobj.unlock()
        self.assertFalse(is_process_alive(pid_file, lock_file))

        self.assertEqual(0, proc.wait())
        self.assertFalse(is_process_alive(pid_file, lock_file))

    def test_marketindexer_ok(self):
        self._do_check_marketindexer_ok('full')

    def test_marketindexer_sleep_after_successfull_indexing(self):
        self._do_check_marketindexer_sleep_after_successfull_indexing('full')


if '__main__' == __name__:
    unittest.main()
