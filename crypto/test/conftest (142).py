import os

from grut.python.object_api.client import objects
import flask
import pytest
import yaml
import yatest.common

from crypta.lib.python.ftp.client.ftp_client import FtpClient
from crypta.lib.python.ftp.testing_server import FtpTestingServer
from crypta.lib.python.sftp.client import SftpClient
from crypta.lib.python.sftp.testing_server import SftpTestingServer
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.test_utils import flask_mock_server
from crypta.s2s.services.conversions_downloader.lib.encrypter import Encrypter


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
    "crypta.s2s.lib.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def frozen_time():
    return "1600000000"


@pytest.fixture(scope="function", autouse=True)
def setup(download_log_logbroker_client, process_log_logbroker_client, object_api_client, client_id):
    consumer_utils.read_all(download_log_logbroker_client.create_consumer())
    consumer_utils.read_all(process_log_logbroker_client.create_consumer())

    with open(yatest.common.test_source_path("data/grut_conversion_source.yaml")) as f:
        conversion_sources = yaml.load(f)

    for conversion_source in conversion_sources:
        conversion_source["meta"]["client_id"] = client_id

    objects.create_objects(object_api_client, "conversion_source", conversion_sources)


@pytest.fixture(scope="function")
def mock_file_server():
    existing_path = "/xxx/link_data.csv"
    missing_path = "/xxx/missing_link_data.csv"

    class MockFileServer(flask_mock_server.FlaskMockServer):
        def __init__(self):
            super(MockFileServer, self).__init__("MockFileServer")

            @self.app.route(existing_path)
            def get_csv():
                return flask.send_file(yatest.common.test_source_path("data/link_data.csv"))

        def get_existing_file_url(self):
            return self.get_url(existing_path)

        def get_missing_file_url(self):
            return self.get_url(missing_path)

        def get_url(self, path):
            return "{url_prefix}{path}".format(url_prefix=self.url_prefix, path=path)

    with MockFileServer() as mock:
        yield mock


@pytest.fixture(scope="session")
def encrypter(direct_encryption_secret):
    return Encrypter(direct_encryption_secret.encode())


@pytest.fixture(scope="function")
def valid_link_credentials(mock_file_server):
    return {
        "url": mock_file_server.get_existing_file_url(),
    }


@pytest.fixture(scope="function")
def invalid_link_credentials(mock_file_server):
    return {
        "url": mock_file_server.get_missing_file_url(),
    }


@pytest.fixture(scope="module")
def sftp_username():
    return "__SFTP_USERNAME__"


@pytest.fixture(scope="module")
def sftp_password():
    return "__SFTP_PASSWORD__"


@pytest.fixture(scope="function")
def sftp_server(sftp_username, sftp_password):
    auths = [(sftp_username, sftp_password)]
    with SftpTestingServer(auths=auths) as s:
        yield s


@pytest.fixture(scope="function")
def sftp_client(sftp_server):
    return SftpClient(host=sftp_server.host, port=sftp_server.port, key_file=sftp_server.key_file)


@pytest.fixture(scope="function")
def valid_sftp_credentials(sftp_server, sftp_client, sftp_username, sftp_password, encrypter):
    dirname = "/xxx"
    filename = "sftp_data.csv"
    path = os.path.join(dirname, filename)

    with sftp_client:
        sftp_client.mkdir(dirname)
        sftp_client.upload(yatest.common.test_source_path("data/sftp_data.csv"), path)

    return {
        "host": sftp_server.host,
        "port": sftp_server.port,
        "login": sftp_username,
        "encrypted_password": encrypter.encrypt(sftp_password),
        "path": path,
    }


@pytest.fixture(scope="function")
def invalid_sftp_credentials(sftp_server, sftp_username, sftp_password, encrypter):
    return {
        "host": sftp_server.host,
        "port": sftp_server.port,
        "login": sftp_username,
        "encrypted_password": encrypter.encrypt("{0}|{0}".format(sftp_password)),
        "path": "/xyz.csv",
    }


@pytest.fixture(scope="module")
def ftp_username():
    return "__FTP_USERNAME__"


@pytest.fixture(scope="module")
def ftp_password():
    return "__FTP_PASSWORD__"


@pytest.fixture(scope="function")
def ftp_server(ftp_username, ftp_password):
    auths = [(ftp_username, ftp_password)]
    with FtpTestingServer(auths=auths) as s:
        yield s


@pytest.fixture(scope="function")
def ftp_client(ftp_server, ftp_username, ftp_password):
    return FtpClient("localhost", ftp_server.port, ftp_username, ftp_password)


@pytest.fixture(scope="function")
def valid_ftp_credentials(ftp_server, ftp_client, ftp_username, ftp_password, encrypter):
    dirname = "/xxx"
    filename = "ftp_data.csv"
    path = os.path.join(dirname, filename)

    ftp_client.mkd(dirname)
    ftp_client.upload(yatest.common.test_source_path("data/ftp_data.csv"), path)

    return {
        "host": "localhost",
        "port": ftp_server.port,
        "login": ftp_username,
        "encrypted_password": encrypter.encrypt(ftp_password),
        "path": path,
    }


@pytest.fixture(scope="function")
def invalid_ftp_credentials(ftp_server, ftp_username, ftp_password, encrypter):
    return {
        "host": "localhost",
        "port": ftp_server.port,
        "login": ftp_username,
        "encrypted_password": encrypter.encrypt("{0}|{0}".format(ftp_password)),
        "path": "/xyz.csv",
    }
