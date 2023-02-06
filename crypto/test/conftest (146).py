import os

import flask
import pytest
import yatest.common

from crypta.lib.python import time_utils
from crypta.lib.python.sftp.client import SftpClient
from crypta.lib.python.sftp.testing_server import SftpTestingServer
from crypta.lib.python.test_utils import flask_mock_server
from crypta.s2s.services.transfer_conversions_to_yt.lib import helpers
from crypta.s2s.services.transfer_conversions_to_yt.lib.default_csv_column_names import DefaultCsvColumnNames


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def mock_file_server():
    path = "/xxx/zzz/conversions.csv"

    class MockFileServer(flask_mock_server.FlaskMockServer):
        def __init__(self):
            super(MockFileServer, self).__init__("MockFileServer")

            @self.app.route(path)
            def get_csv():
                return flask.send_file(yatest.common.test_source_path("data/aliexpress_conversions.csv"))

        def get_file_url(self):
            return "{url_prefix}{path}".format(url_prefix=self.url_prefix, path=path)

    with MockFileServer() as mock:
        yield mock


@pytest.fixture(scope="module")
def custom_column_names():
    return {
        DefaultCsvColumnNames.yclid: "yandex_click_id",
        DefaultCsvColumnNames.conversion_name: "conversion_name",
        DefaultCsvColumnNames.conversion_time: "conversion_time",
        DefaultCsvColumnNames.conversion_value: "conversion_value",
        DefaultCsvColumnNames.conversion_currency: "conversion_currency",
    }


@pytest.fixture(scope="module")
def conversion_name_to_goal_ids():
    return {
        "aff-conversion-web": [1111],
        "aff-conversion-app": [2222, 3333],
        "missing-value-and-currency": [4444],
    }


@pytest.fixture(scope="function")
def frozen_time():
    result = "1640984400"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    return result


@pytest.fixture(scope="module")
def sftp_source_dir():
    return "/xxx/www/zzz"


@pytest.fixture(scope="module")
def sftp_username():
    return "__SFTP_USERNAME__"


@pytest.fixture(scope="module")
def sftp_password():
    result = "__SFTP_PASSWORD__"
    os.environ["CRYPTA_SFTP_PASSWORD"] = result
    return result


@pytest.fixture(scope="function")
def sftp_server(sftp_username, sftp_password):
    auths = [(sftp_username, sftp_password)]
    with SftpTestingServer(auths=auths) as s:
        yield s


@pytest.fixture(scope="function")
def sftp_client(sftp_server):
    return SftpClient(host=sftp_server.host, port=sftp_server.port, key_file=sftp_server.key_file)


@pytest.fixture(scope="function")
def sftp_files_basenames(sftp_client, sftp_source_dir):
    basenames = ["yandex_conversions_2021-11-10", "yandex_conversions_2021-11-11"]
    data_dir = yatest.common.test_source_path("data")

    with sftp_client:
        path = "/"
        for part in [x for x in sftp_source_dir.split("/") if x]:
            path = "{}/{}".format(path, part)
            sftp_client.mkdir(path)

        for basename in basenames:
            filename = "{}{}".format(basename, helpers.CSV_EXT)
            sftp_client.upload(os.path.join(data_dir, filename), os.path.join(sftp_source_dir, filename))

    return basenames
