import pytest

from json import dumps
from time import time
from unittest.mock import patch

from mail.nwsmtp.tests.lib.default_conf import make_conf, HOST
from mail.nwsmtp.tests.lib.env import Env, make_env
from mail.nwsmtp.tests.lib.stubs import nsls, mds
from mail.nwsmtp.tests.lib.util import (make_message, make_mime_message, make_json_part,
                                        make_multipart)


JSON_PART = dumps(
    {"user_info": {"email": "foo@yandex.ru"}, "mail_info": {"received_date": int(time())}}
)
MIME_MESSAGE = make_mime_message("from", "to", "text")[-1]

# All tests within this module would run with the marker
pytestmark = [pytest.mark.mxbackout]


@pytest.fixture
def request_type(request):
    return request.param


@pytest.fixture
def url(request_type, rcpt):
    if request_type == "mailish":
        return f"/mail/store_mailish?uid={rcpt.uid}&fid=1&external_imap_id=1"
    return f"/mail/store?uid={rcpt.uid}&fid=1&service=imap"


@pytest.fixture
def client_impl(request_type, rcpt, store_info):
    if request_type == "mailish":
        return lambda client, *args, **kwargs: client.store_mailish(*args, **kwargs)
    return lambda client, *args, **kwargs: client.store(*args, **kwargs, fid=1)


def pytest_generate_tests(metafunc):
    # inject this fixture into every test below
    metafunc.parametrize("request_type", ["mailish", "regular"], indirect=True)


@pytest.mark.parametrize("message", [
    "",
    "Text",
    "Subject: Hello\r\nText",
    "\r\nText",
    "\r\nText",
    "\nText",
])
async def test_parse_message_fail(rcpt, client_impl, http_client, store_info, message):
    status, body = await client_impl(http_client, rcpt, store_info, message)
    assert status == 400
    assert body == {"code": "bad_request", "message": "bad message"}


@pytest.mark.parametrize("http_code, parts", [
    (400, []),
    (400, [make_json_part(JSON_PART)]),
    (400, [make_json_part(""), MIME_MESSAGE]),
    (200, [make_json_part(JSON_PART), MIME_MESSAGE]),
    (400, [make_json_part("{im broken json}"), MIME_MESSAGE]),
    (400, [MIME_MESSAGE, make_json_part("{}")]),
    (400, [MIME_MESSAGE])
])
async def test_bad_body(url, http_client, http_code, parts):
    multipart = make_multipart(parts)
    status, body = await http_client.post(url, multipart)
    assert status == http_code


async def test_method_not_allowed(url, http_client):
    status, body = await http_client.get(url)
    assert status == 405
    assert body["code"] == "method_not_allowed"
    assert body["message"]


async def test_headers_removed_before_mds(env: Env, rcpt, client_impl, store_info, http_client):
    remove_headers = [h["__text"] for h in env.conf.nwsmtp.mds.remove_headers]
    assert remove_headers, "No headers to remove!"

    message = (
        "From: foo@yandex.ru\r\n"
        "To: bar@yandex.ru\r\n"
        "Subject: Test\r\n\r\n"
        "Hello"
    )

    headers = ""
    for header in remove_headers:
        headers += f"{header}: 1\r\n"

    status, body = await client_impl(http_client, rcpt, store_info, headers + message)

    assert status == 200
    assert len(env.stubs.mds.messages) == 1
    assert not any(header for header in remove_headers
                   if header.encode().lower() in env.stubs.mds.messages[0].lower())


def make_500(*args, **kwargs):
    raise RuntimeError()


async def test_500_on_mds_error(rcpt, client_impl, http_client, store_info, message):
    with patch.object(mds, "make_stid") as patched:
        patched.return_value = make_500
        status, body = await client_impl(http_client, rcpt, store_info, message)

    assert status == 500
    assert body["code"] == "internal_server_error"


async def test_500_on_nsls_error(rcpt, client_impl, http_client, store_info, message):
    with patch.object(nsls, "make_store_resp") as patched:
        patched.return_value = make_500
        status, body = await client_impl(http_client, rcpt, store_info, message)

    assert status == 500
    assert body["code"] == "store_error"


async def test_safely_delete_stids_on_nsls_error(env: Env, rcpt, client_impl, http_client,
                                                 store_info, message):
    stid = mds.make_stid(rcpt.uid)
    with patch.object(mds, "make_stid") as patched:
        patched.return_value = stid
        with patch.object(nsls, "make_store_resp") as patched:
            patched.side_effect = make_500
            await client_impl(http_client, rcpt, store_info, message)

    line_found = await env.nwsmtp.wait_for_str_in_log("nwsmtp", f"should delete stid = {stid}")
    assert line_found, "Special write to log for safely_delete_stids was not found"


async def test_413_on_big_message(env: Env, sender, rcpt, client_impl, store_info, http_client):
    max_post_size = env.conf.web_server.max_post_size + 1
    _, message = make_message(sender, rcpt, text="A" * max_post_size)
    status, body = await client_impl(http_client, rcpt, store_info, message)
    assert status == 413


async def test_lf_as_delimiter(env: Env, rcpt, client_impl, store_info, http_client):
    """Test that we replace LF as delimiter with CRLF
        ..see: https://tools.ietf.org/html/rfc2822#section-2.2
    """
    message = "Header: Value\nHeader: Value\n\nBody"
    status, body = await client_impl(http_client, rcpt, store_info, message)
    assert status == 200
    assert not [i for i in env.stubs.mds.messages[0].split(b"\r\n") if b"\n" in i]


async def test_lf_as_delimiter_and_folding_header(env: Env, rcpt, client_impl, store_info,
                                                  http_client):
    """Test that we do not touch LF in folding header
        ..see: https://tools.ietf.org/html/rfc2822#section-2.2.3
    """
    message = "Header: Value1\n\tValue2\nHeader3: Value\n\nBody"
    status, body = await client_impl(http_client, rcpt, store_info, message)
    assert status == 200
    assert b"Header: Value1\n\tValue2\r\n" in env.stubs.mds.messages[0]


def enable_tvm_for_mds(conf):
    """ We have to override tvm configuration for HOST because of MAILDLV-3342"""
    conf.modules.mds_client.configuration.tvm.use = True
    conf.tvm.destinations = [{
        "id": 2000273,
        "name": "mulcagate",
        "host": HOST
    }]


async def test_going_to_mds_with_tvm_ticket(cluster, users, rcpt, client_impl, store_info, message):
    with make_conf(cluster, customize_with=enable_tvm_for_mds) as conf:
        async with make_env(cluster, users, conf) as env:
            http_client = env.nwsmtp.get_http_client()
            status, body = await client_impl(http_client, rcpt, store_info, message)

            assert status == 200
            assert len(env.stubs.mds.requests) > 0
            for request in env.stubs.mds.requests:
                assert "X-Ya-Service-Ticket" in request.headers


async def test_required_fields_for_sync_delivery(env: Env, rcpt, client_impl, http_client,
                                                 store_info, message):
    status, body = await client_impl(http_client, rcpt, store_info, message)
    assert status == 200

    req = env.stubs.nsls.requests[0]

    assert req["envelope"]["mail_from"]["email"] == "<>"

    assert req["message"]["hints"][0]["sync_dlv"] == ["1"]
    assert req["message"]["hints"][0]["no_such_fid_fail"] == ["1"]

    assert len(req["recipients"]) == 1
    assert req["recipients"][0]["email"] == rcpt.email
    assert req["recipients"][0]["is_local"] == "yes"
    assert req["recipients"][0]["notify"] == {"delay": False, "failure": False, "success": False}
