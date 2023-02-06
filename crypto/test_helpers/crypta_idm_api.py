import os

import requests
import retry
import yatest

from crypta.lib.python import test_utils


class CryptaIdmApi(test_utils.TestBinaryContextManager):
    def __init__(self, working_dir, port, postgres):
        super(CryptaIdmApi, self).__init__("Crypta IDM API", check_exit_code=False, env={
            "CRYPTA_ENVIRONMENT": "testing",
            "PORT": str(port),
            "DISABLE_IDM": "yes",
            "DISABLE_AUTH": "yes",
            "DISABLE_TVM": "yes",
            "POSTGRES_CONNECTION_STRING": f"jdbc:postgresql://{postgres.host}:{postgres.port}/{postgres.dbname}",
            "POSTGRES_USERNAME": postgres.user,
        })

        self.working_dir = working_dir
        self.postgres = postgres
        self.port = port

    def _prepare_start(self):
        api_dependencies = yatest.common.binary_path('crypta/idm/idm_api')
        classpath_jar = yatest.common.binary_path('crypta/idm/idm_api.jar')
        java = yatest.common.runtime.global_resources()['JDK_DEFAULT_RESOURCE_GLOBAL'] + '/bin/java'

        return [java, f"-Djava.library.path={api_dependencies}", '-cp', classpath_jar, '-cp', os.path.join(api_dependencies, '*'), 'ru.yandex.crypta.idm.bin.Main']

    def _wait_until_up(self):
        @retry.retry(tries=300, delay=0.1)
        def check_is_up():
            assert requests.get(f"http://localhost:{self.port}/database/check/can_connect").ok, "Failed to start service"

        check_is_up()
