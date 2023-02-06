import pytest

from hamcrest import (
    anything,
    assert_that,
    equal_to,
    has_entries,
)

from tests_common.user import make_user_oneline
from mail.mops.devpack.components.application import Mops
from mail.devpack.lib.components.mdb import Mdb

from .conftest import (
    fill_folder_with_messages,
    wait_for_completion_all_async_tasks,
    create_user,
    get_message_count_in_folder,
    get_mids_in_folder,
    get_fid,
    generate_name
)


def test_mops_ping(mops_coordinator):
    mops = mops_coordinator.components[Mops]
    response = mops.ping()
    assert response.status_code == 200
    assert response.text == 'pong'


@pytest.mark.parametrize('symbol, type', (
    (None, 'user'),
    ('reply_later', 'reply_later'),
    ('restored', 'restored')
))
def test_mops_create_folder(mops_coordinator, symbol, type):
    mdb = mops_coordinator.components[Mdb]
    mops = mops_coordinator.components[Mops]

    uid = create_user(mops_coordinator)
    mops_api = mops.api(uid=uid)

    folder_name = generate_name()
    response = mops_api.create_folder(name=folder_name, symbol=symbol)
    assert_that(response.status_code, equal_to(200), response.text)
    assert_that(response.json(), has_entries(fid=anything()), response.text)
    fid = int(response.json()['fid'])

    folders = mdb.query('''
        SELECT fid, name
          FROM mail.folders
         WHERE uid = %(uid)s AND fid = %(fid)s AND type = %(type)s
    ''', uid=uid, fid=fid, type=type)
    assert_that(len(folders), equal_to(1))
    assert_that(folders[0], equal_to((fid, folder_name)))


@pytest.mark.parametrize('symbol', ('reply_later', 'restored', ))
def test_mops_forbid_update_folder_symbol(mops_coordinator, symbol):
    mops = mops_coordinator.components[Mops]

    uid = create_user(mops_coordinator)
    mops_api = mops.api(uid=uid)

    folder_name = generate_name()
    response = mops_api.create_folder(folder_name, symbol=symbol)
    assert_that(response.status_code, equal_to(200), response.text)
    assert_that(response.json(), has_entries(fid=anything()), response.text)
    fid = int(response.json()['fid'])

    response = mops_api.update_folder_symbol(fid=fid, symbol=symbol)
    assert_that(response.status_code, equal_to(400), response.text)
    assert_that(response.json(), has_entries(error=anything()), response.text)
    responseError = "macs::error::Category code: 10"
    assert_that(responseError in response.json()['error'], equal_to(True))


@pytest.mark.parametrize("folder_type,mode,trash_expected,hidden_trash_expected", [
    ('inbox', 'mids', True, False),
    ('inbox', 'fid', True, False),
    ('trash', 'mids', False, True),
    ('trash', 'fid', False, True),
    ('hidden_trash', 'mids', False, False),
    ('hidden_trash', 'fid', False, False),
])
def test_mops_remove_from_folders(context, folder_type, mode, trash_expected, hidden_trash_expected):
    uid = make_user_oneline(context, empty=True).uid
    mops_api = context.mops.api(uid=uid)

    context.maildb.execute('''
        SELECT code.create_settings(
            i_uid => %(uid)s,
            i_value => '{"single_settings": {"mail_b2c_can_use_hidden_trash": "on", "hidden_trash_enabled": "on"}}'::json
        )
    ''', uid=uid)

    response = mops_api.create_hidden_trash_folder()
    assert_that(response.status_code, equal_to(200), response.text)

    current_fid = get_fid(context, folder_type)
    trash_fid = get_fid(context, 'trash')
    hidden_trash_fid = get_fid(context, 'hidden_trash')

    MESSAGE_COUNT = 5
    fill_folder_with_messages(context, uid, folder_type, MESSAGE_COUNT)
    assert_that(get_message_count_in_folder(context, fid=current_fid), equal_to(MESSAGE_COUNT))

    kwargs = {}
    if mode == 'mids':
        kwargs['mids'] = get_mids_in_folder(context, current_fid)
    elif mode == 'fid':
        kwargs['fid'] = current_fid
    response = mops_api.remove(**kwargs)
    assert_that(response.status_code, equal_to(200), response.text)
    wait_for_completion_all_async_tasks(context, sec=300)

    assert_that(get_message_count_in_folder(context, fid=current_fid), equal_to(0))
    assert_that(get_message_count_in_folder(context, fid=trash_fid), equal_to(MESSAGE_COUNT * int(trash_expected)))
    assert_that(get_message_count_in_folder(context, fid=hidden_trash_fid), equal_to(MESSAGE_COUNT * int(hidden_trash_expected)))
