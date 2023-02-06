import pytest

from fan.message.transformer import HtmlRegexpTransformer
from fan.links.statistic_wrapper import add_pixel, tag_images

pytestmark = pytest.mark.django_db


@pytest.fixture
def message_mock(mocker, letter):
    message = mocker.MagicMock()
    message.transformer = HtmlRegexpTransformer("")
    return message


@pytest.mark.parametrize("_html", ["<body></body>", "<body>{% opens_counter %}</body>"])
def test_add_pixel(_html, message_mock):
    message_mock.transformer._html = _html
    add_pixel(message_mock)
    assert message_mock.transformer._html == "<body>{% opens_counter %}</body>"


def test_tag_image_absolute(message_mock):
    message_mock.transformer._html = '<img src="http://example.com" />'
    tag_images(message_mock)
    assert message_mock.transformer._html == '<img src="http://example.com" />'


def test_tag_image_relative(message_mock):
    message_mock.transformer._html = '<img src="relative.jpg" />'
    tag_images(message_mock)
    assert message_mock.transformer._html == '<img src="{% autoloaded_file "relative.jpg" %}" />'


def test_tag_image_template(message_mock):
    message_mock.transformer._html = '<img src="{% autoloaded_file "relative.jpg" %}" />'
    tag_images(message_mock)
    assert message_mock.transformer._html == '<img src="{% autoloaded_file "relative.jpg" %}" />'
