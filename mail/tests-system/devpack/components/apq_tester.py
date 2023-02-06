import os
import requests
import retrying

from library.python import resource
from mail.devpack.lib import helpers
from mail.devpack.lib.yhttp_service import YplatformHttpService
from mail.devpack.lib.components.base import YplatformComponent

from mail.devpack.lib.components.sharpei import Sharpei


class ApqTester(YplatformComponent):
    NAME = 'apq_tester'
    DEPS = [Sharpei]

    @staticmethod
    def gen_config(port_generator, config=None):
        return YplatformHttpService.gen_config(port_generator, config=config)

    def __init__(self, env, components):
        super(ApqTester, self).__init__(env, components, binary_name='apq_tester', custom_path='apq/tests-system')
        self.__service = YplatformHttpService(
            env=env,
            name=self.NAME,
            binary_name='apq_tester',
            custom_path='apq/tests-system'
        )

    def _init_root(self):
        devpack = self.__service.format_config(resource.find('apq_tester/config-devpack.yml'))
        helpers.write2file(devpack, os.path.join(self.etc_path, 'apq_tester-devpack.yml'))

    def init_root(self):
        self.__service.init_root()
        self._init_root()

    def start(self):
        self.__service.start('{"result":"pong"}')

    def api(self, uid=None):
        return ApqTesterApi(
            location='http://localhost:%d' % self.webserver_port(),
            uid=uid,
        )


class ApqTesterApi(object):
    def __init__(self, location='http://localhost:1580', uid=None):
        self.location = location
        self.uid = uid

    @retrying.retry(stop_max_delay=10000)
    def ping(self, request_id):
        return requests.get(
            self.location + '/ping',
            headers=self.make_headers(request_id),
            timeout=1,
        )

    @retrying.retry(stop_max_delay=10000)
    def pingdb(self, conninfo, request_id):
        return requests.get(
            self.location + '/pingdb?conninfo={conninfo}'.format(conninfo=conninfo),
            headers=self.make_headers(request_id),
            timeout=1,
        )

    def make_headers(self, request_id):
        return {
            'X-Request-Id': request_id,
        }
