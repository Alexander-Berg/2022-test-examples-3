import pytest
from fan.message.env import TemplateRuntimeError
from fan.message.loader import from_string
from fan.message.message import SendrMessage
from fan.message.template import Template


pytestmark = pytest.mark.django_db


@pytest.fixture
def test_letter(letter):
    letter.subject = "Штука для снятия стрессов"
    letter.from_email = "devnull@yandex.ru"
    letter.from_name = "alsun (Я.ру)"
    letter.save()
    return letter


@pytest.mark.parametrize("_add_reply", [True, False])
def test_emails_reply_to(_add_reply, test_letter, campaign):
    """
    Проверка дополнительных заголовков письма
    """
    test_letter.reply_to = "reply-email@ya.ru" if _add_reply else ""
    campaign.magic_list_id = "list-id"
    message = SendrMessage(test_letter)
    message.mail_to = "Вася <v@ya.ru>"
    message.letter_secret = "SECRET"
    message.render(A="XXX", unsubscribe_link="YYY")

    if _add_reply:
        assert message.message()["Reply-To"] == "reply-email@ya.ru"
    else:
        assert "Reply-To" not in message.message()


@pytest.mark.parametrize(
    "_to, _expect",
    [
        (("vasya", "v@ya.ru"), "=?utf-8?b?dmFzeWE=?= <v@ya.ru>"),
        ("vasya <v@ya.ru>", "=?utf-8?b?dmFzeWE=?= <v@ya.ru>"),
        ("v@ya.ru", "v@ya.ru"),
    ],
)
def test_emails_to_headers(_to, _expect, test_letter):
    message = SendrMessage(test_letter)
    message.mail_to = _to
    message.render()
    assert message.message()["To"] == _expect


def test_jinja_env():
    t = Template("{{ x }}")
    assert t.render(x=42) == "42"
    assert t.render() == ""


def test_jinja_env_required():
    t = Template("{{ x|required }}")
    assert t.render(x=42), "42"
    with pytest.raises(TemplateRuntimeError):
        t.render()


def test_html5_links():
    html = "<a><table><tbody><tr><td>text</td></tr></tbody></table></a>"
    message = from_string(html)
    message.render(A="XXX", unsubscribe_link="YYY")
    assert html in message.html_body


@pytest.mark.parametrize(
    "_tag",
    [
        "area",
        "br",
        "command",
        "embed",
        "hr",
        "img",
        "input",
        "meta",
        "param",
        "source",
    ],
)
def test_tag_duplications(_tag):
    html = "<{}>".format(_tag)
    _expect = "<{}>".format(_tag)

    message = from_string(html)
    message.render()
    assert _expect in message.html_body


def test_col_tag_duplication():
    html = "<table><col></table>"
    _expect = "<col>"
    test_letter.html_body = html

    message = from_string(html)
    message.render()
    assert _expect in message.html_body


@pytest.mark.parametrize("_tag", ["span"])
def test_empty_tag(_tag):
    html = "<{tag}></{tag}>some_text".format(tag=_tag)
    _expect = "<{tag}></{tag}>".format(tag=_tag)
    test_letter.html_body = html

    message = from_string(html)
    message.render()
    assert _expect in message.html_body


def test_style_tag():
    html = """
        <html>
        <head>
            <meta http-equiv="Content-Type">
            <meta http-equiv="X-UA-Compatible" content="IE=edge">
            <style type="text/css">
                body {
                margin: 0 auto !important;
                padding: 0 !important;
                }
                div {
                margin: 0;
                }
                @media only screen and (min-width:768px){
                    .templateContainer{
                        width:600px !important;
                    }
                }
            </style>
        </head>
        <body>
            <p id="test_par"> </p>
            <p class="test_cls"> </p>
        </body></html>
    """

    message = from_string(html)
    message.render()
    for substr in [
        "<html>",
        "<head>",
        "<meta",
        'http-equiv="X-UA-Compatible"',
        "<style",
        "body",
        "div",
        "@media only",
        "</style>",
        "</head>",
        "<body>",
        '<p id="test_par">',
        '<p class="test_cls">',
    ]:
        assert substr in message.html_body
