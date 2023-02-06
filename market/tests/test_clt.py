# -*- coding: utf-8 -*-
import os
import shutil
import logging
import unittest

from six.moves.configparser import ConfigParser
from collections import namedtuple

from market.pylibrary.filelock import FileLock

from market.pylibrary.mindexerlib import commands_infrastructure
from market.pylibrary.mindexerlib import util
import context


class DummyConfig(object):
    def __init__(self, tmp_dir):
        self.lock_dir = tmp_dir
        self.run_dir = tmp_dir
        self.log_dir = tmp_dir
        self.command_status_dir = tmp_dir
        self.mindexer_clt_audit_log_file = tmp_dir + 'mindexer_clt_audit.log'


def contains(text, subtext):
    return -1 != text.find(subtext)


class TestInfrastructure(unittest.TestCase):
    TMP = 'tmp'

    def __clean(self):
        shutil.rmtree(self.TMP, ignore_errors=True, onerror=None)

    def __store_loggers(self):
        self.__loggers_handlers = list(logging.getLogger().handlers)

    def __restore_loggers(self):
        logging.getLogger().handlers = self.__loggers_handlers

    def setUp(self):
        self.__clean()
        self.__store_loggers()
        util.makedirs(self.TMP)

        LogParams = namedtuple('LogParams', 'dir filename is_rotated level')
        self.__dummy_config = DummyConfig(self.TMP)
        log_params = LogParams(self.__dummy_config.log_dir, 'cron.log', False, logging.DEBUG)

        self.__command_runner = commands_infrastructure.CommandRunner()
        self.__command_runner._setup_log('cron', log_params)
        self.__command_runner._setup_command_attrs('test',
                                                   self.__dummy_config.run_dir,
                                                   self.__dummy_config.lock_dir)

        self._cronstatuspath = os.path.join(self.TMP, 'command_status.ini')
        self._logger = logging.getLogger('test_clt')

    def tearDown(self):
        self.__clean()
        self.__restore_loggers()

    def cronlog(self):
        with open(os.path.join(self.TMP, 'cron.log'), 'r') as cl:
            return cl.read()

    def setup_executor(self, command_name, cron=True):
        self.__command_runner._setup_executor(
            'cron' if cron else 'console', command_name,
            logging.getLogger('test_clt'), None,
            command_status_dir=self.__dummy_config.command_status_dir,
            audit_log_file=self.__dummy_config.mindexer_clt_audit_log_file,
            keep_result='1h'
        )

    @staticmethod
    def OK(*args):
        pass

    @staticmethod
    def EXC(*args):
        raise Exception('test_infa_error')

    def test_keep_result(self):
        # emulate call 'OK'
        self.setup_executor('OK')
        for _ in range(2):  # call multiple times to check 'keep_result' opt
            self.assertEqual(
                commands_infrastructure.CommandExecutor.OK,
                self.__command_runner._do_run(
                    self.OK, args=(self.__dummy_config, None), logger=self._logger
                )
            )

        cronlog = self.cronlog()

        self.assertTrue(contains(cronlog, 'test_clt'))
        self.assertTrue(contains(cronlog, 'started'))
        self.assertTrue(contains(cronlog, 'finished'))
        self.assertTrue(contains(cronlog, 'use last succeeded result'))

        self.assertFalse(contains(cronlog, 'ERROR'))
        self.assertFalse(contains(cronlog, 'Exception'))
        self.assertFalse(contains(cronlog, 'test_infa_error'))

        config = ConfigParser()
        config.read([self._cronstatuspath])
        self.assertEqual(config.getint('OK', 'fail'), 0)
        self.assertTrue(config.has_option('OK', 'last_succeeded'))

        self.assertTrue(os.path.exists(os.path.join(self.TMP, 'test.OK.pid')))

    def test_keep_result_non_cron(self):
        # emulate call 'OK'
        self.setup_executor('OK', cron=False)
        for _ in range(2):  # call multiple times to check 'keep_result' opt
            self.assertEqual(
                commands_infrastructure.CommandExecutor.OK,
                self.__command_runner._do_run(
                    self.OK, args=(self.__dummy_config, None), logger=self._logger
                )
            )
        cronlog = self.cronlog()
        self.assertTrue(contains(cronlog, 'use last succeeded result'))

    def test_cmd_failed(self):
        # emulate call 'EXC'
        self.setup_executor('EXC')
        self.assertEqual(commands_infrastructure.CommandExecutor.FAIL,
                         self.__command_runner._do_run(cmd_func=self.EXC,
                                                       args=(self.__dummy_config, None),
                                                       logger=self._logger)
                         )

        cronlog = self.cronlog()

        self.assertTrue(contains(cronlog, 'test_clt'))
        self.assertTrue(contains(cronlog, 'started'))
        self.assertTrue(contains(cronlog, 'failed'))

        self.assertTrue(contains(cronlog, 'ERROR'))
        self.assertTrue(contains(cronlog, "Exception('test_infa_error')"))
        self.assertTrue(contains(cronlog, 'Traceback'))

        config = ConfigParser()
        config.read([self._cronstatuspath])
        self.assertEqual(config.getint('EXC', 'fail'), 1)
        self.assertFalse(config.has_option('EXC', 'last_succeeded'))

        self.assertTrue(os.path.exists(os.path.join(self.TMP, 'test.EXC.pid')))

    def test_call_to_locked(self):
        # EXC - возвращает FAIL != BLOCKED_RETURN_CODE
        # не должен быть записан pid файл
        dummy_filelock = FileLock(os.path.join(self.TMP, 'test.dummy'))
        dummy_filelock.lock(blocking=True)
        self.setup_executor('dummy')
        self.assertEqual(commands_infrastructure.CommandRunner.BLOCKED_RETURN_CODE,
                         self.__command_runner._do_run(self.EXC,
                                                       args=(self.__dummy_config, None),
                                                       logger=self._logger)
                         )

        self.assertFalse(os.path.exists(os.path.join(self.TMP, 'test.dummy.pid')))


class TestConvertor(unittest.TestCase):

    def test_timeout_parser(self):
        self.assertEqual(1, commands_infrastructure.parse_time('1s'))
        self.assertEqual(60, commands_infrastructure.parse_time('1m'))
        self.assertEqual(3600, commands_infrastructure.parse_time('1h'))
        self.assertEqual(86400, commands_infrastructure.parse_time('1d'))
        self.assertEqual(180, commands_infrastructure.parse_time('3m'))


if '__main__' == __name__:
    context.main()
