# coding: utf-8

from __future__ import unicode_literals

import mock
from django.core.mail import EmailMessage

from tester.utils.replace_setting import replace_setting


@replace_setting('EMAIL_USE_TLS', False)
@replace_setting('EMAIL_BACKEND', 'travel.rasp.admin.lib.redirect_email_backend.EmailBackend')
@replace_setting('REDIRECT_EMAILS', ['r@r.ru'])
@replace_setting('REDIRECT_EMAILS_SUBJECT_PREFIX', 'prefix')
@mock.patch('smtplib.SMTP')
def test_redirect(m_smtp):
    instance = mock.Mock()
    m_smtp.return_value = instance

    message = EmailMessage('subject', 'body', from_email='f@f.ru', to=['t@t.ru', 'b@b.ru'])
    message.send(fail_silently=False)

    instance.sendmail.assert_called_once_with('f@f.ru', ['r@r.ru'], mock.ANY)
    message_lines = instance.sendmail.call_args[0][2].splitlines()

    assert 'From: f@f.ru' in message_lines
    assert 'To: r@r.ru' in message_lines
    assert 'body' in message_lines
    assert 'Subject: prefix: subject {to: t@t.ru, b@b.ru}' in message_lines
