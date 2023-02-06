import pytest
from fan.message.letter import load_letter
from fan.message.render import render_preview_html
from fan.testutils.letter import load_test_letter


pytestmark = pytest.mark.django_db


@pytest.fixture
def letter_with_variable_in_different_case_html():
    letter_file = load_test_letter("letter_with_variable_in_different_case.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def campaign_with_letter_with_variable_in_different_case(
    campaign_with_letter, letter_with_variable_in_different_case_html
):
    load_letter(campaign_with_letter.default_letter, letter_with_variable_in_different_case_html)
    yield campaign_with_letter


def test_contains_variable_placeholder(campaign_with_letter):
    html = render_preview_html(campaign_with_letter)
    assert "{{ name }}" in html


def test_contains_variable_placeholders_in_lower_case(
    campaign_with_letter_with_variable_in_different_case,
):
    html = render_preview_html(campaign_with_letter_with_variable_in_different_case)
    assert html.count("{{ var }}") == 5
