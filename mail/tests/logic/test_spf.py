import pytest

from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.mark.mxfront
@pytest.mark.mxcorp
async def test_passing_spf_check(env, sender, rcpt):
    assert env.conf.nwsmtp.spf.use, "Test requires spf enabled"
    client = await env.nwsmtp.get_client()

    msg_id, msg = make_plain_message(sender, rcpt)

    await client.send_message(msg)
    msg = await env.relays.wait_msg(msg_id)

    assert 'spf=pass' in msg.mime['Authentication-Results']
