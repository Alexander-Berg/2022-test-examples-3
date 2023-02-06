# coding: utf-8
from django.test import TestCase
from django.contrib.auth.models import User
import datetime
import time
from django_intranet_stuff.models import Staff, Group, GroupMembership
from mlcore.ml.models import MailList, Subscribers, EmailSubscriber

from milkman.dairy import milkman
G = milkman.deliver


def create_user(login, has_exchange=False):
    if isinstance(login, basestring):
        staff = G(Staff, login=login, work_email=login + '@yandex-team.ru', has_exchange=has_exchange)
        user = G(User, login=login, email=staff.work_email)
        user.staff = staff
        user.save()
        return user
    else:
        return login


def create_maillist(name, inbox=None, imap=None, emails=None):
    if '@' not in name:
        name = '%s@yandex-team.ru' % name
    maillist = G(MailList, email=name, name=name.split('@')[0], alias=name.split('@')[0])
    for login in inbox or []:
        G(Subscribers, list=maillist, user=create_user(login), is_sub=True)
    for login in imap or []:
        G(Subscribers, list=maillist, user=create_user(login), is_imap=True)
    for email in emails or []:
        G(EmailSubscriber, list=maillist, email=email)
    return maillist


class ModelsTest(TestCase):

    def setUp(self):
        super(ModelsTest, self).setUp()

    def test_modified_at(self, **kw):
        """ Проверка 'рассылка подписана на рассылку' """

        A = create_maillist('A', emails=['A@yandex-team.ru'])
        B = create_maillist('B', emails=['B@yandex-team.ru'])

        now = datetime.datetime.now()
        time.sleep(0.1)
        A.set_as_modified()

        # текущий объект не меняется, надо перечитать
        A = MailList.objects.get(pk=A.pk)
        B = MailList.objects.get(pk=B.pk)

        self.assertTrue(A.modified_at > now)
        self.assertTrue(B.modified_at == B.modified_at)
