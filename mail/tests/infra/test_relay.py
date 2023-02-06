import pytest

from operator import attrgetter
try:
    from asyncio.exceptions import TimeoutError
except ImportError:
    from asyncio.futures import TimeoutError

from mail.nwsmtp.tests.lib.stubs import Relay
from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.fixture
async def local_relay(conf):
    async with Relay(conf.nwsmtp.delivery.relays.local) as relay:
        async with relay.get_client() as client:
            yield relay, client


@pytest.mark.parametrize("attr_path", [
    "nwsmtp.delivery.relays.local",
    "nwsmtp.delivery.relays.fallback",
    "nwsmtp.delivery.relays.external"
])
@pytest.mark.mxbackcorp
async def test_message_sent_through_relay(conf, attr_path, sender, rcpt):
    get_conf = attrgetter(attr_path)
    async with Relay(get_conf(conf)) as srv:
        _, msg = make_plain_message(sender, rcpt)
        async with srv.get_client() as client:
            code, reply = await client.send_message(msg)
            assert not code
            assert reply == "OK"


@pytest.mark.mxbackout
async def test_wait_for_message_same_client(local_relay, sender, rcpt):
    relay, client = local_relay
    first_msg_id, msg = make_plain_message(sender, rcpt)
    await client.send_message(msg)

    second_msg_id, msg = make_plain_message(sender, rcpt)
    await client.send_message(msg)

    assert first_msg_id == (await relay.wait_msg(first_msg_id)).msg_id
    assert second_msg_id == (await relay.wait_msg(second_msg_id)).msg_id


@pytest.mark.mxbackout
async def test_wait_for_message_same_message(local_relay, sender, rcpt):
    relay, client = local_relay
    msg_id, msg = make_plain_message(sender, rcpt)
    for i in range(2):
        await client.send_message(msg)

    assert msg_id == (await relay.wait_msg(msg_id)).msg_id
    assert msg_id == (await relay.wait_msg(msg_id)).msg_id

    assert not (await relay.wait_msgs())


@pytest.mark.mxbackout
async def test_wait_for_message_diff_clients(local_relay, sender, rcpt):
    relay, client = local_relay
    first_msg_id, msg = make_plain_message(sender, rcpt)
    await client.send_message(msg)

    async with relay.get_client() as second_client:
        second_msg_id, msg = make_plain_message(sender, rcpt)
        await second_client.send_message(msg)

    assert first_msg_id == (await relay.wait_msg(first_msg_id)).msg_id
    assert second_msg_id == (await relay.wait_msg(second_msg_id)).msg_id


@pytest.mark.mxbackout
async def test_wait_for_all_messages(local_relay, sender, rcpt):
    total = 3
    msgs_sent = []

    relay, client = local_relay
    for i in range(total):
        msg_id, msg = make_plain_message(sender, rcpt)
        await client.send_message(msg)
        msgs_sent.append(msg_id)

    msgs = await relay.wait_msgs()
    assert len(msgs) == 3
    assert sorted(msgs_sent) == sorted([item[0] for item in msgs])

    assert not (await relay.wait_msgs(timeout=0.2))


@pytest.mark.mxbackout
async def test_timeout_when_no_messages_received(local_relay):
    relay, client = local_relay
    with pytest.raises(TimeoutError):
        await relay.wait_msg("123", timeout=0.2)
