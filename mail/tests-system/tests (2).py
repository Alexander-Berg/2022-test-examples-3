#! /usr/bin/python
# -*- coding: utf-8 -*-

import imaplib
from time import sleep
from datetime import datetime

from nose.tools import *
from utils import *
from users import *
from retry import *
from urls import *
from clear import *
from lock_listener import LockListener
import queries
from pyparsing import nestedExpr, ParseResults

newest_count = 10
oldest_count = 10
oldest_flags_and_deletions_chunk_size = 10

STATUS_OK = 1
STATUS_TMP_FAIL = 2
STATUS_PERM_FAIL = 3
STATUS_NEED_RESET = 4


def setup_folders():
    global created_folders
    created_folders = {
        "folder_for_delete": {"messages_count": 3},
        "folder_for_clear": {"messages_count": 1},
        "folder_for_external_clear": {"messages_count": 0},
        "test_folder_01": {"messages_count": 0},
        "test_subfolder": {"messages_count": 0},
        "Sent": {"messages_count": 1},  # user folder "Sent" should be synced too
        "api_mops_folder_01": {"messages_count": 2},
        "api_mops_folder_02": {"messages_count": 0},
        "api_mops_folder_03": {"messages_count": 2},
        "api_mops_test_folder_symbol": {"messages_count": 0},
        "sync_status_folder": {"messages_count": 5},
        "parent/child": {"messages_count": 1},
        "folder_for_tab_tests": {"messages_count": 0},
    }
    for path in created_folders:
        ext_mailbox.create(path)


def setup_messages():
    global msg_ids
    msg_ids = []
    for i in range(newest_count + oldest_flags_and_deletions_chunk_size):
        msg_ids.append(append_message(ext_mailbox))

    for path, folder in created_folders.iteritems():
        for i in range(folder["messages_count"]):
            msg_ids.append(append_message(ext_mailbox, path))

    global msg_id_for_deletion_v1
    msg_id_for_deletion_v1 = append_message(ext_mailbox, "inbox")

    global msg_id_for_deletion_v2
    msg_id_for_deletion_v2 = append_message(ext_mailbox, "inbox")

    global msg_id_for_purge
    msg_id_for_purge = append_message(ext_mailbox, "inbox")

    global msg_id_for_deleting_folder
    msg_id_for_deleting_folder = append_message(ext_mailbox, "folder_for_delete")

    global msg_id_for_folder_for_external_clear
    msg_id_for_folder_for_external_clear = append_message(ext_mailbox, "folder_for_external_clear")

    global msg_id_with_old_date
    # message with internal date equals mailish account reg date - 1 second
    # because we should store messages older than account reg date with quiet=True
    msg_id_with_old_date = append_message(
        ext_mailbox, "inbox", receive_date='"16-Aug-2017 16:59:42 +0300"'
    )

    global msg_id_with_error
    msg_id_with_error = append_message(ext_mailbox, "inbox")

    global msg_id_for_api_mops_mark_label
    msg_id_for_api_mops_mark_label = append_message(ext_mailbox, "inbox")

    global msg_id_for_tab
    msg_id_for_tab = append_message(ext_mailbox, "folder_for_tab_tests")


def setup_user():
    for name, user in TEST_USERS.iteritems():
        if not user["need_authorize"]:
            continue

        resp = auth(user)
        user["uid"] = resp["uid"]
        user["auth_token"] = resp["access_token"]
        user["conn"] = get_connection(user["uid"])
        user["cur"] = get_cursor(user["conn"])

        load_url(unload_user_url(user["uid"]))
        if user["need_clear"]:
            clear_user(user["uid"], user["login"], user["password"], user["imap_host"])

    if "common_mailru_user" not in TEST_USERS:
        raise Exception("test user not found")

    global common_user
    common_user = TEST_USERS["common_mailru_user"]

    global login
    login = common_user["login"]

    global uid
    uid = common_user["uid"]

    global password
    password = common_user["password"]

    global draft_path
    draft_path = common_user["drafts_path"]

    global sent_path
    sent_path = common_user["sent_path"]

    global conn
    conn = get_connection(uid)

    global cur
    cur = get_cursor(conn)

    global ext_mailbox
    ext_mailbox = imaplib.IMAP4_SSL(common_user["imap_host"])
    ext_mailbox.login(login, password)

    global auth_token
    auth_token = ""

    global shard_id
    shard_id = get_user_shard_id(uid)


def setup():
    global lock_listener
    lock_listener = LockListener()
    lock_listener.start()

    global fake_cache
    fake_cache = Cache(CACHE_PORT)
    fake_cache.start()

    try:
        setup_user()
    except Exception as e:
        lock_listener.stop()
        fake_cache.shutdown()
        raise e

    global start_time
    start_time = datetime.now()


def teardown():
    lock_listener.stop()
    fake_cache.shutdown()


def check_auth_response(uid, resp):
    eq_(resp["status"]["status"], 1)
    eq_(int(resp["uid"]), uid)
    assert len(resp["xtoken"]) > 0


def check_account_tables(uid, login, cur):
    account = fetch_one(cur, queries.mailish_account(uid))
    eq_(account["uid"], uid)
    eq_(account["email"], login)

    auth_data = fetch_one(cur, queries.mailish_auth_data(uid))
    eq_(auth_data["uid"], uid)
    eq_(auth_data["lock_flag"], True)


def is_auth_token_valid(auth_token):
    post_data = {
        "format": "json",
        "userip": "127.0.0.1",
        "user_port": 443,
        "oauth_token": auth_token,
    }

    resp = load_json(blackbox_url(), post_data=urllib.urlencode(post_data))
    if resp["status"]["id"] is 0:
        return True
    elif resp["status"]["id"] is 5:
        return False
    else:
        raise Exception("check token error: {}".format(resp["error"]))


@retry(tries=50, delay=2)
def wait_end_of_first_sync(auth_token):
    inbox_fid = "1"
    api_res = messages(auth_token, inbox_fid)
    if api_res[0]["header"]["error"] != 1:
        raise Exception("wait end of first sync error: {}".format(api_res))


def test_first_auth():
    global uid, auth_token, xtoken
    resp = auth(common_user)
    uid = resp["uid"]
    auth_token = resp["access_token"]
    xtoken = resp["xtoken"]
    check_account_tables(uid, login, cur)
    setup_folders()
    setup_messages()


def test_api_when_security_lock_enabled():
    # because we should wait, while account will load
    check_controller_loaded(uid)
    load_url(unload_user_url(uid))
    check_controller_not_loaded(uid)

    execute(cur, queries.update_security_lock(uid, True))
    resp = call_xeno_api(mark_with_label_url(123, "", 123, True), auth_token)
    eq_(resp["status"], 2)


def test_no_auth_data():
    global uid, auth_token, xtoken
    assert is_auth_token_valid(auth_token)
    assert is_auth_token_valid(xtoken)

    check_controller_not_loaded(uid)

    execute(cur, queries.delete_auth_data(uid))
    load_url(load_user_url(uid))
    time.sleep(2)
    controller = check_controller_not_loaded(uid)

    eq_(3, call_xeno_api(settings_url(), auth_token)["status"]["status"])
    assert not is_auth_token_valid(auth_token)
    assert not is_auth_token_valid(xtoken)

    resp = auth(common_user)
    uid = resp["uid"]
    auth_token = resp["access_token"]
    wait_end_of_first_sync(auth_token)


def test_api_with_new_device():
    global common_user, uid, auth_token

    # Get auth data for 2nd device
    resp = auth(common_user, DEVICE_ID_2)
    auth_token_for_device_2 = resp["access_token"]

    # Drop auth data for 1st device
    resp = fetch(cur, queries.mailish_auth_data(uid))
    token_id_for_device_1 = resp[0][1]
    token_id_for_device_2 = resp[1][1]
    execute(cur, queries.delete_auth_data_by_token(uid, token_id_for_device_1))

    # Expect to get an error with x-token
    expected_status = STATUS_PERM_FAIL
    expected_message = "PERM_FAIL account_information: 2001, PERM_FAIL get_user_parameters: 2001, PERM_FAIL yamail_status: 2001, PERM_FAIL settings_setup: 2001, "

    # Invoke API
    resp = call_xeno_api(settings_url(), auth_token_for_device_2)
    received_status = resp["status"]["status"]
    received_message = resp["status"]["phrase"]

    eq_(expected_message, received_message)
    eq_(expected_status, received_status)

    # Restore auth data
    execute(cur, queries.delete_auth_data_by_token(uid, token_id_for_device_2))
    resp = auth(common_user)
    uid = resp["uid"]
    auth_token = resp["access_token"]
    wait_end_of_first_sync(auth_token)


# trash folder not synced right after first sync
def test_messages_for_not_synced_folder():
    trash_fid = "3"
    resp = messages(auth_token, trash_fid)
    eq_(resp[0]["header"]["error"], 2)


def test_messages_for_synced_folder():
    inbox_fid = "1"
    resp = messages(auth_token, inbox_fid)
    eq_(resp[0]["header"]["error"], 1)


@retry(tries=20)
def check_system_folders_inited(uid, cur):
    folders = fetch(cur, queries.mailish_folders(uid))
    system_folders = [1, 2, 3, 4, 6]  # fids of system folders except outbox
    for fid in system_folders:
        inited = False
        for folder in folders:
            if folder["fid"] == fid:
                inited = True
                break
        if not inited:
            raise Exception(
                "system folder with fid {} not inited; inited mailish folders: {}".format(
                    fid, dump_mailish_folders(folders)
                )
            )


def get_fid_by_path(uid, path, cur):
    folders = fetch(cur, queries.mailish_folders(uid))
    for folder in folders:
        if folder["imap_path"] == path:
            return folder["fid"]
    return None


def test_create_subfolder():
    parent_fid = get_fid_by_path(uid, "test_subfolder", cur)
    url = create_folder_url("subfolder", parent_fid)
    eq_(1, call_xeno_api(url, auth_token)["status"]["status"])


@retry(tries=40)
def check_folder_inited(uid, path, cur):
    fid = get_fid_by_path(uid, path, cur)
    if fid == None:
        raise Exception("folder '{}' not found in db".format(path))
    print("found folder {} {}".format(path, fid))


@retry(tries=40)
def check_folder_deleted(uid, path, cur):
    fid = get_fid_by_path(uid, path, cur)
    if fid:
        raise Exception("folder not deleted; fid={} path={}".format(fid, path))


def test_sync_status():
    sync_fid = get_fid_by_path(uid, "sync_status_folder", cur)
    resp = call_xeno_api(sync_status_url(), auth_token)
    eq_(1, resp["status"]["status"])
    for folder in resp["sync_status"]:
        if folder["fid"] == sync_fid:
            eq_(5, folder["external_messages_count"])


def test_folder_sync():
    check_system_folders_inited(uid, cur)
    for path in created_folders:
        check_folder_inited(uid, path, cur)

    path = "test_folder_01"
    ext_mailbox.delete(path)
    check_folder_deleted(uid, path, cur)


def test_access_token_caching():
    key = fake_cache.make_key(common_user["application"], common_user["refresh_token"])
    assert fake_cache.has_query("/get", key), (key, fake_cache.queries)
    assert fake_cache.has_query("/set", key), (key, fake_cache.queries)


def test_no_unnecessary_updates_while_access_token_alive_part1():
    load_url(unload_user_url(uid))
    check_controller_not_loaded(uid)

    fake_cache.reset_queries()
    load_url(load_user_url(uid))
    check_controller_loaded(uid)

    # there should be no /set calls when the tests are completed


def test_missed_folder():
    test_user = TEST_USERS["mailru_user_with_missed_folder"]
    login = test_user["login"]
    password = test_user["password"]
    imap_host = test_user["imap_host"]
    mailbox = imaplib.IMAP4_SSL(imap_host)
    check_response(mailbox.login(login, password))

    ret = mailbox.list()
    check_response(ret)
    folders = ret[1]

    is_child_folder_found = False
    for resp in folders:
        name = get_folder_name(resp)
        assert name != "missed"
        if name == "missed/exist":
            is_child_folder_found = True
    assert is_child_folder_found
    mailbox.logout()

    wait_end_of_first_sync(test_user["auth_token"])
    check_folder_inited(test_user["uid"], "missed", test_user["cur"])
    check_folder_inited(test_user["uid"], "missed/exist", test_user["cur"])


def test_delete_folder_with_subfolders():
    fid = get_fid_by_path(uid, "test_subfolder", cur)
    eq_(1, call_xeno_api(delete_folder_url(fid), auth_token)["status"]["status"])
    check_folder_deleted(uid, "test_subfolder", cur)
    check_folder_deleted(uid, "test_subfolder/subfolder", cur)


def test_mark_read():
    eq_(3, call_xeno_api(mark_read_url("", ""), auth_token)["status"])
    eq_(3, call_xeno_api(mark_read_url("123", ""), auth_token)["status"])
    eq_(3, call_xeno_api(mark_read_url("", "456"), auth_token)["status"])
    eq_(3, call_xeno_api(mark_read_url("123", "456"), auth_token)["status"])
    # todo mark real messages


@retry(tries=90, delay=1)
def check_message_exists(uid, msg_id, cur):
    messages = fetch(cur, queries.messages(uid))
    for message in messages:
        if message["msg_id"] == msg_id:
            return message
    raise Exception(
        "message not found; msg_id: {}, found messages: {}".format(msg_id, dump_messages(messages))
    )


@retry(tries=40)
def check_message_not_exists(uid, msg_id, cur):
    messages = fetch(cur, queries.messages(uid))
    for message in messages:
        if message["msg_id"] == msg_id:
            raise Exception("message found; msg_id: {}".format(msg_id))


def test_sync_message():
    for msg_id in msg_ids:
        check_message_exists(uid, msg_id, cur)


@retry(tries=50)
def check_labels_deleted(uid, mid, del_labels, cur):
    cur_labels = [label["name"] for label in fetch(cur, queries.labels(uid, mid))]
    for label in del_labels:
        if label in cur_labels:
            raise Exception('label "{}" not deleted; cur_labels={}'.format(label, cur_labels))


@retry(tries=50)
def check_labels_exists(uid, mid, labels, cur):
    cur_labels = [label["name"] for label in fetch(cur, queries.labels(uid, mid))]
    for label in labels:
        if not label in cur_labels:
            raise Exception(
                'label "{}" for mid "{}" not found; cur_labels={}'.format(label, mid, cur_labels)
            )


@retry(tries=50)
def check_labels_not_exists(uid, mid, labels, cur):
    cur_labels = [label["name"] for label in fetch(cur, queries.labels(uid, mid))]
    for label in labels:
        if label in cur_labels:
            raise Exception('label "{}" found; cur_labels={}'.format(label, cur_labels))


def test_sync_labels():
    msg_id = append_message(ext_mailbox, flags="(\Flagged)")
    message = check_message_exists(uid, msg_id, cur)
    check_labels_exists(uid, message["mid"], ["priority_high"], cur)

    ext_mailbox.select("inbox")
    ext_mailbox.store("1:*", "-flags", "\\Flagged")
    ext_mailbox.store("1:*", "flags", "\\Answered")

    check_labels_exists(uid, message["mid"], ["answered"], cur)
    check_labels_deleted(uid, message["mid"], ["priority_high"], cur)
    messages = fetch(cur, queries.messages_by_fid(uid, "1"))
    for message in messages:
        check_labels_exists(uid, message["mid"], ["answered"], cur)


def test_stop_sync_when_user_removed_from_shard():
    check_controller_loaded(uid)

    # remove user
    execute(cur, queries.set_user_is_here(uid, False))
    execute(cur, queries.set_here_since_to_current_time(uid))

    # mark last message as Flagged
    ext_mailbox.select("inbox")
    ext_mailbox.store("*:*", "+FLAGS", "\\Flagged")
    ext_mailbox.close()

    # user should be unloaded
    check_controller_not_loaded(uid)


def test_start_sync_when_user_added():
    check_controller_not_loaded(uid)

    # add user
    execute(cur, queries.set_user_is_here(uid, True))
    execute(cur, queries.set_here_since_to_current_time(uid))

    # user should be loaded
    check_controller_loaded(uid)


def check_message_in_ext_mailbox(path, imap_id, flags=[]):
    select = ext_mailbox.select(path)[0]
    eq_("OK", select)
    result, data = ext_mailbox.uid("fetch", str(imap_id), "(FLAGS)")
    eq_("OK", result)
    resp_flags = str(data[0])
    print(data[0])

    if data[0] is None:
        return False
    for flag in flags:
        assert resp_flags.find(flag) != -1

    return True


def check_message_in_ext_mailbox_by_msg_id(path, msg_id):
    select = ext_mailbox.select(path)[0]
    eq_("OK", select)
    result, data = ext_mailbox.fetch("1:*", "BODY.PEEK[HEADER.FIELDS (message-id)]")
    eq_("OK", result)

    if data[0] is None:
        return False
    for msg in data:
        if str(msg[1]).find(str(msg_id)):
            return True
    return False


def test_send_spam():
    mail_from = "roman.romanovich.1945@inbox.ru"
    strong_spam_text = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X\r\n."
    soft_spam_text = "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STRONG-ANTI-UBE-TEST-EMAIL*C.34X\r\n."
    resp = send(auth_token=auth_token, mail_from=mail_from, text=strong_spam_text)
    eq_(3, resp["status"]["status"])
    resp = send(auth_token=auth_token, mail_from=mail_from, text=soft_spam_text)
    eq_(3, resp["status"]["status"])


def test_mark_flagged():
    lid = fetch_one(cur, queries.label_by_name(uid, "priority_high"))["lid"]
    msg_id = append_message(ext_mailbox)
    message = check_message_exists(uid, msg_id, cur)
    mailish_message = fetch_one(cur, queries.mailish_message_by_mid(uid, message["mid"]))

    eq_(1, call_xeno_api(mark_with_label_url(message["mid"], "", lid, True), auth_token)["status"])

    ext_mailbox.select("inbox")
    resp = ext_mailbox.uid("fetch", str(mailish_message["imap_id"]), "FLAGS")[1][0]
    parsed = nestedExpr().parseString("(" + resp.decode("UTF-8") + ")").asList()[0]
    flags_index = parsed[1].index("FLAGS") + 1
    assert u"\\Flagged" in parsed[1][flags_index]

    check_labels_exists(uid, message["mid"], ["priority_high"], cur)


def test_delete_v1():
    message = check_message_exists(uid, msg_id_for_deletion_v1, cur)
    inbox_fid = message["fid"]

    eq_(1, call_xeno_api(delete_items_v1_url(message["mid"], ""), auth_token)["status"])

    message = check_message_exists(uid, msg_id_for_deletion_v1, cur)
    assert message["fid"] != inbox_fid

    eq_(1, call_xeno_api(delete_items_v1_url(message["mid"], ""), auth_token)["status"])
    check_message_not_exists(uid, msg_id_for_deletion_v1, cur)


def test_delete_v2():
    message = check_message_exists(uid, msg_id_for_deletion_v2, cur)
    inbox_fid = message["fid"]

    eq_(
        {},
        call_xeno_api(
            delete_items_v2_url(), auth_token, post_data=json.dumps({"mids": message["mid"]})
        ),
    )
    message = check_message_exists(uid, msg_id_for_deletion_v2, cur)
    assert message["fid"] != inbox_fid


def test_purge():
    message = check_message_exists(uid, msg_id_for_purge, cur)

    eq_(
        {},
        call_xeno_api(
            purge_items_url(), auth_token, post_data=json.dumps({"mids": message["mid"]})
        ),
    )
    check_message_not_exists(uid, msg_id_for_purge, cur)


def get_quiet_status_from_changelog(mid):
    changes = fetch(cur, queries.changes(uid, "store", str(start_time)))
    for change in changes:
        data = json.loads(change["changed"])
        assert len(data) == 1, data
        if data[0]["mid"] == mid:
            return change["quiet"]
    raise Exception("message not found, mid={}".format(mid))


def test_quite_status():
    # for messages older than account creation date we should set quiet=True in mail.changelog
    msg_with_old_date = check_message_exists(uid, msg_id_with_old_date, cur)
    eq_(
        True,
        get_quiet_status_from_changelog(msg_with_old_date["mid"]),
        "mid={}".format(msg_with_old_date["mid"]),
    )

    msg_with_new_date = check_message_exists(uid, msg_ids[-1], cur)
    eq_(
        False,
        get_quiet_status_from_changelog(msg_with_new_date["mid"]),
        "mid={}".format(msg_with_new_date["mid"]),
    )


def test_array_error_response():
    response = call_xeno_api(xlist_url(), "wrong_token")
    for entry in response:
        eq_(3, entry["status"]["status"])


def test_retries():
    message = check_message_exists(uid, msg_id_with_error, cur)

    load_url(unload_user_url(uid))
    check_controller_not_loaded(uid)

    # first set mid of message to null, and errors_counter to 1
    execute(cur, queries.mark_message_as_error(uid, message["mid"]))
    # than actually delete message with mid
    # and it will not be deleted from mailish.messages, because there is no mid there anymore
    execute(cur, queries.delete_messages(uid, [str(message["mid"])]))
    check_message_not_exists(uid, msg_id_with_error, cur)
    load_url(load_user_url(uid))
    check_controller_loaded(uid)

    check_message_exists(uid, msg_id_with_error, cur)


def test_perm_fails():
    eq_(3, call_xeno_api(update_folder_url(0, "new_name"), auth_token)["status"]["status"])


def test_delete_folder():
    path = "folder_for_delete"
    fid = get_fid_by_path(uid, path, cur)
    msgs_in_deleting_folder = []
    for i in range(newest_count + oldest_count):
        msgs_in_deleting_folder.append(append_message(ext_mailbox, path))

    response = call_xeno_api(delete_folder_url(fid), auth_token)
    eq_(1, response["status"]["status"])
    eq_("sync", response["taskType"])
    check_folder_deleted(uid, path, cur)

    trash_path = "Корзина"
    trash_fid = get_fid_by_path(uid, trash_path, cur)
    msg = check_message_exists(uid, msg_id_for_deleting_folder, cur)
    assert msg["fid"] == trash_fid

    for i in range(newest_count + oldest_count):
        assert check_message_in_ext_mailbox_by_msg_id(trash_path, msgs_in_deleting_folder[i])


def test_clear_downloaded_range_when_clear_folder():
    imap_path = "folder_for_clear"
    folder = fetch_one(cur, queries.mailish_folder_by_imap_path(uid, imap_path))
    assert folder["range_start"] != 0
    assert folder["range_end"] != 0

    response = call_xeno_api(clear_folder_url(folder["fid"]), auth_token)
    eq_(1, response["status"])

    folder = fetch_one(cur, queries.mailish_folder_by_imap_path(uid, imap_path))
    eq_(0, folder["range_start"])
    eq_(0, folder["range_end"])


@retry(tries=60)
def check_folder_empty(fid, cur):
    msgs = fetch(cur, queries.mailish_messages_by_fid(uid, fid))
    eq_(0, len(msgs))


def test_clear_folder_from_external():
    imap_path = "folder_for_external_clear"
    folder = fetch_one(cur, queries.mailish_folder_by_imap_path(uid, imap_path))
    msgs = fetch(cur, queries.mailish_messages_by_fid(uid, folder["fid"]))
    eq_(1, len(msgs), msg=msgs)

    clear_folder(imap_path, ext_mailbox)
    check_folder_empty(folder["fid"], cur)


def test_store_drafts():
    mid = store_draft(auth_token)["mid"]
    imap_id = fetch_one(cur, queries.mailish_message_by_mid(uid, mid))["imap_id"]
    assert check_message_in_ext_mailbox(draft_path, imap_id, ["\\Seen"])
    check_labels_exists(uid, mid, ["draft"], cur)

    new_mid = store_draft(auth_token, mid)["mid"]
    new_imap_id = fetch_one(cur, queries.mailish_message_by_mid(uid, new_mid))["imap_id"]
    assert check_message_in_ext_mailbox(draft_path, new_imap_id, ["\\Seen"])

    response = fetch(cur, queries.mailish_message_by_mid(uid, mid))
    eq_(0, len(response))
    assert not check_message_in_ext_mailbox(draft_path, imap_id)

    inbox_mid = store_draft(auth_token, None, "1")["mid"]
    inbox_imap_id = fetch_one(cur, queries.mailish_message_by_mid(uid, inbox_mid))["imap_id"]
    assert check_message_in_ext_mailbox("inbox", inbox_imap_id, ["\\Seen"])


def check_attachment(attachment_path, download_url):
    local_attachment = open(attachment_path, "rb").read()
    downloaded_attachment = urllib2.urlopen(download_url).read()
    eq_(downloaded_attachment, local_attachment)


def test_attachments_response_when_store_draft():
    attachment_path = "plain_text_attachment.txt"
    attachment_mime_type = "text/plain"
    attachment_id = upload_attachment(auth_token, attachment_path)
    resp = store_draft(auth_token, attachments_ids=[attachment_id])
    download_url = resp["attachments"]["attachment"][0]["download_url"]
    check_attachment(attachment_path, download_url)
    eq_(attachment_mime_type, resp["attachments"]["attachment"][0]["mime_type"])


def test_unload_user_on_bad_karma_event():
    check_controller_loaded(uid)
    update_karma(uid, 100)
    check_controller_not_loaded(uid)


def test_load_user_on_good_karma_event():
    check_controller_not_loaded(uid)
    update_karma(uid, 0)
    check_controller_loaded(uid)


def check_no_auth_data(uid):
    cursor = get_cursor(get_connection(uid))
    auth_data = fetch(cursor, queries.mailish_auth_data(uid))
    assert len(auth_data) == 0, auth_data


def test_invalidate_user_auth():
    user = TEST_USERS["common_gmail_user"]
    resp = auth(user)
    check_controller_loaded(user["uid"])
    load_url(invalidate_auth_url(user["uid"]))
    check_controller_not_loaded(user["uid"])
    assert not is_auth_token_valid(resp["xtoken"])
    check_no_auth_data(user["uid"])


def test_auth_with_explicitly_specified_servers_and_ssl_ports():
    user = TEST_USERS["common_zoho_user"]
    check_auth(user)


def test_auth_with_explicitly_specified_servers_bad_imap_host():
    user = dict(TEST_USERS["common_zoho_user"])
    user["imap_host"] = "imap.devops.devopsovich.ru"
    check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: cannot connect to imap server"
    )


def test_auth_with_explicitly_specified_servers_bad_imap_port():
    user = dict(TEST_USERS["common_zoho_user"])
    user["imap_port"] = 994
    check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: cannot connect to imap server"
    )


def test_auth_with_explicitly_specified_servers_bad_imap_login():
    user = dict(TEST_USERS["common_zoho_user"])
    user["login"] = "developer.developerovich@rambler.ru"
    check_auth_failed(user, expected_status=3, expected_phrase="auth error: imap login error")


def test_auth_with_explicitly_specified_servers_bad_imap_password():
    user = dict(TEST_USERS["common_zoho_user"])
    user["password"] = "devops"
    check_auth_failed(user, expected_status=3, expected_phrase="auth error: imap login error")


def test_auth_with_explicitly_specified_servers_bad_smtp_host():
    user = dict(TEST_USERS["common_zoho_user"])
    user["smtp_host"] = "smtp.devops.devopsovich.ru"
    check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: cannot connect to smtp server"
    )


def test_auth_with_explicitly_specified_servers_bad_smtp_port():
    user = dict(TEST_USERS["common_zoho_user"])
    user["smtp_port"] = 464
    check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: cannot connect to smtp server"
    )


def test_auth_with_explicitly_specified_servers_bad_smtp_login():
    user = dict(TEST_USERS["common_zoho_user"])
    user["smtp_login"] = "developer.developerovich@rambler.ru"
    check_auth_failed(user, expected_status=3, expected_phrase="auth error: smtp login error")


def test_auth_with_explicitly_specified_servers_bad_smtp_password():
    user = dict(TEST_USERS["common_zoho_user"])
    user["smtp_password"] = "devops"
    check_auth_failed(user, expected_status=3, expected_phrase="auth error: smtp login error")


def test_auth_by_password_with_bad_imap_password():
    user = dict(TEST_USERS["common_zoho_user"])
    user["auth_type"] = AUTH_TYPE_PASSWORD
    user["password"] = "devops"
    resp = check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: imap login error"
    )
    eq_(resp["hint"]["imap_server"]["host"], "imap.zoho.com")
    eq_(resp["hint"]["imap_server"]["port"], 993)
    eq_(resp["hint"]["imap_server"]["ssl"], True)
    eq_(resp["hint"]["smtp_server"]["host"], "smtp.zoho.com")
    eq_(resp["hint"]["smtp_server"]["port"], 465)
    eq_(resp["hint"]["smtp_server"]["ssl"], True)


def test_auth_by_password_with_yandex_pdd():
    user = dict(TEST_USERS["common_yandex_pdd_user"])
    resp = check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: forbidden provider"
    )
    eq_(resp["provider"], "yandex")


def check_forbidden_provider(user_name, provider):
    user = dict(TEST_USERS[user_name])
    user["auth_type"] = AUTH_TYPE_PASSWORD
    resp = check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: forbidden provider"
    )
    eq_(resp["provider"], provider)


def test_auth_by_password_with_fordidden_provider():
    check_forbidden_provider("common_mailru_user", "mailru")
    check_forbidden_provider("common_gmail_user", "gmail")
    check_forbidden_provider("common_outlook_user", "outlook")


def test_auth_with_fordidden_server():
    resp = check_auth_failed(
        TEST_USERS["common_yandex_user"],
        expected_status=3,
        expected_phrase="auth error: forbidden server",
    )


def test_auth_response_for_incomplete_smtp_params():
    user = dict(TEST_USERS["common_zoho_user"])
    del user["smtp_host"]
    del user["smtp_port"]
    del user["smtp_ssl"]
    check_auth_failed(user, expected_status=3, expected_phrase="auth error: incomplete smtp params")


def test_success_resolve_zoho_provider():
    user = dict(TEST_USERS["common_zoho_user"])
    user["auth_type"] = AUTH_TYPE_PASSWORD
    check_auth(user)


def test_cannot_resolve_because_bad_domain():
    user = dict(TEST_USERS["common_zoho_user"])
    user["login"] = "user@yandexmailgoogleoutlookramblerzoho.com"
    user["auth_type"] = AUTH_TYPE_PASSWORD
    check_auth_failed(
        user, expected_status=3, expected_phrase="auth error: cannot resolve external servers"
    )


def test_release_bucket():
    check_controller_loaded(uid)
    bucket_id = get_bucket_id(shard_id)
    load_url(release_buckets_url([bucket_id], 2))
    check_controller_not_loaded(uid)
    check_controller_loaded(uid)


def test_del_shards_from_bucket():
    check_controller_loaded(uid)
    bucket_id = get_bucket_id(shard_id)
    load_url(del_shards_from_bucket_url(bucket_id, [shard_id]))
    check_controller_not_loaded(uid)


def test_add_bucket():
    check_controller_not_loaded(uid)
    new_bucket_id = "b666"
    load_url(add_bucket_url(new_bucket_id, [shard_id]))
    check_controller_loaded(uid)


def test_del_bucket():
    check_controller_loaded(uid)
    bucket_id = get_bucket_id(shard_id)
    load_url(del_bucket_url(bucket_id))
    check_controller_not_loaded(uid)


def test_add_shards_to_bucket():
    check_controller_not_loaded(uid)
    new_bucket_id = "b667"
    load_url(add_bucket_url(new_bucket_id, []))
    load_url(add_shards_to_bucket_url(new_bucket_id, [shard_id]))
    check_controller_loaded(uid)


def test_api_mops_mark():
    path = "api_mops_folder_01"
    fid = get_fid_by_path(uid, path, cur)
    expected_msg_count = created_folders[path]["messages_count"]
    expected_mids = get_folder_mids(uid, fid, cur)
    mids = check_folder_mids_by_seen_status(uid, fid, False, cur, expected_mids)
    eq_(len(mids), expected_msg_count)
    api_mops_mark(uid, "read", mids)
    mids = check_folder_mids_by_seen_status(uid, fid, True, cur, expected_mids)
    eq_(len(mids), expected_msg_count)


def test_api_mops_move():
    source_path = "api_mops_folder_01"
    dest_path = "api_mops_folder_02"
    source_fid = get_fid_by_path(uid, source_path, cur)
    dest_fid = get_fid_by_path(uid, dest_path, cur)
    expected_msg_count = created_folders[source_path]["messages_count"]
    source_mids = get_folder_mids(uid, source_fid, cur)
    eq_(len(source_mids), expected_msg_count)
    eq_(len(get_folder_mids(uid, dest_fid, cur)), 0)
    api_mops_move(uid, dest_fid, source_mids)
    eq_(len(get_folder_mids(uid, source_fid, cur)), 0)
    eq_(len(get_folder_mids(uid, dest_fid, cur)), expected_msg_count)


def test_api_mops_remove():
    path = "api_mops_folder_03"
    fid = get_fid_by_path(uid, path, cur)
    expected_msg_count = created_folders[path]["messages_count"]
    mids = get_folder_mids(uid, fid, cur)
    eq_(len(mids), expected_msg_count)
    api_mops_remove(uid, mids)
    eq_(len(get_folder_mids(uid, fid, cur)), 0)


def test_api_mops_folder_operations():
    path = "api_mops_create_folder"
    resp = api_mops_create_folder(uid, path)
    created_fid = resp["fid"]
    path = "api_mops_update_folder"
    api_mops_update_folder(uid, created_fid, path)
    updated_fid = str(get_fid_by_path(uid, path, cur))
    eq_(created_fid, updated_fid)
    api_mops_delete_folder(uid, updated_fid)
    eq_(get_fid_by_path(uid, path, cur), None)


def test_api_mops_create_label():
    label_name = "create_label_tst"
    label_color = "9336260"
    label_type = "user"
    resp = api_mops_create_label(uid, label_name, label_color, label_type)
    lid = resp["lid"]
    resp = api_mops_create_label(uid, label_name, label_color, label_type, get_or_create=True)
    eq_(lid, resp["lid"])


def test_api_mops_create_label_by_symbol():
    symbol = "pinned_label"
    resp = api_mops_create_label_by_symbol(uid, symbol)
    lid = resp["lid"]
    resp = api_mops_create_label_by_symbol(uid, symbol, get_or_create=True)
    eq_(lid, resp["lid"])


def test_api_mops_mark_label():
    message = check_message_exists(uid, msg_id_for_api_mops_mark_label, cur)
    mid = str(message["mid"])
    label_name = "mark_label_tst"
    resp = api_mops_create_label(uid, label_name, "9336260", "user")
    lid = str(resp["lid"])
    api_mops_label(uid, [mid], [lid])
    check_labels_exists(uid, message["mid"], [label_name], cur)
    api_mops_label(uid, [mid], [lid], mark=False)
    check_labels_not_exists(uid, message["mid"], [label_name], cur)


def test_api_mops_update_label():
    resp = api_mops_create_label(uid, "update_label_tst", "9336260", "user")
    lid = str(resp["lid"])
    api_mops_update_label(uid, lid, "updated_label")
    updated_lid = str(fetch_one(cur, queries.label_by_name(uid, "updated_label"))["lid"])
    eq_(lid, updated_lid)


def test_api_mops_delete_label():
    label_name = "delete_label_tst"
    resp = api_mops_create_label(uid, label_name, "9336260", "user")
    lid = resp["lid"]
    api_mops_delete_label(uid, lid)
    eq_(len(fetch(cur, queries.label_by_name(uid, label_name))), 0)


def test_api_mops_create_folder_with_symbol():
    path = "create_folder_with_symbol"
    symbol = "discount"
    resp = api_mops_create_folder(uid, path, symbol=symbol)
    fid = resp["fid"]
    folder = fetch_one(cur, queries.folder_by_fid(uid, fid))
    eq_(symbol, folder["type"])


def test_api_mops_set_folder_symbol():
    fid = get_fid_by_path(uid, "api_mops_test_folder_symbol", cur)
    symbol = "archive"
    api_mops_set_folder_symbol(uid, fid, symbol)
    folder = fetch_one(cur, queries.folder_by_fid(uid, fid))
    eq_(symbol, folder["type"])
    api_mops_set_folder_symbol(uid, fid)
    folder = fetch_one(cur, queries.folder_by_fid(uid, fid))
    eq_("user", folder["type"])


def test_api_sendbernar_save():
    draft_fid = "6"
    response = api_sendbernar_save(uid, draft_fid)
    eq_(response["error"], "Success")
    mid = str(response["mid"])
    imap_id = fetch_one(cur, queries.mailish_message_by_mid(uid, mid))["imap_id"]
    assert check_message_in_ext_mailbox(draft_path, imap_id, ["\\Seen"])
    check_labels_exists(uid, mid, ["draft"], cur)

    response = api_sendbernar_save(uid, draft_fid, mid)
    eq_(response["error"], "Success")
    new_mid = str(response["mid"])
    new_imap_id = fetch_one(cur, queries.mailish_message_by_mid(uid, new_mid))["imap_id"]
    assert check_message_in_ext_mailbox(draft_path, new_imap_id, ["\\Seen"])
    check_labels_exists(uid, new_mid, ["draft"], cur)

    response = fetch(cur, queries.mailish_message_by_mid(uid, mid))
    eq_(0, len(response))
    assert not check_message_in_ext_mailbox(draft_path, imap_id)

    not_existing_fid = "not_existing_fid"
    try:
        response = api_sendbernar_save(uid, not_existing_fid)
    except urllib2.HTTPError as e:
        resp = json.loads(e.read())
        eq_("SendMessageFailed", resp["error"])
        eq_("folder not found", resp["explanation"])


def test_no_unnecessary_updates_while_access_token_alive_part2():
    key = fake_cache.make_key(common_user["application"], common_user["refresh_token"])
    assert fake_cache.has_query("/get", key), (key, fake_cache.queries)
    assert not fake_cache.has_query("/set", key), (key, fake_cache.queries)


def test_tabs():
    inbox_fid = get_fid_by_path(uid, "INBOX", cur)
    original_fid = get_fid_by_path(uid, "folder_for_tab_tests", cur)
    msg = check_message_exists(uid, msg_id_for_tab, cur)
    assert msg["tab"] is None

    # Move message to 'social' tab
    resp = call_xeno_api(move_to_folder_url(msg["mid"], get_fid_for_tab("social")), auth_token)
    assert resp["status"] == 1, resp

    moved_msg = check_message_exists(uid, msg_id_for_tab, cur)
    assert moved_msg["fid"] == inbox_fid
    assert moved_msg["tab"] == "social"

    # Move message back to original folder
    resp = call_xeno_api(move_to_folder_url(moved_msg["mid"], original_fid), auth_token)
    assert resp["status"] == 1, resp

    moved_back_msg = check_message_exists(uid, msg_id_for_tab, cur)
    assert moved_back_msg["fid"] == original_fid
    assert moved_back_msg["tab"] is None
