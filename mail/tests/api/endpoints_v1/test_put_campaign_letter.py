import urllib.request, urllib.parse, urllib.error
import zipfile

import pytest
from flaky import flaky
from rest_framework import status

from fan.links.unsubscribe import UNSUBSCRIBE_LINK
from fan.utils.hash import filehash
from fan.models import Campaign
from fan.testutils.letter import load_test_letter, attachments_filelist
from fan.testutils.matchers import (
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
    assert_wrong_state_error,
    assert_render_error,
    assert_forbidden_error,
    assert_doesnt_contain_opens_counter,
)
from fan.message.letter import (
    CONTENT_TYPE_HTML,
    CONTENT_TYPE_ZIP,
)

pytestmark = pytest.mark.django_db


FN_LETTER_FILENAME = "FN_LETTER_FILENAME"
FN_NONE = "FN_NONE"
FN_EMPTY = "FN_EMPTY"


class TestHtmlLetterUpload:
    def do_test_put_valid_html(self, client, user_id, campaign, upload_letter):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(client, user_id, campaign, letter_file, CONTENT_TYPE_HTML)
            assert_status_code(response, status.HTTP_200_OK)
            assert_letter_content_hash_matches(campaign.default_letter, letter_file)
            assert campaign.letter_uploaded == True
            assert campaign.letter_description == "letter.html"

    def test_put_valid_html(self, tvm_api_client, user_id, campaign, upload_letter):
        self.do_test_put_valid_html(tvm_api_client, user_id, campaign, upload_letter)

    def test_response_contains_user_template_variables(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_status_code(response, status.HTTP_200_OK)
            response_data = response.json()
            assert "user_template_variables" in response_data
            assert response_data["user_template_variables"] == ["name"]

    def test_stored_letter_doesnt_contain_opens_counter(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_doesnt_contain_opens_counter(campaign.default_letter.html_body)

    def test_put_valid_html_session_auth(self, auth_api_client, user_id, campaign, upload_letter):
        self.do_test_put_valid_html(auth_api_client, user_id, campaign, upload_letter)

    def test_put_valid_html_wo_auth_localhost(
        self, api_client, user_id, campaign, upload_letter, disable_auth_on_loopback
    ):
        self.do_test_put_valid_html(api_client, user_id, campaign, upload_letter)

    def test_put_valid_html_wo_auth(self, api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML)
            assert_not_authenticated_error(
                response, "Authentication credentials were not provided."
            )

    def test_put_valid_html_with_damaged_tvm_credentials(
        self, tvm_api_client_with_damaged_ticket, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client_with_damaged_ticket,
                user_id,
                campaign,
                letter_file,
                CONTENT_TYPE_HTML,
            )
            assert_not_authenticated_error(
                response, "Authentication credentials were not provided."
            )

    def test_put_valid_html_with_non_registered_tvm_source(
        self,
        tvm_api_client,
        user_id,
        campaign,
        upload_letter,
        tvm_clear_api_v1_allowed_services_list,
    ):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_forbidden_error(response, "forbidden_service")

    def test_put_empty_html(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("empty.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_validation_error(response, "data", "empty")
            assert campaign.letter_uploaded == False
            assert campaign.letter_description == ""

    def test_put_invalid_content_type(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter.html") as letter_file:
            content_type = "INVALID_CONTENT_TYPE"
            response = upload_letter(tvm_api_client, user_id, campaign, letter_file, content_type)
            assert_validation_error(response, "content_type", "not_supported")

    def test_put_no_filename_param(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML, FN_NONE
            )
            assert_validation_error(response, "filename", "not_found")

    def test_put_empty_filename_param(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML, FN_EMPTY
            )
            assert_validation_error(response, "filename", "empty")

    def test_put_too_long_html(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("too_long.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_validation_error(response, "data", "too_long")

    def test_user_template_variables_count_exceeded(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter_with_6_variables.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_validation_error(response, "data", "user_template_variables_count_exceeded")

    def test_user_template_variable_length_exceeded(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter_with_long_variable.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_validation_error(response, "data", "user_template_variable_length_exceeded")

    def test_put_letter_wo_unsubscribe_link(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter_wo_unsubscribe_link.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_validation_error(response, UNSUBSCRIBE_LINK, "not_found")

    def test_put_letter_with_invalid_unsubscribe_link(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter_with_invalid_unsubscribe_link.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_validation_error(response, UNSUBSCRIBE_LINK, "not_found")

    def test_put_letter_with_unknown_tag(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter_with_unknown_tag.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_render_error(
                response,
                "template_runtime_error",
                "Template syntax error:\"Encountered unknown tag 'some_unknown_tag'.\" at line: 7",
            )

    def test_put_letter_with_link_to_unknown_attachment(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter_with_link_to_unknown_attachment.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_render_error(
                response,
                "template_runtime_error",
                "Template runtime error:\"'some_unknown_attachment.jpg'\"",
            )

    @pytest.mark.parametrize(
        "wrong_state",
        [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
    )
    def test_putting_fails_on_wrong_campaign_state(
        self, tvm_api_client, user_id, campaign, upload_letter, wrong_state
    ):
        force_set_campaign_state(campaign, wrong_state)
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_wrong_state_error(response, wrong_state, "draft")

    def test_forbidden_user(self, tvm_api_client, campaign, upload_letter):
        with load_test_letter("letter.html") as letter_file:
            response = upload_letter(
                tvm_api_client, "forbidden_user_id", campaign, letter_file, CONTENT_TYPE_HTML
            )
            assert_forbidden_error(response, "forbidden_user")


class TestZipLetterUpload:
    @flaky(max_runs=5)
    def test_put_valid_zip(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_status_code(response, status.HTTP_200_OK)
            assert_letter_content_hash_matches(campaign.default_letter, letter_file)
            assert campaign.letter_uploaded == True
            assert campaign.letter_description == "letter.zip"

    @flaky(max_runs=5)
    def test_response_contains_user_template_variables(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_status_code(response, status.HTTP_200_OK)
            response_data = response.json()
            assert "user_template_variables" in response_data
            assert response_data["user_template_variables"] == ["name"]

    @flaky(max_runs=5)
    def test_put_valid_zip_attachments(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            attach_filelist = sorted(attachments_filelist(campaign.default_letter))
            zip_filelist = sorted(get_zip_filelist(letter_file))
            assert attach_filelist == zip_filelist

    def test_stored_letter_doesnt_contain_opens_counter(
        self, tvm_api_client, user_id, campaign, upload_letter
    ):
        with load_test_letter("letter.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_doesnt_contain_opens_counter(campaign.default_letter.html_body)

    def test_put_invalid_zip(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("invalid.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_validation_error(response, "data", "invalid_email")
            assert campaign.letter_uploaded == False
            assert campaign.letter_description == ""

    def test_put_damaged_zip(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("damaged.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_validation_error(response, "data", "invalid_zip")

    def test_put_too_long_zip(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("too_long.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_validation_error(response, "data", "too_long")

    def test_put_too_many_attachments(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter_with_too_many_attachments.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_validation_error(response, "data", "too_long")

    def test_put_too_big_attachments(self, tvm_api_client, user_id, campaign, upload_letter):
        with load_test_letter("letter_with_big_attachments.zip") as letter_file:
            response = upload_letter(
                tvm_api_client, user_id, campaign, letter_file, CONTENT_TYPE_ZIP
            )
            assert_validation_error(response, "data", "too_long")


@pytest.fixture
def upload_letter(account):
    def create_url(user_id, campaign, letter_file, filename_to_use):
        query_params = {
            "user_id": user_id,
            "account_slug": account.name,
            "campaign_slug": campaign.slug,
        }
        filename = resolve_filename_param(letter_file, filename_to_use)
        if filename is not None:
            query_params["filename"] = filename
        return "/api/v1/campaign-letter?" + urllib.parse.urlencode(query_params)

    def impl(
        client, user_id, campaign, letter_file, content_type, filename_to_use=FN_LETTER_FILENAME
    ):
        url = create_url(user_id, campaign, letter_file, filename_to_use)
        letter_file.seek(0)
        return client.put(
            path=url,
            data=letter_file.read(),
            content_type=content_type,
        )

    return impl


def resolve_filename_param(letter_file, filename_to_use):
    return {FN_LETTER_FILENAME: letter_file.name, FN_NONE: None, FN_EMPTY: ""}[filename_to_use]


def assert_letter_content_hash_matches(letter, content_sent):
    letter_content_hash = filehash(content_sent)
    assert letter.original_letter_content_hash == letter_content_hash


def get_zip_filelist(letter_file):
    def ignore_zip_filename(filename):
        return (
            len(filename) == 0
            or filename.startswith("._")
            or filename.endswith(".html")
            or filename == ".DS_Store"
        )

    letter_file.seek(0)
    with zipfile.ZipFile(letter_file) as zip_archive:
        filenames = [fn.split("/")[-1] for fn in zip_archive.namelist()]
        return [fn for fn in filenames if not ignore_zip_filename(fn)]


def force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
