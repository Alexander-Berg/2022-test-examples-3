import os
import tarfile
import sys
import time
from pytest import (
    yield_fixture,
)
import yatest
import yatest.common.runtime as runtime
import yatest.common.network as network

from crypta.lib.python.swagger import swagger


@yield_fixture(scope="function")
def api():
    with tarfile.open(yatest.common.build_path("crypta/api/api.tar")) as libs_tar:
        libs_tar.extractall(path=yatest.common.work_path())

    classpath_jar = runtime.binary_path(os.path.join('crypta/api', 'api.jar'))
    java = yatest.common.runtime.global_resources()['JDK_DEFAULT_RESOURCE_GLOBAL'] + '/bin/java'
    cmd = [java, '-cp', classpath_jar, '-cp', os.path.join(yatest.common.work_path(), '*'), 'ru.yandex.crypta.api.Main']
    with network.PortManager() as pm:
        port = str(pm.get_port())
        env = dict(
            CRYPTA_ENVIRONMENT='testing',
            PORT=port,
            DISABLE_IDM_AUTH='yes',
            DISABLE_IDM_ROLES='yes',
            DISABLE_AUTH='yes',
            DISABLE_SCHEDULING='yes',
        )
        try:
            p = yatest.common.execute(cmd, env=env, wait=False, check_exit_code=True, stderr=sys.stderr, stdout=sys.stdout)
            url = 'http://localhost:{}/swagger.json'.format(port)
            yield url
        finally:
            p.kill()


def test_swagger(api):
    time.sleep(45)
    swagger(api, 'no-token')
