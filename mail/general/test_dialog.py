# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from hamcrest import assert_that, equal_to, is_not

from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.msgs_builder import build_plain_text_message
from lib.random_generator import get_random_string
from lib.smtp_send_tools import send_letter
from lib.users import get_users
from threading_data import find_tid_for_user

LONG_RANDOM_PREFIX = get_random_string(30)
RANDOM_PREFIX = get_random_string(5)
SUBJECT = "DialogThreadTest " + LONG_RANDOM_PREFIX
RE_SUBJECT = "Re: {subject}".format(subject=SUBJECT)


def store_new_letter_and_find_tid(from_user, to_user, headers):
    msg = build_plain_text_message(from_email=from_user.email, to_emails=to_user.email,
                                   text=RANDOM_PREFIX + " text_of_letter_was_here.",
                                   headers=headers)
    send_letter(None, to_user.email, msg)
    assert_that(to_user, has_delivered_message_with_msgid(msg[Header.MESSAGE_ID]))
    return find_tid_for_user(to_user, msg[Header.MESSAGE_ID])


@pytest.mark.mxback
@pytest.mark.yaback
def test_threading_dialog():
    first_letter_message_id = "<firstLetterMid@" + LONG_RANDOM_PREFIX + "-1>"
    second_letter_message_id = "<SecondLetterMid@" + LONG_RANDOM_PREFIX + "-1>"
    references = first_letter_message_id
    user1, user2 = get_users("TestThreadingInDialog1", "TestThreadingInDialog2")
    # первый шаг диалога
    thread_id_in_first_user = store_new_letter_and_find_tid(from_user=user1, to_user=user2,
                                                            headers={Header.SUBJECT: SUBJECT,
                                                                     Header.X_YANDEX_SPAM: "1",
                                                                     Header.MESSAGE_ID:
                                                                         first_letter_message_id})
    thread_id_in_second_user = store_new_letter_and_find_tid(from_user=user2, to_user=user1,
                                                             headers={Header.SUBJECT: RE_SUBJECT,
                                                                      Header.X_YANDEX_SPAM: "1",
                                                                      Header.MESSAGE_ID:
                                                                          second_letter_message_id,
                                                                      Header.IN_REPLY_TO:
                                                                          first_letter_message_id,
                                                                      Header.REFERENCES: references})
    references = references + "\n " + second_letter_message_id

    for i in xrange(2, 5):  # проводим несколько шагов диалога
        first_letter_message_id = "<firstLetterMid@{rnd}-{i}>".format(rnd=LONG_RANDOM_PREFIX, i=i)
        tid = store_new_letter_and_find_tid(from_user=user1, to_user=user2,
                                            headers={Header.SUBJECT: RE_SUBJECT,
                                                     Header.X_YANDEX_SPAM: "1",
                                                     Header.MESSAGE_ID: first_letter_message_id,
                                                     Header.IN_REPLY_TO: second_letter_message_id,
                                                     Header.REFERENCES: references})
        references = references + "\n " + first_letter_message_id
        error_msg = "Неверный  tid для письма от первого пользователя второму"
        assert_that(tid, equal_to(thread_id_in_first_user), error_msg)

        second_letter_message_id = "<secondLetterMid@{rnd}-{i}>".format(rnd=LONG_RANDOM_PREFIX, i=i)
        tid = store_new_letter_and_find_tid(from_user=user2, to_user=user1,
                                            headers={Header.SUBJECT: RE_SUBJECT,
                                                     Header.X_YANDEX_SPAM: "1",
                                                     Header.MESSAGE_ID: second_letter_message_id,
                                                     Header.IN_REPLY_TO: first_letter_message_id,
                                                     Header.REFERENCES: references})
        references = references + "\n " + second_letter_message_id
        error_msg = "Неверный tid для письма от второго пользователя первому"
        assert_that(tid, equal_to(thread_id_in_second_user), error_msg)

    error_msg = "id-треда у одного получателя не должны совпадать с id-треда у второго"
    assert_that(thread_id_in_second_user, is_not(equal_to(thread_id_in_first_user)), error_msg)
