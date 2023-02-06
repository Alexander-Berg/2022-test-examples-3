import pytest
from django.test import Client
from django.core.urlresolvers import reverse
from fan.links.unsubscribe import encode_unsubscribe_code2

# XXX move all tests to the upper level
from unsubscribe.helpers import init_fake_typed_log_handler

pytestmark = pytest.mark.django_db


def test_pixel_successfully_loaded(letter):
    response = Client().get(get_pixel_link(letter=letter, email="a@b.ru"))
    assert response.status_code == 200


def test_pixel_successfully_loaded_for_invalid_path(letter):
    response = Client().get("/px/1")
    assert response.status_code == 200


def test_pixel_successfully_loaded_for_invalid_secret(letter):
    response = Client().get("/px/1/1/XXXXXXX")
    assert response.status_code == 200


def test_pixel_typed_log(letter):
    """
    Делает запись в typed лог о показе пикселя
    """
    log = init_fake_typed_log_handler()
    response = Client().get(get_pixel_link(letter=letter, email="a@b.ru"))
    assert len(log.messages) == 1


def test_pixel_typed_log_fields(letter):
    """
    Пишет в typed лог поля с корректными значениями
    """
    log = init_fake_typed_log_handler()
    response = Client().get(get_pixel_link(letter=letter, email="a@b.ru"))
    log_record = dict(log.messages[-1])
    assert all(
        k in log_record for k in ("event", "email", "letter", "campaign", "user_agent", "user_ip")
    )
    assert log_record["event"] == "pixel"
    assert log_record["email"] == "a@b.ru"
    assert log_record["letter"] == letter.id
    assert log_record["campaign"] == letter.campaign.id
    assert log_record["user_ip"] == "127.0.0.1"


def get_pixel_link(letter, email):
    code = encode_unsubscribe_code2(
        campaign_id=letter.campaign.id, email=email, letter_id=letter.id
    )
    path = "%s/%s/%s/" % (letter.campaign.id, letter.id, code)
    return reverse("px", kwargs={"path": path})
