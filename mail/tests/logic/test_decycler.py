import pytest

from hamcrest import assert_that, contains_string, has_entries

from aiosmtplib.errors import SMTPDataError

from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.util import make_plain_message
from mail.nwsmtp.tests.lib.headers import Header

TTL = 8


def switch_on_decycler(conf):
    conf.nwsmtp.decycler.ttl = TTL


@pytest.mark.mxfront
@pytest.mark.parametrize("with_decycler_header, value", [
    (True, str(TTL)),
    (False, "1"),
])
async def test_update_decycler_header_in_async_delivery(cluster, users, sender, rcpt, with_decycler_header, value):
    headers=((Header.X_YANDEX_FWD, str(TTL - 1)),) if with_decycler_header else ()
    msg_id, msg = make_plain_message(sender, rcpt, headers=headers)

    with make_conf(cluster, customize_with=switch_on_decycler) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            code, reply = await client.send_message(msg)
            msg = await env.relays.wait_msg(msg_id)

    assert_that(reply, contains_string("2.0.0 Ok: queued on"))
    assert_that(msg.mime[Header.X_YANDEX_FWD], value)


@pytest.mark.mxbackout
@pytest.mark.parametrize("with_decycler_header, value", [
    (True, str(TTL)),
    (False, "1"),
])
async def test_update_decycler_header_in_sync_delivery(cluster, users, sender, rcpt, with_decycler_header, value):
    headers=((Header.X_YANDEX_FWD, str(TTL - 1)),) if with_decycler_header else ()
    msg_id, msg = make_plain_message(sender, rcpt, headers=headers)

    with make_conf(cluster, customize_with=switch_on_decycler) as conf:
        async with make_env(cluster, users, conf) as env:
            http_client = env.nwsmtp.get_http_client()
            status, body = await http_client.save(sender, msg)
            assert_that(status, 200)
            message = env.stubs.mds.messages[0].decode("utf-8")

    assert_that(message, contains_string(f"{Header.X_YANDEX_FWD}: {value}"))


@pytest.mark.mxbackout
@pytest.mark.parametrize("detect_loop", [True, False])
async def test_cycle_detected_in_sync_delivery(cluster, users, sender, rcpt, store_info, detect_loop):
    _, msg = make_plain_message(sender, rcpt, headers=((Header.X_YANDEX_FWD, str(TTL)),))
    store_info["options"] = {
        "detect_loop": detect_loop
    }
    with make_conf(cluster, customize_with=switch_on_decycler) as conf:
        async with make_env(cluster, users, conf) as env:
            http_client = env.nwsmtp.get_http_client()
            status, body = await http_client.store(rcpt, store_info, msg, fid=1)
            if detect_loop:
                assert_that(status, 409)
                assert_that(
                    body,
                    has_entries(
                        code="loop_detected",
                        message="loop detected",
                    )
                )
            else:
                assert_that(status, 200)


@pytest.mark.mxcorp
@pytest.mark.mxfront
async def test_cycle_detected_in_async_delivery(cluster, users, sender, rcpt):
    msg_id, msg = make_plain_message(sender, rcpt, headers=((Header.X_YANDEX_FWD, str(TTL)),))
    with make_conf(cluster, customize_with=switch_on_decycler) as conf:
        async with make_env(cluster, users, conf) as env:
            client = await env.nwsmtp.get_client()
            with pytest.raises(SMTPDataError) as exc:
                await client.send_message(msg)

    assert_that(exc.value.code, 554)
    assert_that(exc.value.message, contains_string("5.4.0 Error: too many hops"))
