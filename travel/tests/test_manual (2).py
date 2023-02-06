# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

from travel.rasp.library.mail_sender import MailSender

from_mail = 'noreply@yandex-team.ru'
to_mail = '{}@yandex-team.ru'.format(os.getlogin())
host = 'outbound-relay.yandex.net'


class TestMailSender(object):
    def setup_method(self, method):
        self._sender = MailSender(
            host=host,
            from_email=from_mail,
        )

    def test_send_mail_without_content(self):
        self._sender.send_email(
            to_emails=[
                to_mail,
            ],
            subject='фыр фыр',
            body='<h1 style="color: red">фыр фыр</h1>',
            files=[],
        )

    def test_send_mail_with_csv(self):
        csv_file = (
            'файл.csv',
            'partner\tmoney\n'
            'ruset\t1000\n'
        )
        self._sender.send_email(
            to_emails=[
                to_mail,
            ],
            subject='фыр фыр',
            body='<h1 style="color: green">фыр фыр</h1>',
            files=[csv_file],
        )
