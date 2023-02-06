# coding: utf-8

import os
import time
import logging
import socket
import requests

from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from yatest.common.network import PortManager


logger = logging.getLogger()


class CommonProxyTestEnv(BaseEnv):

    def __init__(self, app_name='common_proxy', bin_path=None, **resources):
        super(CommonProxyTestEnv, self).__init__(**resources)
        self.app_name = app_name
        self.bin_path = bin_path
        self.config = self.resources['{}_config'.format(app_name)]
        self._port_manager = None

    @property
    def description(self):
        return 'common_proxy_env'

    def __enter__(self):
        BaseEnv.__enter__(self)
        self.after_environment_init()

        self._http_port = self.port_manager.get_port()
        self._controller_port = self.port_manager.get_port()

        env = {
            'CONTROLLER_PORT': str(self._controller_port),
            'HTTP_PORT': str(self._http_port),
            'PORT': str(self._controller_port),
            'APP_NAME': self.app_name,
        }

        cmd = self.cmd_args
        for k, v in env.items():
            cmd += [self.cmd_arg_variable, '{k}={v}'.format(k=k, v=v)]
        logging.info('Executing arguments: {}'.format(cmd))
        os.environ['IGNORE_YT_LIVENESS'] = 'true'
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            wait=False,
            check_exit_code=False,
        )

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        for i in range(60):
            time.sleep(1)
            if not self.exec_result.running:
                logging.error('{} failed, exit_code={}, check_num={}'.format(
                    self.app_name, self.exec_result.exit_code, i))
            assert self.exec_result.running
            if not sock.connect_ex(('localhost', self._controller_port)):
                return self
        raise Exception('{} not started'.format(self.app_name))

    def __exit__(self, *args):
        BaseEnv.__exit__(self, args)

        if self.exec_result is not None and self.exec_result.running:
            self.exec_result.kill()
        if self.exec_result:
            self.exec_result.wait(check_exit_code=False)
        self.exec_result = None

        if self._port_manager:
            self._port_manager.release()
            self._port_manager = None

    @property
    def port_manager(self):
        if self._port_manager is None:
            self._port_manager = PortManager()
        return self._port_manager

    @property
    def host(self):
        return 'localhost'

    @property
    def controller_port(self):
        return self._controller_port

    @property
    def http_port(self):
        return self._http_port

    @property
    def cmd_arg_variable(self):
        return '-V'

    @property
    def cmd_args(self):
        return [self.bin_path, os.path.abspath(self.config.path)]

    def after_environment_init(self):
        pass


class ServiceConnector:
    def __init__(self, host, port):
        self.host = host
        self.port = port

    def do_request(self, method, path, data=None, headers=None, params=None):
        if headers is None and data:
            headers = {'Content-Type': 'application/x-protobuf'}
        if params is None:
            response = requests.request(
                method,
                'http://{host}:{port}{path}'.format(
                    host=self.host,
                    port=self.port,
                    path=path,
                ),
                headers=headers,
                data=data
            )
        else:
            response = requests.request(
                method,
                'http://{host}:{port}{path}'.format(
                    host=self.host,
                    port=self.port,
                    path=path,
                ),
                headers=headers,
                data=data,
                params=params
            )
        response.data = response.content
        return response

    def put(self, path, data, headers=None, params=None):
        return self.do_request('put', path, data, headers, params)

    def get(self, path, data=None, headers=None, params=None):
        return self.do_request('get', path, data, headers, params)

    def post(self, path, data=None, headers=None, params=None):
        return self.do_request('post', path, data, headers, params)


class ShinyProxyTestEnv(CommonProxyTestEnv):

    def __init__(self, app_name, bin_path=None, environment_variables=None, **resources):
        super(ShinyProxyTestEnv, self).__init__(app_name, bin_path, **resources)
        self.shiny_config = self.resources['{}_shiny_config'.format(app_name)]
        self._environment_variables = environment_variables
        self._environment_variables_path = None

        resources_stubs = {
            'yt_token': YtTokenResource()
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'shiny_proxy_env'

    def __enter__(self):
        start_time = time.time()

        self._shiny_http_port = self.port_manager.get_port()
        CommonProxyTestEnv.__enter__(self)

        # Ожидаем поднятия HTTP-сервера shiny
        started_path = os.path.join(self.output_dir, self.shiny_status_dir, 'started')
        while self.exec_result.running:
            if os.path.exists(started_path) and os.stat(started_path).st_atime >= start_time:
                return self
            time.sleep(0.1)

        raise Exception('Shiny server has not been started')

    def connector(self):
        return ServiceConnector(self.host, self._shiny_http_port)

    def after_environment_init(self):
        if self._environment_variables:
            self._environment_variables_path = os.path.join(self.input_dir, 'env.conf')
            with open(self._environment_variables_path, 'w') as f:
                for k, v in self._environment_variables.items():
                    f.write('{}={}\n'.format(k, str(v).lower() if type(v) is bool else v))

    @property
    def shiny_http_port(self):
        return self._shiny_http_port

    @property
    def shiny_status_dir(self):
        return 'status'

    @property
    def cmd_arg_config(self):
        return '--cp-C'

    @property
    def cmd_arg_variable(self):
        return '--cp-V'

    @property
    def cmd_arg_environment(self):
        return '--cp-E'

    @property
    def cmd_args(self):
        args = [
            self.bin_path,
            '-c', os.path.abspath(self.shiny_config.path),
            '--port', '{}'.format(self._shiny_http_port),
            self.cmd_arg_config, os.path.abspath(self.config.path)
        ]

        if self._environment_variables_path:
            args += [
                self.cmd_arg_environment, os.path.abspath(self._environment_variables_path),
                '--arg-prop', os.path.abspath(self._environment_variables_path)
            ]

        return args

    def do_request(self, method, path, data=None, headers=None):
        if headers is None and data:
            headers = {'Content-Type': 'application/x-protobuf'}
        response = requests.request(
            method,
            'http://{host}:{port}{path}'.format(
                host=self.host,
                port=self.shiny_http_port,
                path=path,
            ),
            headers=headers,
            data=data
        )
        response.data = response.content
        return response

    def put(self, path, data, headers=None):
        return self.do_request('put', path, data, headers)

    def get(self, path, data=None, headers=None):
        return self.do_request('get', path, data, headers)

    def post(self, path, data=None, headers=None):
        return self.do_request('post', path, data, headers)
