import pytest

from hamcrest import (
    anything,
    assert_that,
    equal_to,
    has_entries,
    has_item,
    is_not,
)

from tests_common.user import make_user_oneline
from mail.mops.devpack.components.application import Mops
from mail.devpack.lib.components.mdb import Mdb

from .conftest import (
    fill_folder_with_messages,
    wait_for_completion_all_async_tasks,
    create_user,
    get_message_count_in_folder,
    get_fid,
)


def get_or_create_hidden_trash_fid(mops_api):
    response = mops_api.create_hidden_trash_folder()
    assert_that(response.status_code, equal_to(200), response.text)
    assert_that(response.json(), has_entries(fid=anything()), response.text)
    return int(response.json()['fid'])


def test_mops_create_hidden_trash_folder(mops_coordinator):
    mdb = mops_coordinator.components[Mdb]
    mops = mops_coordinator.components[Mops]

    uid = create_user(mops_coordinator)
    mops_api = mops.api(uid=uid)

    fid = get_or_create_hidden_trash_fid(mops_api)

    folders = mdb.query('''
        SELECT fid
          FROM mail.folders
         WHERE uid = %(uid)s and type = 'hidden_trash'
    ''', uid=uid)
    assert_that(list(folders), has_item((fid,)))


def test_mops_create_hidden_trash_folder_several_times(mops_coordinator):
    mops = mops_coordinator.components[Mops]

    uid = create_user(mops_coordinator)
    mops_api = mops.api(uid=uid)

    fid = get_or_create_hidden_trash_fid(mops_api)

    response = mops_api.create_hidden_trash_folder()
    assert_that(response.status_code, equal_to(200), response.text)
    assert_that(response.json(), has_entries(fid=anything()), response.text)
    fid2 = int(response.json()['fid'])

    assert fid == fid2


def test_mops_purge_hidden_trash_if_user_does_not_have_hidden_trash(context):
    uid = make_user_oneline(context).uid
    mops_api = context.mops.api(uid=uid)

    folders = context.maildb.query('''
        SELECT type
          FROM mail.folders
         WHERE uid = %(uid)s
    ''', uid=uid)
    assert_that(list(folders), is_not(has_item(('hidden_trash',))))  # consider removing the entire test case if 'hidden_trash' became a default folder

    response = mops_api.purge_hidden_trash()
    assert_that(response.status_code, equal_to(400), response.text)


def test_mops_if_admin_search_setting_is_set_we_cannot_purge_folder_or_it_will_break_admin_search(context):
    uid = make_user_oneline(context).uid
    mops_api = context.mops.api(uid=uid)

    fid = get_or_create_hidden_trash_fid(mops_api)

    MESSAGE_COUNT = 10
    fill_folder_with_messages(context, uid, 'hidden_trash', MESSAGE_COUNT)
    assert_that(get_message_count_in_folder(context, fid=fid), equal_to(MESSAGE_COUNT))

    context.maildb.execute('''
        SELECT code.update_settings(i_uid => %(uid)s, i_settings => '{"single_settings": {
            "mail_b2b_admin_search_enabled": "on"
        }}'::json)''', uid=uid)

    response = mops_api.purge_hidden_trash()
    assert_that(response.status_code, equal_to(200), response.text)
    wait_for_completion_all_async_tasks(context, sec=300)
    assert_that(get_fid(context, 'hidden_trash'), equal_to(fid))
    assert_that(get_message_count_in_folder(context, fid=fid), equal_to(MESSAGE_COUNT))


@pytest.mark.parametrize("setting", [
    ('"mail_b2c_can_use_hidden_trash": "on", "hidden_trash_enabled": "on"'),
    (''),
])
def test_mops_if_hidden_trash_setting_or_nothing_is_set_we_can_purge_because_of_frontend_first_delete_folder_and_than_remove_setting(context, setting):
    uid = make_user_oneline(context).uid
    mops_api = context.mops.api(uid=uid)

    fid = get_or_create_hidden_trash_fid(mops_api)

    MESSAGE_COUNT = 10
    fill_folder_with_messages(context, uid, 'hidden_trash', MESSAGE_COUNT)
    assert_that(get_message_count_in_folder(context, fid=fid), equal_to(MESSAGE_COUNT))

    context.maildb.execute(f'''
        SELECT code.update_settings(i_uid => {uid}, i_settings => '{{"single_settings":
            {{ {setting} }}
        }}'::json)''')

    response = mops_api.purge_hidden_trash()
    assert_that(response.status_code, equal_to(200), response.text)
    wait_for_completion_all_async_tasks(context, sec=300)
    assert_that(get_fid(context, 'hidden_trash'), equal_to(fid))
    assert_that(get_message_count_in_folder(context, fid=fid), equal_to(0))
