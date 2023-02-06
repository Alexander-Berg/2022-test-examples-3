import pytest

from uuid import uuid4

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.mark.cluster(CLUSTERS)
async def test_headers_are_removed_from_outgoing_message(env, sender, rcpt):

    removable_headers = []
    if env.conf.remove_headers_list:
        removable_headers = [h["__text"] for h in env.conf.remove_headers_list]

    client = await env.nwsmtp.get_client()
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    msg_id, msg = make_plain_message(sender, rcpt)

    removable_headers = {
        header: uuid4().hex for header in removable_headers
    }

    await client.send_message(msg)
    new_msg = await env.relays.wait_msg(msg_id)

    assert "From" in new_msg.mime
    assert "To" in new_msg.mime
    assert "Subject" in new_msg.mime
    assert "Message-Id" in new_msg.mime
    assert "Date" in new_msg.mime
    for name, uniq_value in removable_headers.items():
        all_values = new_msg.mime.get_all(name)
        assert all_values is None or uniq_value not in all_values
