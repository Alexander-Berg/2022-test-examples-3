import yatest
from crypta.web.checker.lib.checker import createApp


def assert_reply(reply, code, substring):
    assert reply.status_code == code
    assert reply.data.find(substring.encode('ascii')) != -1


def createClient():
    app = createApp("test", yatest.common.runtime.work_path("geodata6.bin"))
    return app.test_client()


def test_relay():
    testClient = createClient()

    for ip in ["8.8.8.8", "SWAG YOLO", ""]:
        reply = testClient.get("https://checker.crypta.yandex.ru/check_relay", headers={"X-Forwarded-For": "{}".format(ip)})
        assert_reply(reply, 200, "NERELAY {}".format(ip))

    for ip in ["146.75.195.77", "2a04:4e41:002f:002b::1:3:4"]:
        reply = testClient.get("https://checker.crypta.yandex.ru/check_relay", headers={"X-Forwarded-For": "{}".format(ip)})
        assert_reply(reply, 200, "ISRELAY {}".format(ip))


def test_headers():
    testClient = createClient()

    originUrl = "https://ya.ru"
    headers = {
        "X-Forwarded-For": "8.8.8.8",
        "Origin": originUrl,
    }

    reply = testClient.get("https://checker.crypta.yandex.ru/check_relay", headers=headers)
    assert reply.status_code == 200
    assert reply.headers.get('Access-Control-Allow-Origin', None) == originUrl
    assert reply.headers.get('Access-Control-Allow-Credentials', None) == "true"


def test_format():
    testClient = createClient()

    headers = {
        "X-Forwarded-For": "8.8.8.8",
    }
    reply = testClient.get("https://checker.crypta.yandex.ru/check_relay?ip=2.3.4.5", headers=headers)
    assert reply.data == b"NERELAY 8.8.8.8 (google) != 2.3.4.5 (orange)"


def test_baddata():
    testClient = createClient()

    headers1 = {"X-Forwarded-For": "1.2.3.4"}
    headers2 = {"X-Forwarded-For": "1234"}
    headers3 = {"X-Forwarded-For": "8.8.8.8"}

    reply = testClient.get("https://checker.crypta.yandex.ru/check_relay?ip=12345", headers=headers1)
    assert reply.data == b"NERELAY 1.2.3.4 (NO YANDEX ISP) != NO SITE IP (NO SITE ISP)"

    reply = testClient.get("https://checker.crypta.yandex.ru/check_relay?ip=8.8.8.8", headers=headers2)
    assert reply.data == b"NERELAY 1234 (NO YANDEX ISP) != 8.8.8.8 (google)"

    reply = testClient.get("https://checker.crypta.yandex.ru/check_relay?ip=8.8.8.8")
    assert reply.data == b"NERELAY NO YANDEX IP (NO YANDEX ISP) != 8.8.8.8 (google)"

    reply = testClient.get("https://checker.crypta.yandex.ru/check_relay?ip=8888", headers=headers3)
    assert reply.data == b"NERELAY 8.8.8.8 (google) != NO SITE IP (NO SITE ISP)"
