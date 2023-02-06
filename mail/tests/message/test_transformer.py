import pytest

from fan.message.transformer import SendrRegexpTransformer

pytestmark = pytest.mark.django_db


@pytest.fixture
def transformer(message):
    return SendrRegexpTransformer(message=message)


def test_apply_to_links(transformer):
    transformer._html = """
    <htm>
    <head></head>
    <body>
    <a href="http://ya.ru">Test</a>
    <a href="https
    :// yandex.ru
    ">
    Test
    </a>
    <a class="test" href="http://ya.ru" >Test</a>
    <a widht="123" href="https:://yaaa.ru/"></a>
    <a class="test"
    href="https://test.ru/">Test</a>
    <a class="some-class other_class" {% if r > 0 %}style=bold{% endif %} href=http://ya.ru{% if p > 10 and q == "abc" %}/q=213{% endif %}>test</a>
    <!--[if gte mso 9]>
    <v:rect xmlns:v="urn:schemas-microsoft-com:vml" fill="true" stroke="false" style="width:620px;height:396px;">
        <v:fill type="tile" src="https://1.png" href='https://test.ru/' color="#ffc000" />
        <v:textbox inset="0,0,0,0">
    <![endif]-->
    КОНТЕНТ ТУТ
    <!--[if gte mso 9]>
    </v:textbox>
    </v:rect>
    <![endif]-->
    </body>
    </html>
    """

    result = """
    <htm>
    <head></head>
    <body>
    <a href="https://click.yandex.ru/*http://ya.ru">Test</a>
    <a href="https://click.yandex.ru/*https
    :// yandex.ru
    ">
    Test
    </a>
    <a class="test" href="https://click.yandex.ru/*http://ya.ru" >Test</a>
    <a widht="123" href="https://click.yandex.ru/*https:://yaaa.ru/"></a>
    <a class="test"
    href="https://click.yandex.ru/*https://test.ru/">Test</a>
    <a class="some-class other_class" {% if r > 0 %}style=bold{% endif %} href=https://click.yandex.ru/*http://ya.ru{% if p > 10 and q == "abc" %}/q=213{% endif %}>test</a>
    <!--[if gte mso 9]>
    <v:rect xmlns:v="urn:schemas-microsoft-com:vml" fill="true" stroke="false" style="width:620px;height:396px;">
        <v:fill type="tile" src="https://1.png" href='https://click.yandex.ru/*https://test.ru/' color="#ffc000" />
        <v:textbox inset="0,0,0,0">
    <![endif]-->
    КОНТЕНТ ТУТ
    <!--[if gte mso 9]>
    </v:textbox>
    </v:rect>
    <![endif]-->
    </body>
    </html>
    """

    def func(link, *args, **kwargs):
        return "https://click.yandex.ru/*{}".format(link)

    transformer.apply_to_links(func)
    assert transformer._html == result


def test_apply_to_images(transformer):
    transformer._html = """
    <html>
    <head>
        <style>
        img {
            background: url('http://yandex.ru');
        }

        span {
            background: url(http://ya.ru);
        }

        p {
            background: url("http://ya.ru");
        }
        /*
          comments
        */
        </style>
    </head>
    <body>
        <img class="test test__test" src="https://img.ya.ru/"/>
        <img src="https://img.ya.ru/" class="test test__test" >
        <img src="https://img.ya.ru/" />
        <div style="background: url('https://1.png') 50% 0 no-repeat #ffc000; min-width: 100%;"></div>
        <!--[if gte mso 9]>
        <v:rect xmlns:v="urn:schemas-microsoft-com:vml" fill="true" stroke="false" style="width:620px;height:396px;">
            <v:fill type="tile" src="https://1.png" color="#ffc000" />
            <v:textbox inset="0,0,0,0">
        <![endif]-->
        КОНТЕНТ ТУТ
        <!--[if gte mso 9]>
        </v:textbox>
        </v:rect>
        <![endif]-->
        <img
        src="https://img.ya.ru/" />
        <img class="some-class other_class" {% if r > 0 %}style=bold{% endif %} src="https://img.ya.ru/{% if p > 10 and q == "abc" %}/q=213{% endif %}" />
        <p background="http://yandex.ru"></p>
    </body>
    </html>
    """

    result = """
    <html>
    <head>
        <style>
        img {
            background: url('https://img.yandex.ru/*http://yandex.ru');
        }

        span {
            background: url(https://img.yandex.ru/*http://ya.ru);
        }

        p {
            background: url("https://img.yandex.ru/*http://ya.ru");
        }
        /*
          comments
        */
        </style>
    </head>
    <body>
        <img class="test test__test" src="https://img.yandex.ru/*https://img.ya.ru/"/>
        <img src="https://img.yandex.ru/*https://img.ya.ru/" class="test test__test" >
        <img src="https://img.yandex.ru/*https://img.ya.ru/" />
        <div style="background: url('https://img.yandex.ru/*https://1.png') 50% 0 no-repeat #ffc000; min-width: 100%;"></div>
        <!--[if gte mso 9]>
        <v:rect xmlns:v="urn:schemas-microsoft-com:vml" fill="true" stroke="false" style="width:620px;height:396px;">
            <v:fill type="tile" src="https://img.yandex.ru/*https://1.png" color="#ffc000" />
            <v:textbox inset="0,0,0,0">
        <![endif]-->
        КОНТЕНТ ТУТ
        <!--[if gte mso 9]>
        </v:textbox>
        </v:rect>
        <![endif]-->
        <img
        src="https://img.yandex.ru/*https://img.ya.ru/" />
        <img class="some-class other_class" {% if r > 0 %}style=bold{% endif %} src="https://img.yandex.ru/*https://img.ya.ru/{% if p > 10 and q == "abc" %}/q=213{% endif %}" />
        <p background="https://img.yandex.ru/*http://yandex.ru"></p>
    </body>
    </html>
    """

    def func(link, *args, **kwargs):
        return "https://img.yandex.ru/*{}".format(link)

    transformer.apply_to_images(func)
    assert transformer._html == result


@pytest.mark.parametrize(
    "source, result",
    (
        (
            "<html><head></head><body><p>test</p></body></html>",
            '<html><head></head><body><p>test</p><img src="px.png"></body></html>',
        ),
        ("", '<img src="px.png">'),
        ("<body></body></body>", '<body><img src="px.png"></body></body>'),
        ("<body></body><body></body>", '<body><img src="px.png"></body><body></body>'),
    ),
)
def test_append_to_body(source, result, transformer):
    transformer._html = source

    transformer.append_to_body('<img src="px.png">')
    assert transformer._html == result


def test_numerate_links(transformer):
    transformer._html = """
    <a href="https://ya.ru"></a>
    <a href="http://yandex.ru"></a>
    <a href="http://example.com/l/{{ sender_campaign_id }}/{{ sender_letter_id }}/1/{{ sender_letter_secret }}/*https://ya.ru"></a>
    <a href="http://example.com/l/{{ sender_campaign_id }}/{{ sender_letter_id }}/test/{{ sender_letter_secret }}/*http://yandex.ru"></a>
    """

    result = """
    <a href="https://ya.ru"></a>
    <a href="http://yandex.ru"></a>
    <a href="{% wrap "1" %}https://ya.ru{% endwrap %}"></a>
    <a href="{% wrap "test" %}http://yandex.ru{% endwrap %}"></a>
    """

    transformer.numerate_links()
    assert transformer._html == result


@pytest.mark.parametrize("_schema", ["http", "https"])
def test_replace_old_pixel(_schema, transformer):
    transformer._html = (
        '<img src="'
        + _schema
        + '://test.example.com/px/{{ sender_campaign_id }}/{{ sender_letter_id }}/{{ sender_letter_secret }}" width="1" height="1" />'
    )
    transformer.replace_old_pixel()
    assert transformer._html == "{% opens_counter %}"
