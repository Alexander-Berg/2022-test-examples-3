import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from unittest.mock import patch
from urllib.parse import urlencode

from mail.nwsmtp.tests.lib.stubs import nsls
from mail.nwsmtp.tests.lib.stubs.nsls import make_store_resp


# All tests within this module would run with the marker
pytestmark = [pytest.mark.mxbackout]


async def test_store_mailish(env, rcpt, http_client, store_info, message):
    store_info["options"] = {"enable_push": True}
    store_info["mail_info"] = {
        "received_date": 1631000246,
        "labels": {
            "symbol": ["seen_label"],
            "lids": ["lid1"]
        }
    }

    status, body = await http_client.store_mailish(rcpt, store_info, message, fid=2, external_imap_id=3)

    assert status == 200
    assert body["mid"]
    assert body["imap_id"]

    assert env.stubs.mds.messages[0].endswith(
        b"X-Yandex-Fwd: 1\r\n"
        b"Message-Id: %s\r\n"
        b"From: second@yandex.ru\r\n"
        b"To: first@yandex.ru\r\n"
        b"Subject: My Subject\r\n\r\n"
        b"Hello" % message["Message-Id"].encode()
    )

    hints = env.stubs.nsls.requests[0]["message"]["hints"]
    assert len(hints) == 1, "Expect single hint"
    hint = hints[0]

    assert_that(
        hint,
        has_entries(
            label=contains_inanyorder(
                "symbol:seen_label",
                "SystMetkaSO:people",
                "SystMetkaSO:trust_5",
                "domain_domain"
            ),
            fid=["2"],
            external_imap_id=["3"],
            received_date=["1631000246"],
            allow_duplicates=["0"],
            disable_push=["0"],
            filters=["0"],
            imap=["0"],
            lid=contains_inanyorder("lid1"),
            no_such_fid_fail=["1"],
            skip_loop_prevention=["1"],
            skip_meta_msg=["0"],
            sync_dlv=["1"],
        )
    )


async def test_duplicate_found(rcpt, http_client, store_info, message):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("deduplicated")
        status, body = await http_client.store_mailish(rcpt, store_info, message)

    assert status == 409
    assert body["code"] == "duplicate_found"


async def test_store_into_non_existing_folder(rcpt, http_client, store_info, message):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("perm_error")
        status, body = await http_client.store_mailish(rcpt, store_info, message)

    assert status == 409
    assert body["code"] == "store_error"


async def test_temporary_store_error(rcpt, http_client, store_info, message):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("temp_error")
        status, body = await http_client.store_mailish(rcpt, store_info, message)

    assert status == 500
    assert body == {"code": "store_error", "message": "nsls temporary error"}


async def test_required_fields_for_mailish_delivery(env, rcpt, http_client, store_info, message):
    status, body = await http_client.store_mailish(rcpt, store_info, message)
    assert status == 200

    req = env.stubs.nsls.requests[0]

    # is_mailish = True is mandatory, it affects BB request and SO resolution
    assert req["recipients"][0]["is_mailish"] is True

    assert req["message"]["hints"][0]["skip_loop_prevention"] == ["1"]


@pytest.mark.parametrize("required_arg", ["uid", "fid", "external_imap_id"])
async def test_required_query_arguments(http_client, required_arg):
    query_args = {"uid": "100500", "fid": "1", "external_imap_id": "1234"}
    query_args.pop(required_arg)

    status, body = await http_client.post("/mail/store_mailish?" + urlencode(query_args))
    assert (status, body) == (400, f"missing argument \"{required_arg}\"".encode())
