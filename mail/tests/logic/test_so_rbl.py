import pytest
import aiosmtplib

from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.mark.mxfront
async def test_so_rbl_not_spam_ip_accepted(env, sender, rcpt):
    my_ip = '::1'
    env.stubs.so_rbl.reset_requests()
    env.stubs.so_rbl.mark_ip_as_not_spam(my_ip)

    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, rcpt)

    send_res = await client.send_message(msg)
    msg = await env.relays.wait_msg(msg_id)
    assert env.stubs.so_rbl.get_requests() == ['::1']
    assert '2.0.0 Ok' in send_res[1]


@pytest.mark.mxfront
async def test_so_rbl_spam_ip_rejected(env, sender, rcpt):
    my_ip = '::1'
    env.stubs.so_rbl.mark_ip_as_spam(my_ip)

    with pytest.raises(aiosmtplib.errors.SMTPConnectError) as exc:
        await env.nwsmtp.get_client()

    assert "554 5.7.1 Service unavailable; Client host [::1] blocked by spam statistics - see http://feedback.yandex.ru/?from=mail-rejects&subject=::1" in exc.value.message
    assert env.stubs.so_rbl.get_requests() == ['::1']
