import pytest
from flaky import flaky
from fan.campaigns.create import create_campaign
from fan.message.letter import unpublish_letter_attachments_noexcept, load_letter
from fan.testutils.letter import load_test_letter


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_avatars_publish):
    pass


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
def letter_with_attachments(account, project, letter_zip):
    campaign = create_campaign(account=account, project=project)
    load_letter(campaign.default_letter, letter_zip)
    return campaign.default_letter


def test_on_empty_letter(mock_avatars_unpublish, empty_letter):
    unpublish_letter_attachments_noexcept(_attachment_paths(empty_letter))
    assert len(mock_avatars_unpublish.unpublished_images) == 0


@flaky(max_runs=5)
def test_on_letter_with_attachments(mock_avatars_unpublish, letter_with_attachments):
    unpublish_letter_attachments_noexcept(_attachment_paths(letter_with_attachments))
    assert len(mock_avatars_unpublish.unpublished_images) == 2


def _attachment_paths(letter):
    return [attachment.publish_path for attachment in letter.attachments.all()]
