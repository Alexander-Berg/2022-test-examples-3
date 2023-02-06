import pytest
from mail.nwsmtp.tests.lib.util import make_plain_message
from aiosmtplib.errors import SMTPRecipientsRefused, SMTPSenderRefused


@pytest.mark.mxbackout
@pytest.mark.mxfront
@pytest.mark.smtp
async def test_rejection_user_with_bad_karma(env, sender, bad_karma_rcpt):
    client = await env.nwsmtp.get_client()

    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    _, msg = make_plain_message(sender, bad_karma_rcpt)

    with pytest.raises(SMTPRecipientsRefused) as exc:
        await client.send_message(msg)

    assert exc.value.recipients[0].code == 550
    assert exc.value.recipients[0].message.startswith("5.7.1 Policy rejection on the target address")


@pytest.mark.yaback
async def test_acceptance_user_with_bad_karma(env, sender, bad_karma_rcpt):
    assert env.conf.nwsmtp.blackbox.client_rcpt_with_bad_karma_receives_mails, "Test requires client_rcpt_with_bad_karma_receives_mails enabled"

    client = await env.nwsmtp.get_client()

    _, msg = make_plain_message(sender, bad_karma_rcpt)

    code, reply = await client.send_message(msg)

    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxbackout
@pytest.mark.mxfront
@pytest.mark.smtp
@pytest.mark.yaback
async def test_acceptance_user_with_threshold_karma(env, sender, threshold_karma_user):
    client = await env.nwsmtp.get_client()

    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    _, msg = make_plain_message(sender, threshold_karma_user)

    code, reply = await client.send_message(msg)

    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxbackout
@pytest.mark.mxfront
@pytest.mark.smtp
async def test_rejection_user_with_temporarily_bad_karma(env, sender, temp_bad_karma_rcpt):
    client = await env.nwsmtp.get_client()

    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    _, msg = make_plain_message(sender, temp_bad_karma_rcpt)
    with pytest.raises(SMTPRecipientsRefused) as exc:
        await client.send_message(msg)

    assert exc.value.recipients[0].code == 451
    assert exc.value.recipients[0].message.replace("[4] ", "").startswith("4.7.1 Requested action aborted: error in processing")


@pytest.mark.yaback
async def test_acceptance_user_with_temporarily_bad_karma(env, sender, temp_bad_karma_rcpt):
    assert env.conf.nwsmtp.blackbox.client_rcpt_with_bad_karma_receives_mails, "Test requires client_rcpt_with_bad_karma_receives_mails enabled"

    client = await env.nwsmtp.get_client()

    _, msg = make_plain_message(sender, temp_bad_karma_rcpt)

    code, reply = await client.send_message(msg)

    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxbackout
@pytest.mark.mxfront
@pytest.mark.smtp
@pytest.mark.yaback
async def test_rejection_blocked_user(env, sender, user_with_blocked_email):
    client = await env.nwsmtp.get_client()

    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    _, msg = make_plain_message(sender, user_with_blocked_email)
    with pytest.raises(SMTPRecipientsRefused) as exc:
        await client.send_message(msg)

    assert exc.value.recipients[0].code == 550
    assert exc.value.recipients[0].message.startswith("5.7.1 Policy rejection on the target address")


@pytest.mark.mxbackout
@pytest.mark.smtp
async def test_acceptance_unknown_user(env, sender, unknown_rcpt, users):
    assert env.conf.nwsmtp.blackbox.allow_unknown_rcpt, "Test requires allow_unknown_rcpt enabled"
    assert users.get(unknown_rcpt.email) is None

    client = await env.nwsmtp.get_client()

    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    _, msg = make_plain_message(sender, unknown_rcpt)

    code, reply = await client.send_message(msg)
    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxfront
async def test_rejection_unknown_user(env, sender, unknown_rcpt, users):
    assert users.get(unknown_rcpt.email) is None

    client = await env.nwsmtp.get_client()

    _, msg = make_plain_message(sender, unknown_rcpt)
    with pytest.raises(SMTPRecipientsRefused) as exc:
        await client.send_message(msg)

    assert exc.value.recipients[0].code == 550
    assert exc.value.recipients[0].message.startswith("5.7.1 No such user")


@pytest.mark.mxcorp
@pytest.mark.mxbackcorp
async def test_acceptance_corp_list(env, sender, corp_list_rcpt):
    assert env.conf.nwsmtp.corp_maillist.use, "Test requires corp_maillist enabled"

    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message(sender, corp_list_rcpt)

    code, reply = await client.send_message(msg)
    assert not code
    assert "2.0.0 Ok: queued on " in reply


@pytest.mark.mxbackout
@pytest.mark.parametrize("login_suffix", ["+", "++", "+afterlogin", "+afterlogin+dog"])
async def test_acceptance_user_with_plus_after_login(env, sender, rcpt, login_suffix):
    client = await env.nwsmtp.get_client()

    rcpt.login += login_suffix
    rcpt.email = rcpt.login + "@" + rcpt.domain

    _, msg = make_plain_message(sender, rcpt)

    code, reply = await client.send_message(msg)

    assert not code
    assert "2.0.0 Ok: queued on " in reply
    assert msg["To"] == rcpt.email


@pytest.mark.mxbackout
@pytest.mark.mxbackcorp
async def test_rejection_unknown_sender(env, rcpt):
    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message("first@gmail.com", rcpt)
    with pytest.raises(SMTPSenderRefused) as exc:
        await client.send_message(msg)

    assert exc.value.code == 553
    assert exc.value.message.startswith("5.7.1 Sender address rejected: user not found")


@pytest.mark.mxfront
@pytest.mark.mxcorp
@pytest.mark.yaback
async def test_send_mail_from_unknown_sender(env, rcpt):
    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message("first@gmail.com", rcpt)

    code, reply = await client.send_message(msg)

    assert not code
    assert "2.0.0 Ok: queued on " in reply
