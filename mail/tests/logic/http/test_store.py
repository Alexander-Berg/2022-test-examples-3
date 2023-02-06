import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from unittest.mock import patch
from urllib.parse import urlencode

from mail.nwsmtp.tests.lib.stubs import nsls, avir, so
from mail.nwsmtp.tests.lib.stubs.nsls import make_store_resp
from mail.nwsmtp.tests.lib.env import Env
from mail.nwsmtp.tests.lib.util import make_message

# All tests within this module would run with the marker
pytestmark = [pytest.mark.mxbackout]


async def test_store(env: Env, sender, rcpt, http_client, store_info):
    _, message = make_message(sender, rcpt, text="Hello\r\n..\r\n")

    store_info["options"] = {
        "detect_virus": True,
        "detect_spam": True,
        "detect_loop": True,
        "enable_push": True,
        "allow_duplicates": True,
        "use_filters": True
    }
    store_info["mail_info"] = {
        "folder_path": "path",
        "folder_spam_path": "",
        "folder_path_delim": "",
        "received_date": 1631000246,
        "labels": {
            "symbol": ["important"],
            "lids": ["lid1"],
            "system": ["recent", "seen"],
            "imap": ["imap_labels1"],
            "user": ["user_label1"]
        },
        "old_mid": 'old_mid'
    }

    status, body = await http_client.store(rcpt, store_info, message, fid=2)

    assert status == 200
    assert body["mid"]

    assert env.stubs.mds.messages[0].endswith(
        b"X-Yandex-Fwd: 1\r\n"
        b"Message-Id: %s\r\n"
        b"From: second@yandex.ru\r\n"
        b"To: first@yandex.ru\r\n"
        b"Subject: My Subject\r\n\r\n"
        b"Hello\r\n..\r\n" % message["Message-Id"].encode()
    )

    hints = env.stubs.nsls.requests[0]["message"]["hints"]
    assert len(hints) == 1, "Expect single hint"
    hint = hints[0]

    assert_that(
        hint,
        has_entries(
            label=contains_inanyorder(
                "symbol:important",
                "SystMetkaSO:people",
                "SystMetkaSO:trust_5",
                "SystMetkaSO:recent",
                "SystMetkaSO:seen",
                "domain_domain"
            ),
            fid=["2"],
            received_date=["1631000246"],
            allow_duplicates=["1"],
            disable_push=["0"],
            filters=["1"],
            imap=["0"],
            lid=contains_inanyorder("lid1"),
            no_such_fid_fail=["1"],
            skip_loop_prevention=["1"],
            skip_meta_msg=["0"],
            sync_dlv=["1"],
            imaplabel=["imap_labels1"],
            userlabel=["user_label1"],
            mid=["old_mid"]
        )
    )


async def test_store_with_un_dot_stuffing(env: Env, sender, rcpt, http_client, store_info):
    _, message = make_message(sender, rcpt, text="Hello\r\n..\r\n")

    store_info["options"] = {
        "un_dot_stuffing": True
    }

    status, body = await http_client.store(rcpt, store_info, message, fid=2)
    assert status == 200

    assert env.stubs.mds.messages[0].endswith(
        b"X-Yandex-Fwd: 1\r\n"
        b"Message-Id: %s\r\n"
        b"From: second@yandex.ru\r\n"
        b"To: first@yandex.ru\r\n"
        b"Subject: My Subject\r\n\r\n"
        b"Hello\r\n.\r\n" % message["Message-Id"].encode()
    )


async def test_store_with_path_params(env: Env, rcpt, http_client, store_info, message):
    store_info["mail_info"] = {
        "received_date": 1631000246,
        "folder_path": "path",
        "folder_spam_path": "spam_path",
        "folder_path_delim": "path_delim",
    }

    status, body = await http_client.store(rcpt, store_info, message)
    assert status == 200

    hints = env.stubs.nsls.requests[0]["message"]["hints"]
    assert len(hints) == 1, "Expect single hint"
    hint = hints[0]

    assert_that(
        hint,
        has_entries(
            folder_path=["path"],
            folder_spam_path=["spam_path"],
            folder_path_delim=["path_delim"],
            received_date=["1631000246"],
        )
    )


async def test_duplicate_found(rcpt, http_client, store_info, message):
    mid = "100500"
    store_response = make_store_resp("deduplicated", mid)
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = store_response
        status, body = await http_client.store(rcpt, store_info, message, fid=1)

    assert status == 200
    assert body == {"mid": "100500"}


async def test_loop_detected(rcpt, http_client, store_info, message):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("decycled")
        status, body = await http_client.store(rcpt, store_info, message, fid=1)

    assert status == 409
    assert body == {"code": "loop_detected", "message": "loop detected"}


async def test_store_into_non_existing_folder(rcpt, http_client, store_info, message):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("perm_error")
        status, body = await http_client.store(rcpt, store_info, message, fid=1)

    assert status == 409
    assert body == {"code": "store_error", "message": "nsls permanent error"}


async def test_temporary_store_error(rcpt, http_client, store_info, message):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("temp_error")
        status, body = await http_client.store(rcpt, store_info, message, fid=1)

    assert status == 500
    assert body == {"code": "store_error", "message": "nsls temporary error"}


async def test_store_virus(rcpt, http_client, store_info, message):
    store_info["options"] = {"detect_virus": True}
    with patch.object(avir, "is_virus") as patched:
        patched.return_value = True
        status, body = await http_client.store(rcpt, store_info, message, fid=1)

    assert status == 406
    assert body == {"code": "rejected_virus", "message": "virus message"}


@pytest.mark.mxbackout
async def test_store_spam(rcpt, http_client, store_info, message):
    store_info["options"] = {"detect_spam": True}
    with patch.object(so, "get_so_resolution") as patched:
        patched.return_value = "SO_RESOLUTION_SPAM"
        status, body = await http_client.store(rcpt, store_info, message, fid=1)
        assert patched.called_once

    assert status == 406
    assert body == {"code": "rejected_spam", "message": "spam message"}


@pytest.mark.parametrize("required_arg", ["uid", "service"])
async def test_required_query_arguments(http_client, required_arg):
    query_args = {"uid": "100500", "fid": "1", "service": "imap"}
    query_args.pop(required_arg)

    status, body = await http_client.post("/mail/store?" + urlencode(query_args))
    assert (status, body) == (400, f"missing argument \"{required_arg}\"".encode())


async def test_required_fid_arguments(rcpt, http_client, store_info, message):
    status, body = await http_client.store(rcpt, store_info, message)
    assert status == 400
    assert body == "missing arguments fid and folder_path"


@pytest.mark.parametrize("detect_spam, expected_call_count", [
    (True, 1),
    (False, 0),
])
async def test_enable_so(env, http_client, rcpt, store_info, message,
                         detect_spam, expected_call_count):
    store_info["options"] = {"detect_spam": detect_spam}

    status, body = await http_client.store(rcpt, store_info, message, fid=1)
    assert status == 200

    assert len(env.stubs.so_in.requests) == expected_call_count
