import pytest

from crypta.web.redirect.src.lib.redirect import create_app


def assert_reply(reply, code, substring):
    assert reply.status_code == code
    assert reply.data.find(substring) != -1


def assert_redirect_reply(reply, code, substring):
    assert reply.status_code == code
    assert reply.headers.get('Location').find(substring) != -1


@pytest.fixture
def client():
    app = create_app("test")
    return app.test_client()


def test_root(client):
    reply = client.get("/")
    assert_reply(reply, 200, b"OK")


def test_common(client):
    reply = client.get("https://crypta.yandex-team.ru", headers={"Cookie": "yandexuid=100500"})
    assert_redirect_reply(reply, 308, "https://crypta.yandex-team.ru/")

    reply = client.get("/crypta.yandex-team.ru/", headers={"Cookie": "yandexuid=100500"})
    assert_redirect_reply(reply, 307, "https://crypta.yandex-team.ru?uid=100500")

    reply = client.get("/crypta.yandex-team.ru/path?x=100&y=lalala", headers={"Cookie": "yandexuid=100500"})
    assert_redirect_reply(reply, 307, "https://crypta.yandex-team.ru/path?uid=100500&x=100&y=lalala")


def test_yandexuid(client):
    yandexuids_and_replies = [
        ("100", "uid=100&"),
        ("", "uid=&"),
        ("x", "uid=&"),
    ]

    test_url = "/crypta.yandex-team.ru/path?x=100"

    for yandexuid, reply_text in yandexuids_and_replies:
        reply = client.get(test_url, headers={"Cookie": "yandexuid={}".format(yandexuid)})
        assert_redirect_reply(reply, 307, reply_text)


def test_puid(client):
    sessionids_and_replies = [
        ('3:1625560616.5.0.1576060291900:YXc3De65VkQIBAAAuAYCKg:7.1|166560262.4671776.302.2:4671776|372240626.613610.2.2:613610|237313.781213.***', "uidType=puid&uid=166560262&"), # noqa
        ("dsafasfsadfdsa", "uidType=puid&uid=&")
    ]

    test_url = "/crypta.yandex-team.ru/path?x=100&use_puid"

    for sessionid, reply_text in sessionids_and_replies:
        reply = client.get(test_url, headers={"Cookie": "Session_id={}".format(sessionid)})
        assert_redirect_reply(reply, 307, reply_text)


def test_patterns(client):
    hosts_and_replies={
        ("search.crypta.yandex-team.ru", "yandexuid%20100500&"),
        ("crypta.yandex-team.ru", "uid=100500&"),
    }

    test_url = "/{host}/path?x=100"

    for host, reply_text in hosts_and_replies:
        reply = client.get(test_url.format(host=host), headers={"Cookie": "yandexuid=100500"})
        assert_redirect_reply(reply, 307, reply_text)


def test_blackhole(client):
    host_and_replies = [
        ("crypta.yandex-team.ru/", "crypta.yandex-team.ru"),
        ("yandex.ru/", "https://wiki.yandex-team.ru/security/readme"),
    ]

    for (host, reply_text) in host_and_replies:
        reply = client.get("/" + host)
        assert_redirect_reply(reply, 307, reply_text)


def test_embedded(client):
    assert_redirect_reply(
        client.get("/embedded", headers={"Cookie": "yandexuid=100500"}),
        307,
        "https://crypta.yandex-team.ru/mini/me?uid=100500"
    )
