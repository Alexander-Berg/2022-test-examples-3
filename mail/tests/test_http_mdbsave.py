# coding: utf-8

import pytest
import queries
import psycopg2
from datetime import datetime
from mdbsave_helpers import create_user
from mdbsave_types import MessageData, Label, User, Symbols
from utils import (
    execute,
    fetch,
    fetch_one,
    get_folder_created_by_http_save,
    http_save,
    http_save_and_get_mid,
    http_save_and_get_rcpt
)


@pytest.fixture(scope="module", autouse=True)
def clean_mdb(env):
    execute(env.conn, "DELETE FROM mail.threads")
    execute(env.conn, "DELETE FROM mail.box")
    execute(env.conn, "DELETE FROM mail.folders")
    execute(env.conn, "DELETE FROM mail.labels")
    execute(env.conn, "DELETE FROM mail.messages")


def test_http_save_message(env):
    user = create_user(env)
    message = MessageData(user, [user])
    mid = http_save_and_get_mid(env, message)

    msg = fetch_one(env.conn, queries.message(user.uid, mid))
    assert msg["subject"] == message.subject
    assert msg["st_id"] == message.stid
    assert msg["size"] == message.size
    assert msg["firstline"] == message.firstline
    assert msg["hdr_message_id"] == message.message_id
    received_date = datetime.fromtimestamp(message.received_date, psycopg2.tz.FixedOffsetTimezone(offset=180, name=None))
    assert msg["hdr_date"] == received_date
    assert msg["received_date"] == received_date

    tab = fetch_one(env.conn, queries.message_tab(user.uid, mid))
    assert tab["tab"] == "relevant"


def test_http_recipients(env):
    sender = create_user(env)
    recipient = create_user(env)
    message = MessageData(sender, [recipient])
    mid = http_save_and_get_mid(env, message)

    recipients = fetch(env.conn, queries.recipients(recipient.uid, mid))
    assert recipients == [
        ["from", sender.display_name, "{}@{}".format(sender.local, sender.domain)],
        ["to", recipient.display_name, "{}@{}".format(recipient.local, recipient.domain)]
    ]


def test_http_that_long_recipients_display_names_are_truncated_when_parsing_exception_was_thrown_and_recipients_were_glued_to_one_record(env):
    sender = create_user(env)
    recipients = []

    recipients_cnt = 5
    rcpt_with_bad_email = 'abcd >, <'
    for i in range(recipients_cnt):
        recipient = create_user(env)
        recipient.local = rcpt_with_bad_email
        recipient.domain = 'foobar'
        recipient.display_name = 'a' * 4096
        recipients.append(recipient)

    message = MessageData(sender, recipients)
    mid = http_save_and_get_mid(env, message)

    recipients_fetched = fetch(env.conn, queries.recipients(recipient.uid, mid))

    assert len(recipients_fetched) == 2

    for recipient_record in recipients_fetched:
        recipient_type, display_name, addr = recipient_record
        if recipient_type == 'to':
            assert len(display_name) <= 1024
            assert addr == ''


def test_http_that_long_recipients_display_names_are_truncated(env):
    sender = create_user(env)
    recipients = []

    should_be = {}
    recipients_cnt = 9
    display_name_max_len = 1024
    for i in range(recipients_cnt):
        recipient = create_user(env)
        recipient.local = str(i) + '_' + 'local'
        recipient.domain = str(i) + '_' + 'domain'
        recipient.display_name = str(i) + '_' + 'a' * 4096
        recipients.append(recipient)

        addr = recipient.local + "@" + recipient.domain
        should_be[addr] = recipient.display_name[0:display_name_max_len]

    message = MessageData(sender, recipients)
    mid = http_save_and_get_mid(env, message)

    recipients_fetched = fetch(env.conn, queries.recipients(recipient.uid, mid))
    assert len(recipients_fetched) == recipients_cnt + 1  # + 1 for 'from'

    for recipient_record in recipients_fetched:
        recipient_type, display_name, addr = recipient_record
        if recipient_type == 'to':
            assert display_name == should_be[addr]


def test_that_long_recipients_display_names_are_truncated_when_display_name_in_utf8(env):
    sender = create_user(env)
    recipients = []

    recipients_cnt = 9
    for i in range(recipients_cnt):
        recipient = create_user(env)
        recipient.local = str(i) + '_' + 'local'
        recipient.domain = str(i) + '_' + 'domain'
        recipient.display_name = str(i) + '_' + '\xe2\x82\xac' * 4096
        recipients.append(recipient)

        addr = recipient.local + "@" + recipient.domain

    message = MessageData(sender, recipients)
    mid = http_save_and_get_mid(env, message)

    recipients_fetched = fetch(env.conn, queries.recipients(recipient.uid, mid))
    assert len(recipients_fetched) == recipients_cnt + 1  # + 1 for 'from'

    for recipient_record in recipients_fetched:
        recipient_type, display_name, addr = recipient_record
        if recipient_type == 'to':
            assert len(display_name) <= 1024


def test_http_multiple_recipients_format(env):
    sender = create_user(env)
    rcpts = [create_user(env) for i in range(3)]
    message = MessageData(sender, rcpts)
    mid = http_save_and_get_mid(env, message)

    recipients = fetch(env.conn, queries.recipients(rcpts[0].uid, mid))
    expected = [
        ["from", sender.display_name, "{}@{}".format(sender.local, sender.domain)]
    ] + [
        ["to", rcpt.display_name, "{}@{}".format(rcpt.local, rcpt.domain)] for rcpt in rcpts
    ]
    assert sorted(recipients) == sorted(expected)


@pytest.mark.parametrize(
    "user_display_name, user_local, user_domain, expected_display_name, expected_email", [
        ("", "local", "domain", "", "local@domain"),
        ("name", "", "domain", '"name" <@domain>', ""),
        ("name", "local", "", '"name" <local>', "")
    ]
)
def test_http_bad_email_address(env, user_display_name, user_local, user_domain, expected_display_name, expected_email):
    user = create_user(env)
    user.display_name = user_display_name
    user.local = user_local
    user.domain = user_domain
    message = MessageData(user, [user])
    mid = http_save_and_get_mid(env, message)

    recipients = fetch(env.conn, queries.recipients(user.uid, mid))
    assert recipients == [
        ["from", expected_display_name, expected_email],
        ["to", expected_display_name, expected_email]
    ]


def test_http_labels_by_name(env):
    user = create_user(env)
    message = MessageData(user, [user])
    label = Label()
    message.labels["0"] = [label]
    mid = http_save_and_get_mid(env, message)

    lbl = fetch_one(env.conn, queries.label_by_name(user.uid, label.name, label.label_type))
    msg = fetch_one(env.conn, queries.message(user.uid, mid))
    assert msg["lids"] == [lbl["lid"]]


def test_http_label_symbols(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.label_symbols["0"] = ["answered_label"]
    mid = http_save_and_get_mid(env, message)

    label = fetch_one(env.conn, queries.label_by_name(user.uid, "answered", "system"))
    msg = fetch_one(env.conn, queries.message(user.uid, mid))
    assert msg["lids"] == [label["lid"]]


def test_http_folder_path(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "\\sent"
    mid = http_save_and_get_mid(env, message)

    msg = fetch_one(env.conn, queries.message(user.uid, mid))
    folder = fetch_one(env.conn, queries.folder_by_type(user.uid, "sent"))
    assert msg["fid"] == folder["fid"]


def test_non_existing_folder_path_fallback_to_inbox(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "non_existing_folder"
    message.no_such_folder_action["0"] = "fallback_to_inbox"
    status, name, code = get_folder_created_by_http_save(env, message)

    assert status == "ok"
    assert name == "Inbox"
    assert code == Symbols.INBOX.Code


def test_non_existing_folder_path_fail(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "non_existing_folder"
    message.no_such_folder_action["0"] = "fail"
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "perm error"


@pytest.mark.parametrize("folder", ["non_existing_folder", "\\non_existing_folder"])
def test_non_existing_folder_path_create_path(env, folder):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = folder
    message.no_such_folder_action["0"] = "create"
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "ok"
    assert name == folder


def test_non_existing_folder_path_create_folder_for_changeable_symbol(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "\\Archive"
    message.no_such_folder_action["0"] = "create"
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "ok"
    assert name == "archive"
    assert code == Symbols.ARCHIVE.Code


def test_non_existing_folder_path_create_folder_for_pending_symbol(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "\\Pending"
    message.no_such_folder_action["0"] = "create"
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "ok"
    assert name == "pending"
    assert code == Symbols.PENDING.Code


def test_existing_folder_path_create_folder_with_suffix_for_pending_symbol(env):
    user = create_user(env)

    message1 = MessageData(user, [user])
    message1.folder_path["0"] = "pending"
    message1.no_such_folder_action["0"] = "create"
    status1, name1, code1 = get_folder_created_by_http_save(env, message1)
    assert status1 == "ok"
    assert name1 == "pending"
    assert code1 == Symbols.NONE.Code

    message2 = MessageData(user, [user])
    message2.folder_path["0"] = "\\Pending"
    message2.no_such_folder_action["0"] = "create"
    status2, name2, code2 = get_folder_created_by_http_save(env, message2)
    assert status2 == "ok"
    assert name2.startswith("pending_")
    assert code2 == Symbols.PENDING.Code

    message3 = MessageData(user, [user])
    message3.folder_path["0"] = "\\Pending"
    message3.no_such_folder_action["0"] = "create"
    status3, name3, code3 = get_folder_created_by_http_save(env, message3)
    assert status3 == "ok"
    assert name3 == name2
    assert code3 == Symbols.PENDING.Code


def test_store_to_folder_itself_if_cant_create_subfolder_with_folderCantBeParent_error(env):
    user = create_user(env)
    message = MessageData(user, [user])
    # \Spam and some other folders are marked as 'childless' folder, so we can't create subfolders within them
    # and we should save messages to the 'childless' folder instead
    message.folder_path["0"] = "\\Spam|non_existing_folder"
    message.no_such_folder_action["0"] = "create"
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "ok"
    assert name == "Spam"
    assert code == Symbols.SPAM.Code


def test_drafts_to_draft_bug_compatibility(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "\\Drafts"
    message.no_such_folder_action["0"] = "create"
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "ok"
    assert name == "Drafts"
    assert code == Symbols.DRAFT.Code


@pytest.mark.parametrize(
    "no_such_folder_action",
    ["create", "fallback_to_inbox", "fail"]
)
def test_empty_folder_path_fail(env, no_such_folder_action):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = ""
    message.no_such_folder_action["0"] = no_such_folder_action
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "perm error"


def test_empty_folder_path_component_ok(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "some||non_existing_folder|"
    message.no_such_folder_action["0"] = "create"
    status, name, code = get_folder_created_by_http_save(env, message)
    assert status == "ok"
    assert name == "some|non_existing_folder"


def test_mime_parts(env):
    user = create_user(env)
    message = MessageData(user, [user])
    mid = http_save_and_get_mid(env, message)

    part = message.mime_parts[0]
    mime_parts = fetch(env.conn, queries.mime_parts(user.uid, mid))
    assert mime_parts == [
        [part.hid, part.content_type, part.content_subtype, part.boundary, part.name, part.charset, part.encoding,
            part.content_disposition, part.filename, part.cid, part.offset, part.offset + part.length]
    ]


def test_attachments(env):
    user = create_user(env)
    message = MessageData(user, [user])
    mid = http_save_and_get_mid(env, message)

    attachment = message.attachments[0]
    attachments = fetch(env.conn, queries.attachments(user.uid, mid))
    assert attachments == [
        [attachment.hid, attachment.attachment_type, attachment.filename, attachment.size]
    ]


def test_equal_subjects_join_into_thread_when_threading_by_subject(env):
    user = create_user(env)
    message1 = MessageData(user, [user])
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "hash"
    message2.thread_meta.hash_namespace = "subject"
    message2.subject = message1.subject
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))
    assert msg1["tid"] == msg2["tid"]


def test_not_equal_subjects_dont_join_when_threading_by_subject(env):
    user = create_user(env)
    message1 = MessageData(user, [user])
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "hash"
    message2.thread_meta.hash_namespace = "subject"
    message2.subject = "some other subject"
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))
    assert msg1["tid"] != msg2["tid"]


def test_equal_from_join_into_thread_when_threading_by_from(env):
    user = create_user(env)
    message1 = MessageData(user, [user])
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "hash"
    message2.thread_meta.hash_namespace = "from"
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))
    assert msg1["tid"] == msg2["tid"]


def test_dont_join_into_thread_when_threading_by_from(env):
    user1 = create_user(env)
    user2 = create_user(env)

    message1 = MessageData(user1, [user1])
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user2, [user1])
    message2.thread_meta.merge_rule = "hash"
    message2.thread_meta.hash_namespace = "from"
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user1.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user1.uid, mid2))
    assert msg1["tid"] != msg2["tid"]


def test_equal_message_id_join_into_thread_when_threading_by_reference(env):
    user = create_user(env)

    message1 = MessageData(user, [user])
    message1.thread_meta.merge_rule = "references"
    message1.message_id = "111"
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "references"
    message2.message_id = "111"
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))
    assert msg1["tid"] == msg2["tid"]


def test_equal_reference_hashes_join_into_thread_when_threading_by_reference(env):
    user = create_user(env)

    message1 = MessageData(user, [user])
    message1.thread_meta.merge_rule = "references"
    message1.thread_meta.reference_hashes = ["1234"]
    message1.message_id = "111"
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "references"
    message2.thread_meta.reference_hashes = ["1234"]
    message2.message_id = "222"
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))
    assert msg1["tid"] == msg2["tid"]


def test_dont_join_into_thread_when_threading_by_reference(env):
    user = create_user(env)

    message1 = MessageData(user, [user])
    message1.thread_meta.merge_rule = "references"
    message1.thread_meta.reference_hashes = ["1234"]
    message1.message_id = "111"
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "references"
    message2.thread_meta.reference_hashes = ["4321"]
    message2.message_id = "222"
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))

    assert msg1["tid"] != msg2["tid"]


def test_mute_messages_in_muted_thread(env):
    user = create_user(env)

    message1 = MessageData(user, [user])
    message1.thread_meta.merge_rule = "hash"
    message1.thread_meta.hash_namespace = "subject"
    message1.label_symbols["0"] = ["mute_label"]
    mid1 = http_save_and_get_mid(env, message1)

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "hash"
    message2.thread_meta.hash_namespace = "subject"
    message2.subject = message1.subject
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))
    mute_label = fetch_one(env.conn, queries.label_by_name(user.uid, "mute", "system"))
    assert msg1["tid"] == msg2["tid"]
    assert msg2["lids"] == [mute_label["lid"]]


def test_remove_remind_label_from_answered_messages(env):
    user = create_user(env)

    message1 = MessageData(user, [user])
    message1.thread_meta.merge_rule = "hash"
    message1.thread_meta.hash_namespace = "subject"
    message1.label_symbols["0"] = ["remindNoAnswer_label"]
    mid1 = http_save_and_get_mid(env, message1)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    remind_label = fetch_one(env.conn, queries.label_by_name(user.uid, "remindme_threadabout:mark", "system"))
    assert remind_label["lid"]
    assert remind_label["lid"] in msg1["lids"]

    message2 = MessageData(user, [user])
    message2.thread_meta.merge_rule = "hash"
    message2.thread_meta.hash_namespace = "subject"
    message2.subject = message1.subject
    message2.received_date = message1.received_date + 1
    mid2 = http_save_and_get_mid(env, message2)

    msg1 = fetch_one(env.conn, queries.message(user.uid, mid1))
    msg2 = fetch_one(env.conn, queries.message(user.uid, mid2))
    assert msg1["tid"] == msg2["tid"]
    assert remind_label["lid"] not in msg1["lids"]


def test_add_spam_label_for_spam_folder(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "\\Spam"
    message.no_such_folder_action["0"] = "fail"
    mid = http_save_and_get_mid(env, message)
    msg = fetch_one(env.conn, queries.message(user.uid, mid))
    assert "spam" in msg["attributes"]


def test_ignore_non_existing_lid(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.lids["0"] = ["999"]
    mid = http_save_and_get_mid(env, message)
    assert mid


def test_ignore_non_existing_label_symbol(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.label_symbols["0"] = ["xxx"]
    mid = http_save_and_get_mid(env, message)
    assert mid


def test_respond_error_on_non_existing_user(env):
    user = User(6666666, "user", "yandex.ru", "user")
    message = MessageData(user, [user])
    rcpt = http_save_and_get_rcpt(env, message)
    assert rcpt["status"] == "temp error"
    assert rcpt["description"] == "sharpei_client error: uid not found"


def test_duplicate_message(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.fid["0"] = "1"
    message.no_such_folder_action["0"] = "fail"

    rcpt1 = http_save_and_get_rcpt(env, message)
    assert rcpt1["status"] == "ok"
    assert not rcpt1["duplicate"]

    rcpt2 = http_save_and_get_rcpt(env, message)
    assert rcpt2["status"] == "ok"
    assert rcpt2["duplicate"]

    assert rcpt1["mid"] == rcpt2["mid"]


def test_ignore_duplicate_check(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.fid["0"] = "1"
    message.no_such_folder_action["0"] = "fail"
    message.ignore_duplicates["0"] = True

    rcpt1 = http_save_and_get_rcpt(env, message)
    assert rcpt1["status"] == "ok"
    assert not rcpt1["duplicate"]

    rcpt2 = http_save_and_get_rcpt(env, message)
    assert rcpt2["status"] == "ok"
    assert not rcpt2["duplicate"]

    assert rcpt1["mid"] != rcpt2["mid"]


def test_no_duplicates_for_imap_append(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.fid["0"] = "1"
    message.no_such_folder_action["0"] = "fail"
    message.ignore_duplicates["0"] = True
    message.imap["0"] = True

    rcpt1 = http_save_and_get_rcpt(env, message)
    assert rcpt1["status"] == "ok"
    assert not rcpt1["duplicate"]

    rcpt2 = http_save_and_get_rcpt(env, message)
    assert rcpt2["status"] == "ok"
    assert not rcpt2["duplicate"]


def test_duplicates_for_imap_append_into_sent(env):
    user = create_user(env)
    message = MessageData(user, [user])
    message.folder_path["0"] = "\\Sent"
    message.no_such_folder_action["0"] = "fail"
    message.ignore_duplicates["0"] = True
    message.imap["0"] = True

    rcpt1 = http_save_and_get_rcpt(env, message)
    assert rcpt1["status"] == "ok"
    assert not rcpt1["duplicate"]

    rcpt2 = http_save_and_get_rcpt(env, message)
    assert rcpt2["status"] == "ok"
    assert rcpt2["duplicate"]


def test_personal_stid(env):
    common_user = create_user(env)
    personal_user = create_user(env)
    message = MessageData(personal_user, [common_user, personal_user])
    message.size = 10000
    message.stid = "123.common.321"
    message.stids["1"] = "123.personal.321"
    message.offset_diffs["1"] = 100

    response = http_save(env, message)["rcpts"]
    for rcpt in response:
        assert rcpt["rcpt"]["status"] == "ok"
        assert rcpt["rcpt"]["mid"]

    common_msg = fetch_one(env.conn, queries.message(common_user.uid, response[0]["rcpt"]["mid"]))
    assert common_msg["st_id"] == "123.common.321"
    assert common_msg["size"] == 10000
    common_mime = fetch(env.conn, queries.mime_parts(common_user.uid, response[0]["rcpt"]["mid"]))
    offset_begin, offset_end = common_mime[0][10:12]
    assert offset_begin == 0
    assert offset_end == 1024

    personal_msg = fetch_one(env.conn, queries.message(personal_user.uid, response[1]["rcpt"]["mid"]))
    assert personal_msg["st_id"] == "123.personal.321"
    assert personal_msg["size"] == 10100
    personal_mime = fetch(env.conn, queries.mime_parts(personal_user.uid, response[1]["rcpt"]["mid"]))
    offset_begin, offset_end = personal_mime[0][10:12]
    assert offset_begin == 100
    assert offset_end == 1124
