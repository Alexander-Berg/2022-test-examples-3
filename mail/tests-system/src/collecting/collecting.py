import pytest
import time
from mocks.fixtures import storage_mock, nw_mock, internal_api_mock
from helpers.utils import Callback
from .fixtures import *

COLLECTING_TIMEOUT = 20


def clean_collectors(collectors_api):
    rpops = collectors_api.list()["rpops"]
    for c in rpops:
        collectors_api.delete(c["popid"])


@pytest.fixture(autouse=True)
def auto_collectors_cleanup(collectors_api):
    clean_collectors(collectors_api)
    yield
    clean_collectors(collectors_api)


def wait_for_collecting_complete(callback):
    for _ in range(COLLECTING_TIMEOUT):
        if callback.called():
            return
        time.sleep(1)


def test_collectors_collecting(
    nw_mock,
    internal_api_mock,
    collectors_api,
    social_task_id,
    collectors_internal_url,
    service_ticket,
    dst_user,
    expected_messages,
):
    empty_chunk_cb = Callback()
    internal_api_mock.set_empty_chunk_cb(empty_chunk_cb)

    collectors_api.create_yandex(social_task_id)
    wait_for_collecting_complete(empty_chunk_cb)

    result_messages = get_result_messages(
        nw_mock, dst_user, collectors_internal_url, service_ticket
    )
    assert expected_messages == result_messages
