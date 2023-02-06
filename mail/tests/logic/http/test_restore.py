import pytest
from aiohttp import web

import json

from hamcrest import assert_that, contains_inanyorder, has_entries

from unittest.mock import patch

from mail.nwsmtp.tests.lib.stubs import nsls, mds
from mail.nwsmtp.tests.lib.stubs.nsls import make_store_resp


def get_error(response):
    return json.loads(response)["code"]


def get_message(response):
    return json.loads(response)["message"]


@pytest.fixture
def restore_request():
    return {"user_info": {"email": "email"}, "mail_info": {"stid": "stid", "fid": "fid", "tab": "tab", "received_date": 1, "lids": ["lid1", "lid2"], "system": ["so"], "symbol": ["seen"]}}


pytestmark = [pytest.mark.mxbackout]


async def test_restored(env, rcpt, http_client, restore_request):
    store_response = make_store_resp("stored", "100500", "")

    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = store_response
        status, body = await http_client.restore(rcpt, restore_request)

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
                "SystMetkaSO:t_tab",
                "SystMetkaSO:so",
                "symbol:seen",
                "domain_domain"
            ),
            allow_duplicates=["0"],
            disable_push=["1"],
            fid=["fid"],
            filters=["0"],
            imap=["0"],
            lid=contains_inanyorder("lid1", "lid2"),
            no_such_fid_fail=["1"],
            received_date=["1"],
            skip_loop_prevention=["1"],
            sync_dlv=["1"],
            skip_meta_msg=["0"]
        )
    )


async def test_406_on_decycled_found(rcpt, http_client, restore_request):
    store_response = make_store_resp("decycled")
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = store_response
        status, body = await http_client.restore(rcpt, restore_request)

    assert status == 406
    assert get_error(body) == "RestoreError"


async def test_406_on_duplicate_found(rcpt, http_client, restore_request):
    store_response = make_store_resp("deduplicated", "100500")
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = store_response
        status, body = await http_client.restore(rcpt, restore_request)

    assert status == 406
    assert get_error(body) == "DuplicateError"
    assert get_message(body) == "100500"


async def test_406_on_nsls_temporary_error(rcpt, http_client, restore_request):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("temp_error")
        status, body = await http_client.restore(rcpt, restore_request)

    assert status == 406
    assert get_error(body) == "ServiceUnavaliable"


async def test_406_on_nsls_permanent_error(rcpt, http_client, restore_request):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_store_resp("perm_error")
        status, body = await http_client.restore(rcpt, restore_request)

    assert status == 406
    assert get_error(body) == "InvalidFid"


async def test_406_on_nsls_406_response_code(rcpt, http_client, restore_request):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.side_effect = web.HTTPNotAcceptable()
        status, body = await http_client.restore(rcpt, restore_request)

    assert status == 406
    assert get_error(body) == "NslsPermanentError"


async def test_406_on_mds_404_response_code(rcpt, http_client, restore_request):
    with patch.object(mds, "make_message") as patched:
        patched.side_effect = web.HTTPNotFound()
        status, body = await http_client.restore(rcpt, restore_request)

    assert status == 406
    assert get_error(body) == "StorageMailNotFound"


async def test_400_on_empty_email_in_request(rcpt, http_client, restore_request):
    restore_request["user_info"]["email"] = ""

    status, body = await http_client.restore(rcpt, restore_request)

    assert status == 400
