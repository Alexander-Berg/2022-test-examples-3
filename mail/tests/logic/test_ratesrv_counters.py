import pytest

from hamcrest import assert_that, starts_with, has_length, is_

from mail.nwsmtp.tests.lib.util import make_plain_message


@pytest.mark.mxfront
async def test_same_counter_names_for_bigml(env, sender, big_ml):
    """Checking that counter names are the same in get and increase requests

    Investigated in MAILDLV-4271
    """
    assert env.conf.nwsmtp.big_ml.use, "Test requires big_ml enabled"
    client = await env.nwsmtp.get_client()
    msg_id, msg = make_plain_message(sender, big_ml)

    await client.send_message(msg)
    msg = await env.relays.wait_msg(msg_id)

    requests = env.stubs.ratesrv.requests

    assert "mxfront:connections_from_ip" in requests[0]["counters"]["0"]["name"]

    get_request = requests[2]["counters"]
    increase_request = requests[3]["counters"]

    assert len(get_request) > 0
    assert len(get_request) == len(increase_request)

    for counter in get_request:
        assert increase_request[counter]["name"] == get_request[counter]


@pytest.mark.yaback
async def test_ratesrv_domain_zero_makes_no_request_and_discards_recipient(env, sender, rcpt_from_ratesrv_zero_domain):
    """
    Test for special `zero` domain in RateSrv.
    We should not send a request to RateSrv if address in RCPT command is from the `zero` domain.
    The response should be like for rcpt_discard_list, i.e. no error code should be emitted.
    """
    client = await env.nwsmtp.get_client()
    _, msg = make_plain_message(sender, rcpt_from_ratesrv_zero_domain)

    code, reply = await client.send_message(msg)

    assert_that(env.stubs.ratesrv.requests, has_length(0))
    assert_that(bool(code), is_(False))
    assert_that(reply, starts_with('2.0.0 Ok, discarded'))
