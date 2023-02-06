from collections import namedtuple

import pytest


SENDER_ADDRESS_REJECTED_RESP = "5.7.1 Sender address rejected: not owned by auth user."
SENDER_ADDRESS_REJECTED_CODE = 553
OK_CODE = 250
OK_MAIL_FROM_PATTERN = "2.1.0 <{mail_from_email}> ok"

Case = namedtuple("Case", ["auth_login_pattern", "mail_from_pattern"])


@pytest.mark.smtp
async def test_mail_from_not_equals_auth_login(env, prod_sender, prod_rcpt):
    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()
    await client.auth_plain(prod_sender.email, prod_sender.passwd)
    code, msg = await client.execute_command("MAIL FROM:<{login}>".format(login=prod_rcpt.email).encode())
    assert code == SENDER_ADDRESS_REJECTED_CODE
    assert SENDER_ADDRESS_REJECTED_RESP in msg


@pytest.mark.smtp
@pytest.mark.parametrize("case", [
    Case("{login}", "{login}@ya.ru"),
    Case("{login}", "{login}@yandex.ru"),
    Case("{login}", "{login}@yandex.kz"),
    Case("{login}", "{login}@yandex.by"),
    Case("{login}", "{login}@yandex.com"),
    Case("{login}@ya.ru", "{login}@yandex.com"),
    Case("{login}@yandex.ru", "{login}@yandex.com"),
    Case("{login}@yandex.kz", "{login}@yandex.com")
])
async def test_mail_from_equals_auth_login_with_different_domains(env, case, prod_sender):
    env.stubs.blackbox.set_glue_different_zones_emails(True)
    env.stubs.blackbox.set_allow_only_login(True)

    client = await env.nwsmtp.get_client()
    await client._ehlo_or_helo_if_needed()

    auth_login = case.auth_login_pattern.format(login=prod_sender.login)
    auth_pwd = prod_sender.passwd
    mail_from = case.mail_from_pattern.format(login=prod_sender.login)
    await client.auth_plain(auth_login, auth_pwd)
    code, msg = await client.mail(mail_from)
    assert code == OK_CODE
    assert OK_MAIL_FROM_PATTERN.format(mail_from_email=mail_from) in msg
