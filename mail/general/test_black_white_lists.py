# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from hamcrest import assert_that, none, equal_to, all_of, is_not

from lib.fids import SPAM_FID, INBOX_FID
from lib.matchers.delivery import has_delivered_message_with_msgid, has_no_delivered_messages_with_msgid
from lib.matchers.labels import has_user_label
from lib.msgs_builder import STANDARD_SPAM_TEXT, STRONG_SPAM_TEXT, VIRUS_TEXT
from lib.psql import find_mail_box_row_by_mid, find_mail_message_row_by_hdr_message_id
from lib.smtp_send_tools import send_plain_text_message
from lib.users import get_user
from lib.psql import get_fids_of_messages_with_msg_id

NOSPAM_TEXT = "NOSPAM"
# У BlackWhiteListOwner созданы фильтры:
# 1)UserInBlackList отправлен в черный список,
# 2)UserInWhiteList отправлен в белый список.
# 3)Для всех писем,кроме спама: Если «Тема» содержит «ALL_WO_SPAM» — пометить письмо меткой «ALL_WO_SPAM»
# 4)Для всех писем, включая спам: Если «Тема» содержит «ALL_WITH_SPAM» — пометить письмо меткой «ALL_WITH_SPAM»
# 5)Только для спама: Если «Тема» содержит «SPAM_ONLY» — пометить письмо меткой «SPAM_ONLY»
#
# Ниже представлены раздражители фильтров пользователя BlackWhiteListOwner и по совместительству названия меток,
# которые проставляют соотвествующие фильтры
ALL_WO_SPAM = "ALL_WO_SPAM"
ALL_WITH_SPAM = "ALL_WITH_SPAM"
SPAM_ONLY = "SPAM_ONLY"


@pytest.mark.mxback
@pytest.mark.parametrize("text_of_msg", [NOSPAM_TEXT, STANDARD_SPAM_TEXT])
def test_ham_spam_letters_nondelivery_to_visible_mailbox_from_black_listed_user(text_of_msg):
    sender = get_user("UserInBlackList")
    rcpt = get_user("BlackWhiteListOwner")

    msg_id = send_plain_text_message(sender, rcpt.email, text=text_of_msg)
    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    mail_messages_row = find_mail_message_row_by_hdr_message_id(rcpt.uid, msg_id)
    mail_box_row = find_mail_box_row_by_mid(rcpt.uid, mail_messages_row.mid)
    assert_that(mail_box_row, none())


@pytest.mark.bigmail
@pytest.mark.mxback
@pytest.mark.parametrize("text_of_msg", [NOSPAM_TEXT, STANDARD_SPAM_TEXT])
def test_ham_spam_letters_delivery_to_inbox_from_white_listed_user(text_of_msg):
    sender = get_user("UserInWhiteList")
    rcpt = get_user("BlackWhiteListOwner")

    msg_id = send_plain_text_message(sender, rcpt.email, text=text_of_msg)
    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    assert_that(get_fids_of_messages_with_msg_id(rcpt, msg_id), equal_to([INBOX_FID]))


@pytest.mark.bigmail
@pytest.mark.mxfront
@pytest.mark.mxback
@pytest.mark.mxbackout
@pytest.mark.smtp
@pytest.mark.parametrize("text_of_msg", [VIRUS_TEXT, STRONG_SPAM_TEXT])
def test_white_list_does_not_change_strongspam_and_virus_letters_nondelivery(text_of_msg):
    sender = get_user("UserInWhiteList")
    rcpt = get_user("BlackWhiteListOwner")
    msg_id = send_plain_text_message(sender, rcpt.email, text=text_of_msg, check=False)
    assert_that(rcpt, has_no_delivered_messages_with_msgid(msg_id))


@pytest.mark.bigmail
@pytest.mark.mxback
@pytest.mark.parametrize("text_of_msg, expected_fid", [(NOSPAM_TEXT, INBOX_FID), (STANDARD_SPAM_TEXT, SPAM_FID)])
def test_blacklist_logic_works_only_for_rcpt_with_blacklisted_user(text_of_msg, expected_fid):
    sender = get_user("UserInBlackList")
    rcpt = get_user("BlackWhiteListOwner")
    rcpt2 = get_user("DefaultRcpt")

    msg_id = send_plain_text_message(sender, [rcpt.email, rcpt2.email], text=text_of_msg)

    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    mail_messages_row = find_mail_message_row_by_hdr_message_id(rcpt.uid, msg_id)
    mail_box_row = find_mail_box_row_by_mid(rcpt.uid, mail_messages_row.mid)
    assert_that(mail_box_row, none())

    assert_that(rcpt2, has_delivered_message_with_msgid(msg_id))
    assert_that(get_fids_of_messages_with_msg_id(rcpt2, msg_id), equal_to([expected_fid]))


@pytest.mark.bigmail
@pytest.mark.mxback
@pytest.mark.parametrize("text_of_msg, expected_fid", [(NOSPAM_TEXT, INBOX_FID), (STANDARD_SPAM_TEXT, SPAM_FID)])
def test_whitelist_logic_works_only_for_rcpt_with_blacklisted_user(text_of_msg, expected_fid):
    sender = get_user("UserInWhiteList")
    rcpt = get_user("BlackWhiteListOwner")
    rcpt2 = get_user("DefaultRcpt")

    msg_id = send_plain_text_message(sender, [rcpt.email, rcpt2.email], text=text_of_msg)

    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))
    assert_that(get_fids_of_messages_with_msg_id(rcpt, msg_id), equal_to([INBOX_FID]))

    assert_that(rcpt2, has_delivered_message_with_msgid(msg_id))
    assert_that(get_fids_of_messages_with_msg_id(rcpt2, msg_id), equal_to([expected_fid]))


@pytest.mark.bigmail
@pytest.mark.mxback
@pytest.mark.parametrize("text_of_msg", [NOSPAM_TEXT, STANDARD_SPAM_TEXT])
def test_filters_for_white_listed_letters(text_of_msg):
    sender = get_user("UserInWhiteList")
    rcpt = get_user("BlackWhiteListOwner")

    msg_id = send_plain_text_message(sender, rcpt.email, subject=ALL_WO_SPAM + ALL_WITH_SPAM + SPAM_ONLY,
                                     text=text_of_msg)
    assert_that(rcpt, has_delivered_message_with_msgid(msg_id))

    mail_messages_row = find_mail_message_row_by_hdr_message_id(rcpt.uid, msg_id)
    mail_box_row = find_mail_box_row_by_mid(rcpt.uid, mail_messages_row.mid)
    assert_that(mail_box_row,
                all_of(has_user_label(ALL_WO_SPAM), has_user_label(ALL_WITH_SPAM), is_not(has_user_label(SPAM_ONLY))))
