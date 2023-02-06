import pytest

from crypta.lib.python.rtmr.log_serializers import redir_log_serializer


def test_serialize_redir_log():
    return redir_log_serializer.serialize_redir_log({
        "yuid": "1231241562845",
        "unixtime": "1500000000",
        "ips": "158.58.128.12,158.58.128.12",
        "HTTP_REFERER": "https://yandex.ru/search",
        "user_agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 YaBrowser/19.9.2.228 Yowser/2.5 Safari/537.36",
        "url": "//yandex.ru/",
        "extra": "extra",
    })


def test_missing_field():
    with pytest.raises(AssertionError):
        redir_log_serializer.serialize_redir_log({})
