#! /usr/bin/python

import imaplib
from utils import *
from users import *
from lock_listener import LockListener
import queries

TRASH_FOLDER = ["&bboepgrabdceoaq9bda-"]

SYSTEM_FOLDERS = [
    "inbox",
    "&bceepwqwbdw-",  # spam
    "&bb4eqgq,beaemaqybdsenqq9bd0eswq1-",  # sent
    "&bccenqrabd0epgqybdgeogq4-",  # drafts
    "&bcgemaqxbdsepgq9bes-",  # templates
] + TRASH_FOLDER


def get_mids_to_delete(cur, uid):
    messages = fetch(cur, queries.messages(uid))
    return [str(row["mid"]) for row in messages]


def clear_local(uid):
    conn = get_connection(uid)
    cur = get_cursor(conn)

    execute(cur, queries.set_user_is_here(uid, True))

    execute(cur, queries.delete_labels(uid))

    execute(cur, queries.delete_mailish_account(uid))

    mids = get_mids_to_delete(cur, uid)
    if len(mids) > 0:
        execute(cur, queries.delete_messages(uid, mids))

    execute(cur, queries.delete_mailish_folders(uid))
    execute(cur, queries.delete_user_folders(uid))

    conn.commit()
    conn.close()


def check_response(resp):
    if resp[0].lower() != "ok":
        print("imap command failed: " + str(resp))
        exit(1)


def is_system_folder(name):
    return name.lower() in SYSTEM_FOLDERS


def get_folder_name(resp):
    chunks = resp.split(" ")
    if len(chunks) == 0:
        raise Exception("can't get folder name from response: " + resp)
    return chunks[-1].strip('"')


def find_trash_folder(folders):
    for resp in folders:
        name = get_folder_name(resp)
        if name.lower() in TRASH_FOLDER:
            return name
    return None


def clear_folder(name, mailbox):
    check_response(mailbox.select(name))
    check_response(mailbox.store("1:*", "+FLAGS", "\\Deleted"))
    check_response(mailbox.close())


def clear_external(login, password, imap_host):
    mailbox = imaplib.IMAP4_SSL(imap_host)
    check_response(mailbox.login(login, password))

    ret = mailbox.list()
    check_response(ret)
    folders = ret[1]

    trash = find_trash_folder(folders)

    for resp in reversed(folders):
        name = get_folder_name(resp)
        clear_folder(name, mailbox)
        if not is_system_folder(name):
            check_response(mailbox.delete(name))

    if trash:
        clear_folder(trash, mailbox)

    mailbox.logout()


def clear_user(uid, login, password, imap_host):
    print("clear uid {} login {} imap_host {}".format(uid, login, imap_host))
    clear_external(login, password, imap_host)
    clear_local(uid)
