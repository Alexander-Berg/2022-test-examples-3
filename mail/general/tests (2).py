# coding: utf-8

from django.test import TestCase
from mlcore.subscribe.backends.yandex_team.models import YandexTeamBackendContext
from mlcore.ml.models import MailList, CorporateDomain
from django_intranet_stuff.models import Staff
from django.contrib.auth.models import User
from mlcore.tasks import base, low_level, operations
from milkman.dairy import milkman
from datetime import datetime
import time
from mlcore.interaction.pdd import PDDError
from django.conf import settings

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


def create_maillist(maillist_name, domain='yandex-team.ru', readonly=False, is_deleted=False,
                    is_sub=True, is_imap=False, use_cmail=False, fsuid=None):
    maillist_email = maillist_name+'@'+domain
    imap_name = maillist_name.replace('.', '-')
    if domain != 'yandex-team.ru':
        imap_name = maillist_name.replace('.', '-')+'-at-'+domain.replace('.', '-')
        maillist_email = imap_name+'@'+domain
        maillist_name = imap_name
    maillist = G(MailList, name=maillist_name, email=maillist_email, is_sub=is_sub, is_imap=is_imap,
                 readonly=readonly, is_deleted=is_deleted, modified_at=datetime.now(), use_cmail=use_cmail)
    G(YandexTeamBackendContext, passport_name=imap_name, maillist=maillist, fsuid=fsuid)
    return maillist


def create_corp_domain(domain_name, is_pdd=False, is_corp=True, is_altdomain=False, comment='test domain'):
    mx_type = None
    if is_pdd:
        mx_type = CorporateDomain.MX_PDD
    elif is_corp:
        mx_type = CorporateDomain.MX_CORP

    return milkman.deliver(
        CorporateDomain,
        domain=domain_name,
        mx_type=mx_type,
        is_altdomain=is_altdomain,
        comment=comment,
    )


class CmailTasksTestCase(TestCase):
    def setUp(self):
        self.maillist_use_cmail = create_maillist('use_cmail', use_cmail=True)
        self.maillist_not_cmail = create_maillist('not_use_cmail', use_cmail=False)
        self.context = {
            'initiator': 'test_admin',
            'comment': u'Test cmail procedures',
        }
        self.user = create_user('testuser')

    def get_full_data(self, maillist_name, domain='yandex-team.ru'):
        return {
            'domain': domain, 'email': maillist_name+'@'+domain, 'imap_name': maillist_name,
            'info': maillist_name, 'info_en': maillist_name,
            'is_internal': True, 'is_open': True,
            'name': maillist_name+'@'+domain, 'omit_yandex_team': False, 'responsible': set([self.user.email])
        }

    def not_cmail_tasks(self):
        for f_task in (base.subscribe_inbox, base.subscribe_both):
            r = f_task(self.context, self.user, self.maillist_not_cmail).delay()
            self.assertEqual(r.status, 'SUCCESS')

        data_not_cmail = self.get_full_data('maillist_not_cmail')
        r = base.create_maillist(self.context, data_not_cmail, self.user).delay()
        self.assertEqual(r.status, 'SUCCESS')



class CreateMaillistTasksTestCase(TestCase):
    def setUp(self):
        self.user = create_user('robot-yndx-maillists')

    def get_full_data(self, maillist_name, domain='yandex-team.ru'):
        maillist_email = maillist_name+'@'+domain
        imap_name = maillist_name.replace('.', '-')
        if domain != 'yandex-team.ru':
            imap_name = maillist_name.replace('.', '-')+'-at-'+domain.replace('.', '-')
        return {
            'domain': domain, 'email': maillist_email, 'imap_name': imap_name,
            'info': 'test', 'info_en': 'test',
            'is_internal': True, 'is_open': True,
            'name': maillist_email, 'omit_yandex_team': False, 'responsible': set([self.user.email])
        }

    def create_maillist_with_check(self, domain, maillist_name='test_support', fail=False):
        data = self.get_full_data(maillist_name, domain)
        context = {
            'initiator': 'test_admin',
            'comment': u'Test create maillist: %s' % data['email'],
        }
        r = operations.create_maillist.delay(context, data, self.user)
        time.sleep(2)
        if fail:
            failure_statuses = []
            for child in r.children:
                time.sleep(1)
                if child.status == 'FAILURE':
                    failure_statuses.append(child.status)
            self.assertNotEqual(len(failure_statuses), 0)
        else:
            self.assertEqual(r.status, 'SUCCESS')

    def test_create_maillist_with_yandex_team_ru(self):
        create_corp_domain('yandex-team.ru')
        self.create_maillist_with_check('yandex-team.ru')

    def test_create_maillist_with_passport_alias_and_corp_domain_in_ml(self):
        create_corp_domain('auto.ru', is_altdomain=True)
        self.create_maillist_with_check('auto.ru')

    def test_create_with_pdd_domain_corp_domain_in_ml_and_not_register_in_pdd(self):
        create_corp_domain('adfox.ru', is_pdd=True)
        self.create_maillist_with_check('adfox.ru')

    # def test_create_with_pdd_domain_corp_domain_in_ml_and_other_admin_uid_in_pdd(self):
    #     create_corp_domain('pm1.havroshik.ru', is_pdd=True)
    #     self.create_maillist_with_check('pm1.havroshik.ru', fail=True)

    def test_create_with_pdd_domain_corp_domain_in_ml_and_register_in_pdd(self):
        create_corp_domain('adv.yandex.ru', is_pdd=True)
        self.create_maillist_with_check('adv.yandex.ru')

    # def test_create_with_pdd_domain_corp_domain_not_in_ml(self):
    #     self.create_maillist_with_check('ololo.yandex.ru', fail=True)

    def test_create_exist_maillist_in_yateam_blacklist(self):
        self.create_maillist_with_check('yandex-team.ru', maillist_name='support', fail=True)

    def test_create_exist_maillist_in_big_blacklist(self):
        self.create_maillist_with_check('support.yandex.ru', maillist_name='partner', fail=False)

