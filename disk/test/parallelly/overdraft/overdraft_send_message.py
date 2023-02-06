# -*- coding: utf-8 -*-
import contextlib
import datetime
from copy import deepcopy

import mock
from parameterized import parameterized

from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.common.util.overdraft import BLOCK_OVERDRAFT_STATUS, LIGHT_OVERDRAFT_STATUS
from mpfs.core.billing.dao.overdraft import OverdraftDAO
from mpfs.core.overdraft.tasks import create_overdraft_send_message_tasks
from mpfs.core.support import comment
from mpfs.core.user.base import User
from mpfs.metastorage.mongo.collections.system import DiskInfoCollection
from test.base_suit import SharingTestCaseMixin
from test.helpers.size_units import GB
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PassportStub, BazingaInterfaceStub

from test.base import DiskTestCase, time_machine


class OverdraftSendMessageManagerTestCase(DiskTestCase):
    def test_create_send_message_tasks(self):
        uids = ['1111111', '2222222']
        for uid in uids:
            self.create_user(uid)
            user = User(uid)
            user.set_overdraft_info(datetime.date.today())

        with BazingaInterfaceStub() as stub:
            create_overdraft_send_message_tasks()
            stub.bulk_create_tasks.assert_called_once()
            args, _ = stub.bulk_create_tasks.call_args
            tasks = args[0]
            assert len(tasks) == 2
            assert sorted(uids) == sorted(task.uid for task in tasks)


class OverdraftSendMessageWorkerTestCase(DiskTestCase, SharingTestCaseMixin):
    current_date = datetime.datetime(2021, 12, 1)
    light_overdraft_date = current_date - datetime.timedelta(days=10)
    hard_overdraft_date = current_date - datetime.timedelta(days=30)
    block_overdraft_date = current_date - datetime.timedelta(days=60)

    @contextlib.contextmanager
    def _prepare_context(self, has_disk=True, experiment_enabled=True, used_space=1000, limit_space=100,
                         overdraft_date=None, last_email_date=None, last_push_date=None, is_user_deleted=False):
        if limit_space is not None:
            DiskInfoCollection().put(self.uid, '/limit', 100 * GB)
        DiskInfoCollection().put(self.uid, '/total_size', used_space * GB)
        OverdraftDAO().update_or_create(self.uid, overdraft_date or self.block_overdraft_date,
                                        last_email_date=last_email_date, last_push_date=last_push_date)
        userinfo = deepcopy(DEFAULT_USERS_INFO['deleted_account']) if is_user_deleted else deepcopy(DEFAULT_USERS_INFO[self.uid])
        userinfo['has_disk'] = has_disk

        with PassportStub(userinfo), \
            enable_experiment_for_uid('new_overdraft_strategy', self.uid, enabled=experiment_enabled), \
            time_machine(self.current_date), \
            mock.patch('mpfs.core.pushnotifier.queue.PushQueue.put', return_value=None) as send_push, \
            mock.patch('mpfs.core.overdraft.notifications.send_email_async_by_uid') as send_email:
            yield send_push, send_email

    @parameterized.expand([
        ('without disk', {'has_disk': False}),
        ('without experiment', {'experiment_enabled': False}),
        ('space correct', {'used_space': 50}),
        ('space small over', {'used_space': 100.1}),
    ])
    def test_logic(self, name, context_params):
        with self._prepare_context(**context_params):
            self._run_task()
            comments = comment.select(self.uid)
            assert len(comments) == 0

    @parameterized.expand([
        (None, True),
        (current_date - datetime.timedelta(days=1), True),
        (current_date - datetime.timedelta(seconds=60), False),
    ])
    def test_send_push(self, last_push_date, need_send_push):
        with self._prepare_context(last_push_date=last_push_date) as (send_push, _):
            self._run_task()

            if need_send_push:
                assert send_push.call_count == 1
                assert send_push.call_args[0][0] == {
                    'class': 'quota_overdraft_regular',
                    'uid': self.uid,
                    'xiva_data': {
                        'root': {
                            "tag": "quota_overdraft_regular",
                            "parameters": {
                                'overdraft_lite_date': '2021-10-02',
                                'overdraft_status': BLOCK_OVERDRAFT_STATUS,
                                'overdraft_hard_date': '2021-10-16',
                                'overdraft_block_date': '2021-11-15'
                            }
                        }
                    }
                }
                assert OverdraftDAO().get(self.uid)['last_push_date'].date() == self.current_date.date()

                # пробуем еще раз, не отправилось
                self._run_task()
                assert send_push.call_count == 1
            else:
                assert send_push.call_count == 0

                if last_push_date is None:
                    assert OverdraftDAO().get(self.uid)['last_push_date'] is None
                else:
                    assert OverdraftDAO().get(self.uid)['last_push_date'].date() == last_push_date.date()

    def test_send_email_for_block(self):
        with self._prepare_context(overdraft_date=self.block_overdraft_date) as (_, send_email):
            self._run_task()

            assert send_email.call_count == 1
            assert send_email.call_args[0] == (self.uid, 'the_user_is_locked')

            assert User(self.uid).is_blocked()

            comments = comment.select(self.uid)
            assert len(comments) == 1
            assert comments[0]['comment'] == 'blocked for overdraft'
            assert comments[0]['moderator'] == 'system'

            # еще раз, ничего не изменилось
            self._run_task()
            assert User(self.uid).is_blocked()
            assert len(comments) == 1
            assert send_email.call_count == 1

    @parameterized.expand([
        (1, None, True),  # Отправляем в самый первый день
        (2, None, True),  # Отправляем на следующий день, если не отправляли в самый первый день
        (2, 1, False),  # Не отправляем на сл. день, если отправили вчера
        (4, 2, False),  # Не отправляем в день, когда не нужно отправлять
        (8, 7, True),  # Отправляем во второй раз, если отправили только в первый раз
        (8, 0, False),  # Не отправляем во второй раз, если отправили сегодня
        (9, 1, False),  # Не отправляем во второй раз на сл. день, если отправили вчера
        (9, 7, True),  # Отправляем во второй раз на сл. день, если отправили давно
        (1, None, False, True),  # Не отправляем, если пользователь удален
    ])
    def test_send_email_for_light(self, days_after_overdraft, days_after_last_email, need_send, is_user_deleted=False):
        last_email_date = None
        if days_after_last_email is not None:
            last_email_date = self.current_date - datetime.timedelta(days=days_after_last_email)

        with self._prepare_context(
            overdraft_date=self.current_date - datetime.timedelta(days=days_after_overdraft),
            last_email_date=last_email_date, is_user_deleted=is_user_deleted
        ) as (_, send_email):
            self._run_task()
            assert not User(self.uid).is_blocked()

            if need_send:
                assert send_email.call_count == 1
                assert send_email.call_args[0] == (self.uid, 'the_space_is_over')
                assert send_email.call_args[1]['template_args'] == {'space': 1000 * GB, 'base_space': 100 * GB}
                assert OverdraftDAO().get(self.uid)['last_email_date'].date() == self.current_date.date()
            else:
                assert send_email.call_count == 0
                if not is_user_deleted:
                    assert OverdraftDAO().get(self.uid)['last_email_date'].date() == last_email_date.date()

            self._run_task()
            assert send_email.call_count == (1 if need_send else 0)

    @parameterized.expand([
        (16, 3, 29),
        (16, 1, None),
        (17, 5, 28),
        (23, 3, 22),
        (23, None, 22),
        (1, None, None, True),  # Не отправляем, если пользователь удален
    ])
    def test_send_email_for_hard(self, days_after_overdraft, days_after_last_email, days_left, is_user_deleted=False):
        last_email_date = None
        if days_after_last_email is not None:
            last_email_date = self.current_date - datetime.timedelta(days=days_after_last_email)

        with self._prepare_context(
            overdraft_date=self.current_date - datetime.timedelta(days=days_after_overdraft),
            last_email_date=last_email_date, is_user_deleted=is_user_deleted
        ) as (_, send_email):
            self._run_task()
            assert not User(self.uid).is_blocked()

            if days_left is not None:
                assert send_email.call_count == 1
                assert send_email.call_args[0] == (self.uid, 'the_blocking_is_going_in_days')
                assert send_email.call_args[1]['template_args'] == {'days': days_left}
                assert OverdraftDAO().get(self.uid)['last_email_date'].date() == self.current_date.date()
            else:
                assert send_email.call_count == 0
                if not is_user_deleted:
                    assert OverdraftDAO().get(self.uid)['last_email_date'].date() == last_email_date.date()

            self._run_task()
            assert send_email.call_count == (1 if days_left is not None else 0)

    @parameterized.expand([
        (block_overdraft_date, True),
        (hard_overdraft_date, False),
        (light_overdraft_date, False),
    ])
    def test_kick_user(self, overdraft_date, need_kick):
        self.create_user(self.user_1['uid'])
        self.create_user(self.user_2['uid'])

        group1 = self.create_shared_folder(self.uid, '/disk/testfolder1', [self.user_1['uid'], self.user_2['uid']])['gid']
        group2 = self.create_shared_folder(self.user_1['uid'], '/disk/testfolder2', [self.uid, self.user_2['uid']])['gid']

        with self._prepare_context(overdraft_date=overdraft_date) as (_, send_email):
            self._run_task()

            if need_kick:
                self.check_uids_in_group(group1, self.uid, [self.uid])
                assert sorted([send_email.call_args_list[1][0][0], send_email.call_args_list[2][0][0]]) == sorted([self.user_1['uid'], self.user_2['uid']])
                assert send_email.call_args_list[1][0][1] == 'dont_have_access_to_shared_folder'
                assert send_email.call_args_list[2][0][1] == 'dont_have_access_to_shared_folder'
            else:
                self.check_uids_in_group(group1, self.uid, [self.uid, self.user_1['uid'], self.user_2['uid']])
                assert send_email.call_count == 1

            self.check_uids_in_group(group2, self.user_1['uid'], [self.uid, self.user_1['uid'], self.user_2['uid']])

            self._run_task()
            if need_kick:
                assert send_email.call_count == 3
            else:
                assert send_email.call_count == 1

    def test_new_overdraft(self):
        sid = self._buy(1000)

        with self._prepare_context(used_space=500, limit_space=None) as (send_push, send_email):
            self.billing_ok(
                'service_delete',
                {'uid': self.uid, 'sid': sid, 'ip': '127.0.0.1'},
            )

            assert send_push.call_count == 2
            assert send_push.call_args_list[1][0][0] == {
                'class': 'quota_overdraft',
                'uid': self.uid,
                'xiva_data': {
                    'root': {
                        'tag': 'quota_overdraft',
                        'parameters': {
                            'overdraft_lite_date': '2021-12-01',
                            'overdraft_status': LIGHT_OVERDRAFT_STATUS,
                            'overdraft_hard_date': '2021-12-15',
                            'overdraft_block_date': '2022-01-14',
                            'overdraft_reset_count': 0,
                        }
                    }
                }
            }
            assert send_email.call_count == 1
            assert send_email.call_args[0] == (self.uid, 'the_space_is_over')
            assert send_email.call_args[1]['template_args'] == {'space': 500 * GB, 'base_space': 10 * GB}

            self._run_task()

            assert send_push.call_count == 3
            assert send_push.call_args_list[2][0][0] == {
                'class': 'quota_overdraft_regular',
                'uid': self.uid,
                'xiva_data': {
                    'root': {
                        'tag': 'quota_overdraft_regular',
                        'parameters': {
                            'overdraft_lite_date': '2021-12-01',
                            'overdraft_status': LIGHT_OVERDRAFT_STATUS,
                            'overdraft_hard_date': '2021-12-15',
                            'overdraft_block_date': '2022-01-14',
                            'overdraft_reset_count': 0,
                        }
                    }
                }
            }
            assert send_email.call_count == 1

    @parameterized.expand([
        (100, [('block_user', 'blocked for overdraft')], False),  # Купил мало места, не разблокируем
        (1000, [('block_user', 'blocked for overdraft')], True),  # Купил много места, разблокируем
        (1000, [('block_user', 'manual block')], False),  # Заблокирован не по овердрафту
        (1000, [('block_user', 'manual block'), ('unblock_user', 'manual unblock'), ('block_user', 'blocked for overdraft')], True),  # Был раньше заблокирован и разблокирован
        (1000, [('block_user', 'blocked for overdraft'), ('unblock_user', 'unblocked for overdraft'), ('block_user', 'manual block')], False),  # Теперь заблокирован руками
        (1000, [('block_user', 'blocked for overdraft'), ('comment', 'simple comment')], True),  # Обычный комментарий в середине
    ])
    def test_buy(self, buy_space, comments, need_unblock):
        for type, text in comments:
            comment.create(self.uid, 'system', type, {'comment': text})
        User(self.uid).set_block(True)

        with self._prepare_context(overdraft_date=self.block_overdraft_date, used_space=500, limit_space=10):
            self._buy(buy_space)
            if need_unblock:
                assert not User(self.uid).is_blocked()

                new_comments = comment.select(self.uid)
                assert len(new_comments) == len(comments) + 1
                assert new_comments[-1]['type'] == 'unblock_user'
                assert new_comments[-1]['comment'] == 'Unblock after buy service'
            else:
                assert User(self.uid).is_blocked()

    def _buy(self, amount_gb):
        return self.billing_ok(
            'service_create',
            {'uid': self.uid, 'line': 'partner', 'pid': 'yandex_b2b_mail_pro', 'product.amount': GB * amount_gb,
             'auto_init_user': 1, 'ip': '127.0.0.1'},
        )['sid']

    def _run_task(self):
        from mpfs.core.overdraft.logic.overdraft_send_message import BazingaOverdraftSendMessageWorker
        BazingaOverdraftSendMessageWorker(self.uid).run()
