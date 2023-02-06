# coding: utf-8

import os
import time
import logging
import requests

from market.idx.pylibrary.trace_log.yatf.tracelog_parser import parse_tracelog
from market.idx.yatf.common import get_binary_path
from market.idx.yatf.test_envs.base_env import BaseEnv

logger = logging.getLogger()


def miner_binary_path():
    return get_binary_path(os.path.join('market', 'idx', 'datacamp', 'miner', 'bin', 'miner'))


class MinerTestEnv(BaseEnv):

    def __init__(self, **resources):
        super(MinerTestEnv, self).__init__(**resources)

        self.config = self.resources['miner_cfg']
        self.input_topic = self.resources.get('input_topic')
        self.output_topic = self.resources.get('output_topic')
        self.binary_path = self.resources.get('miner_binary_path') or miner_binary_path()

    @property
    def description(self):
        return 'miner_env'

    @property
    def resource_dependencies(self):
        return {
            'miner_cfg': [
                'shops_outlet_mmap'
            ]
        }

    def __enter__(self):
        BaseEnv.__enter__(self)
        cmd = [self.binary_path, os.path.abspath(self.config.path)]
        os.environ['IGNORE_YT_LIVENESS'] = 'true'

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            wait=False,
            check_exit_code=False
        )

        for i in range(20):
            time.sleep(1)
            if self.exec_result.running:
                logger.info(
                    'miner started:\n'
                    'controller_port: %d\n'
                    'input_topic: %s\n'
                    'output_topic: %s\n',
                    self.controller_port,
                    self.input_topic.topic if self.input_topic else None,
                    self.output_topic.topic if self.output_topic else None)
                return self

            logger.error('miner not started yet, exit_code=%s, check_num=%s', self.exec_result.exit_code, i)

        raise RuntimeError('Failed start miner')

    @property
    def offer_trace_log(self):
        return parse_tracelog(self.config.trace_path)

    def __exit__(self, *args):
        BaseEnv.__exit__(self, args)
        if self.exec_result is not None and self.exec_result.running:
            self.exec_result.kill()
        if self.exec_result:
            self.exec_result.wait(check_exit_code=False)
        self.exec_result = None

    @property
    def host(self):
        return 'localhost'

    @property
    def controller_port(self):
        return self.config.controller_port

    @property
    def http_port(self):
        return self.config.http_port

    @property
    def monservice_port(self):
        return self.config.monservice_port

    @property
    def metrics(self):
        url = 'http://{host}:{port}/metrics/json'.format(host=self.host, port=self.monservice_port)
        return requests.post(url).json()['sensors']
