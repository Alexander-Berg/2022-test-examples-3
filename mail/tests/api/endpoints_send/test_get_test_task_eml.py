import pytest
import urllib.request, urllib.parse, urllib.error
from email import message_from_string as parse_eml
from rest_framework import status
from fan.models import TestSendTask as TSendTask
from fan.testutils.letter import get_html_body
from fan.testutils.matchers import assert_status_code

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


@pytest.fixture
def test_send_task_with_user_template_variables(campaign_with_letter):
    task = TSendTask(
        campaign=campaign_with_letter,
        recipients=["recipient@test.ru"],
        user_template_variables={"name": "Any Name"},
    )
    task.save()
    return task


def test_missing_task_param_gives_400():
    response = do_request(None)
    assert_status_code(response, status.HTTP_400_BAD_REQUEST)


def test_on_success_gives_200_and_eml(test_send_tasks):
    response = do_request(test_send_tasks[0].id, "recipient@test.ru")
    assert_status_code(response, status.HTTP_200_OK)
    assert "To: recipient@test.ru" in response.content.decode("ascii")


def test_substitutes_user_template_variables(test_send_task_with_user_template_variables):
    response = do_request(test_send_task_with_user_template_variables.id, "recipient@test.ru")
    assert_status_code(response, status.HTTP_200_OK)
    assert "Subject: {{ title }}" in response.content.decode("ascii")  # doesn't render subject
    assert "Dear Any Name!" in get_html_body(parse_eml(response.content.decode("ascii")))


def do_request(task_id, recipient=""):
    args = {
        "task_id": task_id,
        "recipient": recipient,
    }
    url = "/api/send/test-task-eml?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.get(url)
