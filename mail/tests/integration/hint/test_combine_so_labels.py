from hamcrest import contains_inanyorder, has_entries, has_entry, only_contains
from multidict import MultiDict

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
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.mock import Tupita


def test_mapping_personal_so_labels_to_mdb_labels(context):
    rcpts = [DEFAULT_RCPT_0, DEFAULT_RCPT_1]
    users = make_users(rcpts)
    headers = MultiDict([
        (Header.X_YANDEX_HINT, make_hint_value(
            fid=4,
            label=[Label.GREETING.FullAlias, Label.PROMO.FullAlias]
        )),
        (Header.X_YANDEX_HINT, make_hint_value(
            email=rcpts[0],
            replace_so_labels=1,
            label=[Label.PF_HAM.FullAlias, Label.PERSONAL.FullAlias]
        )),
        (Header.X_YANDEX_HINT, make_hint_value(
            email=rcpts[1],
            replace_so_labels=1,
            label=[Label.PF_SPAM.FullAlias]
        )),
    ])

    msg_id, msg = make_message(rcpts=rcpts, headers=headers)

    for rcpt in rcpts:
        Blackbox.expect_call_success(context, users[rcpt])
        Furita.expect_blackwhitelist_call_success(context, users[rcpt])
        users[rcpt].is_shared_stid = True

    Mds.expect_put_call_success(
        context=context, user=users[rcpts[0]], expected_equal_headers={Header.MESSAGE_ID: msg_id}
    )

    for rcpt in rcpts:
        Furita.expect_list_call_success(context=context, uid=users[rcpt].uid)
        Tupita.expect_check_call_success(context=context, uid=users[rcpt].uid)
    MdbSave.expect_call_success(context=context, users=users, request_body_matcher=as_json(
        has_entry("rcpts", only_contains(
            has_entry("rcpt", has_entries(
                user=has_entry("uid", str(users[rcpts[0]].uid)),
                message=has_entry("labels", contains_inanyorder(
                    has_entry("name", Label.PERSONAL.Name),
                    has_entry("name", Label.PF_HAM.Name)
                ))
            )),
            has_entry("rcpt", has_entries(
                user=has_entry("uid", str(users[rcpts[1]].uid)),
                message=has_entry("labels", contains_inanyorder(
                    has_entry("name", Label.PF_SPAM.Name)
                ))
            ))
        ))
    ))

    result = context.nsls.send_message(msg)
    context.pyremock.assert_expectations()
    code, error = result["all"]
    assert NslsResponses.OK.code == code
    assert NslsResponses.OK.error in error
