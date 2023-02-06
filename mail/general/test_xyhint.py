# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from email.utils import make_msgid

from hamcrest import assert_that

from lib.matchers.delivery import has_delivered_message_with_msgid
from lib.psql import find_mail_message_row_by_hdr_message_id
from lib.storage import download_message
from lib.users import get_users
from lib.smtp_send_tools import send_plain_text_message
from lib.hint import make_x_yandex_hint


def test_x_yandex_hint_removed_before_put_to_mds(is_yarovaya_enabled):
    # MAILDLV-3313
    sender, rcpt = get_users("DefaultSender", "DefaultRcpt")

    name, value = make_x_yandex_hint(label="SystMetkaSO:people")
    msg_id = make_msgid()
    send_plain_text_message(sender, [rcpt.email], msg_id=msg_id, headers={name: value})

    users = [rcpt]
    if is_yarovaya_enabled:
        users.append(sender)

    for user in users:
        assert_that(user, has_delivered_message_with_msgid(msg_id))
        msg_meta = find_mail_message_row_by_hdr_message_id(user.uid, msg_id)
        msg = download_message(msg_meta.st_id)
        assert "X-Yandex-Hint" not in msg, \
            "[MAILDLV-3313] X-Yandex-Hint must be removed before put to MDS, stid: %s" % msg_meta.st_id
