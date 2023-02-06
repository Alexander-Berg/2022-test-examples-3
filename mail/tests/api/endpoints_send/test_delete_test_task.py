import pytest
import urllib.request, urllib.parse, urllib.error
from rest_framework import status
from fan.models import TestSendTask as Task
from fan.testutils.matchers import assert_status_code


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


def test_missing_task_id():
    response = do_request(None)
    assert_status_code(response, status.HTTP_400_BAD_REQUEST)


def test_unexisted_task():
    response = do_request(-1)
    assert_status_code(response, status.HTTP_404_NOT_FOUND)


def test_success(test_send_tasks):
    response = do_request(test_send_tasks[0].id)
    assert_status_code(response, status.HTTP_200_OK)
    assert len(Task.objects.all()) == len(test_send_tasks) - 1


def do_request(task_id):
    args = {
        "task_id": task_id,
    }
    url = "/api/send/test-task?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.delete(url)
