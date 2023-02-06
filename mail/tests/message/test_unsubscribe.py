import pytest
from fan.links.unsubscribe import get_unsubscribe_links

pytestmark = pytest.mark.django_db


@pytest.fixture
def unsubscribe_link(mocker):
    unsub_link = "<sender_unsubscribe_link>"
    mocker.patch("fan.links.unsubscribe.get_unsubscribe_link", return_value=unsub_link)
    return unsub_link


@pytest.fixture
def render_context():
    return {
        "unsubscribe_link": "<unsubscribe_link>",
        "global_unsubscribe_link": "<global_unsubscribe_link>",
    }


def test_get_unsubscribe_link_returns_values_from_render_context(render_context, account):
    links = get_unsubscribe_links(
        render_context=render_context, letter_secret="", allow_custom=True
    )
    assert links["unsubscribe_link"] == render_context["unsubscribe_link"]
    assert links["global_unsubscribe_link"] == render_context["global_unsubscribe_link"]


def test_get_unsubscribe_link_generates_new_values(render_context, account, unsubscribe_link):
    links = get_unsubscribe_links(
        render_context=render_context, letter_secret="", allow_custom=False
    )
    assert links["unsubscribe_link"] == unsubscribe_link
    assert links["global_unsubscribe_link"] == unsubscribe_link + "?type=global"
