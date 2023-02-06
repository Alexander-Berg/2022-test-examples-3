import pytest
from fan.message.render import generate_rendering_params
from fan.links.unsubscribe import decode_unsubscribe_code2


pytestmark = pytest.mark.django_db


def test_content(campaign):
    recipient = "recipient@test.ru"
    params = generate_rendering_params(campaign.id, campaign.default_letter.id, recipient)
    assert len(list(params.keys())) == 2
    assert "recipient" in params
    assert "secret" in params
    assert params["recipient"] == recipient


def test_secret(campaign):
    recipient = "recipient@test.ru"
    params = generate_rendering_params(campaign.id, campaign.default_letter.id, recipient)
    secret = params["secret"]
    email, campaign_id, letter_id, _, for_testing = decode_unsubscribe_code2(secret)
    assert email == recipient
    assert campaign_id == campaign.id
    assert letter_id == campaign.default_letter.id
    assert for_testing == False
