import os

import copy
import logging
import requests
import shutil
import socket
import time
import yatest.common.network as network

from hamcrest import (
    assert_that,
    equal_to,
)

from .resources.idxapi_cfg import IdxApiConf

from market.idx.yatf.common import get_source_path, get_binary_path
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.test_envs.base_env import BaseEnv


def _DATA_DIR():
    return os.path.join(
        get_source_path(),
        'market', 'idx', 'feeds', 'feedparser', 'test', 'data'
    )


def _DATA():
    return {
        name: FileResource(os.path.join(_DATA_DIR(), filename))
        for name, filename in list({
            'countries_utf8_c2n': 'countries_utf8.c2n',
            'currency_rates_xml': 'currency_rates.xml',
            'geo_c2p': 'geo.c2p',
        }.items())
    }


class IdxApiTestEnv(BaseEnv, network.PortManager):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        resources['idxapi_cfg'] = resources.get('idxapi_cfg', IdxApiConf())
        super(IdxApiTestEnv, self).__init__(**resources)
        self.resources.update({
            name: copy.deepcopy(_DATA()[name])
            for name in set(_DATA().keys()) - set(resources.keys())
        })

    def __enter__(self, path=None):
        BaseEnv.__enter__(self)
        network.PortManager.__enter__(self)
        if path is None:
            relative_path = os.path.join(
                'market',
                'idx',
                'api',
                'bin',
                'idxapi'
            )
            absolute_path = get_binary_path(relative_path)
            path = absolute_path

        currency_rates_path = self.resources['currency_rates_xml'].path
        countries_path = self.resources['countries_utf8_c2n'].path
        geo_path = self.resources['geo_c2p'].path
        geo_dir = os.path.join(self.input_dir, 'var', 'lib', 'yandex', 'getter', 'geobase', 'recent')
        os.makedirs(geo_dir)
        shutil.copy(countries_path, os.path.join(geo_dir, 'countries_utf8.c2n'))
        shutil.copy(geo_path, os.path.join(geo_dir, 'geo.c2p'))
        currency_rates_dir = os.path.join(self.input_dir, 'var', 'lib', 'yandex', 'getter', 'currency_rates', 'recent')
        os.makedirs(currency_rates_dir)
        shutil.copy(currency_rates_path, os.path.join(currency_rates_dir, 'currency_rates.xml'))

        self._port = self.get_port()

        cmd = [
            path,
            '--prefix', self.input_dir,
            '--port', str(self._port),
            '--slow', '--fast'
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            wait=False,
            env={'IDXAPI_LOCAL_CONFIG_PATH': self.resources['idxapi_cfg'].path},
            check_exit_code=False)

        for i in range(60):
            time.sleep(1)
            if not self.exec_result.running:
                logging.error('cluster failed, exit_code=%s, check_num=%s' % (self.exec_result.exit_code, i))
            assert self.exec_result.running
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            if not sock.connect_ex(('127.0.0.1', self._port)):
                return self
        raise Exception('idxapi not started')

    def __exit__(self, *args):
        BaseEnv.__exit__(self, *args)
        network.PortManager.__exit__(self, *args)
        if self.exec_result is not None and self.exec_result.running:
            self.exec_result.kill()
        self.exec_result = None

    @property
    def host(self):
        return 'localhost'

    @property
    def description(self):
        return 'idxapi'

    @property
    def port(self):
        return self._port

    def get(self, uri, params={}, check_status=True):
        if uri is None:
            uri = ''
        if uri != '' and not uri.startswith('/'):
            uri = '/' + uri
        resp = requests.get(
            'http://{host}:{port}{uri}'.format(
                host=self.host,
                port=self.port,
                uri=uri
            ), params=params)
        if check_status:
            assert_that(
                resp.status_code, equal_to(requests.codes.ok),
                'Error in get request to idxapi; code: {code}; response:"{resp}"; request:{req}'.format(
                    code=resp.status_code,
                    resp=resp.text,
                    req=resp.url
                )
            )
        return resp
