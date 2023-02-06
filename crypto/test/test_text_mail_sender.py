import pytest

import yatest

from crypta.lib.python.smtp.text_mail_sender import TextMailSender


@pytest.mark.parametrize("from_addr, to_addrs", [
    ("from_yandex.ru", ["to_yandex.ru"]),
    ("from_yandex.ru", ["to_1_yandex.ru", "to_2_yandex.ru"])
])
def test_default_addresses(local_smtp_server, from_addr, to_addrs):
    with local_smtp_server:
        smtp_host, smtp_port = local_smtp_server.local_address
        mail_sender = TextMailSender(smtp_host, smtp_port, from_addr, to_addrs)
        mail_sender.send("test_subject", "test_text")
    return yatest.common.canonical_file("mail_0.txt", local=True)


@pytest.mark.parametrize("from_addr, to_addrs, cc", [
    ("from_yandex.ru", ["to_yandex.ru"], ["cc_yandex.ru"]),
    ("from_yandex.ru", ["to_1_yandex.ru", "to_2_yandex.ru"], ["cc_1_yandex.ru", "cc_2_yandex.ru"])
])
def test_default_addresses_and_cc(local_smtp_server, from_addr, to_addrs, cc):
    with local_smtp_server:
        smtp_host, smtp_port = local_smtp_server.local_address
        mail_sender = TextMailSender(smtp_host, smtp_port, from_addr, to_addrs, cc)
        mail_sender.send("test_subject", "test_text")
    return yatest.common.canonical_file("mail_0.txt", local=True)


@pytest.mark.parametrize("from_addr, to_addrs, cc", [
    ("from_yandex.ru", ["to_yandex.ru"], ["cc_yandex.ru"]),
    ("from_yandex.ru", ["to_1_yandex.ru", "to_2_yandex.ru"], ["cc_1_yandex.ru", "cc_2_yandex.ru"])
])
def test_overload_default_addresses(local_smtp_server, from_addr, to_addrs, cc):
    with local_smtp_server:
        smtp_host, smtp_port = local_smtp_server.local_address
        mail_sender = TextMailSender(smtp_host, smtp_port, "default_from_yandex.ru", ["default_to_yandex.ru"], ["defaul_cc_yandex.ru"])
        mail_sender.send("custom_addrs_subject", "custom_addrs_text", from_addr, to_addrs, cc)
    return yatest.common.canonical_file("mail_0.txt", local=True)


@pytest.mark.parametrize("from_addr, to_addrs", [
    ("from_yandex.ru", ["to_yandex.ru"]),
    ("from_yandex.ru", ["to_1_yandex.ru", "to_2_yandex.ru"])
])
def test_no_default_addresses(local_smtp_server, from_addr, to_addrs):
    with local_smtp_server:
        smtp_host, smtp_port = local_smtp_server.local_address
        mail_sender = TextMailSender(smtp_host, smtp_port)
        mail_sender.send("test_subject", "test_text", from_addr, to_addrs)
    return yatest.common.canonical_file("mail_0.txt", local=True)


def test_default_and_custom_addrs(local_smtp_server):
    with local_smtp_server:
        smtp_host, smtp_port = local_smtp_server.local_address
        mail_sender = TextMailSender(smtp_host, smtp_port, "default_from_yandex.ru", ["default_to_yandex.ru"])
        mail_sender.send("default_addrs_subject", "default_addrs_text")
        mail_sender.send("custom_addrs_subject", "custom_addrs_text", "custom_from_yandex.ru", ["custom_to_yandex.ru"])
    return [yatest.common.canonical_file("mail_0.txt", local=True), yatest.common.canonical_file("mail_1.txt", local=True)]


def test_no_addresses_specified(local_smtp_server):
    with local_smtp_server:
        smtp_host, smtp_port = local_smtp_server.local_address
        mail_sender = TextMailSender(smtp_host, smtp_port)
        with pytest.raises(Exception):
            mail_sender.send("test_subject", "test_text")


@pytest.mark.parametrize("from_addr, to_addrs", [
    ("from_yandex.ru", []),
    ("from_yandex.ru", [""]),
    ("from_yandex.ru", ["", "to_yandex.ru"]),
    ("from_yandex.ru", None),
    ("", ["to_yandex.ru"]),
    (None, ["to_yandex.ru"]),
])
def test_empty_addresses_specified(local_smtp_server, from_addr, to_addrs):
    with local_smtp_server:
        smtp_host, smtp_port = local_smtp_server.local_address
        mail_sender = TextMailSender(smtp_host, smtp_port, from_addr, to_addrs)
        with pytest.raises(Exception):
            mail_sender.send("test_subject", "test_text")
