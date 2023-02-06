import io
import urllib.request, urllib.parse, urllib.error

import pytest
from flaky import flaky

from django.test.client import encode_multipart, BOUNDARY, MULTIPART_CONTENT

from rest_framework import status

from fan.links.unsubscribe import UNSUBSCRIBE_LINK
from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_forbidden_error,
    assert_render_error,
    assert_html_contains_attachments_publish_paths,
)
from fan.message.letter import load_letter
from fan.testutils.letter import load_test_letter, attachments_filelist, attachments_file_sizes
from fan.utils.zip import create_zip_file

pytestmark = pytest.mark.django_db


@pytest.fixture
def multipart_letter_files():
    return ["multipart/twi.png", "multipart/vk.png", "multipart/index.html"]


@pytest.fixture
def multipart_letter_with_big_attaches():
    return ["multipart/big1.png", "multipart/big2.png", "multipart/letter_with_big_attaches.html"]


@pytest.fixture
def multipart_letter_valid_html():
    return ["letter.html"]


@pytest.fixture
def multipart_letter_too_long_html():
    return ["too_long.html"]


@pytest.fixture
def multipart_letter_with_invalid_unsubscribe_link():
    return ["letter_with_invalid_unsubscribe_link.html"]


@pytest.fixture
def multipart_letter_without_unsubscribe_link():
    return ["letter_wo_unsubscribe_link.html"]


@pytest.fixture
def multipart_letter_without_attachment():
    return ["multipart/index.html", "multipart/vk.png"]


@pytest.fixture
def multipart_letter_without_html():
    return ["multipart/twi.png", "multipart/vk.png"]


@pytest.fixture
def multipart_letter_with_unused_attachments():
    return ["multipart/twi.png", "multipart/vk.png", "multipart/index.html", "multipart/unused.png"]


@pytest.fixture
def multipart_letter_base():
    content = create_content(["multipart/twi.png", "multipart/base.html"])
    files = dict([(file.name, file) for file in content["upload_letter"]])
    zip_file = create_zip_file(files, "letter.zip")
    zip_file.ATTACHMENTS_COUNT = 1
    yield zip_file
    zip_file.close()


@pytest.fixture
def multipart_letter_add_image_zip():
    content = create_content(["multipart/vk.png", "multipart/add_image.html"])
    files = dict([(file.name, file) for file in content["upload_letter"]])
    zip_file = create_zip_file(files, "letter.zip")
    yield zip_file
    zip_file.close()


@pytest.fixture
def preloaded_base_letter(campaign, mock_avatars_publish, multipart_letter_base):
    mock_avatars_publish.publish_path = "/test/path1"
    load_letter(campaign.default_letter, multipart_letter_base)
    return multipart_letter_base


@flaky(max_runs=5)
def test_put_valid_multipart(tvm_api_client, user_id, account, campaign, multipart_letter_files):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_files
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.letter_uploaded
    assert campaign.letter_description == "letter.zip"
    assert "<body>" in campaign.default_letter.html_body
    assert sorted(attachments_filelist(campaign.default_letter)) == ["twi.png", "vk.png"]
    assert sorted(attachments_file_sizes(campaign.default_letter)) == [622, 727]
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


@flaky(max_runs=5)
def test_response_contains_user_template_variables(
    tvm_api_client, user_id, account, campaign, multipart_letter_files
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_files
    )
    assert_status_code(response, status.HTTP_200_OK)
    response_data = response.json()
    assert "user_template_variables" in response_data
    assert response_data["user_template_variables"] == ["name"]


@flaky(max_runs=5)
def test_put_valid_multipart_session_auth(
    auth_api_client, user_id, account, campaign, multipart_letter_files
):
    response = do_multipart_request(
        auth_api_client, user_id, account, campaign, multipart_letter_files
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.letter_uploaded
    assert campaign.letter_description == "letter.zip"
    assert "<body>" in campaign.default_letter.html_body
    assert sorted(attachments_filelist(campaign.default_letter)) == ["twi.png", "vk.png"]
    assert sorted(attachments_file_sizes(campaign.default_letter)) == [622, 727]
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


@flaky(max_runs=5)
def test_put_valid_multipart_wo_auth_localhost(
    api_client, user_id, account, campaign, multipart_letter_files, disable_auth_on_loopback
):
    response = do_multipart_request(api_client, user_id, account, campaign, multipart_letter_files)
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.letter_uploaded
    assert campaign.letter_description == "letter.zip"
    assert "<body>" in campaign.default_letter.html_body
    assert sorted(attachments_filelist(campaign.default_letter)) == ["twi.png", "vk.png"]
    assert sorted(attachments_file_sizes(campaign.default_letter)) == [622, 727]
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


@flaky(max_runs=5)
def test_put_valid_html(tvm_api_client, user_id, account, campaign, multipart_letter_valid_html):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_valid_html
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.letter_uploaded
    assert campaign.letter_description == "letter.zip"
    assert "<body>" in campaign.default_letter.html_body


def test_put_valid_multipart_wo_auth(
    api_client, user_id, account, campaign, multipart_letter_files
):
    response = do_multipart_request(api_client, user_id, account, campaign, multipart_letter_files)
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_put_valid_multipart_with_damaged_tvm_credentials(
    tvm_api_client_with_damaged_ticket, user_id, account, campaign, multipart_letter_files
):
    response = do_multipart_request(
        tvm_api_client_with_damaged_ticket, user_id, account, campaign, multipart_letter_files
    )
    assert_not_authenticated_error(response, "Authentication credentials were not provided.")


def test_put_valid_multipart_with_non_registered_tvm_source(
    tvm_api_client,
    user_id,
    account,
    campaign,
    multipart_letter_files,
    tvm_clear_api_v1_allowed_services_list,
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_files
    )
    assert_forbidden_error(response, "forbidden_service")


def test_put_multipart_with_empty_data(tvm_api_client, user_id, account, campaign):
    response = do_multipart_request(tvm_api_client, user_id, account, campaign)
    assert_validation_error(response, "data", "empty")


def test_put_multipart_with_too_long_html(
    tvm_api_client, user_id, account, campaign, multipart_letter_too_long_html
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_too_long_html
    )
    assert_validation_error(response, "data", "too_long")


def test_put_letter_wo_unsubscribe_link(
    tvm_api_client, user_id, account, campaign, multipart_letter_without_unsubscribe_link
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_without_unsubscribe_link
    )
    assert_validation_error(response, UNSUBSCRIBE_LINK, "not_found")


def test_put_letter_w_invalid_link(
    tvm_api_client, user_id, account, campaign, multipart_letter_with_invalid_unsubscribe_link
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_with_invalid_unsubscribe_link
    )
    assert_validation_error(response, UNSUBSCRIBE_LINK, "not_found")


def test_forbidden_user(tvm_api_client, account, campaign, multipart_letter_files):
    response = do_multipart_request(
        tvm_api_client, "forbidden_user_id", account, campaign, multipart_letter_files
    )
    assert_forbidden_error(response, "forbidden_user")


def test_load_letter_with_link_to_unknown_attachment(
    tvm_api_client, user_id, account, campaign, multipart_letter_without_attachment
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_without_attachment
    )
    assert_render_error(
        response, "template_runtime_error", "Template runtime error:\"'./twi.png'\""
    )


def test_load_letter_without_html_body(
    tvm_api_client, user_id, account, campaign, multipart_letter_without_html
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_without_html
    )
    assert_validation_error(response, "data", "invalid_email")


def test_load_letter_with_big_attachments(
    tvm_api_client, user_id, account, campaign, multipart_letter_with_big_attaches
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_with_big_attaches
    )
    assert_validation_error(response, "data", "too_long")


@flaky(max_runs=5)
def test_load_letter_with_unused_attachments(
    tvm_api_client, user_id, account, campaign, multipart_letter_with_unused_attachments
):
    response = do_multipart_request(
        tvm_api_client, user_id, account, campaign, multipart_letter_with_unused_attachments
    )
    assert_status_code(response, status.HTTP_200_OK)
    assert campaign.letter_uploaded
    assert campaign.letter_description == "letter.zip"
    assert "<body>" in campaign.default_letter.html_body
    assert sorted(attachments_filelist(campaign.default_letter)) == ["twi.png", "vk.png"]
    assert sorted(attachments_file_sizes(campaign.default_letter)) == [622, 727]
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


@flaky(max_runs=5)
def test_update_base_letter_add_image(
    campaign, mock_avatars_publish, preloaded_base_letter, multipart_letter_add_image_zip
):
    mock_avatars_publish.publish_path = "/test/path2"
    load_letter(campaign.default_letter, multipart_letter_add_image_zip)
    assert (
        len(campaign.default_letter.attachments.all())
        == preloaded_base_letter.ATTACHMENTS_COUNT + 1
    )
    assert "/test/path1" in campaign.default_letter.html_body
    assert "/test/path2" in campaign.default_letter.html_body
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


def create_content(files):
    content = {"upload_letter": []}
    for file_name in files:
        with load_test_letter(file_name) as letter_file:
            letter_file.seek(0)
            file = io.BytesIO(letter_file.read())
            file.name = letter_file.name
            content["upload_letter"].append(file)
    return content


def do_multipart_request(client, user_id=None, account=None, campaign=None, letter_files=[]):
    query_params = {
        "user_id": user_id,
        "account_slug": account.name,
        "campaign_slug": campaign.slug,
    }
    url = "/api/v1/campaign-letter?" + urllib.parse.urlencode(
        {k: v for k, v in list(query_params.items()) if v is not None}
    )
    content = create_content(letter_files)
    return client.put(
        path=url,
        data=encode_multipart(BOUNDARY, content),
        content_type=MULTIPART_CONTENT,
    )
