# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

from travel.rasp.bus.library.mail_sender import mail_sender

to_mail = '{}@yandex-team.ru'.format(os.getlogin())


def test_send_mail_without_content():
    mail_sender.send_email(
        to_emails=[
            to_mail,
        ],
        subject='фыр фыр',
        body='<h1 style="color: red">фыр фыр</h1>',
        files=[],
    )


def test_send_mail_with_csv():
    csv_file = (
        'some_important.csv',
        'partner\tmoney\n'
        'ruset\t1000\n'
    )
    mail_sender.send_email(
        to_emails=[
            to_mail,
        ],
        subject='фыр фыр',
        body='<h1 style="color: green">фыр фыр</h1>',
        files=[csv_file],
    )
