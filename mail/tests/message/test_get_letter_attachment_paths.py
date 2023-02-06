import pytest
from flaky import flaky
from fan.campaigns.create import create_campaign
from fan.message.letter import get_letter_attachment_paths, load_letter
from fan.testutils.letter import load_test_letter


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_avatars_publish, mock_avatars_unpublish):
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


def test_on_empty_letter(empty_letter):
    attachment_paths = get_letter_attachment_paths(empty_letter)
    assert len(attachment_paths) == 0


@flaky(max_runs=5)
def test_on_letter_with_attachments(letter_with_attachments):
    attachment_paths = get_letter_attachment_paths(letter_with_attachments)
    assert len(attachment_paths) == 2
