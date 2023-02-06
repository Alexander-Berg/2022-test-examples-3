from hamcrest import (
    assert_that,
    has_entries,
    has_entry,
    has_items
)

from mail.notsolitesrv.tests.integration.lib.util.headers import Header
from mail.notsolitesrv.tests.integration.lib.util.hint import make_hint_value
from mail.notsolitesrv.tests.integration.lib.util.message import (
    make_http_message,
    make_message,
    make_stid,
    make_stid_prefix
)
from mail.notsolitesrv.tests.integration.lib.util.responses import NslsResponses
from mail.notsolitesrv.tests.integration.lib.util.user import (
    DEFAULT_RCPT_0,
    DEFAULT_RCPT_1,
    DEFAULT_SENDER,
    make_users
)
from mail.notsolitesrv.tests.integration.lib.expectation.blackbox.mock import Blackbox
from mail.notsolitesrv.tests.integration.lib.expectation.furita.mock import Furita
from mail.notsolitesrv.tests.integration.lib.expectation.furita.response import FORWARD_TO_EMAIL
from mail.notsolitesrv.tests.integration.lib.expectation.mdbsave.mock import MdbSave
from mail.notsolitesrv.tests.integration.lib.expectation.mds.mock import Mds
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.mock import Tupita


def test_success(context):
    rcpts = [DEFAULT_SENDER, DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    hint = make_hint_value(email=DEFAULT_SENDER, save_to_sent=1, phone="0123456789")
    msg_id, msg = make_message(rcpts=rcpts, headers={Header.X_YANDEX_HINT: hint})
    users = make_users(rcpts)
    user_without_sender_uids = [user.uid for rcpt, user in users.items() if rcpt != DEFAULT_SENDER]

    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
        Furita.expect_blackwhitelist_call_success(context, users[rcpt])
        users[rcpt].is_shared_stid = True
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[1]],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for uid in user_without_sender_uids:
        Furita.expect_list_call_success(context=context, uid=uid)
        Tupita.expect_check_call_success(context=context, uid=uid)
    MdbSave.expect_call_success(context=context, users=users)

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error


def test_success_via_http_with_forward(context):
    rcpts = [DEFAULT_RCPT_0]
    msg_id, mimetext_message = make_message(rcpts=rcpts)
    users = make_users(rcpts=rcpts)
    rcpt = rcpts[0]
    user = users[rcpt]
    user.is_mailish = True
    stid = make_stid(make_stid_prefix(user.uid, user.is_shared_stid))

    Mds.expect_get_call_success(context=context, stid=stid, message=mimetext_message.as_string())
    Blackbox.expect_call_success(context, user)
    Furita.expect_list_call_success(context=context, uid=user.uid)
    Tupita.expect_check_call_success(context=context, uid=user.uid, matched_queries=["id"])
    MdbSave.expect_call_success(context=context, users=users)
    Mds.expect_put_call_success(
        context=context,
        user=user,
        unit_type=None,
        ns=b"mail-tmp",
        stid_prefix="mail:",
        expected_equal_headers={Header.MESSAGE_ID: msg_id})

    http_message = make_http_message(stid, user, mimetext_message)
    result = context.nsls.send_http_message(http_message)
    context.pyremock.assert_expectations()
    assert result.status_code == NslsResponses.HTTP_OK.code
    nsls_response = result.json()
    assert_that(nsls_response, has_entry("recipients", has_items(
        has_entries({"status": "stored", "is_local": "yes", "email": rcpt}),
        has_entries({"status": "new", "email": FORWARD_TO_EMAIL})
    )))
    assert context.relay.storage.get_messages(msg_id) is not None


def test_when_blackbox_return_error_nsls_will_return_451(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    msg_id, msg = make_message(rcpts=rcpts)
    users = make_users(rcpts)
    Blackbox.expect_call_server_error(context, users[DEFAULT_RCPT_0])
    Blackbox.expect_call_server_error(context, users[DEFAULT_RCPT_1])

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.ERR_451.code == code
    assert NslsResponses.ERR_451.error in error
    parts = error.split(b";")
    assert len(parts) == 3
    assert parts[2] == b" internal error"


def test_when_furita_return_error_nsls_will_return_451(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    msg_id, msg = make_message(rcpts=rcpts)
    users = make_users(rcpts)
    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
    Furita.expect_blackwhitelist_call_server_error(context, users[DEFAULT_RCPT_0])
    Furita.expect_blackwhitelist_call_server_error(context, users[DEFAULT_RCPT_1])

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.ERR_451.code == code
    assert NslsResponses.ERR_451.error in error
    parts = error.split(b";")
    assert len(parts) == 3
    assert parts[2] == b" black/white list error"


def test_when_mds_return_error_nsls_will_return_451(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    msg_id, msg = make_message(rcpts=rcpts)
    users = make_users(rcpts)
    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
        Furita.expect_blackwhitelist_call_success(context, users[rcpt])
        users[rcpt].is_shared_stid = True
    Mds.expect_put_call_server_error(context=context, user=users[rcpts[0]])

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.ERR_451.code == code
    assert NslsResponses.ERR_451.error in error
    parts = error.split(b";")
    assert len(parts) == 3
    assert parts[2] == b" storage error"


def test_when_meta_save_op_returns_error_meta_is_sent_and_nsls_returns_250(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    msg_id, mimetext_message = make_message(rcpts=rcpts)
    users = make_users(rcpts)
    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
        Furita.expect_blackwhitelist_call_success(context, users[rcpt])
        users[rcpt].is_shared_stid = True

    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_list_call_success(context=context, uid=users[rcpt].uid)
        Tupita.expect_check_call_success(context=context, uid=users[rcpt].uid)
    MdbSave.expect_call_server_error(context=context, users=users, times=2)

    result = context.nsls.send_message(mimetext_message)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert code == NslsResponses.OK.code
    assert NslsResponses.OK.error in error
    relay_messages = context.relay.storage.get_messages(msg_id)
    assert relay_messages is not None
    assert len(relay_messages) == 2
    assert sorted(relay_messages[0].envelope.rcpt_tos + relay_messages[1].envelope.rcpt_tos) == sorted(rcpts)


def test_when_furita_returns_error_for_all_users_meta_is_sent_and_nsls_returns_250(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    msg_id, mimetext_message = make_message(rcpts=rcpts)
    users = make_users(rcpts=rcpts)

    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
        Furita.expect_blackwhitelist_call_success(context, users[rcpt])
        users[rcpt].is_shared_stid = True
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_list_call_server_error(context=context, uid=users[rcpt].uid, times=2)

    result = context.nsls.send_message(mimetext_message)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert code == NslsResponses.OK.code
    assert NslsResponses.OK.error in error
    relay_messages = context.relay.storage.get_messages(msg_id)
    assert relay_messages is not None
    assert len(relay_messages) == 2
    assert sorted(relay_messages[0].envelope.rcpt_tos + relay_messages[1].envelope.rcpt_tos) == sorted(rcpts)
