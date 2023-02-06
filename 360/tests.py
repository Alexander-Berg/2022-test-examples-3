# -*- coding: utf-8 -*-

from functools import wraps

import simplejson
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Group, Permission
from django.core.management import call_command
from django.core.urlresolvers import reverse
from django.test import Client, TestCase
from django_yauth.user import YandexUser, YandexUserDescriptor
from mock import MagicMock, patch

from .models import GlobalPermission
from .utils import clean_search_string, is_valid_ip, strip_email


def delete_objects():
    GlobalPermission.objects.all().delete()
    get_user_model().objects.all().delete()
    Group.objects.all().delete()


def patch_yauser_authentication(username, is_authenticated=True):
    """
    :param username: is only for print
    :param is_authenticated: bool
    :return: patch_yauser
    """

    def patch_yauser(func):
        @wraps(func)
        @patch.object(YandexUserDescriptor, '_get_yandex_user', autospec=True)
        def inner(*args, **kwargs):
            user_to_mock = YandexUser(12344, False,
                                      {'display_name': username,
                                       'login': username},
                                      False, [], '')

            user_to_mock.is_authenticated = MagicMock(return_value=is_authenticated)
            args[-1].return_value = user_to_mock
            return func(*args[:-1], **kwargs)

        return inner

    return patch_yauser


class WinxAdminTest():

    @classmethod
    def setUpClass(cls):
        # create groups with perms
        call_command('winx_update_permission_groups')

    @classmethod
    def tearDownClass(cls):
        delete_objects()

    def tearDown(self):
        get_user_model().objects.all().delete()

    def add_user_role(self, login, role, status_code=200):
        cl = Client()
        _role = simplejson.dumps({'role': role})
        resp = cl.post(reverse('add-role'), data=dict(login=login, role=_role))
        self.assertEqual(resp.status_code, status_code)
        # for example
        # self.add_user_role('terran', 'security')

    @patch_yauser_authentication(username='terran', is_authenticated=True)
    def test_user_authentication_true(self):
        self.client.login()
        resp = self.client.get(reverse('homepage'))
        self.assertEqual(resp.status_code, 200)

    @patch_yauser_authentication(username='terran', is_authenticated=False)
    def test_user_authentication_false(self):
        self.client.login()
        resp = self.client.get(reverse('homepage'))
        self.assertEqual(resp.status_code, 302)

    @patch_yauser_authentication(username='terran', is_authenticated=True)
    def test_user_perm_view_support_actions(self):
        # terran has no roles
        self.client.login()
        resp = self.client.get(reverse('support_actions'))
        self.assertEqual(resp.status_code, 200)
        self.assertIn('Access denied!', resp.content)
        self.assertTemplateUsed(resp, 'access_denied.html')
        self.assertTemplateNotUsed(resp, 'support_actions.html')

        # terran has other role
        self.add_user_role('terran', 'support-light')
        self.client.login()
        resp = self.client.get(reverse('support_actions'))
        self.assertEqual(resp.status_code, 200)
        self.assertIn('Access denied!', resp.content)
        self.assertTemplateUsed(resp, 'access_denied.html')
        self.assertTemplateNotUsed(resp, 'support_actions.html')

        # grant right role with right perm
        self.add_user_role('terran', 'security')
        self.client.login()
        resp = self.client.get(reverse('support_actions'))
        self.assertEqual(resp.status_code, 200)
        self.assertTemplateUsed(resp, 'support_actions.html')
        self.assertTemplateNotUsed(resp, 'access_denied.html')


class TestUtils(TestCase):

    def test_clean_search_string(self):
        self.assertEqual(clean_search_string(''), '')
        self.assertEqual(clean_search_string('somelogin'), 'somelogin')
        self.assertEqual(clean_search_string('   somelogin   '), 'somelogin')
        self.assertEqual(clean_search_string('  >  <<somelogin  >>  <<'), 'somelogin')
        self.assertEqual(clean_search_string('request@domain.com'), 'request@domain.com')
        self.assertEqual(clean_search_string('request@domain.com  >'), 'request@domain.com')
        self.assertEqual(clean_search_string('<   request<<<>>>@ domain.<<<com  >>>'), 'request@domain.com')
        self.assertEqual(clean_search_string('request@yandex.ru>'), 'request@yandex.ru')
        self.assertEqual(clean_search_string('>>uid:  123312123<<'), 'uid:123312123')
        self.assertEqual(clean_search_string('>>suid:     123312123<<'), 'suid:123312123')
        self.assertEqual(clean_search_string(u'  < somelogin@доменко.рф  <<'), u'somelogin@доменко.рф')
        self.assertEqual(clean_search_string(None), None)
        self.assertEqual(clean_search_string(123), 123)

    def test_strip_email(self):
        test_cases = [
            # (None, None),
            # (123, 123),
            ('', ''),
            ('somelogin', 'somelogin'),
            ('uid: 12345', 'uid: 12345'),
            ('request@domain.com', 'request@domain.com'),
            ('request@domain.com  >>>', 'request@domain.com'),
        ]

        #     (u'< somelogin@доменко.рф', u'somelogin@доменко.рф'),
        #     (u'< somelogin@доменко.рф >>', u'somelogin@доменко.рф'),
        #     ('login@yandex.ru%werty', 'login@yandex.ru'),
        #     ('login@yandex.ru$werr', 'login@yandex.ru'),
        #     ('login@yandex.ru,werr', 'login@yandex.ru'),
        #     ('login@yandex.ru$werr', 'login@yandex.ru'),
        #     ('login@yandex.ru$werr', 'login@yandex.ru'),
        # ]

        for raw_string, result in test_cases:
            self.assertEqual(strip_email(raw_string), result)

    def test_is_valid_ip(self):
        test_cases = [
            ('123', False),
            ('123.0.0', False),
            ('123.0.0.277', False),
            ('127.0.0. 1', False),
            ('127.0.0.1', True),
            ('::', True),
            ('::1', True),
            ('FF01::101', True),
            ('::FFFF:129.144.52.38', True),
            ('FEDC:BA98:7654:3210:FEDC:BA98:7654:3210', True),
        ]
        for ip_string, result in test_cases:
            self.assertEqual(is_valid_ip(ip_string), result)


class GlobalPermissionModelTest(TestCase):

    def tearDown(self):
        delete_objects()

    def test_proxy_permissions(self):
        # there are some permissions of models
        perms_count = Permission.objects.all().count()
        self.assertTrue(perms_count > 0)
        # self.assertEqual(perms_count, 36)

        # but GlobalPermission is empty
        self.assertEqual(GlobalPermission.objects.all().count(), 0)

        perm = GlobalPermission.objects.create(codename='can_do_it', name='Can do it')
        perm.save()

        self.assertEqual(Permission.objects.all().count(), perms_count + 1)
        self.assertEqual(GlobalPermission.objects.all().count(), 1)

    def test_update_permission_groups_command(self):
        self.assertEqual(GlobalPermission.objects.all().count(), 0)
        self.assertEqual(Group.objects.all().count(), 0)

        call_command('winx_update_permission_groups')
        self._check_group_permissions()

        # run command again - nothing will change
        call_command('winx_update_permission_groups')
        self._check_group_permissions()

    def test_user_permissions(self):
        self.assertEqual(GlobalPermission.objects.all().count(), 0)
        self.assertEqual(Group.objects.all().count(), 0)

        # create permissions and groups
        call_command('winx_update_permission_groups')
        self._check_group_permissions()

        perms = dict((p.codename, p) for p in Permission.objects.filter(content_type__name="global_permission"))

        # check if user without group has permissions
        terran = get_user_model().objects.create(username='terran')
        self.assertEqual(terran.groups.count(), 0)
        self.assertEqual(len(terran.get_all_permissions()), 0)
        for perm in GlobalPermission.objects.all():
            self.assertFalse(terran.has_perm(perm))

        # check if user in some group has permissions
        group = Group.objects.get(name='abuse')
        terran.groups.add(group)
        terran.save()
        terran = get_user_model().objects.get(username='terran')
        self.assertEqual(terran.groups.count(), 1)

        self.assertTrue(terran.has_perm('yandex_winx_admin.view_userinfo'))
        self.assertTrue(terran.has_perm('yandex_winx_admin.logout_change_password'))
        self.assertTrue(terran.has_perm('yandex_winx_admin.lock_user'))
        self.assertTrue(terran.has_perm('yandex_winx_admin.view_lock_history'))
        self.assertTrue(terran.has_perm('yandex_winx_admin.dkim_verify'))
        self.assertEqual(len(terran.get_all_permissions()), 5)

        permissions = set('yandex_winx_admin.%s' % p for p in perms)

        granted = {
            'yandex_winx_admin.view_userinfo', 'yandex_winx_admin.logout_change_password',
            'yandex_winx_admin.lock_user', 'yandex_winx_admin.view_lock_history',
            'yandex_winx_admin.dkim_verify'
        }
        self._check_user_group_permissions(username='user1',
                                           groupname='abuse',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {
            'yandex_winx_admin.view_userinfo', 'yandex_winx_admin.view_lock_history',
            'yandex_winx_admin.view_filters', 'yandex_winx_admin.view_blacklist',
            'yandex_winx_admin.view_whitelist', 'yandex_winx_admin.view_mail_settings',
            'yandex_winx_admin.view_folders', 'yandex_winx_admin.view_labels',
            'yandex_winx_admin.view_collectors', 'yandex_winx_admin.view_recalculators',
            'yandex_winx_admin.view_other_settings', 'yandex_winx_admin.view_user_journal',
            'yandex_winx_admin.view_logs', 'yandex_winx_admin.view_reminders', 'yandex_winx_admin.view_antispam',
            'yandex_winx_admin.enable_filters', 'yandex_winx_admin.launch_reindexation',
            'yandex_winx_admin.launch_counters_recalculation', 'yandex_winx_admin.toggle_social_avatars',
            'yandex_winx_admin.toggle_team_social_avatars', 'yandex_winx_admin.transfer_user_back_to_oracle',
            'yandex_winx_admin.dkim_verify', 'yandex_winx_admin.edit_collectors',

            'yandex_winx_admin.view_organization_settings',
            'yandex_winx_admin.view_organization_imports',
            'yandex_winx_admin.view_organization_import_logs',
        }
        self._check_user_group_permissions(username='user2',
                                           groupname='support_light',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {
            'yandex_winx_admin.launch_reindexation', 'yandex_winx_admin.launch_counters_recalculation',
            'yandex_winx_admin.edit_mailbox_size', 'yandex_winx_admin.clear_folders',
            'yandex_winx_admin.clear_address_book', 'yandex_winx_admin.mark_folders_as_read',
            'yandex_winx_admin.remove_labels', 'yandex_winx_admin.make_imap_folders_english',
            'yandex_winx_admin.switch_imap', 'yandex_winx_admin.switch_pop', 'yandex_winx_admin.edit_filters',
            'yandex_winx_admin.enable_all_filters', 'yandex_winx_admin.check_pop3_delivery',
            'yandex_winx_admin.toggle_social_avatars', 'yandex_winx_admin.toggle_team_social_avatars',
            'yandex_winx_admin.transfer_user_back_to_oracle', 'yandex_winx_admin.dkim_verify'
        }
        self._check_user_group_permissions(username='user3',
                                           groupname='support_spec',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {
            'yandex_winx_admin.make_imap_folders_english', 'yandex_winx_admin.switch_imap',
            'yandex_winx_admin.switch_pop', 'yandex_winx_admin.check_pop3_delivery',
            'yandex_winx_admin.dkim_verify'
        }
        self._check_user_group_permissions(username='user7',
                                           groupname='protocols_support',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {
            'yandex_winx_admin.view_user_journal', 'yandex_winx_admin.view_team_user_journal',
            'yandex_winx_admin.dkim_verify', 'yandex_winx_admin.view_logs', 'yandex_winx_admin.view_team_logs'
        }
        self._check_user_group_permissions(username='user8',
                                           groupname='journal_viewer',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {
            'yandex_winx_admin.view_team_userinfo', 'yandex_winx_admin.view_team_lock_history',
            'yandex_winx_admin.view_team_filters', 'yandex_winx_admin.view_team_blacklist',
            'yandex_winx_admin.view_team_whitelist', 'yandex_winx_admin.view_team_mail_settings',
            'yandex_winx_admin.view_team_folders', 'yandex_winx_admin.view_team_labels',
            'yandex_winx_admin.view_team_collectors', 'yandex_winx_admin.edit_team_collectors',
            'yandex_winx_admin.view_team_recalculators', 'yandex_winx_admin.view_team_other_settings',
            'yandex_winx_admin.view_team_user_journal', 'yandex_winx_admin.view_team_reminders',
            'yandex_winx_admin.enable_team_filters', 'yandex_winx_admin.edit_team_filters',
            'yandex_winx_admin.launch_team_reindexation', 'yandex_winx_admin.launch_team_counters_recalculation',
            'yandex_winx_admin.edit_team_mailbox_size', 'yandex_winx_admin.clear_team_folders',
            'yandex_winx_admin.clear_team_address_book', 'yandex_winx_admin.mark_team_folders_as_read',
            'yandex_winx_admin.remove_team_labels', 'yandex_winx_admin.make_team_imap_folders_english',
            'yandex_winx_admin.switch_team_imap', 'yandex_winx_admin.switch_team_pop',
            'yandex_winx_admin.dkim_verify', 'yandex_winx_admin.view_team_logs'
        }
        self._check_user_group_permissions(username='user4',
                                           groupname='support_internal',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {
            'yandex_winx_admin.view_userinfo', 'yandex_winx_admin.view_filters',
            'yandex_winx_admin.view_blacklist', 'yandex_winx_admin.view_whitelist',
            'yandex_winx_admin.view_mail_settings', 'yandex_winx_admin.view_folders',
            'yandex_winx_admin.view_labels', 'yandex_winx_admin.view_collectors',
            'yandex_winx_admin.view_other_settings', 'yandex_winx_admin.view_user_journal',
            'yandex_winx_admin.view_antispam', 'yandex_winx_admin.view_reminders',
            'yandex_winx_admin.check_pop3_delivery', 'yandex_winx_admin.view_team_userinfo',
            'yandex_winx_admin.view_team_filters', 'yandex_winx_admin.view_team_blacklist',
            'yandex_winx_admin.view_team_whitelist', 'yandex_winx_admin.view_team_mail_settings',
            'yandex_winx_admin.view_team_folders', 'yandex_winx_admin.view_team_labels',
            'yandex_winx_admin.view_team_collectors', 'yandex_winx_admin.view_team_reminders',
            'yandex_winx_admin.edit_team_collectors', 'yandex_winx_admin.view_team_other_settings',
            'yandex_winx_admin.view_team_user_journal', 'yandex_winx_admin.check_team_pop3_delivery',
            'yandex_winx_admin.toggle_social_avatars', 'yandex_winx_admin.toggle_team_social_avatars',
            'yandex_winx_admin.dkim_verify', 'yandex_winx_admin.view_logs', 'yandex_winx_admin.view_team_logs',
            'yandex_winx_admin.make_imap_folders_english', 'yandex_winx_admin.edit_collectors',
            'yandex_winx_admin.ps_billing_refund',
            'yandex_winx_admin.view_organization_settings',
            'yandex_winx_admin.view_organization_imports', 'yandex_winx_admin.update_organization_imports',
            'yandex_winx_admin.view_organization_import_logs',
        }
        self._check_user_group_permissions(username='user5',
                                           groupname='admin',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {'yandex_winx_admin.view_support_actions', 'yandex_winx_admin.dkim_verify'}
        self._check_user_group_permissions(username='user6',
                                           groupname='security',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {'yandex_winx_admin.view_support_actions', 'yandex_winx_admin.dkim_verify'}
        self._check_user_group_permissions(username='user6',
                                           groupname='security',
                                           granted=granted,
                                           restricted=permissions - granted)

        # add user5 to another group
        granted = {
            'yandex_winx_admin.view_userinfo', 'yandex_winx_admin.logout_change_password',
            'yandex_winx_admin.lock_user', 'yandex_winx_admin.view_lock_history',
            'yandex_winx_admin.view_support_actions', 'yandex_winx_admin.dkim_verify'
        }
        self._check_user_group_permissions(username='user6',
                                           groupname='abuse',
                                           granted=granted,
                                           restricted=permissions - granted)

        # update perms - grants have to stay the same
        call_command('winx_update_permission_groups')
        self._check_group_permissions()

        self._check_user_group_permissions(username='user6',
                                           groupname='abuse',
                                           granted=granted,
                                           restricted=permissions - granted)

        granted = {
            'yandex_winx_admin.view_message_info', 'yandex_winx_admin.view_team_message_info',
            'yandex_winx_admin.view_mailbox_list', 'yandex_winx_admin.view_team_mailbox_list',
            'yandex_winx_admin.purge_message', 'yandex_winx_admin.purge_team_message'
        }
        self._check_user_group_permissions(username='user9',
                                           groupname='god_mode',
                                           granted=granted,
                                           restricted=permissions - granted)

    def _check_user_group_permissions(self, username, groupname, granted, restricted):
        user, created = get_user_model().objects.get_or_create(username=username)

        # there are no permissions at first
        if created:
            for perm in granted:
                self.assertFalse(user.has_perm(perm))
            for perm in restricted:
                self.assertFalse(user.has_perm(perm))

        user.groups.add(Group.objects.get(name=groupname))
        user.save()
        user = get_user_model().objects.get(username=username)

        # and now permissions are there
        for perm in granted:
            self.assertTrue(user.has_perm(perm))
        for perm in restricted:
            self.assertFalse(user.has_perm(perm))

    def _check_group_permissions(self, perms_count=78, groups_count=10):
        self.assertEqual(GlobalPermission.objects.all().count(), perms_count)
        self.assertEqual(Group.objects.all().count(), groups_count)

        perms = dict((p.codename, p) for p in Permission.objects.filter(content_type__name="global_permission"))

        group = Group.objects.get(name='abuse')
        perms_names = ['view_userinfo', 'logout_change_password', 'lock_user', 'view_lock_history', 'dkim_verify']
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())

        group = Group.objects.get(name='support_light')
        perms_names = [
            'view_userinfo',
            'view_lock_history',
            'view_filters',
            'view_blacklist',
            'view_whitelist',
            'view_mail_settings',
            'view_folders',
            'view_labels',
            'view_collectors',
            'view_recalculators',
            'view_other_settings',
            'view_user_journal',
            'view_antispam',
            'view_logs',
            'enable_filters',
            'launch_reindexation',
            'launch_counters_recalculation',
            'toggle_social_avatars',
            'toggle_team_social_avatars',
            'transfer_user_back_to_oracle',
            'dkim_verify',
            'view_reminders',
            'edit_collectors',

            'view_organization_settings',
            'view_organization_imports',
            'view_organization_import_logs',
        ]
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())

        group = Group.objects.get(name='support_spec')
        perms_names = [
            'launch_reindexation',
            'launch_counters_recalculation',
            'edit_mailbox_size',
            'clear_folders',
            'clear_address_book',
            'mark_folders_as_read',
            'remove_labels',
            'make_imap_folders_english',
            'switch_imap',
            'switch_pop',
            'edit_filters',
            'enable_all_filters',
            'check_pop3_delivery',
            'toggle_social_avatars',
            'toggle_team_social_avatars',
            'transfer_user_back_to_oracle',
            'dkim_verify'
        ]
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())

        group = Group.objects.get(name='support_internal')
        perms_names = [
            'view_team_userinfo',
            'view_team_lock_history',
            'view_team_filters',
            'view_team_blacklist',
            'view_team_whitelist',
            'view_team_mail_settings',
            'view_team_folders',
            'view_team_labels',
            'view_team_collectors',
            'edit_team_collectors',
            'view_team_recalculators',
            'view_team_other_settings',
            'view_team_user_journal',
            'view_team_logs',
            'view_team_reminders',
            'enable_team_filters',
            'edit_team_filters',
            'launch_team_reindexation',
            'launch_team_counters_recalculation',
            'edit_team_mailbox_size',
            'clear_team_folders',
            'clear_team_address_book',
            'mark_team_folders_as_read',
            'remove_team_labels',
            'make_team_imap_folders_english',
            'switch_team_imap',
            'switch_team_pop',
            'dkim_verify'
        ]
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())

        group = Group.objects.get(name='admin')
        perms_names = [
            'view_userinfo',
            'view_filters',
            'view_blacklist',
            'view_whitelist',
            'view_mail_settings',
            'view_folders',
            'view_labels',
            'view_collectors',
            'view_other_settings',
            'view_user_journal',
            'view_antispam',
            'view_reminders',
            'view_team_userinfo',
            'view_team_filters',
            'view_team_blacklist',
            'view_team_whitelist',
            'view_team_mail_settings',
            'view_team_folders',
            'view_team_labels',
            'view_team_collectors',
            'edit_team_collectors',
            'view_team_other_settings',
            'view_team_user_journal',
            'view_team_reminders',
            'view_logs',
            'view_team_logs',
            'check_pop3_delivery',
            'check_team_pop3_delivery',
            'toggle_social_avatars',
            'toggle_team_social_avatars',
            'dkim_verify',
            'make_imap_folders_english',
            'edit_collectors',
            'ps_billing_refund',

            'view_organization_settings',
            'view_organization_imports',
            'update_organization_imports',
            'view_organization_import_logs',
        ]
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())

        group = Group.objects.get(name='security')
        perms_names = ['view_support_actions', 'dkim_verify']
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())

        group = Group.objects.get(name='refund_support')
        perms_names = ['ps_billing_refund']
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())

        group = Group.objects.get(name='god_mode')
        perms_names = [
            'view_message_info',
            'view_team_message_info',
            'view_mailbox_list',
            'view_team_mailbox_list',
            'purge_message',
            'purge_team_message'
        ]
        self.assertItemsEqual([perms[perm_name] for perm_name in perms_names], group.permissions.all())


class WinxUpravlyatorAPITest():
    response_ok = dict(code=0)

    @classmethod
    def setUpClass(cls):
        # create groups with perms
        call_command('winx_update_permission_groups')

    @classmethod
    def tearDownClass(cls):
        delete_objects()

    def tearDown(self):
        # delete_objects()
        get_user_model().objects.all().delete()

    def test_info(self):
        cl = Client()
        resp = cl.get(reverse('info'))
        self.assertEqual(resp.status_code, 200)
        resp = simplejson.loads(resp.content)

        self.assertEqual(resp['code'], 0)
        self.assertEqual(resp['roles']['slug'], 'role')
        self.assertEqual(resp['roles']['name'], 'Access type')
        self.assertEqual(
            resp['roles']['values'],
            {
                'admin': 'Admin',
                'abuse': 'Abuse',
                'support_internal': 'Support internal',
                'support_light': 'Support light',
                'support_spec': 'Support spec',
                'protocols_support': 'Protocols support',
                'god_mode': 'God mode',
                'refund_support': 'Refund support',
                'journal_viewer': 'Journal viewer',
                'security': 'Security'
            }
        )
        del resp['roles']['slug']
        del resp['roles']['name']
        del resp['roles']['values']
        self.assertEqual(resp['roles'], {})
        del resp['roles']
        self.assertEqual(resp, {'code': 0})

    def test_add_role(self):
        self.assertEqual(get_user_model().objects.filter(username='terran').count(), 0)

        cl = Client()
        role = simplejson.dumps({'role': 'security'})
        resp = cl.post(reverse('add-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        self.assertEqual(get_user_model().objects.filter(username='terran').count(), 1)
        terran = get_user_model().objects.get(username='terran')
        self.assertIn(Group.objects.get(name='security'), terran.groups.all())
        self.assertTrue(terran.has_perm('yandex_winx_admin.view_support_actions'))

    def test_add_role_fake(self):
        user_model = get_user_model()
        self.assertEqual(user_model.objects.filter(username='terran').count(), 0)

        # no such role/group - fake_role
        cl = Client()
        role = simplejson.dumps({'role': 'fake_role'})
        resp = cl.post(reverse('add-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=500, error='Group matching query does not exist.'))

        self.assertEqual(user_model.objects.filter(username='terran').count(), 1)

    def test_remove_role(self):
        self.assertEqual(get_user_model().objects.filter(username='terran').count(), 0)

        # grant role to user
        cl = Client()
        role = simplejson.dumps({'role': 'security'})
        resp = cl.post(reverse('add-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        # check user perms
        terran = get_user_model().objects.get(username='terran')
        self.assertTrue(terran.has_perm('yandex_winx_admin.view_support_actions'))

        # remove role from user
        cl = Client()
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        # user was not deleted
        self.assertEqual(get_user_model().objects.filter(username='terran').count(), 1)
        terran = get_user_model().objects.get(username='terran')

        # user was deprived from group admin and perm has gone
        self.assertEqual(terran.groups.count(), 0)
        self.assertFalse(terran.has_perm('yandex_winx_admin.view_support_actions'))

    def test_remove_role_fake(self):
        self.assertEqual(get_user_model().objects.filter(username='terran').count(), 0)

        role = simplejson.dumps({'access': 'admin'})  # no such role
        cl = Client()
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        role = simplejson.dumps({'role': 'admin'})  # no user
        cl = Client()
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        # user exists
        get_user_model().objects.create(username='terran')
        self.assertEqual(get_user_model().objects.filter(username='terran').count(), 1)

        role = simplejson.dumps({'role': 'admin'})
        cl = Client()
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

    def test_get_user_roles(self):
        cl = Client()
        # get roles of user who does not exist: error
        resp = cl.get(reverse('get-user-roles'), data=dict(login='fake_user'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=400, fatal=u'User "fake_user" does not exist.'))

        # add role to terran
        role = simplejson.dumps({'role': 'admin'})
        resp = cl.post(reverse('add-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-user-roles'), data=dict(login='terran'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              roles=[dict(role='admin')]))

        # add another role to terran
        role = simplejson.dumps({'role': 'support_spec'})
        resp = cl.post(reverse('add-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-user-roles'), data=dict(login='terran'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              roles=[dict(role='admin'), dict(role='support_spec')]))

        # deprive roles from terran
        role = simplejson.dumps({'role': 'admin'})
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-user-roles'), data=dict(login='terran'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              roles=[dict(role='support_spec')]))

        role = simplejson.dumps({'role': 'support_spec'})
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-user-roles'), data=dict(login='terran'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              roles=[]))

    def test_get_all_roles(self):
        cl = Client()
        # no granted roles
        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0, users=[]))

        # add role to terran
        role = simplejson.dumps({'role': 'admin'})
        resp = cl.post(reverse('add-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              users=[dict(login='terran',
                                          roles=[dict(role='admin')])]))

        # add role to protos
        role = simplejson.dumps({'role': 'abuse'})
        resp = cl.post(reverse('add-role'), data=dict(login='protos', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              users=[dict(login='protos',
                                          roles=[dict(role='abuse')]),
                                     dict(login='terran',
                                          roles=[dict(role='admin')]), ]))

        # add another role to terran
        role = simplejson.dumps({'role': 'support_spec'})
        resp = cl.post(reverse('add-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              users=[dict(login='protos',
                                          roles=[dict(role='abuse')]),
                                     dict(login='terran',
                                          roles=[dict(role='admin'), dict(role='support_spec')]), ]))

        # remove role from protos which is not granted to him - support_internal - no changes
        role = simplejson.dumps({'role': 'support_internal'})
        resp = cl.post(reverse('remove-role'), data=dict(login='protos', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)
        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              users=[dict(login='protos',
                                          roles=[dict(role='abuse')]),
                                     dict(login='terran',
                                          roles=[dict(role='admin'), dict(role='support_spec')]), ]))

        # remove role from protos
        role = simplejson.dumps({'role': 'abuse'})
        resp = cl.post(reverse('remove-role'), data=dict(login='protos', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              users=[dict(login='terran',
                                          roles=[dict(role='admin'), dict(role='support_spec')]), ]))

        # remove both roles from terran
        role = simplejson.dumps({'role': 'admin'})
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0,
                              users=[dict(login='terran',
                                          roles=[dict(role='support_spec')]), ]))

        role = simplejson.dumps({'role': 'support_spec'})
        resp = cl.post(reverse('remove-role'), data=dict(login='terran', role=role))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content), self.response_ok)

        resp = cl.get(reverse('get-all-roles'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(simplejson.loads(resp.content),
                         dict(code=0, users=[]))
