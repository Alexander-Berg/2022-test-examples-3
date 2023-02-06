import os

import requests
import retry
import yatest

from crypta.lib.python import test_utils


class CryptaApi(test_utils.TestBinaryContextManager):
    def __init__(self, working_dir, yt_proxy, yql_connection_string, port, postgres, tvm_api, audience_api):
        tvm_test_port = tvm_api['port']

        tvm_id = tvm_api['issue_id']
        tvm_secret = tvm_api['secret']

        audience_source_tvm_id = tvm_api['issue_id']
        audience_tvm_secret = tvm_api['secret']

        cryptaidor_audience_tvm_id = tvm_api['issue_id']
        cryptaidor_tvm_secret = tvm_api['secret']

        audience_destination_tvm_id = tvm_api['issue_id']

        super(CryptaApi, self).__init__("Crypta API", check_exit_code=False, env={
            "CRYPTA_ENVIRONMENT": "testing",
            "PORT": str(port),
            "DISABLE_IDM": "yes",
            "DISABLE_AUTH": "yes",
            "DISABLE_SCHEDULING": "yes",
            "YT_PROXY": yt_proxy,
            "SENECA_HOSTS": yt_proxy,
            "YT_TOKEN": "FAKE",
            "POSTGRES_CONNECTION_STRING": f"jdbc:postgresql://{postgres.host}:{postgres.port}/{postgres.dbname}",
            "POSTGRES_USERNAME": postgres.user,
            "POSTGRES_PASSWORD": "",
            "GEODATA": yatest.common.binary_path("geobase/data/v6/geodata6.bin"),
            "YQL_CONNECTION_STRING": yql_connection_string,
            "YQL_TOKEN": "FAKE",
            "TVM_TEST_PORT": str(tvm_test_port),
            "TVM_ID": str(tvm_id),
            "TVM_SECRET": str(tvm_secret),
            "AUDIENCE_SOURCE_TVM_ID": str(audience_source_tvm_id),
            "AUDIENCE_TVM_SECRET": str(audience_tvm_secret),
            "CRYPTAIDOR_AUDIENCE_TVM_ID": str(cryptaidor_audience_tvm_id),
            "CRYPTAIDOR_TVM_SECRET": str(cryptaidor_tvm_secret),
            "AUDIENCE_DESTINATION_TVM_ID": str(audience_destination_tvm_id),
            "AUDIENCE_API_URL": str(audience_api.host),
            "AUDIENCE_API_PORT": str(audience_api.port),
            "AUDIENCE_API_SCHEME": "http"
        })

        self.working_dir = working_dir
        self.yt_proxy = yt_proxy
        self.postgres = postgres
        self.port = port

    def _prepare_start(self):
        api_dependencies = yatest.common.binary_path('crypta/api/api')
        classpath_jar = yatest.common.binary_path('crypta/api/api.jar')
        java = yatest.common.runtime.global_resources()['JDK_DEFAULT_RESOURCE_GLOBAL'] + '/bin/java'

        return [java, f"-Djava.library.path={api_dependencies}", '-cp', classpath_jar, '-cp', os.path.join(api_dependencies, '*'), 'ru.yandex.crypta.api.Main']

    def _wait_until_up(self):
        @retry.retry(tries=300, delay=0.1)
        def check_is_up():
            assert requests.get(f"http://localhost:{self.port}/database/check/can_connect").ok, "Failed to start service"

        check_is_up()
