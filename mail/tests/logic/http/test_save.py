import pytest

import json

from hamcrest import assert_that, contains_inanyorder, has_entries

from unittest.mock import patch

from mail.nwsmtp.tests.lib.stubs import nsls
from mail.nwsmtp.tests.lib.util import make_message
from mail.nwsmtp.tests.lib.stubs.nsls import make_store_resp
from mail.nwsmtp.tests.lib.stubs import so, avir


def get_error(response):
    return json.loads(response)["error"]


pytestmark = [pytest.mark.mxbackout]


async def test_save(env, sender, rcpt, http_client):
    store_response = make_store_resp("stored", "100500", "")

    _, msg = make_message(rcpt, sender)

    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = store_response
        status, body = await http_client.save(sender, msg, lids=["lid1", "lid2"], received_date="228",
                                              system=["system"], symbol=["symbol"], detect_spam="1",
                                              old_mid="100500", fid="14")

    assert status == 200
    assert json.loads(body)["mid"] == "100500"

    assert len(env.stubs.nsls.requests) == 1
    req = env.stubs.nsls.requests[0]

    assert len(req["message"]["hints"]) == 1
    hint = req["message"]["hints"][0]

    assert_that(
        hint,
        has_entries(
            label=contains_inanyorder(
                "SystMetkaSO:people",
                "SystMetkaSO:trust_5",
                "SystMetkaSO:system",
                "symbol:symbol",
                "domain_domain"
            ),
            allow_duplicates=["0"],
            disable_push=["1"],
            fid=["14"],
            filters=["0"],
            imap=["0"],
            lid=contains_inanyorder("lid1", "lid2"),
            no_such_fid_fail=["1"],
            received_date=["228"],
            skip_loop_prevention=["1"],
            sync_dlv=["1"],
            skip_meta_msg=["1"],
            mid=["100500"],
        )
    )


async def test_406_on_nsls_temporary_error(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender)

    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("temp_error")
        status, body = await http_client.save(sender, msg)

    assert status == 406
    assert get_error(body) == "ServiceUnavaliable"


async def test_406_on_nsls_permanent_error(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender)

    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("perm_error")
        status, body = await http_client.save(sender, msg)

    assert status == 406
    assert get_error(body) == "SendMessageFailed"


async def test_400_on_empty_uid_in_request(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender)

    sender.uid = ""
    status, body = await http_client.save(sender, msg)

    assert status == 400


async def test_406_on_spam_found(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        status, body = await http_client.save(sender, msg, detect_spam="1")
        assert patched.called_once

    assert status == 406
    assert get_error(body) == "Spam"


async def test_406_on_strong_spam_found(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender)

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_REJECT"
        status, body = await http_client.save(sender, msg, detect_spam="1")
        assert patched.called_once

    assert status == 406
    assert get_error(body) == "StrongSpam"


async def test_200_on_spam_with_captcha(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender, headers=(
        ("X-Yandex-Captcha-Entered", "yes"),
    ))

    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        status, body = await http_client.save(sender, msg, detect_spam="1")
        assert patched.called_once

    assert status == 200


async def test_406_on_strong_virus_found(sender, rcpt, http_client):
    _, msg = make_message(rcpt, sender)

    with patch.object(avir, "is_virus") as patched:
        patched.return_value = True
        status, body = await http_client.save(sender, msg, detect_virus="1")
        assert patched.not_called

    assert status == 406
    assert get_error(body) == "Virus"
