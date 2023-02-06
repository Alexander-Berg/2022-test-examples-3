# coding: utf-8

from django.test import TestCase
import unittest
from milkman.dairy import milkman

# Models
from mlcore.ml.models import MailList, SuidLookup
from mlcore.permissions.models import ListPermission, Type
from django_intranet_stuff.models import Staff, Group, GroupMembership, Log, LastUpdate
from django.contrib.auth.models import User
from datetime import datetime

from mlcore.permissions.utils import can_read, get_subgroups, get_parent_groups
from mlcore.permissions.center_group_synchro import import_group_membership_log, synchronize_center_logs
from .utils import is_external_staff
from mlcore.utils.getters import get_staff


def create_group(**kw):
    kw.setdefault('created_at', datetime.now())
    kw.setdefault('modified_at', datetime.now())
    o = Group(**kw)
    o.save()
    return o

def G(cls, after_save=None, **kw):
    o = milkman.deliver(cls, **kw)
    if after_save:
        for k, v in after_save.items():
            setattr(o, k, v)
        o.save()
    return o

def prepare_groups(self):
    """
    MPTT-дерево для групп своими руками(rgth, url, lft на рис). Малая часть того, что содержится
    в настоящих Departments-Groups

    level
    0                                    (1 deps 28)
                             /                                     \
    1            (2 yandex 5)                                  (22 as (assessors) 27)
                       |                                              |
    2           (3 ya_pers 20)                                    (23 as_rus 26)
             /         |           \                                  |
    3 (4 yp_mail 5)  (6 yp_auth 7) (8 ya_pers_com 19)             (24 as_rus_msk1 25)
                                       |                 \
    4                            (9 com_aux 16)         (17_com_related 18)
                        /              |           \
    5        (10 aux_pers 11) (12 aux_inter 13) (14 aux_enter 15)

    """

    self.deps = create_group(id=1, url='deps', level=0, parent_id=None, lft=1, rght=28, tree_id=2, intranet_status=1)

    self.ya = create_group(id=2, url='yandex', level=1, parent_id=1, lft=2, rght=21, tree_id=2, intranet_status=1, created_at=datetime.now(), modified_at=datetime.now())
    self.as_all = create_group(id=3, url='as', level=1, parent_id=1, lft=22, rght=27, tree_id=2, intranet_status=1)

    self.ya_pers = create_group(id=4, url='yandex_personal', level=2, parent_id=2, lft=3, rght=20, tree_id=2, intranet_status=1)
    self.as_rus = create_group(id=5, url='as_rus', level=2, parent_id=3, lft=23, rght=26, tree_id=2, intranet_status=1)

    self.ya_pers_mail = create_group(id=6, url='yandex_personal_mail', level=3, parent_id=4, lft=4, rght=5, tree_id=2, intranet_status=1)
    self.ya_pers_auth = create_group(id=7, url='yandex_personal_auth', level=3, parent_id=4, lft=6, rght=7, tree_id=2, intranet_status=1)
    self.ya_pers_com = create_group(id=8, url='yandex_personal_com', level=3, parent_id=4, lft=8, rght=19, tree_id=2, intranet_status=1)
    self.as_msk1 = create_group(id=9, url='as_rus_msk1', level=3, parent_id=5, lft=24, rght=25, tree_id=2, intranet_status=1)

    self.aux = create_group(id=10, url='yandex_personal_com_aux', level=4, parent_id=8, lft=9, rght=16, tree_id=2, intranet_status=1)
    self.rel = create_group(id=11, url='yandex_personal_com_related', level=4, parent_id=8, lft=17, rght=18, tree_id=2, intranet_status=1)

    self.aux_pers = create_group(id=12, url='yandex_personal_com_aux_person', level=5, parent_id=10, lft=10, rght=11, tree_id=2, intranet_status=1)
    self.aux_inter = create_group(id=13, url='yandex_personal_com_aux_inter', level=5, parent_id=10, lft=12, rght=13, tree_id=2, intranet_status=1)
    self.aux_enter = create_group(id=14, url='yandex_personal_com_aux_entertain', level=5, parent_id=10, lft=14, rght=15, tree_id=2, intranet_status=1)


class PermissionsTestCase(TestCase):
    def setUp(self):
        prepare_groups(self)
        login = 'fantasy'
        self.staff_s = G(Staff, login=login, after_save={'user': G(User, is_active=True, username=login)})

        login = 'akhmetov'
        self.staff_u = G(Staff, login=login, after_save={'user': G(User, is_active=True, username=login)})

        login = 'astel'
        self.staff_a = G(Staff, login=login, after_save={'user': G(User, is_active=True, username=login)})

        self.user_s = self.staff_s.user
        self.user_u = self.staff_u.user
        self.user_a = self.staff_a.user

        self.group = G(Group)
        G(GroupMembership, after_save={'staff':self.staff_u, 'group':self.group})

        self.list_open = G(MailList, parent=None, is_open=True, fsuid=100)
        self.list_closed = G(MailList, parent=None, is_open=False, fsuid=101)

        # milkman.deliver(Subscribers, list=self.list_open, user=self.user_u, type='imap')
        # milkman.deliver(Subscribers, list=self.list_closed, user=self.user_u, type='imap')

        self.read_type = G(Type, name='read', id=1)
        self.write_type = G(Type, name='write', id=2)

        G(ListPermission, user=self.user_s, list=self.list_closed, approved=True, type=self.read_type)
        G(ListPermission, user=self.user_s, list=self.list_open, approved=True, type=self.read_type)

        G(ListPermission, user=self.user_u, list=self.list_closed, approved=False, type=self.read_type)
        G(ListPermission, user=self.user_u, list=self.list_open, approved=True, type=self.read_type)

        G(ListPermission, user=self.user_a, list=self.list_closed, approved=False, type=self.read_type)
        G(ListPermission, user=self.user_a, list=self.list_open, approved=False, type=self.read_type)

        G(SuidLookup, login=self.list_open.name, suid=self.list_open.fsuid)
        G(SuidLookup, login=self.list_closed.name, suid=self.list_closed.fsuid)

        G(LastUpdate, update_type=1101, type='membership_log')
        G(LastUpdate, update_type=1102, type='ml_center_log_parse')

    def test_parent_groups(self):
        """ Проверяем, совпадают ли надгруппы с созданной выше схемой """
        groups = Group.objects.filter(url='yandex_personal_com_aux_person')
        parent_groups = get_parent_groups(groups).values_list('url', flat=True)
        self.assertIn('yandex_personal_com_aux', parent_groups)
        self.assertIn('yandex_personal_com', parent_groups)
        self.assertIn('yandex_personal', parent_groups)
        self.assertIn('yandex', parent_groups)

    def test_subgroups(self):
        """ Проверяем, совпадают ли подгруппы с созданной выше схемой """
        groups = Group.objects.filter(url='as')
        subgroups = get_subgroups(groups).values_list('url', flat=True)
        self.assertIn('as_rus', subgroups)
        self.assertIn('as_rus_msk1', subgroups)

    def test_can_read(self):
        """Проверим права на чтение у соответствующих юзеров """
        self.assertTrue(can_read(self.user_s, self.list_open))
        self.assertTrue(can_read(self.user_u, self.list_open))
        self.assertTrue(can_read(self.user_a, self.list_open))

        self.assertTrue(can_read(self.user_s, self.list_closed))
        self.assertFalse(can_read(self.user_u, self.list_closed))
        self.assertFalse(can_read(self.user_a, self.list_closed))

    def test_membership_log(self):
        """
        Проверяем, что сигнал на пост-удаление работает:
        смотрим на дату последнего удаления GroupMembership,
        смотрим в базу django_intranet_stuff_log
        смотрим на дату в intranet_lastupdate в поле membership_log
        """
        gm = milkman.deliver(GroupMembership)
        now_time = datetime.now()
        gm.delete()
        modified_at = Log.objects.filter(model_name='GroupMembership').latest('modified_at').modified_at
        #  проверка времени с точностью до минуты
        self.assertEqual(modified_at.year, now_time.year)
        self.assertEqual(modified_at.month, now_time.month)
        self.assertEqual(modified_at.hour, now_time.hour)
        self.assertEqual(modified_at.minute, now_time.minute, modified_at)

        lu = import_group_membership_log(None)
        self.assertEqual(lu, modified_at, lu)

    def test_ml_center_log_parse(self):
        """
        Проверяем, что сигнал на пост-сохранение работает:
        смотрим на дату последнего добавления в Group,
        смотрим в базу django_intranet_stuff_log
        смотрим на дату в intranet_lastupdate в поле membership_log
        """
        g = milkman.deliver(Group, intranet_status=0)
        now_time = datetime.now()
        modified_at = Log.objects.filter(model_name='Group').latest('modified_at').modified_at
        #  проверка времени с точностью до минуты
        self.assertEqual(modified_at.year, now_time.year)
        self.assertEqual(modified_at.month, now_time.month)
        self.assertEqual(modified_at.hour, now_time.hour)
        self.assertEqual(modified_at.minute, now_time.minute, modified_at)

        sc = synchronize_center_logs(None)
        self.assertEqual(sc, modified_at, sc)


    def test_affiliation(self):
        """
        Проверяем, что внешний пользователь не может читать внутреннюю рассылку,
        а все остальные - могут.
        """
        external = G(User, after_save={'staff': G(Staff, affiliation='external')})
        assert external.staff.affiliation == 'external'
        assert is_external_staff(external)

        yandex =   G(User, after_save={'staff': G(Staff, affiliation='yandex')})
        assert not is_external_staff(yandex)

        yamoney =  G(User, after_save={'staff': G(Staff, affiliation='yamoney')})
        assert not is_external_staff(yamoney)


        int_list = G(MailList, external_staff_can_read=False, is_open=True)
        assert int_list.external_staff_can_read is False

        ext_list = G(MailList, external_staff_can_read=True, is_open=True)
        assert ext_list.external_staff_can_read is True


        # открытую внешнюю рассылку можно читать
        self.assertTrue(can_read(yamoney, ext_list))
        self.assertTrue(can_read(yandex, ext_list))
        self.assertTrue(can_read(external, ext_list))

        # нельзя читать во внутреннюю рассылку внешним сотрудникам
        self.assertFalse(can_read(external, int_list))

        # можно читать во внутреннюю рассылку яндексоидам и яманевцам
        # TODO: дописать тест, в таком варианте он не должен работать
        #self.assertTrue(can_read(yamoney, int_list))
        #self.assertTrue(can_read(yandex, int_list))

