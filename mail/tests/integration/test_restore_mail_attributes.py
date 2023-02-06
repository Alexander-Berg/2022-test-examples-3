import pytest
import time
from hamcrest import (
    assert_that,
    equal_to,
)

from tests_common.mdb import user_connection
from pymdb.operations import DeleteMessages, PurgeDeletedMessages
from pymdb.vegetarian import fill_messages_in_folder, SAMPLE_STIDS, SAMPLE_WINDAT_STIDS
from mail.barbet.devpack.components.barbet import BarbetDevpack as Barbet

from .conftest import create_user, random_mix, get_folder_by_type


def wait_for_complete(barbet_api, uid, task_type, times=30):
    is_complete = lambda resp: resp and resp.status_code == 200 \
        and task_type in resp.json() and 'state' in resp.json()[task_type] \
        and resp.json()[task_type]['state'] == 'complete'

    status = None
    for _ in range(times):
        status = barbet_api.status(uid)
        if is_complete(status):
            return
        time.sleep(1)
    assert is_complete(status)


def set_messages_attr(context, uid, mids, attr='mulca-shared'):
    context.maildb.execute('''
                UPDATE mail.messages
                   SET attributes = attributes || %(attr)s::mail.message_attributes
                 WHERE uid = %(uid)s
                   AND mid = ANY(%(mids)s)
            ''', attr=attr, uid=uid, mids=mids)


def get_messages_with_attr_count(context, uid, attr='mulca-shared'):
    res = context.maildb.query('''
                SELECT COUNT(*)
                  FROM mail.messages
                 WHERE uid = %(uid)s
                   AND %(attr)s::mail.message_attributes = ANY(attributes)
            ''', uid=uid, attr=attr)
    return res[0][0]


def get_mailbox_count(context, uid):
    res = context.maildb.query('''
                SELECT COUNT(*)
                  FROM mail.box
                 WHERE uid = %(uid)s
            ''', uid=uid)
    return res[0][0]


def create_user_settings(context, uid):
    context.maildb.execute('''
                SELECT code.create_settings(
                    i_uid => %(uid)s,
                    i_value => '{"single_settings": {"show_folders_tabs": ""}}'::json
                )
            ''', uid=uid)


@pytest.mark.parametrize(
    "messages_count",
    [4, 2],
)
@pytest.mark.parametrize(
    "attr, mulca_count",
    [('mulca-shared', 2), ('synced', 0)],
)
def test_restore_mulcashared_attribute(context, messages_count, mulca_count, attr):
    barbet = context.barbet.components[Barbet]
    uid = create_user(context.barbet)
    barbet_api = barbet.api(suid=uid, mdb='pg', uid=uid)
    mulca_count = min(mulca_count, messages_count)

    expected_msg_count = messages_count - mulca_count
    stids = random_mix(SAMPLE_WINDAT_STIDS, SAMPLE_STIDS, expected_msg_count, mulca_count)

    with user_connection(context, uid) as conn:
        inbox = get_folder_by_type(context, uid, 'inbox')
        barbet_api.update_settings(uid, fids=inbox.fid)
        mids = fill_messages_in_folder(conn, uid, inbox, limit=messages_count, stids=stids)

        set_messages_attr(context, uid, mids, attr)
        create_user_settings(context, uid)

        barbet_api.create(uid)
        wait_for_complete(barbet_api, uid, 'primary')

        DeleteMessages(conn, uid)(mids).commit()
        PurgeDeletedMessages(conn, uid)(mids).commit()

        mids_count = get_mailbox_count(context, uid)
        assert_that(mids_count, equal_to(0))

        mulca_shared_mids_count = get_messages_with_attr_count(context, uid, attr)
        assert_that(mulca_shared_mids_count, equal_to(0))

        barbet_api.restore(uid, method='full_hierarchy')
        wait_for_complete(barbet_api, uid, 'restore')

        mids_count = get_mailbox_count(context, uid)
        assert_that(mids_count, equal_to(expected_msg_count))

        mulca_shared_mids_count = get_messages_with_attr_count(context, uid, attr)
        assert_that(mulca_shared_mids_count, equal_to(expected_msg_count))

        inbox = get_folder_by_type(context, uid, 'inbox')
        assert_that(inbox.message_seen, equal_to(0))
        assert_that(inbox.message_count, equal_to(expected_msg_count))
