from hamcrest import (
    has_entries,
    has_entry,
    only_contains
)
from mail.notsolitesrv.tests.integration.lib.util.headers import Header
from mail.notsolitesrv.tests.integration.lib.util.message import make_message
from mail.notsolitesrv.tests.integration.lib.util.responses import NslsResponses
from mail.notsolitesrv.tests.integration.lib.util.user import (
    DEFAULT_RCPT_0,
    DEFAULT_RCPT_1,
    make_users
)
from mail.notsolitesrv.tests.integration.lib.expectation.blackbox.mock import Blackbox
from mail.notsolitesrv.tests.integration.lib.expectation.furita.mock import Furita
from mail.notsolitesrv.tests.integration.lib.expectation.furita.response import FORWARD_TO_EMAIL
from mail.notsolitesrv.tests.integration.lib.expectation.matchers import as_json
from mail.notsolitesrv.tests.integration.lib.expectation.mdbsave.mock import MdbSave
from mail.notsolitesrv.tests.integration.lib.expectation.mds.mock import Mds
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.mock import Tupita


def test_success_with_drop_and_forward(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    msg_id, mimetext_message = make_message(rcpts=rcpts)
    users = make_users(rcpts=rcpts)
    users[rcpts[0]].org_id="0"
    users[rcpts[1]].org_id="1"
    user_without_drop_rule_uids = [user.uid for rcpt, user in users.items() if rcpt != DEFAULT_RCPT_0]

    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
        Furita.expect_blackwhitelist_call_success(context, users[rcpt])
        users[rcpt].is_shared_stid = True
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_get_call_success(context=context, org_id=users[rcpt].org_id)
        Tupita.expect_check_call_success(context=context, uid=users[rcpt].org_id, matched_queries=["0"])
    for uid in user_without_drop_rule_uids:
        Furita.expect_list_call_success(context=context, uid=uid)
        Tupita.expect_check_call_success(context=context, uid=uid)
    MdbSave.expect_call_success(context=context, users=users, request_body_matcher=as_json(
        has_entry("rcpts", only_contains(
            has_entry("rcpt", has_entries({
                "user": has_entry("uid", str(users[rcpts[0]].uid)),
                "folders": has_entry("original", has_entry("path", has_entry("path", "\\inbox"))),
                "actions": has_entries({
                    "use_filters": False,
                    "original": has_entry("store_as_deleted", True)
                })
            })),
            has_entry("rcpt", has_entries({
                "user": has_entry("uid", str(users[rcpts[1]].uid)),
                "actions": has_entries({
                    "use_filters": True,
                    "original": has_entry("store_as_deleted", False)
                })
            }))
        ))
    ))

    result = context.nsls.send_message(mimetext_message)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert code == NslsResponses.OK.code
    assert NslsResponses.OK.error in error
    relay_messages = context.relay.storage.get_messages(msg_id)
    assert relay_messages is not None
    assert len(relay_messages) == 1
    assert relay_messages[0].envelope.rcpt_tos == [FORWARD_TO_EMAIL]
    assert relay_messages[0].envelope.mail_from == "domain_forward@ya.ru"


def test_when_furita_returns_error_for_all_domain_rule_requests_meta_is_sent_and_nsls_returns_250(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    msg_id, mimetext_message = make_message(rcpts=rcpts)
    users = make_users(rcpts=rcpts)
    users[rcpts[0]].org_id="0"
    users[rcpts[1]].org_id="1"

    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
        Furita.expect_blackwhitelist_call_success(context, users[rcpt])
        users[rcpt].is_shared_stid = True
    Mds.expect_put_call_success(
        context=context,
        user=users[rcpts[0]],
        expected_equal_headers={Header.MESSAGE_ID: msg_id})
    for rcpt in rcpts:
        Furita.expect_get_call_server_error(context=context, org_id=users[rcpt].org_id, times=2)

    result = context.nsls.send_message(mimetext_message)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert code == NslsResponses.OK.code
    assert NslsResponses.OK.error in error
    relay_messages = context.relay.storage.get_messages(msg_id)
    assert relay_messages is not None
    assert len(relay_messages) == 2
    assert sorted(relay_messages[0].envelope.rcpt_tos + relay_messages[1].envelope.rcpt_tos) == sorted(rcpts)
