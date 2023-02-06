# coding: utf-8

import os
import sys
import time
import copy
import signal

import logging
import yatest.common

from market.idx.yatf.test_envs.base_env import BaseEnv

from market.idx.admin.config_daemon.yatf.resources.final_config import FinalConfig

logger = logging.getLogger('config_daemon.test_env')

DAEMON_BIN = yatest.common.binary_path(os.path.join('market', 'idx', 'admin', 'config_daemon', 'bin', 'config_daemon'))


class DaemonTestEnv(BaseEnv):
    def __init__(self, env_type=None, mitype=None, zk=None, yaconf_mode=False, run_once=False, **resources):
        '''Передача ресурсов демона в отдельное поле окружения'''

        super(DaemonTestEnv, self).__init__(**resources)
        self.zk = zk
        self.yaconf_mode = yaconf_mode
        self.env_type = env_type
        self.mitype = mitype
        self.run_once = run_once

    def __enter__(self):
        super(DaemonTestEnv, self).__enter__()
        return self

    @property
    def final_config(self):
        if not self.outputs['final_config'].config_data:
            self.outputs['final_config'].load()
        return self.outputs['final_config'].config_data

    @property
    def env(self):
        '''Окружение для запуска демона'''

        return {
            'ZK_CONFIG_PATH': self.resources['zk_config'].path,
            'ENV_TYPE': self.env_type,
            'MI_TYPE': self.mitype
        }

    def hup_signal_handler(self, signum, frame):
        pass

    def execute(self, path=DAEMON_BIN):
        self.do_execute(path=path)

    def do_execute(self, path=DAEMON_BIN):
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)

        if not os.path.exists(self.input_dir):
            os.makedirs(self.input_dir)

        if not os.path.exists(DAEMON_BIN):
            raise RuntimeError('Failed to find binary file')

        final_config_path = os.path.join(self.input_dir, 'local.ini.override')
        pidfile_path = os.path.join(self.input_dir, 'user.pid')

        logger.info('Final config file: ' + final_config_path)
        logger.info('User pidfile: ' + pidfile_path)
        logger.info('Daemon binary file: ' + DAEMON_BIN)

        env = copy.deepcopy(self.env)

        cmd = [DAEMON_BIN, '--config-dir', self.input_dir, '--pidfile', pidfile_path]

        if self.yaconf_mode:
            cmd.extend(['--yaconf-mode'])
        if self.run_once:
            cmd.extend(['--run-once'])

        signal.signal(signal.SIGHUP, self.hup_signal_handler)

        self.exec_result = self.try_execute_under_gdb(
            cmd, cwd=self.output_dir, wait=self.run_once, env=env, check_exit_code=False, stderr=sys.stderr
        )

        if not self.run_once:
            is_started = False
            for i in range(5):
                time.sleep(1)
                if self.exec_result.running and not is_started:
                    logger.info('Config_daemon started in less than {} second(s)'.format(i + 1))
                    is_started = True

            if not is_started:
                raise Exception('Failed to start config_daemon')

        self.outputs.update({
            'final_config': FinalConfig(final_config_path, self.yaconf_mode)
        })

    def __exit__(self, *args):
        BaseEnv.__exit__(self, args)
        if self.exec_result is not None and self.exec_result.running:
            self.exec_result.kill()
        self.exec_result = None
