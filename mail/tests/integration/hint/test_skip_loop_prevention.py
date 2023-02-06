from mail.notsolitesrv.tests.integration.lib.util.headers import Header
from mail.notsolitesrv.tests.integration.lib.util.hint import make_hint_value
from mail.notsolitesrv.tests.integration.lib.util.message import make_message
from mail.notsolitesrv.tests.integration.lib.util.responses import NslsResponses
from mail.notsolitesrv.tests.integration.lib.util.user import DEFAULT_RCPT_0, make_users

from mail.notsolitesrv.tests.integration.lib.expectation.blackbox.mock import Blackbox
from mail.notsolitesrv.tests.integration.lib.expectation.furita.mock import Furita
from mail.notsolitesrv.tests.integration.lib.expectation.mdbsave.mock import MdbSave
from mail.notsolitesrv.tests.integration.lib.expectation.mds.mock import Mds
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.mock import Tupita
from mail.notsolitesrv.tests.integration.lib.expectation.mds.request import __MESSAGES_STORAGE__


def test_when_skip_loop_prevention_off_will_add_forward_header_than_loop_detected_will_not_store_msg_and_response_ok(context):
    rcpts = [DEFAULT_RCPT_0]
    hint_value = make_hint_value(skip_loop_prevention=0)
    msg_id, msg = make_message(headers={Header.X_YANDEX_HINT: hint_value})
    users = make_users(rcpts)

    Blackbox.expect_call_success(context, users[rcpts[0]])
    Furita.expect_blackwhitelist_call_success(context, users[rcpts[0]])
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_existing_headers=[Header.X_YANDEX_FORWARD],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_list_call_success(context=context, uid=users[rcpt].uid)
        Tupita.expect_check_call_success(context=context, uid=users[rcpt].uid)
    MdbSave.expect_call_success(context=context, users=users)

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error

    rec_msg = __MESSAGES_STORAGE__[msg_id]
    msg_id, msg = make_message(headers={Header.X_YANDEX_HINT: hint_value, Header.X_YANDEX_FORWARD: rec_msg[Header.X_YANDEX_FORWARD]})
    Blackbox.expect_call_success(context, users[rcpts[0]])
    Furita.expect_blackwhitelist_call_success(context, users[rcpts[0]])

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error


def test_when_skip_loop_prevention_on_will_not_add_forward_header(context):
    rcpts = [DEFAULT_RCPT_0]
    hint_value = make_hint_value(skip_loop_prevention=1)
    msg_id, msg = make_message(headers={Header.X_YANDEX_HINT: hint_value})
    users = make_users(rcpts)

    Blackbox.expect_call_success(context, users[rcpts[0]])
    Furita.expect_blackwhitelist_call_success(context, users[rcpts[0]])
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_not_existing_headers=[Header.X_YANDEX_FORWARD],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_list_call_success(context=context, uid=users[rcpt].uid)
        Tupita.expect_check_call_success(context=context, uid=users[rcpt].uid)
    MdbSave.expect_call_success(context=context, users=users)

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error


def test_when_skip_loop_prevention_on_and_loop_detected_will_store_msg(context):
    rcpts = [DEFAULT_RCPT_0]
    hint_value = make_hint_value(skip_loop_prevention=0)
    msg_id, msg = make_message(headers={Header.X_YANDEX_HINT: hint_value})
    users = make_users(rcpts)

    Blackbox.expect_call_success(context, users[rcpts[0]])
    Furita.expect_blackwhitelist_call_success(context, users[rcpts[0]])
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_existing_headers=[Header.X_YANDEX_FORWARD],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_list_call_success(context=context, uid=users[rcpt].uid)
        Tupita.expect_check_call_success(context=context, uid=users[rcpt].uid)
    MdbSave.expect_call_success(context=context, users=users)

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error

    rec_msg = __MESSAGES_STORAGE__[msg_id]
    hint_value = make_hint_value(skip_loop_prevention=1)
    msg_id, msg = make_message(headers={Header.X_YANDEX_HINT: hint_value, Header.X_YANDEX_FORWARD: rec_msg[Header.X_YANDEX_FORWARD]})
    Blackbox.expect_call_success(context, users[rcpts[0]])
    Furita.expect_blackwhitelist_call_success(context, users[rcpts[0]])
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_existing_headers=[Header.X_YANDEX_FORWARD],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_list_call_success(context=context, uid=users[rcpt].uid)
        Tupita.expect_check_call_success(context=context, uid=users[rcpt].uid)
    MdbSave.expect_call_success(context=context, users=users)

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error
