import pytest

from hamcrest import assert_that, has_entries, has_length

from mail.nwsmtp.tests.lib.configurable import with_dkim_keys
from mail.nwsmtp.tests.lib.util import make_message_from_string
from mail.nwsmtp.tests.lib.default_conf import make_conf
from mail.nwsmtp.tests.lib.env import make_env
from mail.nwsmtp.tests.lib.headers import Header


@with_dkim_keys
def config_customizer(conf):
    conf.nwsmtp.blackbox.allow_unknown_rcpt = 1
    conf.nwsmtp.blackbox.check_sender = 0
    conf.nwsmtp.auth.use = False


def extract_dkim_signature_tags(dkim_signature_value):
    return dict([x.strip().split('=', maxsplit=1) for x in dkim_signature_value.split(';') if x.lstrip()])


MESSAGE_WITHOUT_DKIM_SIGNATURE = (
    "From: mx-test-user29 mx-test-user29 <mx-test-user29@yandex.ru>\r\n"
    "To: \"mx-test-user29@yandex.ru\" <mx-test-user29@yandex.ru>\r\n"
    "Subject: dkim\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 01 Oct 2021 16:38:52 +0300\r\n"
    "Message-Id: <1847201633095514@mail.yandex.ru>\r\n"
    "Content-Transfer-Encoding: 7bit\r\n"
    "Content-Type: text/html\r\n\r\n"
    "<div>dkim</div>\r\n.\r\ntext"
)


MESSAGE_WITH_EXTERNAL_SENDER = (
    "From: pig@external.ru\r\n"
    "To: \"mx-test-user29@yandex.ru\" <mx-test-user29@yandex.ru>\r\n"
    "Subject: dkim\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 01 Oct 2021 16:38:52 +0300\r\n"
    "Message-Id: <1847201633095514@mail.yandex.ru>\r\n"
    "Content-Transfer-Encoding: 7bit\r\n"
    "Content-Type: text/html\r\n\r\n"
    "<div>dkim</div>"
)


MESSAGE_WITH_CORRECT_DKIM_SIGNATURE = (
    "DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail; t=1633095532; bh=AzvkhOZhCncZIFS1CclN/AukPJYGYDxoUZ+Ift0ICrk=; h=Message-Id:Date:Subject:To:From;\n\t"
    "b=OXza3WbvhXcgjvh7jsTOKiWT8ThZa8KCDRHirXE7wIZJSRxdkpC3FGJaa2h60Z25Yzlzzd0ed3LY/zgjUGcwWr87uad1TpnkQcXviORdl/IIP8KBlXGfT3ubhKNBXsjyEJaiy4nQeO9Sf6mUAipnMneJYAKZbMP3kjCUoSYYEvk=\r\n"
    "From: mx-test-user29 mx-test-user29 <mx-test-user29@yandex.ru>\r\n"
    "To: \"mx-test-user29@yandex.ru\" <mx-test-user29@yandex.ru>\r\n"
    "Subject: dkim\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 01 Oct 2021 16:38:52 +0300\r\n"
    "Message-Id: <1847201633095514@mail.yandex.ru>\r\n"
    "Content-Transfer-Encoding: 7bit\r\n"
    "Content-Type: text/html\r\n\r\n"
    "<div>dkim</div>"
)


MESSAGE_WITH_INCORRECT_DKIM_SIGNATURE = (
    "DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail; t=1633095532; bh=AzvkhOZhCncZIFS1CclN/AukPJYGYDxoUZ+Ift0ICrk=; h=Message-Id:Date:Subject:To:From;\n\t"
    "b=OXza3WbvhXcgjvh7jsTOKiWT8ThZa8KCDRHirXE7wIZJSRxdkpC3FGJaa2h60Z25Yzlzzd0ed3LY/zgjUGcwWr87uad1TpnkQcXviORdl/IIP8KBlXGfT3ubhKNBXsjyEJaiy4nQeO9Sf6mUAipnMneJYAKZbMP3kjCUoSYYEve=\r\n"
    "From: mx-test-user29 mx-test-user29 <mx-test-user29@yandex.ru>\r\n"
    "To: \"mx-test-user29@yandex.ru\" <mx-test-user29@yandex.ru>\r\n"
    "Subject: dkim\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 01 Oct 2021 16:38:52 +0300\r\n"
    "Message-Id: <1847201633095514@mail.yandex.ru>\r\n"
    "Content-Transfer-Encoding: 7bit\r\n"
    "Content-Type: text/html\r\n\r\n"
    "<div>dkim</div>"
)


MESSAGE_WITH_INCORRECT_AND_CORRECT_DKIM_SIGNATURES = (
    "DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail; t=1633095532; bh=AzvkhOZhCncZIFS1CclN/AukPJYGYDxoUZ+Ift0ICrk=; h=Message-Id:Date:Subject:To:From;\n\t"
    "b=OXza3WbvhXcgjvh7jsTOKiWT8ThZa8KCDRHirXE7wIZJSRxdkpC3FGJaa2h60Z25Yzlzzd0ed3LY/zgjUGcwWr87uad1TpnkQcXviORdl/IIP8KBlXGfT3ubhKNBXsjyEJaiy4nQeO9Sf6mUAipnMneJYAKZbMP3kjCUoSYYEve=\r\n"
    "DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail; t=1633095532; bh=AzvkhOZhCncZIFS1CclN/AukPJYGYDxoUZ+Ift0ICrk=; h=Message-Id:Date:Subject:To:From;\n\t"
    "b=OXza3WbvhXcgjvh7jsTOKiWT8ThZa8KCDRHirXE7wIZJSRxdkpC3FGJaa2h60Z25Yzlzzd0ed3LY/zgjUGcwWr87uad1TpnkQcXviORdl/IIP8KBlXGfT3ubhKNBXsjyEJaiy4nQeO9Sf6mUAipnMneJYAKZbMP3kjCUoSYYEvk=\r\n"
    "From: mx-test-user29 mx-test-user29 <mx-test-user29@yandex.ru>\r\n"
    "To: \"mx-test-user29@yandex.ru\" <mx-test-user29@yandex.ru>\r\n"
    "Subject: dkim\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 01 Oct 2021 16:38:52 +0300\r\n"
    "Message-Id: <1847201633095514@mail.yandex.ru>\r\n"
    "Content-Transfer-Encoding: 7bit\r\n"
    "Content-Type: text/html\r\n\r\n"
    "<div>dkim</div>"
)


MESSAGE_WITH_CORRECT_AND_INCORRECT_DKIM_SIGNATURES = (
    "DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail; t=1633095532; bh=AzvkhOZhCncZIFS1CclN/AukPJYGYDxoUZ+Ift0ICrk=; h=Message-Id:Date:Subject:To:From;\n\t"
    "b=OXza3WbvhXcgjvh7jsTOKiWT8ThZa8KCDRHirXE7wIZJSRxdkpC3FGJaa2h60Z25Yzlzzd0ed3LY/zgjUGcwWr87uad1TpnkQcXviORdl/IIP8KBlXGfT3ubhKNBXsjyEJaiy4nQeO9Sf6mUAipnMneJYAKZbMP3kjCUoSYYEvk=\r\n"
    "DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed; d=yandex.ru; s=mail; t=1633095532; bh=AzvkhOZhCncZIFS1CclN/AukPJYGYDxoUZ+Ift0ICrk=; h=Message-Id:Date:Subject:To:From;\n\t"
    "b=OXza3WbvhXcgjvh7jsTOKiWT8ThZa8KCDRHirXE7wIZJSRxdkpC3FGJaa2h60Z25Yzlzzd0ed3LY/zgjUGcwWr87uad1TpnkQcXviORdl/IIP8KBlXGfT3ubhKNBXsjyEJaiy4nQeO9Sf6mUAipnMneJYAKZbMP3kjCUoSYYEve=\r\n"
    "From: mx-test-user29 mx-test-user29 <mx-test-user29@yandex.ru>\r\n"
    "To: \"mx-test-user29@yandex.ru\" <mx-test-user29@yandex.ru>\r\n"
    "Subject: dkim\r\n"
    "MIME-Version: 1.0\r\n"
    "Date: Fri, 01 Oct 2021 16:38:52 +0300\r\n"
    "Message-Id: <1847201633095514@mail.yandex.ru>\r\n"
    "Content-Transfer-Encoding: 7bit\r\n"
    "Content-Type: text/html\r\n\r\n"
    "<div>dkim</div>"
)


@pytest.mark.mxbackcorp
@pytest.mark.mxbackout
@pytest.mark.smtpcorp
@pytest.mark.smtp
@pytest.mark.yaback
async def test_check_correct_structure_of_dkim_signature(cluster, users):
    with make_conf(cluster, customize_with=config_customizer) as conf:
        assert conf.dkim_keys.conf.mode in ['sign', 'signverify']

        async with make_env(cluster, users, conf) as env:
            msg = make_message_from_string(MESSAGE_WITHOUT_DKIM_SIGNATURE)
            msg_id = msg["Message-Id"]
            client = await env.nwsmtp.get_client()
            _, reply = await client.send_message(msg)
            assert "2.0.0 Ok: queued on" in reply
            msg = await env.relays.wait_msg(msg_id)

    assert Header.DKIM_SIGNATURE in msg.mime
    assert Header.AUTHENTICATION_RESULTS in msg.mime

    dkim_signature_tags = extract_dkim_signature_tags(msg.mime[Header.DKIM_SIGNATURE])
    authentication_results = msg.mime[Header.AUTHENTICATION_RESULTS]

    assert "dkim=pass" in authentication_results
    assert_that(
        dkim_signature_tags,
        has_entries(
            v="1",
            a="rsa-sha256",
            c="relaxed/relaxed",
            d="yandex.ru",
            s="mail",
            t=has_length(10),
            bh="sFJRYCXIXFf7UzFy8awdp6u9oGC7ApjHWVbgek43ymY=",
            h="Message-Id:Date:Subject:To:From",
            b=has_length(180)
        )
    )


@pytest.mark.mxbackcorp
@pytest.mark.mxbackout
@pytest.mark.smtpcorp
@pytest.mark.smtp
@pytest.mark.yaback
async def test_check_correct_structure_of_dkim_signature_from_fouras(cluster, users, external_dkim_domain):
    with make_conf(cluster, customize_with=config_customizer) as conf:
        assert conf.dkim_keys.conf.mode in ['sign', 'signverify']

        async with make_env(cluster, users, conf) as env:
            env.stubs.fouras.add_dkim_domains(external_dkim_domain)
            msg = make_message_from_string(MESSAGE_WITH_EXTERNAL_SENDER)
            msg_id = msg["Message-Id"]
            client = await env.nwsmtp.get_client()
            _, reply = await client.send_message(msg)
            assert "2.0.0 Ok: queued on" in reply
            msg = await env.relays.wait_msg(msg_id)
            assert_that(env.stubs.fouras.requests, has_length(1))

    assert Header.DKIM_SIGNATURE in msg.mime
    assert Header.AUTHENTICATION_RESULTS in msg.mime

    dkim_signature_tags = extract_dkim_signature_tags(msg.mime[Header.DKIM_SIGNATURE])
    authentication_results = msg.mime[Header.AUTHENTICATION_RESULTS]

    assert "dkim=pass" in authentication_results
    assert_that(
        dkim_signature_tags,
        has_entries(
            v="1",
            a="rsa-sha256",
            c="relaxed/relaxed",
            d=external_dkim_domain.domain,
            s=external_dkim_domain.selector,
            t=has_length(10),
            bh="AzvkhOZhCncZIFS1CclN/AukPJYGYDxoUZ+Ift0ICrk=",
            h="Message-Id:Date:Subject:To:From",
            b=has_length(180)
        )
    )


@pytest.mark.mxfront
@pytest.mark.parametrize("message, result", [
    (MESSAGE_WITH_CORRECT_DKIM_SIGNATURE, "pass"),
    (MESSAGE_WITH_INCORRECT_DKIM_SIGNATURE, "fail"),
    (MESSAGE_WITH_INCORRECT_AND_CORRECT_DKIM_SIGNATURES, "pass"),
    (MESSAGE_WITH_CORRECT_AND_INCORRECT_DKIM_SIGNATURES, "fail"),
], ids=["correct", "incorrect", "incorrect_and_correct", "correct_and_incorrect"])
async def test_check_correct_dkim_signature(cluster, users, message, result):
    with make_conf(cluster, customize_with=config_customizer) as conf:
        async with make_env(cluster, users, conf) as env:
            msg = make_message_from_string(message)
            msg_id = msg["Message-Id"]
            client = await env.nwsmtp.get_client()
            _, reply = await client.send_message(msg)
            assert "2.0.0 Ok: queued on" in reply
            msg = await env.relays.wait_msg(msg_id)

    assert Header.AUTHENTICATION_RESULTS in msg.mime

    authentication_results = msg.mime[Header.AUTHENTICATION_RESULTS]

    assert f"dkim={result}" in authentication_results
