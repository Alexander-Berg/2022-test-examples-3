# coding: utf-8
import json

from milkman.dairy import milkman

from mlcore.ml.models import MailList, Subscribers, SuidLookup
from mlcore.subscribe.backends.yandex_team.models import YandexTeamBackendContext
from mlcore.permissions.models import GroupPermission, ListPermission, Type
from django_intranet_stuff.models import Staff, Group, GroupMembership
from django.contrib.auth.models import User
from mlcore.ml.models import MailList, Subscribers, EmailSubscriber

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
    G(
        YandexTeamBackendContext,
        passport_name=maillist.name,
        maillist=maillist
    )
    for login in inbox or []:
        G(Subscribers, list=maillist, user=create_user(login), is_sub=True)
    for login in imap or []:
        G(Subscribers, list=maillist, user=create_user(login), is_imap=True)
    for email in emails or []:
        G(EmailSubscriber, list=maillist, email=email)
    return maillist


def create_token(token, ip='127.0.0.1'):
    from django_api_auth.models import Token
    return G(Token, token=token, ips=ip)


def prepare_database(self):
    login = 'chuck_norris'
    u = milkman.deliver(User, username=login)
    self.staff = milkman.deliver(
        Staff,
        login=login,
        user=u,
        work_email='u@yandex-team.ru'
    )
    self.user = self.staff.user

    bad_login = 'capone'
    u = milkman.deliver(User, username=bad_login)
    self.bad_staff = milkman.deliver(
        Staff,
        login=bad_login,
        user=u,
    )
    self.bad_user = self.bad_staff.user

    self.user_suid = milkman.deliver(
        SuidLookup,
        login=login
    ).suid
    self.bad_user_suid = milkman.deliver(
        SuidLookup,
        login=bad_login,
    ).suid

    self.group = milkman.deliver(Group)
    milkman.deliver(
        GroupMembership,
        staff=self.staff,
        group=self.group
    )

    self.list_open = milkman.deliver(
        MailList,
        email='list_open@yandex-team.ru',
        parent=None,
        is_open=True,
        fsuid=100
    )
    self.list_closed = milkman.deliver(
        MailList,
        parent=None,
        is_open=False,
        fsuid=101
    )
    self.list_group = milkman.deliver(
        MailList,
        parent=None,
        is_open=False,
        fsuid=102
    )
    self.list_readonly = milkman.deliver(
        MailList,
        email='readonly_email@y-t.ru',
        is_open=False,
        readonly=True,
        parent=None,
        fsuid=103
    )
    self.bad_list = milkman.deliver(
        MailList,
        is_open=False,
        parent=None,
        name='bad-girls',
        fsuid=104,
    )

    milkman.deliver(Subscribers, list=self.list_open, user=self.user, stype='imap')
    milkman.deliver(Subscribers, list=self.list_closed, user=self.user, stype='imap')
    milkman.deliver(Subscribers, list=self.list_group, user=self.user, stype='imap')

    self.read_type = milkman.deliver(Type, name='read', id=1)
    self.write_type = milkman.deliver(Type, name='write', id=2)

    milkman.deliver(
        ListPermission,
        user=self.user,
        list=self.list_closed,
        approved=True,
        type=self.read_type
    )
    milkman.deliver(
        ListPermission,
        user=self.bad_user,
        list=self.list_open,
        approved=True,
        type=self.read_type
    )
    milkman.deliver(
        ListPermission,
        user=self.bad_user,
        list=self.list_readonly,
        approved=True,
        type=self.write_type
    )

    milkman.deliver(
        GroupPermission,
        group=self.group,
        list=self.list_readonly,
        type=self.write_type
    )
    milkman.deliver(
        GroupPermission,
        group=self.group,
        list=self.list_group,
        type=self.read_type
    )

    milkman.deliver(SuidLookup, login=self.list_open.name, suid=self.list_open.fsuid)
    milkman.deliver(SuidLookup, login=self.list_closed.name, suid=self.list_closed.fsuid)
    milkman.deliver(SuidLookup, login=self.list_group.name, suid=self.list_group.fsuid)

    self.staff_maillist = milkman.deliver(
        MailList,
        parent=None,
        is_open=False,
        fsuid='1120000000044965',
        fid='2370001160000011400',
        email='staff@yandex-team.ru'
    )

    self.maillist1 = milkman.deliver(MailList, name='maillist1', email='maillist1@yandex-team.ru', 
                                     fsuid=1200395487)
    milkman.deliver(
        YandexTeamBackendContext,
        passport_name=self.maillist1.name,
        fsuid=self.maillist1.fsuid,
        fid=self.maillist1.fid,
        maillist=self.maillist1,
    )


class ApiMixin(object):
    def setUp(self):
        prepare_database(self)

    def get_default_response(self, **args):
        request = self.factory.get(self.url, args)
        response = self.view(request)
        return response

    def get_json_response(self, **extra):
        return json.loads(self.get_default_response(**extra).content)

    def test_ok(self):
        """ Проверим что отвечает 200 """
        response = self.get_default_response()
        assert response.status_code == 200
        assert response._headers['content-type'] == ('Content-Type',
                                                     'application/json')

