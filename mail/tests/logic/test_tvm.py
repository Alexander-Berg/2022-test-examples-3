import pytest

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.mark.cluster(CLUSTERS)
async def test_service_ticket_requested(env, sender, rcpt):
    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)
    msg_id, msg = make_plain_message(sender, rcpt)
    await client.send_message(msg, sender.email, rcpt.email)

    assert len(env.stubs.blackbox.requests) > 0
    for request in env.stubs.blackbox.requests:
        assert "X-Ya-Service-Ticket" in request.headers
