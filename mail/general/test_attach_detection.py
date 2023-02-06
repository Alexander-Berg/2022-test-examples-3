# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest
from hamcrest import assert_that, equal_to

from lib.headers import Header
from lib.matchers.delivery import has_delivered_message_with_subject
from lib.msgs_builder import build_message_from_file
from lib.psql import find_mail_message_row_by_subject
from lib.random_generator import get_random_string
from lib.smtp_send_tools import send_letter
from lib.users import get_user
from lib.utils import get_letter_path, get_description


# Основной тикет-свод правил по аттачам: [MPROTO-9]
#
# В итоге сейчас считаем аттачами парты:
# c параметром name из Content-Type или filename из Content-Disposition;
# c Content-Type: message/rfc822; являющиеся альтернативными партами с Content-Type не text.
#
# Исключения: наличие Content-Disposition: inline; Content-Type: application/pkcs7-signature;
# наличие Content-ID; название "smime.p7s".
# [MPROTO-3053] Парты с Content-Type: application/pgp-signature не считаем аттачами
#
# [MPROTO-2462]"Дисковые аттачи" пусть и инлайновые - считаются аттачами,
# определяются по наличию filename:narod_attachment_links.html.

@pytest.mark.mxback
@pytest.mark.yaback
@pytest.mark.mxfront
@pytest.mark.parametrize("eml_name, expected", get_description("attach_detection_yes").items())
def test_detect_attach(eml_name, expected):
    rcpt = get_user("DefaultRcpt")

    subject = get_random_string(50)
    fd = open(get_letter_path("attach_detection_yes", eml_name))
    msg = build_message_from_file(fd, rcpt.email, {Header.SUBJECT: subject})
    send_letter(None, rcpt.email, msg)

    assert_that(rcpt, has_delivered_message_with_subject(subject))
    assert_that(sorted(find_mail_message_row_by_subject(rcpt.uid, subject).attaches),
                equal_to(sorted(expected["attaches"])),
                "Некоректный список attach-ей для письма {eml_name} для тестового кейса: \"{comment}\"".format(
                    eml_name=eml_name, comment=expected["comment"]))


@pytest.mark.mxback
@pytest.mark.yaback
@pytest.mark.mxfront
@pytest.mark.parametrize("eml_name, expected", get_description("attach_detection_no").items())
def test_detect_no_attaches(eml_name, expected):
    rcpt = get_user("DefaultRcpt")

    subject = get_random_string(50)
    fd = open(get_letter_path("attach_detection_no", eml_name))
    msg = build_message_from_file(fd, rcpt.email, {Header.SUBJECT: subject})
    send_letter(None, rcpt.email, msg)

    assert_that(rcpt, has_delivered_message_with_subject(subject))
    assert not find_mail_message_row_by_subject(rcpt.uid, subject).attaches, \
        "Ошибочно обнаружены attach-и для письма {eml_name} для тестового кейса: \"{comment}\"".format(
            eml_name=eml_name, comment=expected["comment"])
