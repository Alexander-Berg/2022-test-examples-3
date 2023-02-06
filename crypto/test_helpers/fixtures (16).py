import logging
import os
import random
import string

from grut.python.object_api.client import objects
import pytest
import yatest.common

from crypta.lib.python import time_utils
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig
from crypta.lib.python.logbroker.test_helpers.simple_logbroker_client import SimpleLogbrokerClient
from crypta.s2s.lib.conversion_source_client import ConversionSourceClient
from crypta.s2s.lib.test_helpers.local_conversions_downloader import LocalConversionsDownloader
from crypta.s2s.lib.test_helpers.local_conversions_processor import LocalConversionsProcessor


pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "grut.tests.ft.fixtures.conftest",
]


@pytest.fixture(scope="session")
def download_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "download-log")


@pytest.fixture(scope="session")
def process_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "process-log")


@pytest.fixture(scope="session")
def download_log_logbroker_client(download_log_logbroker_config):
    with SimpleLogbrokerClient(download_log_logbroker_config) as client:
        yield client


@pytest.fixture(scope="session")
def process_log_logbroker_client(process_log_logbroker_config):
    with SimpleLogbrokerClient(process_log_logbroker_config) as client:
        yield client


@pytest.fixture(scope="session")
def download_log_producer(download_log_logbroker_client):
    producer = download_log_logbroker_client.create_producer()
    yield producer
    producer.stop().result()


@pytest.fixture(scope="session")
def process_log_producer(process_log_logbroker_client):
    producer = process_log_logbroker_client.create_producer()
    yield producer
    producer.stop().result()


@pytest.fixture(scope="session")
def direct_encryption_secret():
    return "abc-xyz"


@pytest.fixture(scope="function")
def local_conversions_downloader(grut_address, yt_stuff, download_log_logbroker_config, process_log_logbroker_config, direct_encryption_secret, mock_solomon_server, frozen_time):
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = frozen_time
    os.environ["GRUT_TOKEN"] = "__GRUT_TOKEN__"
    os.environ["GOOGLE_SERVICE_ACCOUNT_KEY"] = "{}"
    os.environ["DIRECT_ENCRYPTION_SECRET"] = direct_encryption_secret

    with LocalConversionsDownloader(
        working_dir=yatest.common.test_output_path(),
        grut_address=grut_address,
        yt_proxy=yt_stuff.get_server(),
        download_log_logbroker_config=download_log_logbroker_config,
        process_log_logbroker_config=process_log_logbroker_config,
        solomon_url=mock_solomon_server.url_prefix,
    ) as service:
        yield service


@pytest.fixture(scope="function")
def local_conversions_processor(
    grut_address,
    mock_cdp_api,
    yt_stuff,
    process_log_logbroker_config,
    tvm_api,
    max_order_age_days,
    max_backup_size,
):
    tvm_id = tvm_api.issue_id()

    os.environ["GRUT_TOKEN"] = "__GRUT_TOKEN__"
    os.environ["TVM_SECRET"] = tvm_api.get_secret(tvm_id)

    with yatest.common.network.PortManager() as port_manager, LocalConversionsProcessor(
        working_dir=yatest.common.test_output_path(),
        grut_address=grut_address,
        cdp_api_address="http://{}:{}".format(mock_cdp_api.host, mock_cdp_api.port),
        yt_proxy=yt_stuff.get_server(),
        process_log_logbroker_config=process_log_logbroker_config,
        tvm_id=tvm_id,
        cdp_api_tvm_id=tvm_api.issue_id(),
        max_order_age_days=max_order_age_days,
        max_backup_size=max_backup_size,
        stats_port=port_manager.get_port(),
        env={},
    ) as service:
        yield service


@pytest.fixture(scope="function")
def client_id(object_api_client):
    meta = {"id": random_int_id()}
    spec = {"name": random_string(), "chief_uid": random_int_id()}
    objects.create_objects(object_api_client, "client", [{"meta": meta, "spec": spec}])
    return meta["id"]


@pytest.fixture(scope="function")
def grut_address():
    return os.environ["OBJECT_API_ADDRESS_0"]


def random_string():
    return ''.join(random.choice(string.ascii_letters) for _ in range(10))


def random_int_id():
    return random.randint(1000, 1000000000)


@pytest.fixture(scope="function")
def conversion_source_client(object_api_client):
    return ConversionSourceClient(object_api_client, logging.getLogger("test_logger"))
