# coding: utf-8
import json

from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory
from django.db import IntegrityError
from django.conf import settings
from django.contrib.auth.models import User
from mlcore.subscribe.backends.yandex_team.models import YandexTeamBackendContext

from django_intranet_stuff.models import Staff, Group, GroupMembership

from mlcore.ml.models import MailList, Subscribers, EmailSubscriber
from mlcore.permissions.models import GroupPermission, ListPermission, Type
from mlcore.nwsmtp_connect.views import delivery_info, force_update_cache
from mlcore.nwsmtp_connect.models import NwsmtpInfoCache

from mlcore.nwsmtp_connect.emailinfo import EmailInfo
from mlcore.apiv2.tests.base import ApiMixin, G, create_user, create_maillist
from datetime import datetime

# Примеры с боевого blackbox
# TODO: замокать API blackbox


def user_for_delivery(login, domain='yandex-team.ru', has_exchange=False,
                      affiliation='yandex', is_dismissed=False, work_email=None):
    if not work_email:
        work_email = login+'@'+domain
    user = G(User, username=login, email=work_email)
    staff = G(Staff, login=login, user=user, work_email=work_email, affiliation=affiliation,
              has_exchange=has_exchange, is_dismissed=is_dismissed)
    return staff.user


def staff_for_delivery(login, domain='yandex-team.ru', has_exchange=False,
                       affiliation='yandex', is_dismissed=False, work_email=None):
    if not work_email:
        work_email = login+'@'+domain
    user = G(User, username=login, email=work_email)
    staff = G(Staff, login=login, user=user, work_email=work_email, affiliation=affiliation,
              has_exchange=has_exchange, is_dismissed=is_dismissed)
    return staff


def maillist_for_delivery(maillist_name, domain='yandex-team.ru', readonly=False, is_deleted=False,
                          is_sub=True, is_imap=False):
    maillist_email = maillist_name+'@'+domain
    imap_name = maillist_name.replace('.', '-')
    if domain not in settings.YANDEX_TEAM_DOMAINS:
        imap_name = maillist_name.replace('.', '-')+'-at-'+domain.replace('.', '-')
        maillist_email = imap_name+'@'+domain
        maillist_name = imap_name
    maillist = G(MailList, name=maillist_name, email=maillist_email, is_sub=is_sub, is_imap=is_imap,
                 readonly=readonly, is_deleted=is_deleted, modified_at=datetime.now())
    G(YandexTeamBackendContext, passport_name=imap_name, maillist=maillist)
    return maillist


def create_many_emailsubscribers(maillist, lst_emails):
    for email in lst_emails:
        G(EmailSubscriber, list=maillist, email=email)


def create_many_inbox_subscribers(maillist, lst_staff_emails):
    for email in lst_staff_emails:
        login, domain = email.split('@')
        user = user_for_delivery(login, domain, work_email=email)
        G(Subscribers, list=maillist, user=user, is_sub=True)


def create_list_permission_to_read(maillist, lst_users):
    read_type = G(Type, name='read', id=1)
    for user in lst_users:
        G(ListPermission, user=user, list=maillist, approved=True, type=read_type)


def create_group_permission_to_read(maillist, lst_users):
    read_type = G(Type, name='read', id=1)
    for user in lst_users:
        G(ListPermission, user=user, list=maillist, approved=True, type=read_type)


def create_list_permission_to_write(maillist, lst_users):
    write_type = G(Type, name='write', id=2)
    for user in lst_users:
        G(ListPermission, user=user, list=maillist, approved=True, type=write_type)


def create_group_permission_to_write(maillist, group, lst_staff):
    write_type = G(Type, name='write', id=2)
    for staff in lst_staff:
        G(GroupMembership, staff=staff, group=group)
    G(GroupPermission, group=group, list=maillist, type=write_type)


class DeliveryMaillistTestCase(ApiMixin, TestCase):
    def setUp(self):
        super(DeliveryMaillistTestCase, self).setUp()
        self.url = reverse('apiv2:delivery_info')
        self.factory = RequestFactory()
        self.view = delivery_info

    def get_default_response(self, **extra):
        return super(DeliveryMaillistTestCase, self).get_default_response(**extra)

    def get_json_response(self, **extra):
        return json.loads(self.get_default_response(**extra).content)

    def test_ok(self):
        pass

    # ===== Проверка класса EmailInfo =====

    def test_email_info_class_base(self, **extra):
        """
        Проверка раскрытия класса EmailInfo с одним INBOX-подписичком
        """
        maillist = create_maillist('ml1@yandex-team.ru', inbox=['vfaronov', ])
        data = EmailInfo(maillist.email).as_dict()
        assert data['subscribers']['inbox'] == [u'vfaronov@mail.yandex-team.ru', u'ml1@mail.yandex-team.ru']

    # ===== Проверка раскрытия рассылок =====

    def test_expand_subscribtion_maillist_to_maillist(self, **kw):
        """
        Проверка 'рассылка подписана на рассылку' - верно раскрылась
        """
        ml31 = maillist_for_delivery('ml31')
        create_many_inbox_subscribers(ml31, ['fantasy@yandex-team.ru'])
        ml32 = maillist_for_delivery('ml32')
        create_many_emailsubscribers(ml32, [ml31.email])
        create_many_inbox_subscribers(ml32, ['ignition@yandex-team.ru'])
        ml33 = maillist_for_delivery('ml33')
        create_many_inbox_subscribers(ml33, ['pierre@yandex-team.ru'])
        create_many_emailsubscribers(ml33, [ml31.email, ml32.email])

        create_many_emailsubscribers(ml31, [ml33.email, ml32.email])

        data = self.get_json_response(email=ml33.email)
        self.assertEquals(sorted(data['subscribers']['inbox']),
                          sorted([u'fantasy@mail.yandex-team.ru', u'ignition@mail.yandex-team.ru',
                                  u'pierre@mail.yandex-team.ru', u'ml31@mail.yandex-team.ru',
                                  u'ml32@mail.yandex-team.ru', u'ml33@mail.yandex-team.ru']))

    def test_circle_maillist_to_maillist(self, **kw):
        """
        Проверка 'цикл в рассылке'
        """
        ml31 = maillist_for_delivery('ml31')
        create_many_inbox_subscribers(ml31, ['fantasy@yandex-team.ru'])
        ml32 = maillist_for_delivery('ml32')
        create_many_emailsubscribers(ml32, [ml31.email])
        create_many_inbox_subscribers(ml32, ['ignition@yandex-team.ru'])
        ml33 = maillist_for_delivery('ml33')
        create_many_inbox_subscribers(ml33, ['pierre@yandex-team.ru'])
        create_many_emailsubscribers(ml33, [ml31.email, ml32.email])

        data = self.get_json_response(email=ml33.email)
        self.assertEquals(sorted(data['subscribers']['inbox']),
                          sorted([u'fantasy@mail.yandex-team.ru', u'ignition@mail.yandex-team.ru',
                                  u'pierre@mail.yandex-team.ru', u'ml31@mail.yandex-team.ru',
                                  u'ml32@mail.yandex-team.ru', u'ml33@mail.yandex-team.ru']))

    def test_shared_folder(self):
        """
        Проверяем, что для рассылки отдался folder_email с доменом mail.yandex-team.ru
        """
        maillist3 = maillist_for_delivery('maillist3')
        G(EmailSubscriber, list=maillist3, email='maillist3@mail.yandex-team.ru')
        data = self.get_json_response(email=maillist3.email)
        self.assertListEqual(data['subscribers']['inbox'], [u'maillist3@mail.yandex-team.ru'])

    def test_maillist_in_cache(self):
        """
        Проверяем, что если данные для рассылки есть в кеше, то данные берутся из кеша
        """
        maillist = maillist_for_delivery('maillist_cache')
        create_many_emailsubscribers(maillist, ['test1', 'test2@yandex.ru', 'test3@mail.yandex-team.ru', 'test4@ld.yandex.ru'])
        response = self.get_default_response(email=maillist.email, update_cache='yes')
        data = json.loads(response.content)
        output_emails = sorted(data['subscribers']['inbox'])
        correct_emails = [maillist.name+'@mail.yandex-team.ru', 'test2@yandex.ru', 'test3@mail.yandex-team.ru', 'test4@ld.yandex.ru']
        self.assertListEqual(output_emails, correct_emails)
        self.assertIsNone(response._headers.get('x-ml-cached-on'))

        # Проверяем, что второй раз взялось из кэша
        response = self.get_default_response(email=maillist.email)
        self.assertIsNotNone(response._headers['x-ml-cached-on'])
        data = json.loads(response.content)
        output_emails = sorted(data['subscribers']['inbox'])
        output_emails.sort()
        correct_emails.sort()
        self.assertEquals(output_emails, correct_emails)

    def test_maillist_not_in_cache(self):
        """
        Если рассылки нет в кеше, но в кеше есть рассылка с imap_name в домене mail.yandex-team.ru,
        то не должны брать данные для такой рассылки из кеша
        """
        body = '{"status": "ok", "is_open": true, "is_internal": true, "type": "maillist",' \
               '"subscribers": {"inbox":["ml@mail.yandex-team.ru", "fantasy@mail.yandex-team.ru", ' \
               '"ignition@mail.yandex-team.ru", "pierre@ld.yandex.ru",  "pierre@mail.yandex-team.ru", ' \
               '"alex89@ld.yandex.ru", "alex89@mail.yandex-team.ru"]}}'
        G(
            NwsmtpInfoCache, version='1.0', headers='{"X-ML-Cached-On": "public, max-age=60"}',
            body=body, modified_at=datetime.now(), email='ml@mail.yandex-team.ru'
        )
        response = self.get_default_response(email='ml@mail.yandex-team.ru')
        data = json.loads(response.content)
        output_emails_for_mail = sorted(data['subscribers']['inbox'])
        correct_emails_for_mail = sorted(['ml@mail.yandex-team.ru', 'fantasy@mail.yandex-team.ru',
                                          'ignition@mail.yandex-team.ru', 'pierre@ld.yandex.ru',
                                          'pierre@mail.yandex-team.ru', 'alex89@ld.yandex.ru',
                                          'alex89@mail.yandex-team.ru'])
        self.assertListEqual(output_emails_for_mail, correct_emails_for_mail)
        self.assertIsNotNone(response._headers.get('x-ml-cached-on'))

        # Проверяем, что для рассылки в домене yandex-team.ru не берутся данные из кеша для mail.yandex-team.ru
        maillist_yt = maillist_for_delivery('ml', domain='yandex-team.ru')
        response = self.get_default_response(email=maillist_yt.email)
        self.assertIsNone(response._headers.get('x-ml-cached-on'))
        data = json.loads(response.content)
        output_emails_for_yt = sorted(data['subscribers']['inbox'])
        correct_emails_for_yt = [maillist_yt.name + '@mail.yandex-team.ru']
        self.assertEquals(output_emails_for_yt, correct_emails_for_yt)
        self.assertIsNone(response._headers.get('x-ml-cached-on'))

        # Проверяем, что результаты ответов разные
        self.assertNotEqual(output_emails_for_mail, output_emails_for_yt)

    def test_maillist_in_domain_mail_yandex_team(self):
        """
        Для рассылки с доменом mail.yandex-team.ru - отвечаем 410 и не разворачиваем рассылку.
        """
        maillist = maillist_for_delivery('maillist_mail_yandex_team_ru')
        G(EmailSubscriber, list=maillist, email='maillist1@mail.yandex-team.ru')
        response = self.get_default_response(email='maillist_mail_yandex_team_ru@mail.yandex-team.ru')
        data = json.loads(response.content)
        self.assertEqual(data['error'], 'no entity with email maillist_mail_yandex_team_ru@mail.yandex-team.ru')
        self.assertEqual(response.status_code, 410)

    def test_maillist_not_in_domain_yandex_team_ru(self):
        """
        Проверим, что для рассылки вне yandex-team.ru домена, delivery/info отвечает 200
        """
        maillist = maillist_for_delivery('maillist', domain='yandex-team.com')
        maillist2 = maillist_for_delivery('maillist2')
        G(EmailSubscriber, list=maillist, email=maillist2.email)
        response = self.get_default_response(email=maillist.email)
        data = json.loads(response.content)
        correct_emails = sorted(['maillist@mail.yandex-team.ru', 'maillist2@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)
        self.assertEqual(response.status_code, 200)

    def test_maillist_not_in_passport(self):
        """
        Проверяем выдачу для рассылки, которой нет в паспорте, но есть в ML.
        (Тест станет более актуальным после добавления blackbox_mock_class)
        """
        maillist = maillist_for_delivery('maillist')
        G(EmailSubscriber, list=maillist, email='newmaillist@yandex-team.ru')
        data = self.get_json_response(email=maillist.email)
        correct_emails = sorted(['maillist@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)

    def test_modified_at_when_change_subscribers_and_emailsubscribers(self):
        """
        TODO: сделать в моделях Subscribers & Emailsubscribers кастомное сохранение с изменением поля modified_at.
        В mailList модели также есть поле changed. На него тоже можно смотреть для изменения modified_at

        Проверить, что дата изменения поменялась после добавления или удаления подписчика.
        Проверить, что дата изменения поменялась после добавления или удаления внешнего email-a.
        """
        body = '{"status": "ok", "is_open": true, "is_internal": true, "type": "maillist", ' \
               '"subscribers": {"inbox": ["ml@mail.yandex-team.ru", "fantasy@ld.yandex.ru", ' \
               '"fantasy@mail.yandex-team.ru", "pierre@ld.yandex.ru", "pierre@mail.yandex-team.ru"]}}'
        G(
            NwsmtpInfoCache, version='1.0', headers='{"X-ML-Cached-On": "public, max-age=60"}',
            body=body, modified_at=datetime(2014, 12, 31, 23, 59, 59, 999999), email='ml@yandex-team.ru'
        )
        maillist = create_maillist('ml')
        data = self.get_json_response(email=maillist.email)

        self.assertEqual(sorted(data['subscribers']['inbox']), sorted([
            'ml@mail.yandex-team.ru',
            'fantasy@ld.yandex.ru', 'fantasy@mail.yandex-team.ru',
            'pierre@ld.yandex.ru', 'pierre@mail.yandex-team.ru'
        ]))
        time_now = datetime.now()
        maillist.modified_at = time_now

        create_many_inbox_subscribers(maillist, ['fantasy@yandex-team.ru', 'pierre@yandex-team.ru'])
        create_many_emailsubscribers(maillist, ['otrs@otrs-inbox.yandex-team.ru', 'separator@separator.yandex-team.ru'])

        # TODO: fix it
        # self.assertNotEqual(maillist.modified_at, time_now)
        force_update_cache(maillist.email)
        data = self.get_json_response(email=maillist.email)
        correct_emails = sorted(['ml@mail.yandex-team.ru', 'otrs@otrs-inbox.yandex-team.ru',
                                 'separator@separator.yandex-team.ru', 'fantasy@mail.yandex-team.ru',
                                 'pierre@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)

    def test_modified_at_when_maillist_change_settings(self):
        """
        TODO: подумать как сделать именно при изменении полей модели.

        Проверить, что дата изменения поменялась после изменения свойств рассылки
        """
        maillist = create_maillist('ml')
        time_now = datetime.now()
        maillist.modified_at = time_now
        maillist.is_internal = True
        maillist.save()
        self.assertNotEqual(maillist.modified_at, time_now)

    def test_maillist_with_dismissed_users(self):
        """
        Проверить рассылку, в которой есть уволенные сотрудники
        """
        antipin = user_for_delivery(
            'a-antipin', has_exchange=True, is_dismissed=True,
            work_email='a-antipin@yandex-team.com'
        )
        juanych = user_for_delivery(
            'juanych', has_exchange=False, is_dismissed=True,
            work_email='juanych@yandex-team.ru'
        )
        zubchik = user_for_delivery(
            'zubchick', has_exchange=False, is_dismissed=False,
            work_email='zubchick@yandex-team.com.tr'
        )
        maillist = maillist_for_delivery('ml', domain='support.yamoney.ru')
        maillist2 = maillist_for_delivery('ml2', domain='support.yandex.ru')

        G(Subscribers, list=maillist, user=antipin, is_sub=True, is_imap=True)
        G(Subscribers, list=maillist, user=juanych, is_sub=True, is_imap=True)
        G(Subscribers, list=maillist, user=zubchik, is_sub=True, is_imap=True)
        create_many_emailsubscribers(maillist, ['test@google.com', 'maillist@yandex-team.ru', maillist2.email])

        data = self.get_json_response(email=maillist.email)
        correct_emails = sorted(['zubchick@mail.yandex-team.ru',
                                 'test@google.com',
                                 'ml2-at-support-yandex-ru@mail.yandex-team.ru',
                                 'ml-at-support-yamoney-ru@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)

    def test_deleted_maillist(self):
        """
        Проверить, что отдается только shared_folder для удаленной рассылки
        """
        maillist = maillist_for_delivery('deleted_ml', is_deleted=True)
        # создадим email-подписчиков, чтоб проверить, что их не возвращаем
        create_many_emailsubscribers(maillist, [
            'ignition@ld.yandex.ru', 'ignition@mail.yandex-team.ru',
            'shelkovin@mail.yandex-team.ru', 'm1@ld.yandex.ru'
        ])

        response = self.get_default_response(email=maillist.email)
        data = json.loads(response.content)
        # Изменили поведение для удаленных рассылок. Теперь для удаленных рассылок отдаем только shared_folder.
        # см. ML-1467
        self.assertEqual(response.status_code, 200)
        self.assertEqual(data['subscribers']['inbox'], ['deleted_ml@mail.yandex-team.ru'])

    def test_maillist_with_points_and_dash(self):
        """
        Проверить раскрытие рассылки с точками и с дефисами
        """
        maillist_with_points = maillist_for_delivery('want.to.work', domain='auto.ru')
        create_many_inbox_subscribers(maillist_with_points, [])
        mlysov = user_for_delivery(
            'mlysov', has_exchange=False, is_dismissed=False,
            work_email='mlysov@yandex-team.ru', domain='auto.ru'
        )
        G(Subscribers, list=maillist_with_points, user=mlysov, is_sub=True, is_imap=True)

        response = self.get_default_response(email=maillist_with_points.email)
        data = json.loads(response.content)
        correct_emails = sorted(['mlysov@mail.yandex-team.ru', 'want-to-work-at-auto-ru@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)
        self.assertEqual(response.status_code, 200)

    def test_maillist_with_points_and_dash_has_equal_response(self):
        """
        Проверяем, что рассылки с точками и дефисами возвращают одинаковые ответы,
        одинаковых подписчиков
        """
        maillist_with_points = maillist_for_delivery('work.at.big.company', domain='auto.ru')
        data_for_points = self.get_json_response(email=maillist_with_points.email)
        self.assertEqual(sorted(data_for_points['subscribers']['inbox']),
                         ['work-at-big-company-at-auto-ru@mail.yandex-team.ru'])

        self.assertRaises(IntegrityError, maillist_for_delivery, 'work-at-big-company', domain='auto.ru',)

    def test_old_cmail_maillist_alias(self):
        """
        Проверим выдачу для старых cmail-рассылочных алиасов
        """
        context = maillist_for_delivery('context', domain='yandex-team.ru', is_sub=True, is_imap=False)
        planning = maillist_for_delivery('planning', domain='yandex-team.ru', is_sub=True, is_imap=True)
        create_many_inbox_subscribers(planning, ['vikulova@yandex-team.com', 'ealebedev@yandex-team.ru',
                                                 'mazhaev@yandex-team.com.ua', 'tsumareva@yandex-team.com.tr',
                                                 'nvfrolova@yandex-team.ru'])
        create_many_emailsubscribers(planning, ['planning@ld.yandex.ru'])
        create_many_emailsubscribers(context, [planning.email])

        response = self.get_default_response(email=context.email)
        data = json.loads(response.content)
        correct_emails = sorted(['vikulova@mail.yandex-team.ru', 'ealebedev@mail.yandex-team.ru',
                                 'mazhaev@mail.yandex-team.ru', 'tsumareva@mail.yandex-team.ru',
                                 'nvfrolova@mail.yandex-team.ru', 'planning@ld.yandex.ru',
                                 'planning@mail.yandex-team.ru', 'context@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)
        self.assertEqual(response.status_code, 200)

    def test_readonly_maillist_who_can_write(self):
        """
        Протестировать рассылку, в которую разрешено писать не всем пользователям (readonly=True).
        Тестируем поле who_can_write.
        """
        maillist = maillist_for_delivery('oebs-info', readonly=True)
        ananke = user_for_delivery('ananke')
        lerarunge = user_for_delivery('lerarunge', is_dismissed=True)
        granatka = user_for_delivery('granatka')

        create_list_permission_to_write(maillist, [granatka, ananke, lerarunge])
        create_many_inbox_subscribers(maillist, ['fantasy@auto.ru', 'pierre@yandex-team.com.ua',
                                                 'art@yandex-team.ru', 'svetleo@yandex-team.ru'])
        create_many_emailsubscribers(maillist, ['ignition@ld.yandex.ru', 'ignition@mail.yandex-team.ru',
                                                'shelkovin@mail.yandex-team.ru',
                                                'm1@ld.yandex.ru'])

        response = self.get_default_response(email=maillist.email)
        data = json.loads(response.content)
        correct_emails = sorted(['fantasy@mail.yandex-team.ru', 'pierre@mail.yandex-team.ru',
                                'art@mail.yandex-team.ru', 'svetleo@mail.yandex-team.ru',
                                'ignition@ld.yandex.ru', 'ignition@mail.yandex-team.ru',
                                'shelkovin@mail.yandex-team.ru', 'm1@ld.yandex.ru',
                                'oebs-info@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)
        self.assertEqual(data["readonly"], [True])
        self.assertEqual(sorted(data['who_can_write']), sorted([
            'ananke@yandex-team.ru', 'ananke@yandex-team.com', 'ananke@yandex-team.com.tr', 'ananke@yandex-team.com.ua',
            'granatka@yandex-team.ru', 'granatka@yandex-team.com', 'granatka@yandex-team.com.tr',
            'granatka@yandex-team.com.ua'
        ]))
        self.assertEqual(response.status_code, 200)

    def test_readonly_maillist_who_can_write_if_group_permission_exists(self):
        """
        Протестировать рассылку, в которую разрешено писать группе.
        """
        maillist = maillist_for_delivery('ml', readonly=True)
        group = G(Group, name='group_can_write')

        s1 = staff_for_delivery('art')
        s2 = staff_for_delivery('svetleo')
        s3 = staff_for_delivery('vfaronov')
        s4 = staff_for_delivery('juanych', is_dismissed=True)
        create_group_permission_to_write(maillist, group, [s1, s2, s3, s4])
        create_many_emailsubscribers(maillist, ['ignition@ld.yandex.ru', 'ignition@mail.yandex-team.ru',
                                                'shelkovin@mail.yandex-team.ru',
                                                'm1@ld.yandex.ru'])

        response = self.get_default_response(email=maillist.email)
        data = json.loads(response.content)
        correct_emails = sorted(['ignition@ld.yandex.ru',  'ignition@mail.yandex-team.ru',
                                 'shelkovin@mail.yandex-team.ru', 'm1@ld.yandex.ru',
                                 'ml@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)
        self.assertEqual(data["readonly"], [True])
        self.assertEqual(sorted(data['who_can_write']), sorted([
            'art@yandex-team.ru', 'art@yandex-team.com', 'art@yandex-team.com.tr', 'art@yandex-team.com.ua',
            'svetleo@yandex-team.ru', 'svetleo@yandex-team.com', 'svetleo@yandex-team.com.tr',
            'svetleo@yandex-team.com.ua',
            'vfaronov@yandex-team.ru', 'vfaronov@yandex-team.com', 'vfaronov@yandex-team.com.tr',
            'vfaronov@yandex-team.com.ua'
        ]))
        self.assertEqual(response.status_code, 200)

    def test_internal_maillist_list_permission_can_read_user(self):
        """
        Протестировать рассылку, которую не всем пользователям разрешено читать.
        Не должны обращать внимания на признак чтения.
        """
        maillist = maillist_for_delivery('ml', readonly=True)
        ananke = user_for_delivery('ananke')
        lerarunge = user_for_delivery('lerarunge', is_dismissed=True)
        granatka = user_for_delivery('granatka')

        create_list_permission_to_read(maillist, [ananke, lerarunge, granatka])

        create_many_emailsubscribers(maillist, ['ignition@ld.yandex.ru', 'ignition@mail.yandex-team.ru',
                                                'shelkovin@mail.yandex-team.ru',
                                                'm1@ld.yandex.ru'])
        create_many_inbox_subscribers(maillist, ['fantasy@auto.ru', 'pierre@yandex-team.com.ua',
                                                 'art@yandex-team.ru', 'svetleo@yandex-team.ru'])
        G(Subscribers, list=maillist, user=ananke, is_sub=True)
        G(Subscribers, list=maillist, user=lerarunge, is_sub=True)
        G(Subscribers, list=maillist, user=granatka, is_sub=True)

        data = self.get_json_response(email=maillist.email)
        correct_emails = sorted(['ignition@ld.yandex.ru',  'ignition@mail.yandex-team.ru',
                                 'shelkovin@mail.yandex-team.ru', 'm1@ld.yandex.ru',
                                 'ml@mail.yandex-team.ru', 'ananke@mail.yandex-team.ru', 'granatka@mail.yandex-team.ru',
                                 'fantasy@mail.yandex-team.ru', 'pierre@mail.yandex-team.ru',
                                 'art@mail.yandex-team.ru', 'svetleo@mail.yandex-team.ru'])
        self.assertEqual(sorted(data['subscribers']['inbox']), correct_emails)
        self.assertEqual(data["readonly"], [True])
        self.assertEqual(sorted(data['who_can_write']), [])

    def test_expand_imap_email(self):
        """
        Протестируем раскрытие рассылки для
        maillist.email|
        imap-name@mail.yandex-team.ru|
        imap-name@yandex-team.ru
        """
        maillist = maillist_for_delivery('ml', domain='yandex-team.com')
        yt_domain = '@yandex-team.ru'
        mail_domain = '@mail.yandex-team.ru'

        data = self.get_json_response(email=maillist.email)
        self.assertEqual(sorted(data['subscribers']['inbox']), ['ml@mail.yandex-team.ru'])

        data = self.get_json_response(email=maillist.name+yt_domain)
        self.assertEqual(sorted(data['subscribers']['inbox']), ['ml@mail.yandex-team.ru'])

        response = self.get_default_response(email=maillist.name+mail_domain)
        self.assertEqual(response.status_code, 410)

    def test_expand_email_with_conflict_emails(self):
        """
        Проверим, что для email-ов вида partner-support@market.yandex.ru и partner.support@market.yandex.ru
        - с одинаковым email-ом в yandexteambackendcontext возвращаются результаты из delivery/info

        """
        partner_support = G(MailList, name='partner-support@market.yandex.ru', email='partner1@market.yandex.ru')
        partner_point_support = G(MailList, name='partner.support@market.yandex.ru', email='partner2@market.yandex.ru')

        imap_name = 'partner-support-at-market-yandex-ru'
        G(YandexTeamBackendContext, passport_name=imap_name, maillist=partner_support)
        G(YandexTeamBackendContext, passport_name=imap_name, maillist=partner_point_support)

        data = self.get_json_response(email=imap_name+'@yandex-team.ru')

        self.assertEqual(sorted(data['subscribers']['inbox']),
                         ['partner-support-at-market-yandex-ru@mail.yandex-team.ru'])

    def test_expand_email_with_conflict_emails_and_ordered_by__is_deleted(self):
        """
        Проверим, что для email-ов вида partner-support@market.yandex.ru и partner.support@market.yandex.ru
        с одинаковым email-ом в yandexteambackendcontext возвращаются результаты из delivery/info
        При этом одна рассылка удаленная, а 2я - нет. Проверим, что результат вернется для актуальной рассылки.
        """

        partner_support = G(MailList, name='partner-support@market.yandex.ru', email='partner1@market.yandex.ru')
        partner_point_support_deleted = G(
            MailList, name='partner.support@market.yandex.ru', email='partner2@market.yandex.ru',
            is_deleted=True
        )

        imap_name = 'partner-support-at-market-yandex-ru'
        G(YandexTeamBackendContext, passport_name=imap_name, maillist=partner_support)
        G(YandexTeamBackendContext, passport_name=imap_name, maillist=partner_point_support_deleted)

        create_many_emailsubscribers(partner_support, [
            'ignition@ld.yandex.ru', 'ignition@mail.yandex-team.ru',
            'shelkovin@mail.yandex-team.ru', 'm1@ld.yandex.ru'
        ])
        create_many_emailsubscribers(partner_point_support_deleted, [
            'shelkovin@mail.yandex-team.ru', 'm1@ld.yandex.ru'
        ])

        data = self.get_json_response(email=imap_name+'@yandex-team.ru')

        sorted_correct_emails = sorted([
            'partner-support-at-market-yandex-ru@mail.yandex-team.ru', 'ignition@ld.yandex.ru',
            'ignition@mail.yandex-team.ru', 'shelkovin@mail.yandex-team.ru', 'm1@ld.yandex.ru'
        ])
        self.assertEqual(sorted(data['subscribers']['inbox']), sorted_correct_emails)

    def test_maillist_with_tld_domain(self):
        """
        ML-1472:
        На примере рассылок support@yandex-team.ru & support@yandex-team.com проверим,
        что отдается разная информация для этих рассылок.
        А для несуществующей в базе ML рассылки с email-ом support@yandex-team.com.ua отдается информация о подписчиках,
        как для support@yandex-team.ru
        """
        support_ru = maillist_for_delivery('support', is_imap=True)
        support_com = G(MailList, name='support@yandex-team.com', email='support@yandex-team.com', is_sub=True, is_imap=True,
          readonly=False, is_deleted=False, modified_at=datetime.now())
        G(YandexTeamBackendContext, passport_name='support', maillist=support_com)

        emailsubs_for_ru = [
            's@yandex.ru', 'pierre@mail.yandex-team.ru', 'ignition@mail.yandex-team.ru'
        ]
        emailsubs_for_com = [
            'sp@ld.yandex.ru', 'bonifaci@mail.yandex-team.ru'
        ]

        create_many_emailsubscribers(support_ru, emailsubs_for_ru)
        create_many_emailsubscribers(support_com, emailsubs_for_com)

        data = self.get_json_response(email=support_ru.email)
        self.assertEqual(sorted(data['subscribers']['inbox']),
                         sorted(emailsubs_for_ru + ['support@mail.yandex-team.ru']))
        data = self.get_json_response(email=support_com.email)
        self.assertEqual(sorted(data['subscribers']['inbox']),
                         sorted(emailsubs_for_com + ['support@mail.yandex-team.ru']))

        data = self.get_json_response(email='support@yandex-team.com.tr')
        self.assertEqual(sorted(data['subscribers']['inbox']),
                         sorted(emailsubs_for_ru) + ['support@mail.yandex-team.ru'])

    def test_maillist_without_tld_domain_in_cache(self):
        """
        ML-1472:
        Проверим, что если информация содержится в кеше для email-a: staff@yandex-team.ru, то тот же самый ответ будет
        выдаваться и для staff@yandex-team.com|com.ua|com.tr, если явно не создано рассылок с таким email-ом
        (аналогично тому, что их нет в кеше).
        """
        staff_email = 'staff@yandex-team.ru'
        staff_email_com_ua = 'staff@yandex-team.com.ua'
        staff_email_com_tr = 'staff@yandex-team.com.tr'
        staff_email_com = 'staff@yandex-team.com'

        maillist_for_delivery('staff', is_imap=True, is_sub=True)

        body = '{"status": "ok", "is_open": true, "is_internal": true, "type": "maillist",' \
               '"subscribers": {"inbox":["staff@mail.yandex-team.ru", "fantasy@mail.yandex-team.ru", ' \
               '"ignition@mail.yandex-team.ru", "pierre@ld.yandex.ru",  "pierre@mail.yandex-team.ru", ' \
               '"alex89@ld.yandex.ru", "alex89@mail.yandex-team.ru", "art@mail.yandex-team.ru", ' \
               '"art@mail.yandex-team.ru"]}}'
        G(
            NwsmtpInfoCache, version='1.0', headers='{"X-ML-Cached-On": "public, max-age=60"}',
            body=body, modified_at=datetime.now(), email=staff_email
        )
        response = self.get_default_response(email=staff_email)
        data = json.loads(response.content)
        output_emails = sorted([
            "staff@mail.yandex-team.ru", "fantasy@mail.yandex-team.ru", "ignition@mail.yandex-team.ru",
            "pierre@ld.yandex.ru",  "pierre@mail.yandex-team.ru", "alex89@ld.yandex.ru",
            "alex89@mail.yandex-team.ru", "art@mail.yandex-team.ru", "art@mail.yandex-team.ru"
        ])

        data = self.get_json_response(email=staff_email)
        self.assertEqual(sorted(data['subscribers']['inbox']), output_emails)

        data = self.get_json_response(email=staff_email_com)
        self.assertEqual(sorted(data['subscribers']['inbox']), output_emails)

        data = self.get_json_response(email=staff_email_com_ua)
        self.assertEqual(sorted(data['subscribers']['inbox']), output_emails)

        data = self.get_json_response(email=staff_email_com_tr)
        self.assertEqual(sorted(data['subscribers']['inbox']), output_emails)

    def test_maillist_with_tld_domain_in_cache(self):
        """
        ML-1472:
        Проверим, что если информация содержится в кеше для email-a hello@yandex-team.ru,
        и если есть данные в кеше для hello@yandex-team.com, то отдаются разные ответы.
        (аналогично тому, что их нет в кеше).
        """
        hello_email = 'hello@yandex-team.ru'
        hello_email_com = 'hello@yandex-team.com'

        body = '{"status": "ok", "is_open": true, "is_internal": true, "type": "maillist",' \
               '"subscribers": {"inbox":["hello@mail.yandex-team.ru"]}}'
        G(
            NwsmtpInfoCache, version='1.0', headers='{"X-ML-Cached-On": "public, max-age=60"}',
            body=body, modified_at=datetime.now(), email=hello_email
        )

        body = '{"status": "ok", "is_open": true, "is_internal": true, "type": "maillist",' \
               '"subscribers": {"inbox":["hello-at-yandex-team-com@mail.yandex-team.ru"]}}'
        G(
            NwsmtpInfoCache, version='1.0', headers='{"X-ML-Cached-On": "public, max-age=60"}',
            body=body, modified_at=datetime.now(), email=hello_email_com
        )

        response = self.get_default_response(email=hello_email)
        data = json.loads(response.content)
        self.assertEqual(sorted(data['subscribers']['inbox']), ["hello@mail.yandex-team.ru"])

        data = self.get_json_response(email=hello_email_com)
        self.assertEqual(sorted(data['subscribers']['inbox']), ["hello-at-yandex-team-com@mail.yandex-team.ru"])


    # ===== Проверка раскрытия пользователей =====

    def test_expand_exchange_users_with_mail_domain(self):
        """
        Проверяем, что delivery/info раскрывает для exchange пользователей email в домене mail.yandex-team.ru и
        ld.yandex.ru
        """
        exchange_user = create_user('aleksundra', has_exchange=True)
        ml = create_maillist('ml', inbox=[exchange_user, ])
        data = self.get_json_response(email=ml.email)
        self.assertEquals(sorted(data['subscribers']['inbox']), sorted([
            u'aleksundra@mail.yandex-team.ru', u'aleksundra@ld.yandex.ru', u'ml@mail.yandex-team.ru'
        ]))

    def test_expand_inbox_both_imap_users(self, **extra):
        """
        Проверка, что для INBOX & BOTH пользователей отдаем в delivery/info,
        а IMAP_USER-a не отдаем
        """
        maillist = create_maillist('ml2', inbox=['fantasy', ], imap=['u21', ], emails=['a@b.com'])
        data = self.get_json_response(email=maillist.email)
        self.assertEquals(data['type'], 'maillist')
        self.assertEquals(sorted(data['subscribers']['inbox']),
                          sorted([u'fantasy@mail.yandex-team.ru', u'a@b.com', u'ml2@mail.yandex-team.ru']))

    def test_user_info(self, **extra):
        """ Простая проверка """
        u = create_user('svetleo')
        data = self.get_json_response(email=u.email)
        self.assertEquals(data['emails'], [u'svetleo@mail.yandex-team.ru', ])
        self.assertEquals(data['type'], 'user')

    def test_expand_not_exchange_user(self, **extra):
        """ Раскрываем не exchange-пользователя в рассылке"""
        maillist = create_maillist('ml2', inbox=['lavrinenko', ])
        data = self.get_json_response(email=maillist.email)
        self.assertEquals(data['subscribers']['inbox'], [u'lavrinenko@mail.yandex-team.ru',
                                                         u'ml2@mail.yandex-team.ru'])

    def test_expand_exchange_user(self):
        """Раскрываем exchange-пользователя в рассылке"""
        exchange_user = create_user('aleksundra', has_exchange=True)
        ml51 = create_maillist('ml51', inbox=[exchange_user, ])
        data = self.get_json_response(email=ml51.email)
        self.assertEquals(sorted(data['subscribers']['inbox']), sorted([
            u'aleksundra@ld.yandex.ru', u'aleksundra@mail.yandex-team.ru', u'ml51@mail.yandex-team.ru'
        ]))

    def test_user_with_email_not_equal_work_email(self):
        """
        Проверить, что раскрываются пользователи с email-ами, которые не совпадают с work_email в стаффе.
        """
        user_for_delivery('ignition', work_email='ignition@yandex-team.com')
        data = self.get_json_response(email='ignition@yandex-team.ru')
        self.assertEquals(data['emails'], ['ignition@mail.yandex-team.ru', ])
        self.assertEquals(data['type'], 'user')
        self.assertEquals(data['login'], 'ignition')
        self.assertEquals(data['status'], 'ok')

    def test_user_with_alias(self):
        """
        Проверим, что алиасы (напр. @auto.ru) раскрываются.
        """
        user_for_delivery('fantasy', work_email='fantasy@yandex-team.ru')
        data = self.get_json_response(email='fantasy@auto.ru')
        self.assertEquals(data['emails'], ['fantasy@mail.yandex-team.ru', ])
        self.assertEquals(data['type'], 'user')
        self.assertEquals(data['login'], 'fantasy')
        self.assertEquals(data['status'], 'ok')

    def test_dismissed_user(self):
        """
        Ничего не должны отдавать на уволенного сотрудника.
        TODO: подумать еще над этим: MPROTO-1900
        """
        user_for_delivery('juanych', work_email='juanych@yandex-team.ru', is_dismissed=True)
        data = self.get_json_response(email='juanych@yandex-team.ru')

        self.assertEquals(data['emails'], [])
        self.assertEquals(data['type'], 'user')
        self.assertEquals(data['login'], 'juanych')
        self.assertEquals(data['status'], 'ok')

    def test_yamoney_domain_user(self):
        """
        Проверим выдачу для сотрудников денег (со стороны delivery/info отдаем error, т.к. этот домен в почте
        мы не обрабатываем).
        """
        user_for_delivery('chernishev', work_email='chernishev@yandex-team.ru', affiliation='yamoney')
        data = self.get_json_response(email='chernishev@yamoney.ru')

        self.assertEquals(data['error'], 'no entity with email chernishev@yamoney.ru')
        self.assertEquals(data['status'], 'error')

    def test_yamoney_domain_user_with_work_email_not_equal_yamoney(self):
        """
        Проверим выдачу для сотрудников Я.Денег, у которых в поле work_email указан yandex-team.ru домен.
        """
        # У этого пользователя нет Exchange-почты.
        grachevanv = user_for_delivery('grachevanv', work_email='grachevanv@yandex-team.ru', affiliation='yamoney')
        data = self.get_json_response(email='grachevanv@yandex-team.ru')

        self.assertEquals(data['emails'], ['grachevanv@yandex-team.ru'])
        self.assertEquals(data['status'], 'ok')

        # У этого пользователя есть Exchange-почта.
        eugenio = user_for_delivery('eugenio', work_email='eugenio@yandex-team.ru', affiliation='yamoney', has_exchange=True)
        data = self.get_json_response(email='eugenio@yandex-team.ru')

        self.assertEquals(data['emails'], ['eugenio@ld.yandex.ru', 'eugenio@mail.yandex-team.ru'])
        self.assertEquals(data['status'], 'ok')

    def test_external_domain_user(self):
        """
        Проверим выдачу для внешнего email-a (со стороны delivery/info отдаем error, т.к. этот домен в почте
        мы не обрабатываем).
        """
        user_for_delivery('fantasy')
        data = self.get_json_response(email='fantasy@gmail.com')

        self.assertEquals(data['error'], 'no entity with email fantasy@gmail.com')
        self.assertEquals(data['status'], 'error')

    def test_robot(self):
        """
        Проверим выдачу для робота
        """
        robot = user_for_delivery('robot-yndx-maillists')
        data = self.get_json_response(email=robot.email)

        self.assertEquals(data['emails'], ['robot-yndx-maillists@mail.yandex-team.ru'])
        self.assertEquals(data['status'], 'ok')

    def test_old_cmail_user_alias(self):
        """
        Проверим выдачу для старых cmail-пользователяских алиасов
        (например для tokza@, tvt@, bernard@ и др), для dj@ -> tvt@ редирект не создался, кстати.
        """
        tokza = user_for_delivery('tokza', work_email='tokza@yandex-team.ru', has_exchange=False)
        a_point_karpov = maillist_for_delivery('a.karpov')
        G(Subscribers, list=a_point_karpov, user=tokza, is_sub=True)
        data = self.get_json_response(email=a_point_karpov.email)
        self.assertEquals(data['subscribers']['inbox'], ['a-karpov@mail.yandex-team.ru', 'tokza@mail.yandex-team.ru'])

        bernard = user_for_delivery('bernard', work_email='bernard@yandex-team.ru', has_exchange=False)
        blukey = maillist_for_delivery('blukey')
        G(Subscribers, list=blukey, user=bernard, is_sub=True)
        data = self.get_json_response(email=blukey.email)
        self.assertEquals(data['subscribers']['inbox'], ['blukey@mail.yandex-team.ru', 'bernard@mail.yandex-team.ru'])

    # https://st.yandex-team.ru/ML-1443
    def test_user_with_point_as_crm_email(self):
        """
        Проверяем, что email сотрудники, которые совпадают с crm-email-ом - раскрываются по-разному
        и не конфликтуют.
        TODO: выпилить, после того, как сотрудники на стаффе:
        a-pavlova@yandex-team.ru
        d-melnikova@yandex-team.ru
        pavlova-m@yandex-team.ru
        поменяют свой логин на другой.
        """
        user_for_delivery('a-pavlova', has_exchange=True)
        maillist = maillist_for_delivery('a.pavlova')
        tcrm = maillist_for_delivery('tcrm')
        G(EmailSubscriber, list=maillist, email='tcrm@yandex-team.ru')
        data_for_tcrm = self.get_json_response(email='a.pavlova@yandex-team.ru')
        data_for_user = self.get_json_response(email='a-pavlova@yandex-team.ru')
        self.assertEqual(data_for_tcrm['subscribers']['inbox'], ['tcrm@mail.yandex-team.ru'])
        self.assertEqual(sorted(data_for_user['emails']),
                         sorted(['a-pavlova@ld.yandex.ru', 'a-pavlova@mail.yandex-team.ru']))
        self.assertNotEqual(data_for_tcrm['subscribers']['inbox'], data_for_user['emails'])

        user_for_delivery('d-melnikova', has_exchange=True)
        maillist = maillist_for_delivery('d.melnikova')
        G(EmailSubscriber, list=maillist, email='tcrm@yandex-team.ru')
        data_for_tcrm = self.get_json_response(email='d.melnikova@yandex-team.ru')
        data_for_user = self.get_json_response(email='d-melnikova@yandex-team.ru')
        self.assertEqual(data_for_tcrm['subscribers']['inbox'], ['tcrm@mail.yandex-team.ru'])
        self.assertEqual(sorted(data_for_user['emails']),
                         sorted(['d-melnikova@mail.yandex-team.ru', 'd-melnikova@ld.yandex.ru']))
        self.assertNotEqual(data_for_tcrm['subscribers']['inbox'], data_for_user['emails'])

        user_for_delivery('pavlova-m', has_exchange=True)
        maillist = maillist_for_delivery('pavlova.m')
        G(EmailSubscriber, list=maillist, email='tcrm@yandex-team.ru')
        data_for_tcrm = self.get_json_response(email='pavlova.m@yandex-team.ru')
        data_for_user = self.get_json_response(email='pavlova-m@yandex-team.ru')
        self.assertEqual(data_for_tcrm['subscribers']['inbox'], ['tcrm@mail.yandex-team.ru'])
        self.assertEqual(sorted(data_for_user['emails']),
                         sorted(['pavlova-m@ld.yandex.ru', 'pavlova-m@mail.yandex-team.ru']))
        self.assertNotEqual(data_for_tcrm['subscribers']['inbox'], data_for_user['emails'])

    def test_email_subscribers_with_spaces(self):
        """
        Проверить раскрытие пользователей с сырыми email-ами (с пробелами в начале и в конце)
        """
        user_for_delivery('fantasy')
        data = self.get_json_response(email=' fantasy@yandex-team.ru ')
        self.assertEqual(data['emails'], ['fantasy@mail.yandex-team.ru'])

    def test_user_not_in_passport(self):
        """
        Проверяем выдачу для юзера, которого нет в паспорте.
        """
        user_for_delivery('user111')
        data = self.get_json_response(email='user111@yandex-team.ru')
        self.assertEqual(data['status'], 'error')

    def test_user_only_in_passport_not_in_staff(self):
        """
        TODO: Проверяем выдачу для юзера, который есть в паспорте, но его нет в стаффе
        (не сравнивала пока данные из стаффа с данными из паспорта)
        """
        pass

    def test_user_in_domain_startswith_mail_yandex_team(self):
        """
        Проверить, что не выдаем ничего для юзера в домене mail.yandex-team.ru|сom|com.tr|com.ua
        """
        user_for_delivery('fantasy')
        check_domains = ['@mail.yandex-team.ru', '@mail.yandex-team.com',
                         '@mail.yandex-team.com.ua', '@mail.yandex-team.com.tr']
        for domain in check_domains:
            data = self.get_json_response(email='fantasy'+domain)
            self.assertEqual(data['status'], 'error')

    def test_expand_so_many_users(self, **extra):
        """
        Проверим, что expand правильно разворачивает рассылку в плоский список
        TODO: выделить простые случаи, разбить на части
        """
        # тестовые данные для проверки expand
        # рассылки
        maillist1 = self.maillist1

        maillist2 = G(MailList, email='maillist2@yandex-team.ru', name='maillist2')
        G(
            YandexTeamBackendContext,
            passport_name=maillist2.name,
            maillist=maillist2,
        )
        maillist3 = G(MailList, email='maillist3@yandex-team.ru', name='maillist3')
        G(
            YandexTeamBackendContext,
            passport_name=maillist3.name,
            maillist=maillist3,
        )
        maillist4 = G(MailList, email='maillist4@yandex-team.ru', name='maillist4')
        G(
            YandexTeamBackendContext,
            passport_name=maillist4.name,
            maillist=maillist4,
        )

        # пользователи
        fantasy = G(User, login='fantasy', email='fantasy@yandex-team.ru')
        fantasy.staff = G(Staff, login='fantasy', work_email='fantasy@yandex-team.ru')
        fantasy.save()

        lavrinenko = G(User, login='lavrinenko', email='lavrinenko@yandex-team.ru')
        pierre = G(User, login='pierre', email='pierre@yandex-team.ru')
        kapp = G(User, login='kapp', email='kapp@yandex-team.ru')
        zubchik = G(User, login='zubchik', email='zubchik@yandex-team.ru')
        granatka = G(User, login='granatka', email='granatka@yandex-team.ru')
        alexkoshelev = G(User, login='alexkoshelev', email='alexkoshelev@yandex-team.ru')

        # подписчики рассылок
        G(Subscribers, list=maillist1, user=fantasy, is_imap=True)
        G(Subscribers, list=maillist1, user=lavrinenko, is_imap=True)
        G(Subscribers, list=maillist1, user=pierre, is_imap=True)
        G(EmailSubscriber, list=maillist1, email='kewtree@gmail.com')
        G(EmailSubscriber, list=maillist1, email='s@lavr.me')
        G(EmailSubscriber, list=maillist1, email='maillist2@yandex-team.ru')

        G(Subscribers, list=maillist2, user=fantasy, is_sub=True)
        G(Subscribers, list=maillist2, user=lavrinenko, is_imap=True)
        G(Subscribers, list=maillist2, user=pierre, is_imap=True)
        G(Subscribers, list=maillist2, user=zubchik, is_sub=True)
        G(EmailSubscriber, list=maillist2, email='kewtree@gmail.com')
        G(EmailSubscriber, list=maillist2, email='zubchik@yandex.ru')
        G(EmailSubscriber, list=maillist2, email='pierre@gmail.com')
        G(EmailSubscriber, list=maillist2, email='maillist3@yandex-team.ru')

        G(Subscribers, list=maillist3, user=pierre, is_sub=True)
        G(Subscribers, list=maillist3, user=alexkoshelev, is_sub=True)
        G(Subscribers, list=maillist3, user=granatka, is_imap=True)
        G(EmailSubscriber, list=maillist3, email='maillist1@yandex-team.ru')
        G(EmailSubscriber, list=maillist3, email='maillist2@yandex-team.ru')
        G(EmailSubscriber, list=maillist3, email='maillist4@yandex-team.ru')

        G(Subscribers, list=maillist4, user=kapp, is_imap=True, is_sub=True)
        G(EmailSubscriber, list=maillist4, email='pierre@yandex-team.ru')
        G(EmailSubscriber, list=maillist4, email='maillist3@yandex-team.ru')
        G(EmailSubscriber, list=maillist4, email='maillist1@yandex-team.ru')

        # проверяем, что плоский список тот, который ожидали
        correct_emails = [
            u'kewtree@gmail.com',
            u's@lavr.me',
            u'fantasy@mail.yandex-team.ru',
            u'zubchik@yandex.ru',
            u'pierre@gmail.com',
            u'maillist1@mail.yandex-team.ru',
            u'maillist2@mail.yandex-team.ru',
            u'maillist3@mail.yandex-team.ru',
            u'maillist4@mail.yandex-team.ru',
        ]

        response = self.get_default_response(email='maillist1@yandex-team.ru', update_cache='yes')
        data = json.loads(response.content)
        output_emails = data['subscribers']['inbox']
        output_emails.sort()
        correct_emails.sort()
        self.assertEquals(output_emails, correct_emails)
        self.assertIsNone(response._headers.get('x-ml-cached-on'))

        # Проверяем, что второй раз взялось из кэша
        response = self.get_default_response(email='maillist1@yandex-team.ru')
        print response._headers
        self.assertIsNotNone(response._headers['x-ml-cached-on'])
        data = json.loads(response.content)
        output_emails = data['subscribers']['inbox']
        output_emails.sort()
        correct_emails.sort()
        self.assertEquals(output_emails, correct_emails)
