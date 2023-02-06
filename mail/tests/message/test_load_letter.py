import pytest
from flaky import flaky
from zipfile import BadZipfile
import requests
from rest_framework import status
from fan.message.exceptions import TemplateRuntimeError, UnsubscribeLinkNotFoundError
from fan.message.letter import (
    load_letter,
    AttachmentsCountExceeded,
    AttachmentsSizeExceeded,
    UserTemplateVariablesCountExceeded,
    UserTemplateVariableLengthExceeded,
)
from fan.message.loader import HTMLWrongFormat
from fan.testutils.letter import load_test_letter, attachments_filelist, attachments_file_sizes
from fan.testutils.matchers import (
    assert_html_contains_attachments_publish_paths,
    assert_doesnt_contain_opens_counter,
)


pytestmark = pytest.mark.django_db


@pytest.fixture
def body_file(mocker):
    b_file = mocker.MagicMock()
    b_file.read.return_value = '<html><head></head><body><a href="http://example.ru"></a><a href="{{ unsubscribe_link }}">Unusbscribe</a></body></html>'.encode(
        "utf-8"
    )
    b_file.name = "index.html"
    return b_file


@pytest.fixture
def letter_html():
    letter_file = load_test_letter("letter.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_6_variables_html():
    letter_file = load_test_letter("letter_with_6_variables.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_long_variable_html():
    letter_file = load_test_letter("letter_with_long_variable.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_wo_unsubscribe_link_html():
    letter_file = load_test_letter("letter_wo_unsubscribe_link.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_invalid_unsubscribe_link_html():
    letter_file = load_test_letter("letter_with_invalid_unsubscribe_link.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_unknown_tag_html():
    letter_file = load_test_letter("letter_with_unknown_tag.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_link_to_unknown_attachment_html():
    letter_file = load_test_letter("letter_with_link_to_unknown_attachment.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_link_to_unknown_attachment_zip():
    letter_file = load_test_letter("letter_with_link_to_unknown_attachment.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_zip():
    letter_file = load_test_letter("letter.zip")
    letter_file.ATTACHMENTS_COUNT = 2
    yield letter_file
    letter_file.close()


@pytest.fixture
def invalid_letter_zip():
    letter_file = load_test_letter("invalid.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def damaged_zip():
    file = load_test_letter("damaged.zip")
    yield file
    file.close()


@pytest.fixture
def letter_with_unused_attachments_zip():
    letter_file = load_test_letter("letter_with_unused_attachments.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_too_many_attachments_zip():
    letter_file = load_test_letter("letter_with_too_many_attachments.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_with_big_attachments_zip():
    letter_file = load_test_letter("letter_with_big_attachments.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_base_zip():
    letter_file = load_test_letter("letter_base.zip")
    letter_file.ATTACHMENTS_COUNT = 1
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_base_add_image_zip():
    letter_file = load_test_letter("letter_base_add_image.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_base_add_too_many_images_zip():
    letter_file = load_test_letter("letter_base_add_too_many_images.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_base_add_too_big_image_zip():
    letter_file = load_test_letter("letter_base_add_too_big_image.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_base_add_missing_image_zip():
    letter_file = load_test_letter("letter_base_add_missing_image.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_base_delete_image_zip():
    letter_file = load_test_letter("letter_base_delete_image.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def letter_base_replace_image_zip():
    letter_file = load_test_letter("letter_base_replace_image.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def preloaded_base_letter(campaign, mock_avatars_publish, letter_base_zip):
    mock_avatars_publish.publish_path = "/test/path1"
    load_letter(campaign.default_letter, letter_base_zip)
    return letter_base_zip


@pytest.fixture
def upload_letter_html_body_exception(mocker):
    mocker.patch("fan.message.letter.upload_letter_html_body", side_effect=Exception)


def test_load_letter_html(campaign, letter_html):
    load_letter(campaign.default_letter, letter_html)
    assert campaign.letter_uploaded == True
    assert campaign.letter_description == "letter.html"
    assert "<body>" in campaign.default_letter.html_body, "letter body doesn't contain <body> tag"
    assert_doesnt_contain_opens_counter(campaign.default_letter.html_body)
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


def test_user_template_variables_count_exceeded(campaign, letter_with_6_variables_html):
    with pytest.raises(UserTemplateVariablesCountExceeded):
        load_letter(campaign.default_letter, letter_with_6_variables_html)


def test_user_template_variable_length_exceeded(campaign, letter_with_long_variable_html):
    with pytest.raises(UserTemplateVariableLengthExceeded):
        load_letter(campaign.default_letter, letter_with_long_variable_html)


def test_load_letter_wo_unsubscribe_link(campaign, letter_wo_unsubscribe_link_html):
    with pytest.raises(UnsubscribeLinkNotFoundError):
        load_letter(campaign.default_letter, letter_wo_unsubscribe_link_html)


def test_load_letter_with_invalid_unsubscribe_link(
    campaign, letter_with_invalid_unsubscribe_link_html
):
    with pytest.raises(UnsubscribeLinkNotFoundError):
        load_letter(campaign.default_letter, letter_with_invalid_unsubscribe_link_html)


def test_load_letter_with_unknown_tag(campaign, letter_with_unknown_tag_html):
    with pytest.raises(
        TemplateRuntimeError,
        match="Template syntax error:\"Encountered unknown tag 'some_unknown_tag'",
    ):
        load_letter(campaign.default_letter, letter_with_unknown_tag_html)


def test_load_letter_with_link_to_unknown_attachment(
    campaign, letter_with_link_to_unknown_attachment_html
):
    with pytest.raises(
        TemplateRuntimeError, match="Template runtime error:\"'some_unknown_attachment.jpg'\""
    ):
        load_letter(campaign.default_letter, letter_with_link_to_unknown_attachment_html)


def test_load_zip_letter_with_link_to_unknown_attachment(
    campaign, letter_with_link_to_unknown_attachment_zip
):
    with pytest.raises(
        TemplateRuntimeError, match="Template runtime error:\"'some_unknown_attachment.jpg'\""
    ):
        load_letter(campaign.default_letter, letter_with_link_to_unknown_attachment_zip)


def test_load_letter_with_too_many_attachments(campaign, letter_with_too_many_attachments_zip):
    with pytest.raises(AttachmentsCountExceeded):
        load_letter(campaign.default_letter, letter_with_too_many_attachments_zip)


def test_load_letter_with_big_attachments(campaign, letter_with_big_attachments_zip):
    with pytest.raises(AttachmentsSizeExceeded):
        load_letter(campaign.default_letter, letter_with_big_attachments_zip)


@flaky(max_runs=5)
def test_load_letter_doesnt_store_unused_attachments(campaign, letter_with_unused_attachments_zip):
    load_letter(campaign.default_letter, letter_with_unused_attachments_zip)
    assert len(campaign.default_letter.attachments.all()) == 2
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


@flaky(max_runs=5)
def test_load_letter_zip(campaign, letter_zip):
    load_letter(campaign.default_letter, letter_zip)
    assert campaign.letter_uploaded == True
    assert campaign.letter_description == "letter.zip"
    assert "<body>" in campaign.default_letter.html_body, "letter body doesn't contain <body> tag"
    assert sorted(attachments_filelist(campaign.default_letter)) == ["twi.png", "vk.png"]
    assert sorted(attachments_file_sizes(campaign.default_letter)) == [622, 727]
    assert_html_contains_attachments_publish_paths(campaign.default_letter)
    assert_doesnt_contain_opens_counter(campaign.default_letter.html_body)


@flaky(max_runs=5)
def test_load_letter_substitutes_local_images_with_published(campaign, letter_zip):
    load_letter(campaign.default_letter, letter_zip)
    assert (
        campaign.default_letter.html_body.count("https://avatars.mdst.yandex.net/get-sender/") == 2
    )
    assert campaign.default_letter.html_body.count("autoloaded_file") == 0


def test_load_invalid_letter_zip(campaign, invalid_letter_zip):
    with pytest.raises(HTMLWrongFormat):
        load_letter(campaign.default_letter, invalid_letter_zip)


def test_load_damaged_zip(campaign, damaged_zip):
    with pytest.raises(BadZipfile):
        load_letter(campaign.default_letter, damaged_zip)


@flaky(max_runs=5)
def test_load_base_letter(campaign, mock_avatars_publish, letter_base_zip):
    mock_avatars_publish.publish_path = "/test/path1"
    load_letter(campaign.default_letter, letter_base_zip)
    assert len(campaign.default_letter.attachments.all()) == letter_base_zip.ATTACHMENTS_COUNT
    assert "/test/path1" in campaign.default_letter.html_body
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


@flaky(max_runs=5)
def test_update_base_letter_add_image(
    campaign, mock_avatars_publish, preloaded_base_letter, letter_base_add_image_zip
):
    mock_avatars_publish.publish_path = "/test/path2"
    load_letter(campaign.default_letter, letter_base_add_image_zip)
    assert (
        len(campaign.default_letter.attachments.all())
        == preloaded_base_letter.ATTACHMENTS_COUNT + 1
    )
    assert "/test/path1" in campaign.default_letter.html_body
    assert "/test/path2" in campaign.default_letter.html_body
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


@flaky(max_runs=5)
def test_update_base_letter_add_too_many_images(
    campaign, mock_avatars_publish, preloaded_base_letter, letter_base_add_too_many_images_zip
):
    with pytest.raises(AttachmentsCountExceeded):
        load_letter(campaign.default_letter, letter_base_add_too_many_images_zip)


@flaky(max_runs=5)
def test_update_base_letter_add_too_big_image(
    campaign, preloaded_base_letter, letter_base_add_too_big_image_zip
):
    with pytest.raises(AttachmentsSizeExceeded):
        load_letter(campaign.default_letter, letter_base_add_too_big_image_zip)


@flaky(max_runs=5)
def test_update_base_letter_add_missing_image(
    campaign, preloaded_base_letter, letter_base_add_missing_image_zip
):
    with pytest.raises(
        TemplateRuntimeError, match="Template runtime error:\"'some_unknown_attachment.jpg'\""
    ):
        load_letter(campaign.default_letter, letter_base_add_missing_image_zip)


@flaky(max_runs=5)
def test_update_base_letter_delete_image(
    campaign, preloaded_base_letter, letter_base_delete_image_zip
):
    load_letter(campaign.default_letter, letter_base_delete_image_zip)
    assert_html_contains_attachments_publish_paths(campaign.default_letter)
    assert (
        len(campaign.default_letter.attachments.all())
        == preloaded_base_letter.ATTACHMENTS_COUNT - 1
    )


@flaky(max_runs=5)
def test_update_base_letter_replace_image(
    campaign, mock_avatars_publish, preloaded_base_letter, letter_base_replace_image_zip
):
    mock_avatars_publish.publish_path = "/test/path2"
    load_letter(campaign.default_letter, letter_base_replace_image_zip)
    assert len(campaign.default_letter.attachments.all()) == preloaded_base_letter.ATTACHMENTS_COUNT
    assert "/test/path1" not in campaign.default_letter.html_body
    assert "/test/path2" in campaign.default_letter.html_body
    assert_html_contains_attachments_publish_paths(campaign.default_letter)


def test_first_publish_with_timeout(campaign, letter_base_zip, mock_avatars_publish):
    mock_avatars_publish.raise_timeout_on_call_number = 1
    with pytest.raises(requests.exceptions.Timeout):
        load_letter(campaign.default_letter, letter_base_zip)
    assert len(mock_avatars_publish.published_images) == 0
    assert len(campaign.default_letter.attachments.all()) == 0


def test_second_publish_with_timeout(
    campaign, letter_zip, mock_avatars_publish, mock_avatars_unpublish
):
    mock_avatars_publish.raise_timeout_on_call_number = 2
    with pytest.raises(requests.exceptions.Timeout):
        load_letter(campaign.default_letter, letter_zip)
    assert len(mock_avatars_publish.published_images) == 1
    assert set(mock_avatars_publish.published_images) == set(
        mock_avatars_unpublish.unpublished_images
    )
    assert len(campaign.default_letter.attachments.all()) == 0


def test_first_publish_with_error(campaign, letter_base_zip, mock_avatars_publish):
    mock_avatars_publish.resp_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    mock_avatars_publish.resp_code_on_call_number = 1
    with pytest.raises(requests.exceptions.HTTPError):
        load_letter(campaign.default_letter, letter_base_zip)
    assert len(mock_avatars_publish.published_images) == 0
    assert len(campaign.default_letter.attachments.all()) == 0


def test_second_publish_with_error(
    campaign, letter_zip, mock_avatars_publish, mock_avatars_unpublish
):
    mock_avatars_publish.resp_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    mock_avatars_publish.resp_code_on_call_number = 2
    with pytest.raises(requests.exceptions.HTTPError):
        load_letter(campaign.default_letter, letter_zip)
    assert len(mock_avatars_publish.published_images) == 1
    assert set(mock_avatars_publish.published_images) == set(
        mock_avatars_unpublish.unpublished_images
    )
    assert len(campaign.default_letter.attachments.all()) == 0


def test_error_while_uploading_to_db(
    campaign,
    letter_zip,
    mock_avatars_publish,
    mock_avatars_unpublish,
    upload_letter_html_body_exception,
):
    with pytest.raises(Exception):
        load_letter(campaign.default_letter, letter_zip)
    assert len(mock_avatars_publish.published_images) == letter_zip.ATTACHMENTS_COUNT
    assert set(mock_avatars_publish.published_images) == set(
        mock_avatars_unpublish.unpublished_images
    )
