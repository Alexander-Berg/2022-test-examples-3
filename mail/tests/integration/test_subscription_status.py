from hamcrest import has_entries, has_entry, only_contains

from mail.notsolitesrv.tests.integration.lib.util.headers import Header
from mail.notsolitesrv.tests.integration.lib.util.hint import make_hint_value
from mail.notsolitesrv.tests.integration.lib.util.labels import Label
from mail.notsolitesrv.tests.integration.lib.util.message import make_message
from mail.notsolitesrv.tests.integration.lib.util.responses import NslsResponses
from mail.notsolitesrv.tests.integration.lib.util.user import make_users, DEFAULT_RCPT_0, DEFAULT_RCPT_1

from mail.notsolitesrv.tests.integration.lib.expectation.blackbox.mock import Blackbox
from mail.notsolitesrv.tests.integration.lib.expectation.furita.mock import Furita
from mail.notsolitesrv.tests.integration.lib.expectation.matchers import as_json
from mail.notsolitesrv.tests.integration.lib.expectation.mdbsave.mock import MdbSave
from mail.notsolitesrv.tests.integration.lib.expectation.mds.mock import Mds
from mail.notsolitesrv.tests.integration.lib.expectation.msearch.mock import MSearch
from mail.notsolitesrv.tests.integration.lib.expectation.msettings.mock import MSettings
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.mock import Tupita


def test_set_delivery_to_trash_and_disable_filters_for_users_with_hidden_subscriptions(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    users = make_users(rcpts)
    user_with_filters_in_use_uids = [user.uid for rcpt, user in users.items() if rcpt != DEFAULT_RCPT_1]
    subscription = "subscription@some-domain.ru"
    msg_id, msg = make_message(
        sender=subscription,
        rcpts=rcpts,
        headers={Header.X_YANDEX_HINT: make_hint_value(label=[Label.MT_NEWS.FullAlias])}
    )

    user0 = users[rcpts[0]]
    user1 = users[rcpts[1]]

    for user in [user0, user1]:
        Blackbox.expect_call_success(context, user)
        Furita.expect_blackwhitelist_call_success(context, user)
        user.is_shared_stid = True

    Mds.expect_put_call_success(
        context=context, user=user0, expected_equal_headers={Header.MESSAGE_ID: msg_id}
    )

    MSettings.expect_call_success(context=context, uid=user0.uid)
    MSettings.expect_call_success(context=context, uid=user1.uid)

    MSearch.expect_call_success(context=context, expected_statuses=[
        dict(uid=user0.uid, email=subscription, status="active"),
        dict(uid=user1.uid, email=subscription, status="hidden")
    ])

    for uid in user_with_filters_in_use_uids:
        Furita.expect_list_call_success(context=context, uid=uid)
        Tupita.expect_check_call_success(context=context, uid=uid)
    MdbSave.expect_call_success(context=context, users=users, request_body_matcher=as_json(
        has_entry("rcpts", only_contains(
            has_entry("rcpt", has_entries(
                user=has_entry("uid", str(user0.uid)),
                folders=has_entry("original", has_entry("path", has_entry("path", "\\Inbox")))
            )),
            has_entry("rcpt", has_entries(
                user=has_entry("uid", str(user1.uid)),
                folders=has_entry("original", has_entry("path", has_entry("path", "\\Trash")))
            ))
        ))
    ))

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error


def test_set_delivery_to_pending_for_users_with_opt_in_subscriptions(context):
    rcpts = [DEFAULT_RCPT_0]
    users = make_users(rcpts)
    subscription = "subscription@some-domain.ru"
    msg_id, msg = make_message(
        sender=subscription,
        rcpts=rcpts,
        headers={Header.X_YANDEX_HINT: make_hint_value(label=[Label.MT_NEWS.FullAlias])}
    )

    user0 = users[rcpts[0]]

    Blackbox.expect_call_success(context, user0)
    Furita.expect_blackwhitelist_call_success(context, user0)

    Mds.expect_put_call_success(
        context=context, user=user0, expected_equal_headers={Header.MESSAGE_ID: msg_id}
    )

    MSettings.expect_call_success(
        context=context,
        uid=user0.uid,
        expected_settings={
            "mail_b2c_can_use_opt_in_subs": "on",
            "opt_in_subs_enabled": "on"
        }
    )

    MSearch.expect_call_success(
        context=context,
        expected_statuses=[dict(uid=user0.uid, email=subscription, status="pending")],
        opt_in_uids=[user0.uid]
    )

    MdbSave.expect_call_success(context=context, users=users, request_body_matcher=as_json(
        has_entry("rcpts", only_contains(
            has_entry("rcpt", has_entries(
                user=has_entry("uid", str(user0.uid)),
                folders=has_entry("original", has_entry("path", has_entry("path", "\\Pending"))),
                actions=has_entries({
                    "use_filters": False,
                    "original": has_entry("no_such_folder", "create")
                })
            ))
        ))
    ))

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error


def test_when_msettings_return_error_nsls_will_return_451(context):
    rcpts = [DEFAULT_RCPT_0]
    users = make_users(rcpts)
    subscription = "subscription@some-domain.ru"
    msg_id, msg = make_message(
        sender=subscription,
        rcpts=rcpts,
        headers={Header.X_YANDEX_HINT: make_hint_value(label=[Label.MT_NEWS.FullAlias])}
    )

    user0 = users[rcpts[0]]

    Blackbox.expect_call_success(context, user0)
    Furita.expect_blackwhitelist_call_success(context, user0)

    Mds.expect_put_call_success(
        context=context, user=user0, expected_equal_headers={Header.MESSAGE_ID: msg_id}
    )

    MSettings.expect_call_server_error(context=context, uid=user0.uid)

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.ERR_451.code == code
    assert NslsResponses.ERR_451.error in error


def test_when_msearch_return_error_nsls_will_return_451(context):
    rcpts = [DEFAULT_RCPT_0]
    users = make_users(rcpts)
    subscription = "subscription@some-domain.ru"
    msg_id, msg = make_message(
        sender=subscription,
        rcpts=rcpts,
        headers={Header.X_YANDEX_HINT: make_hint_value(label=[Label.MT_NEWS.FullAlias])}
    )

    user0 = users[rcpts[0]]

    Blackbox.expect_call_success(context, user0)
    Furita.expect_blackwhitelist_call_success(context, user0)

    Mds.expect_put_call_success(
        context=context, user=user0, expected_equal_headers={Header.MESSAGE_ID: msg_id}
    )

    MSettings.expect_call_success(context=context, uid=user0.uid)

    MSearch.expect_call_server_error(context=context, uid=user0.uid, email=subscription)

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.ERR_451.code == code
    assert NslsResponses.ERR_451.error in error
