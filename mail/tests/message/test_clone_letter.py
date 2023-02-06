import pytest
import requests
from flaky import flaky
from fan.campaigns.create import create_campaign
from fan.message.letter import clone_letter, load_letter
from fan.testutils.letter import load_test_letter
from fan.models import LetterAttachment


pytestmark = pytest.mark.django_db


@pytest.fixture
def letter_zip():
    letter_file = load_test_letter("letter.zip")
    yield letter_file
    letter_file.close()


@pytest.fixture
def empty_letter(account, project):
    campaign = create_campaign(account=account, project=project)
    return campaign.default_letter


@pytest.fixture
def another_empty_letter(account, project):
    campaign = create_campaign(account=account, project=project)
    return campaign.default_letter


@pytest.fixture
def source_letter(account, project, letter_zip):
    campaign = create_campaign(account=account, project=project)
    load_letter(campaign.default_letter, letter_zip)
    return campaign.default_letter


@pytest.fixture
def preloaded_letter(account, project, letter_zip):
    campaign = create_campaign(account=account, project=project)
    load_letter(campaign.default_letter, letter_zip)
    return campaign.default_letter


@pytest.fixture
def upload_letter_html_body_exception(mocker):
    mocker.patch("fan.message.letter.upload_letter_html_body", side_effect=Exception)


@flaky(max_runs=5)
def test_publishes_attachments(mock_avatars_publish, empty_letter, source_letter):
    mock_avatars_publish.published_images = []
    clone_letter(empty_letter, source_letter)
    assert len(mock_avatars_publish.published_images) == 2


@flaky(max_runs=5)
def test_unpublishes_attachments_on_avatars_error(
    mock_avatars_publish, mock_avatars_unpublish, empty_letter, source_letter
):
    mock_avatars_publish.published_images = []
    mock_avatars_publish.raise_timeout_on_call_number = 2
    with pytest.raises(requests.exceptions.Timeout):
        clone_letter(empty_letter, source_letter)
    assert (
        len(mock_avatars_publish.published_images)
        == len(mock_avatars_unpublish.unpublished_images)
        == 1
    )
    assert set(_published_image_names(mock_avatars_publish)) == set(
        _unpublished_image_names(mock_avatars_unpublish)
    )


@flaky(max_runs=5)
def test_unpublishes_attachments_on_db_error(
    mock_avatars_publish,
    mock_avatars_unpublish,
    empty_letter,
    source_letter,
    upload_letter_html_body_exception,
):
    mock_avatars_publish.published_images = []
    with pytest.raises(Exception):
        clone_letter(empty_letter, source_letter)
    assert (
        len(mock_avatars_publish.published_images)
        == len(mock_avatars_unpublish.unpublished_images)
        == 2
    )
    assert set(_published_image_names(mock_avatars_publish)) == set(
        _unpublished_image_names(mock_avatars_unpublish)
    )


@flaky(max_runs=5)
def test_substitutes_publish_paths_with_cloned(mock_avatars_publish, empty_letter, source_letter):
    mock_avatars_publish.published_images = []
    clone_letter(empty_letter, source_letter)
    assert all(
        [
            (image_name in empty_letter.html_body)
            for image_name in _published_image_names(mock_avatars_publish)
        ]
    )


@flaky(max_runs=5)
def test_uploads_cloned_attachments(mock_avatars_publish, empty_letter, source_letter):
    mock_avatars_publish.published_images = []
    clone_letter(empty_letter, source_letter)
    assert set(_attachment_image_names(empty_letter)) == set(
        _published_image_names(mock_avatars_publish)
    )


@flaky(max_runs=5)
def test_sets_letter_meta(mock_avatars_publish, empty_letter, source_letter):
    clone_letter(empty_letter, source_letter)
    assert empty_letter.original_letter_content_hash != ""
    assert empty_letter.description != ""


def test_raises_exception_if_target_letter_is_not_empty(
    mock_avatars_publish, mock_avatars_unpublish, preloaded_letter, source_letter
):
    with pytest.raises(RuntimeError):
        clone_letter(preloaded_letter, source_letter)


def test_allows_cloning_empty_letter(
    mock_avatars_publish,
    mock_avatars_unpublish,
    preloaded_letter,
    empty_letter,
    another_empty_letter,
):
    clone_letter(empty_letter, another_empty_letter)
    assert len(empty_letter.html_body) == 0


def _attachment_image_names(letter):
    attachments = LetterAttachment.objects.filter(letter=letter)
    return [attachment.publish_path.split("/")[-2] for attachment in attachments]


def _published_image_names(mock_avatars_publish):
    return [image[0] for image in mock_avatars_publish.published_images]


def _unpublished_image_names(mock_avatars_unpublish):
    return [image[0] for image in mock_avatars_unpublish.unpublished_images]
