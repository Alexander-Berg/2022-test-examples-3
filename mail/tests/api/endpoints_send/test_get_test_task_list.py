import pytest
import urllib.request, urllib.parse, urllib.error
from rest_framework import status
from fan.testutils.matchers import assert_status_code, assert_validation_error, is_subdict


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


def test_missing_count():
    response = do_request(None)
    assert_status_code(response, status.HTTP_400_BAD_REQUEST)


def test_count_is_not_digit():
    response = do_request("abc")
    assert_validation_error(response, "count", "invalid_type")


def test_count_is_zero():
    response = do_request(0)
    assert_validation_error(response, "count", "invalid_value")


def test_count_is_negative():
    response = do_request(-1)
    assert_validation_error(response, "count", "invalid_value")


def test_no_tasks():
    response = do_request(1)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 0


def test_single_task(test_send_tasks):
    response = do_request(1)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 1
    assert is_subdict(task_fields(test_send_tasks[0]), response_data[0])


def test_multiple_tasks(test_send_tasks):
    response = do_request(2)
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == 2
    assert is_subdict(task_fields(test_send_tasks[0]), response_data[0])
    assert is_subdict(task_fields(test_send_tasks[1]), response_data[1])


def test_count_greater_than_number_of_tasks(test_send_tasks):
    response = do_request(2 * len(test_send_tasks))
    response_data = response.json()
    assert_status_code(response, status.HTTP_200_OK)
    assert len(response_data) == len(test_send_tasks)


def do_request(count):
    args = {
        "count": count,
    }
    url = "/api/send/test-task-list?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.get(url)


def task_fields(task):
    return {
        "id": task.id,
        "account_slug": task.campaign.account.name,
        "campaign_slug": task.campaign.slug,
    }
