import pytest

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.env import Env
from mail.nwsmtp.tests.lib.util import make_plain_message, has_ssl_transport, get_session_id
from mail.nwsmtp.tests.lib.received_header import (get_received_info, build_received,
                                                   build_received_with_source, CERTIFICATE_PART)


def get_protocol(env, client, is_auth_required):
    protocol = "ESMTP"
    if has_ssl_transport(client):
        protocol += "S"
    if is_auth_required:
        protocol += "A"
    return protocol


@pytest.mark.cluster(CLUSTERS)
async def test_received_header_prepends_message(env: Env, sender, rcpt):
    # Force to ipv4 because in sandbox we are unable to resolve ptr for ipv6
    client = await env.nwsmtp.get_client(ipv6=False)
    if env.nwsmtp.is_auth_required():
        await client.login(sender.email, sender.passwd)

    msg_id, msg = make_plain_message(sender, rcpt)
    reply = await client.send_message(msg, sender.email, rcpt.email)
    msg = await env.relays.wait_msg(msg_id)

    received_info = get_received_info(env, get_session_id(reply), get_protocol(env, client, env.nwsmtp.is_auth_required()))

    headers = b""
    if msg.envelope.content.startswith(b"X-Yandex-Internal:"):
        headers = b"X-Yandex-Internal: 1\r\n"

    received = build_received_with_source(received_info)
    if env.conf.nwsmtp.message_processing.hide_source_in_received:
        received = build_received(received_info)

    headers += received
    assert msg.envelope.content.startswith(headers)

    if has_ssl_transport(client):
        assert msg.mime["Received"].endswith(CERTIFICATE_PART)
