import pytest

from aiosmtplib.errors import SMTPDataError

from mail.nwsmtp.tests.lib.users import BigMLUser, make_by_login

MESSAGE = (
    "Subject: Hello\r\n\r\n"
    "Text"
)

pytestmark = [pytest.mark.smtp]

NOT_OWNED_BY_USER_SMTP_REPLY = "5.7.0 Sender or From header address rejected:" \
                               " not owned by authorized user"


@pytest.fixture
async def smtp_client(env, sender):
    client = await env.nwsmtp.get_client()
    await client.login(sender.email, sender.passwd)
    return client


@pytest.mark.smtpcorp
@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_auth_and_header_same_success(sender, rcpt, header, smtp_client):
    headers = f"{header}: {sender.email}\r\n"
    await smtp_client.sendmail(sender.email, rcpt.email, headers + MESSAGE)


@pytest.mark.smtpcorp
async def test_sender_has_precedence_success(sender, rcpt, smtp_client):
    headers = f"Sender: {sender.email}\r\n" \
              f"From: {rcpt.email}\r\n"
    await smtp_client.sendmail(sender.email, rcpt.email, headers + MESSAGE)


@pytest.mark.smtpcorp
async def test_take_first_when_many_froms(sender, rcpt, smtp_client):
    headers = f"From: {sender.email}\r\n" \
              f"From: {rcpt.email}\r\n"
    await smtp_client.sendmail(sender.email, rcpt.email, headers + MESSAGE)


@pytest.mark.smtpcorp
@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_auth_and_header_differ_reject(sender, rcpt, header, smtp_client):
    headers = f"{header}: {rcpt.email}\r\n"

    with pytest.raises(SMTPDataError) as exc:
        await smtp_client.sendmail(sender.email, rcpt.email, headers + MESSAGE)

    assert exc.value.code == 550
    assert exc.value.message.startswith(NOT_OWNED_BY_USER_SMTP_REPLY)


@pytest.mark.smtpcorp
@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_email_from_header_not_found_in_blackbox_reject(sender, rcpt, header, smtp_client):
    headers = f"{header}: some@mail.ru\r\n"

    with pytest.raises(SMTPDataError) as exc:
        await smtp_client.sendmail(sender.email, rcpt.email, headers + MESSAGE)

    assert exc.value.code == 550
    assert exc.value.message.startswith(NOT_OWNED_BY_USER_SMTP_REPLY)


@pytest.mark.smtpcorp
async def test_sender_has_precedence_reject(sender, rcpt, smtp_client):
    headers = f"Sender: some@mail.ru\r\n" \
              f"From: {sender.email}\r\n"

    with pytest.raises(SMTPDataError) as exc:
        await smtp_client.sendmail(sender.email, rcpt.email, headers + MESSAGE)

    assert exc.value.code == 550
    assert exc.value.message.startswith(NOT_OWNED_BY_USER_SMTP_REPLY)


@pytest.mark.smtpcorp
@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_unable_to_parse_email_reject(sender, rcpt, header, smtp_client):
    headers = f"{header}: foo.baz.bar.com\r\n"

    with pytest.raises(SMTPDataError) as exc:
        await smtp_client.sendmail(sender.email, rcpt.email, headers + MESSAGE)

    assert exc.value.code == 550
    assert exc.value.message.startswith(f"5.7.0 {header} header syntax is invalid")


@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_send_as_maillist_auth_by_subscriber_accept(env, big_ml, rcpt, header):
    subscriber = next(s for s in big_ml.subscribers.values() if s.uid != big_ml.uid)

    client = await env.nwsmtp.get_client()
    await client.login(subscriber.email, subscriber.passwd)

    headers = f"{header}: {big_ml.email}\r\n"
    await client.sendmail(subscriber.email, rcpt.email, headers + MESSAGE)


@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_send_as_maillist_auth_by_regular_user_reject(env, sender, big_ml, rcpt, header):
    assert sender.uid != big_ml.uid

    client = await env.nwsmtp.get_client()
    await client.login(sender.email, sender.passwd)

    headers = f"{header}: {big_ml.email}\r\n"
    with pytest.raises(SMTPDataError) as exc:
        await client.sendmail(sender.email, rcpt.email, headers + MESSAGE)

    assert exc.value.code == 550
    assert exc.value.message.startswith(NOT_OWNED_BY_USER_SMTP_REPLY)


@pytest.fixture
def sender_in_white_list(env, users):
    conf = env.nwsmtp.conf
    if conf.is_corp():
        user = make_by_login("wlist", corp=True, uid=conf.control_from.uids[0])
        users.add(user)
        return user

    wlist_domain = conf.control_from.domains[0]
    user = BigMLUser(f"user@{wlist_domain}", is_ml=True)
    users.add(user)
    return user


@pytest.mark.smtpcorp
@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_email_from_header_not_found_in_blackbox_accept_by_whitelist(env, sender_in_white_list,
                                                                           rcpt, header):
    client = await env.nwsmtp.get_client()
    await client.login(sender_in_white_list.email, sender_in_white_list.passwd)

    headers = f"{header}: some@mail.ru\r\n"
    await client.sendmail(sender_in_white_list.email, rcpt.email, headers + MESSAGE)


@pytest.mark.smtpcorp
@pytest.mark.parametrize("header", ["Sender", "From"])
async def test_user_from_header_has_another_uid_accept_by_whitelist(env, sender_in_white_list,
                                                                    rcpt, header):
    client = await env.nwsmtp.get_client()
    await client.login(sender_in_white_list.email, sender_in_white_list.passwd)

    headers = f"{header}: {rcpt.email}\r\n"
    await client.sendmail(sender_in_white_list.email, rcpt.email, headers + MESSAGE)
