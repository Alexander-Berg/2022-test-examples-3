import pytest

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.nwsmtp.tests.lib.users import make_email


def get_login(email) -> str:
    login = email.split("@", 1)[0]
    assert len(login) != 0
    return login


@pytest.mark.cluster(CLUSTERS)
@pytest.mark.parametrize("logins_in, logins_out", [
    [("second",), ("second",)],
    [("second", "second"), ("second",)],
    [("second", "seCond"), ("second",)],
    [("sEcond", "second"), ("sEcond",)]
])
async def test_skip_duplicated_rcpts(env, sender, rcpt, logins_in, logins_out):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    emails_in = [make_email(login, env.is_corp()) for login in logins_in]

    msg_id, msg = make_plain_message(sender, rcpt)
    await client.send_message(msg, sender.email, emails_in)
    msg = await env.relays.wait_msg(msg_id)

    if env.conf.nwsmtp.delivery_to_sender_control.use:
        logins_out += (get_login(sender.email),)

    # On corp all addresses are maillists and final email is taken
    #  from CORP_ML response, not from rcpt-to.
    if env.nwsmtp.conf.nwsmtp.corp_maillist.use:
        logins_out = [login.lower() for login in logins_out]

    logins = [get_login(email) for email in msg.envelope.rcpt_tos]
    assert sorted(logins) == sorted(logins_out)


@pytest.mark.cluster(["mxbackcorp", "mxbackout"])
@pytest.mark.parametrize("logins_in, logins_out", [
    [("first", "second"), ("first", "second",)],
    [("fiRst", "second"), ("fiRst", "second",)]
])
async def test_skip_adding_duplicated_sender_when_same_rcpt_exists(env, sender, rcpt, logins_in, logins_out):
    assert env.conf.nwsmtp.delivery_to_sender_control.use and \
        get_login(sender.email) == "first", \
        "Test requires adding sender to rcpts feature enabled"

    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    emails_in = [make_email(login, env.is_corp()) for login in logins_in]

    msg_id, msg = make_plain_message(sender, rcpt)
    await client.send_message(msg, sender.email, emails_in)
    msg = await env.relays.wait_msg(msg_id)

    # On corp all addresses are maillists and final email is taken
    #  from CORP_ML response, not from rcpt-to.
    if env.nwsmtp.conf.nwsmtp.corp_maillist.use:
        logins_out = [login.lower() for login in logins_out]

    logins = [get_login(email) for email in msg.envelope.rcpt_tos]
    assert sorted(logins) == sorted(logins_out)
