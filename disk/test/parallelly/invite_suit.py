# -*- coding: utf-8 -*-
from test.base import DiskTestCase

from mpfs.invite import manager
from mpfs.invite import errors
from mpfs.invite import contact
from mpfs.invite import code
from mpfs.core import base as core
from mpfs import invite
from mpfs.core.user.invites import INVITES_REFERRAL_BONUS, INVITES_USER_BONUS


class CommonInviteMixin(DiskTestCase):
    invite_params = {'referer': 'domain.com',
                     'ip': '1.1.1.1'}

    archivate_params = {'reason': 'bad user!'}

    invited_friend = '12'
    invited_friend_a = '34'
    invited_friend_b = '56'
    invited_friend_c = '78'


class BasicInviteTestCase(CommonInviteMixin):
    def test_generate_and_remove_invite(self):
        c = code.ExpendableCode.Create('mpfs', **self.invite_params)
        self.assertNotEqual(c, None)
        self.assertEqual(c.meta.get('ip'), self.invite_params.get('ip'))
        c.delete()

    def test_try_to_get_wrong_invite(self):
        self.assertRaises(errors.InviteNotFound, code.Code.Load, 'mpfs', 'FAKE')

    def test_generate_then_reserve_then_activate(self):
        c = code.ExpendableCode.Create('mpfs')
        self.assertNotEqual(c, None)

        c.reserve(u'test@test.ru', **self.invite_params)
        self.assertEqual(c.meta.get('reserved').get('ip'), self.invite_params.get('ip'))
        self.assertRaises(errors.InviteReserved, c.reserve, u'test@test.ru')

        c.activate(**self.invite_params)
        self.assertEqual(c.meta.get('activated').get('ip'), self.invite_params.get('ip'))
        self.assertRaises(errors.InviteActivated, c.activate)

    def test_list_codes(self):
        codeA = code.ExpendableCode.Create('mpfs')
        codeB = code.ExpendableCode.Create('mpfs')
        code.ExpendableCode.Create('mpfs')

        self.assertEqual(len(code.select('mpfs', {'state': code.FREE})),
                         3)
        self.assertEqual(len(code.select('mpfs', {'state': code.FREE}, None, None, 1)),
                         1)

        codeA.activate()
        self.assertEqual(len(code.select('mpfs', {'state': code.FREE})),
                         2)
        self.assertEqual(len(code.select('mpfs', {'state': code.ACTIVATED})),
                         1)

        codeB.reserve(u'test@test.ru', **self.invite_params)
        self.assertEqual(len(code.select('mpfs', {'state': code.FREE})),
                         1)
        self.assertEqual(len(code.select('mpfs', {'state': code.RESERVED},
                                         {'reserved': {'ip': self.invite_params.get('ip')}})),
                         1)

    def test_generate_and_remove_email(self):
        e_1 = contact.Contact.Create('mpfs', u'test@test.ru', 'email', **self.invite_params)
        e_2 = contact.Contact.Create('mpfs', u'слон@колбаса.рф', 'email')
        self.assertNotEqual(e_1, None)
        self.assertNotEqual(e_2, None)
        self.assertEqual(e_1.meta.get('ip'), self.invite_params.get('ip'))

    def test_try_to_push_invalid_email(self):
        self.assertRaises(errors.ContactInvalid,
                          contact.Contact.Create, 'mpfs', u'b%\\ddm.zz@test.ru', 'email')

    def test_try_to_push_already_existed_email(self):
        e = contact.Contact.Create('mpfs', u'test@test.ru', 'email', **self.invite_params)
        self.assertRaises(errors.ContactAlreadyExist,
                          contact.Contact.Create, 'mpfs', u'test@test.ru', 'email')
        e.archivate('TESTTEST')
        self.assertRaises(errors.ContactAlreadyExist,
                          contact.Contact.Create, 'mpfs', u'test@test.ru', 'email')

    def test_generate_then_archivate_email(self):
        e = contact.Contact.Create('mpfs', u'test@test.ru', 'email', **self.invite_params)
        self.assertEqual(len(contact.select('mpfs', 'new')), 1)
        self.assertEqual(len(contact.select('mpfs', 'archive')), 0)

        e.archivate('TESTTEST', **self.archivate_params)
        self.assertEqual(len(contact.select('mpfs', 'new')), 0)
        self.assertEqual(len(contact.select('mpfs', 'archive')), 1)
        self.assertEqual(e.meta.get('reason'), self.archivate_params.get('reason'))

    def test_list_emails(self):
        contact.Contact.Create('mpfs', u'test@test.ru', 'email', **self.invite_params)
        contact.Contact.Create('mpfs', u'слон@колбаса.рф', 'email')

        self.assertEqual(len(contact.select('mpfs', 'new')),
                         2)
        self.assertEqual(len(contact.select('mpfs', 'new', {'address': u'test@test.ru'})),
                         1)
        self.assertEqual(len(contact.select('mpfs', 'new', {'address': u'test@test.ru'},
                                            {'ip': self.invite_params.get('ip')})),
                         1)
        self.assertEqual(len(contact.select('mpfs', 'new', {'address': u'test@test.ru'},
                                            {'ip': '1.2.3.4'})),
                         0)

    def test_check_if_yandex_email(self):
        e_not_yandex = contact.Contact.Create('mpfs', u'test@test.ru', 'email')
        e_yandex = contact.Contact.Create('mpfs', u'test@yandex.ru', 'email')

        self.assertTrue(e_yandex.is_yandex)
        self.assertFalse(e_not_yandex.is_yandex)

    def test_do_not_allow_PDD_email(self):
        self.assertRaises(errors.ContactNotServed,
                          contact.Contact.Create, 'mpfs', 'oleg@leksunin.com', 'email')

    def test_push_facebook_address(self):
        contact.Contact.Create('mpfs', u'test@test.ru', 'fb', **self.invite_params)
        contact.Contact.Create('mpfs', u'слон@колбаса.рф', 'fb')

        self.assertEqual(len(contact.select('mpfs', 'new')),
                         2)
        self.assertEqual(len(contact.select('mpfs', 'new', {'address': u'test@test.ru'})),
                         1)
        self.assertEqual(len(contact.select('mpfs', 'new', {'address': u'test@test.ru'},
                                            {'ip': self.invite_params.get('ip')})),
                         1)
        self.assertEqual(len(contact.select('mpfs', 'new', {'address': u'test@test.ru'},
                                            {'ip': '1.2.3.4'})),
                         0)

    def test_generate_and_send_code_for_email(self):
        manager.push_email('mpfs', 'devnull@yandex.ru', **self.invite_params)
        c = manager.generate_and_send_expendable_code('mpfs', 'devnull@yandex.ru')
        self.assertEqual(c.meta.get('reserved').get('ip'), self.invite_params.get('ip'))
        self.assertEqual(c.meta.get('reserved').get('email'), 'devnull@yandex.ru')
        self.assertEqual(len(manager.list_new_emails('mpfs')), 0)
        self.assertEqual(len(manager.list_archived_emails('mpfs')), 1)
        self.assertEqual(len(manager.list_codes('mpfs', {'state': code.RESERVED})), 1)

    def test_try_generate_code_for_blocked_email(self):
        e = manager.push_email('mpfs', u'test@test.ru', **self.invite_params)
        manager.block_email('mpfs', u'test@test.ru', **self.archivate_params)
        self.assertRaises(errors.ContactArchivatedOrBlocked,
                          manager.generate_and_send_expendable_code, 'mpfs', u'test@test.ru')
        e = contact.Contact.Load('mpfs', u'test@test.ru', 'email')
        e.delete()

    def test_try_activate_unexisted_code(self):
        self.assertRaises(errors.InviteNotFound, manager.activate_code, 'mpfs', 'FAKE', self.uid)

    def test_generate_expendable_code(self):
        c = manager.generate_expendable_code('mpfs')
        manager.activate_code('mpfs', c.hash, '334')
        self.assertRaises(errors.InviteActivated, manager.activate_code, 'mpfs', c.hash, '444')

    def test_generate_eternal_code(self):
        c = manager.generate_eternal_code('mpfs')
        manager.activate_code('mpfs', c.hash, '334')

    def test_activate_some_users(self):
        c = manager.generate_eternal_code('mpfs')
        manager.activate_code('mpfs', c.hash, '112')
        manager.activate_code('mpfs', c.hash, '222')
        c = code.Code.Load('mpfs', c.hash)
        self.assertEqual(c.meta, {'activated': [{'uid': '112'}, {'uid': '222'}]})

    def test_check_standart_userinfo(self):
        c = manager.generate_eternal_code('mpfs')
        self.assertRaises(errors.InviteHasNoOwner, manager.code_info, 'mpfs', c.hash)
        c.delete()

        c = manager.generate_eternal_code('mpfs', referral=self.uid)
        self.assertEqual(manager.code_info('mpfs', c.hash), 'Vasily P.')
        c.delete()

    def test_check_hashed_userinfo(self):
        c = manager.generate_eternal_code('mpfs', referral=self.uid)

        uid_info = self.json_ok('user_invite_info', {'uid': self.uid})
        hash_info = self.json_ok('user_invite_info', {'hash': c.hash})

        self.assertEqual(uid_info, hash_info)
        c.delete()

    def test_activate_already_existed_user(self):
        c = manager.generate_eternal_code('mpfs', referral=self.uid)
        manager.activate_code('mpfs', c.hash, '668')
        self.assertRaises(errors.UserAlreadyExists, manager.activate_code, 'mpfs', c.hash, '668')


class ReferralWorkflowTestCase(CommonInviteMixin):

    def check_suspicious(self):
        """Проверяет есть ли подозрительные приглашения."""
        req = self.get_request({'uid': self.uid})
        activated_invites = core.user_invite_activated(req)
        found = next((True
                      for invite in activated_invites
                      if invite['state'] == 'suspicious'),
                     False)
        return found

    def _activate_some_friends(self):
        # получаем хеш
        req = self.get_request({'uid': self.uid, 'hash': None})
        rhash = core.user_invite_hash(req)

        # регистрируем друзей
        for uid in (self.invited_friend_a, self.invited_friend_b, self.invited_friend_c):
            args = {'uid': uid,
                    'code': rhash,
                    'project': 'mpfs',
                    'info': {}}
            invite.activate(self.get_request(args))

            args = {'uid' : uid, 'project': 'disk', 'hash': None}
            info = core.user_invite_info(self.get_request(args))
            self.assertEqual(info.get('referral').get('uid'), self.uid)

        # проверяем количество зарегистрировавшихся
        req = self.get_request({'uid': self.uid, 'hash': None})
        activated_list = core.user_invite_activated(req)
        self.assertEqual(len(activated_list), 3)

    def test_make_eternal_invite_for_user(self):
        args = {'uid': self.uid}
        rhash = core.user_invite_hash(self.get_request(args))
        self.assertEqual(core.user_invite_hash(self.get_request(args)), rhash)

    def test_workflow_with_correct_soft(self):
        '''
        Проверяем полный цикл заведения по инвайту с правильным ПО
        '''
        self._activate_some_friends()

        # начинаем проставлять софт
        # запоминаем лимиты до утановки
        old_referral_limit = self.space_limit(self.uid)

        old_user_a_limit = self.space_limit(self.invited_friend_a)

        # Ставим софт рефералу
        args = {'uid' : self.uid, 'id': 'XXXXXX', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Ставим софт приведенному
        args = {'uid' : self.invited_friend_a, 'id': 'ZZZZZZ', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Проверяем лимиты
        new_referral_limit = self.space_limit(self.uid)
        self.assertEqual(old_referral_limit + INVITES_REFERRAL_BONUS, new_referral_limit)

        new_user_a_limit = self.space_limit(self.invited_friend_a)
        self.assertEqual(old_user_a_limit + INVITES_USER_BONUS, new_user_a_limit)

    def test_workflow_with_wrong_soft(self):
        '''
        Проверяем полный цикл заведения по инвайту с неправильным ПО
        '''
        self._activate_some_friends()

        # начинаем проставлять софт
        # запоминаем лимиты до установки
        old_referral_limit = self.space_limit(self.uid)

        old_user_b_limit = self.space_limit(self.invited_friend_b)

        # Ставим софт рефералу
        args = {'uid' : self.uid, 'id': 'XXXXXX', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Ставим мобильный софт приведенному
        args = {'uid' : self.invited_friend_b, 'id': 'YYYYYY', 'project': 'disk',
                'type': 'mobile', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Проверяем - ничего не произошло
        new_referral_limit = self.space_limit(self.uid)
        self.assertEqual(old_referral_limit, new_referral_limit)

        new_user_b_limit = self.space_limit(self.invited_friend_b)
        self.assertEqual(old_user_b_limit, new_user_b_limit)

        # Ставим софт на тот же комп, что и тому, кто приглашал
        args = {'uid' : self.invited_friend_b, 'id': 'XXXXXX', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Проверяем, что ничего не изменилось
        new_referral_limit = self.space_limit(self.uid)
        self.assertEqual(old_referral_limit, new_referral_limit)

        new_user_b_limit = self.space_limit(self.invited_friend_b)
        self.assertEqual(old_user_b_limit, new_user_b_limit)

        # Смотрим, что появилась запись о подозрительном пользователе
        self.assertTrue(self.check_suspicious())

        # Ставим подозрительному ПО на нормальный комп
        args = {'uid' : self.invited_friend_b, 'id': 'ABCDEF', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Проверяем, что место добавилось
        new_referral_limit = self.space_limit(self.uid)
        self.assertEqual(old_referral_limit + INVITES_REFERRAL_BONUS, new_referral_limit)

        new_user_b_limit = self.space_limit(self.invited_friend_b)
        self.assertEqual(old_user_b_limit + INVITES_USER_BONUS, new_user_b_limit)

        # Смотрим, что запись о подозрительном исчезла
        self.assertFalse(self.check_suspicious())

    def test_activate_overbonused_user_and_get_no_bonus(self):
        self._activate_some_friends()

        # Ставим софт рефералу
        args = {'uid' : self.uid, 'id': 'XXXXXX', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Ставим софт первому приведенному
        args = {'uid' : self.invited_friend_a, 'id': 'ZZZZZZ', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

         # Ставим софт второму приведенному
        args = {'uid' : self.invited_friend_b, 'id': 'ABCDEF', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

         # Запоминаем лимиты
        old_referral_limit = self.space_limit(self.uid)

        args = {'uid' : self.invited_friend_c, 'project': 'disk'}
        old_user_c_limit = self.space_limit(self.invited_friend_c)

        # Ставим софт третьему приведенному
        args = {'uid' : self.invited_friend_c, 'id': '111111', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'mac'}}
        core.user_install_device(self.get_request(args))

        # Проверяем
        new_referral_limit = self.space_limit(self.uid)
        self.assertEqual(old_referral_limit, new_referral_limit)

        new_user_c_limit = self.space_limit(self.invited_friend_c)
        self.assertEqual(old_user_c_limit + INVITES_USER_BONUS, new_user_c_limit)

        # Смотрим на запись о юзере
        args = {'uid': self.uid,}
        activated_list = core.user_invite_activated(self.get_request(args))
        self.assertEqual(activated_list[2]['state'], 'nospace')

    def test_activate_two_friends_with_same_id(self):
        self._activate_some_friends()

        # Ставим софт рефералу
        args = {'uid' : self.uid, 'id': 'XXXXXX', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # Ставим софт первому приведенному
        args = {'uid' : self.invited_friend_a, 'id': 'ZZZZZZ', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # запоминаем лимиты второго
        old_user_b_limit = self.space_limit(self.invited_friend_b)

        # Ставим точно такой же софт второму другу
        args = {'uid' : self.invited_friend_b, 'id': 'ZZZZZZ', 'project': 'disk',
                'type': 'desktop', 'info': {'os':'win'}}
        core.user_install_device(self.get_request(args))

        # проверяем, что второй места не получил
        new_user_b_limit = self.space_limit(self.invited_friend_b)
        self.assertEqual(old_user_b_limit, new_user_b_limit)
