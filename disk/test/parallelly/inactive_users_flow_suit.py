# -*- coding: utf-8 -*-

import contextlib
from datetime import datetime
from test.base import DiskTestCase, time_machine
from test.helpers.stubs.services import PassportStub, QuellerStub

import mock
from dateutil.relativedelta import relativedelta
from mpfs.config import settings
from mpfs.core.inactive_users_flow.dao import InactiveUsersFlowDAO, InactiveUsersFlowDAOItem, InactiveUsersFlowState
from mpfs.core.inactive_users_flow.logic import (
    BLOCK_COMMENT_TEXT, EmailCampaign, ExcludeReason, FlowManager, can_be_unblock_inactive_flow, get_last_block_comment,
    is_blocked_by_inactive_flow
)
from mpfs.core.user.base import User

USER_OVERDRAFT_RESTRICTIONS_THRESHOLD = settings.user['overdraft']['restrictions_threshold']


@contextlib.contextmanager
def mocks(last_active_dt=None):
    if last_active_dt is None:
        last_active_dt = datetime.strptime('2012-01-01', '%Y-%m-%d')

    with mock.patch('mpfs.core.email.logic._send_email_async') as send_email, \
            mock.patch('mpfs.core.inactive_users_flow.logic.FlowItemProcessor._get_last_active_date', return_value=last_active_dt), \
            QuellerStub():
        yield send_email


@mock.patch('mpfs.core.inactive_users_flow.logic.INACTIVE_USERS_FLOW_ENABLED', True)
class InactiveUsersFlowTestCase(DiskTestCase):

    def check_flow_item(self, uid, state, exclude_reason=None):
        flow_item = InactiveUsersFlowDAO().get_by_uid(uid)
        assert flow_item.state == state
        if exclude_reason is not None:
            assert flow_item.debug_data['exclude_reason'] == exclude_reason.value

    def check_send_one_email(self, send_email_mock, template_id):
        assert send_email_mock.call_count == 1
        args, kwargs = send_email_mock.call_args
        email, send_template_id = args
        assert send_template_id == template_id

    @staticmethod
    def create_flow_item(uid, state=InactiveUsersFlowState.new, start_time=None, update_time=None, debug_data=None):
        item = InactiveUsersFlowDAOItem()
        item.uid = uid
        item.state = state
        item.start_time = start_time
        item.update_time = update_time
        item.debug_data = debug_data
        InactiveUsersFlowDAO().bulk_insert([item])
        return item

    def test_success_delete_flow(self):
        self.create_flow_item(self.uid)

        with mocks() as send_email:
            FlowManager.process()
            self.check_send_one_email(send_email, EmailCampaign.block_warning)
            self.check_flow_item(self.uid, InactiveUsersFlowState.block_warning_1)
            assert User(self.uid).is_blocked() == 0

        now_dt = datetime.now()
        with time_machine(now_dt + relativedelta(days=7)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.block_warning)
                self.check_flow_item(self.uid, InactiveUsersFlowState.block_warning_2)
                assert User(self.uid).is_blocked() == 0

        with time_machine(now_dt + relativedelta(days=14)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.blocked)
                self.check_flow_item(self.uid, InactiveUsersFlowState.blocked)
                assert User(self.uid).is_blocked() == 1

        with time_machine(now_dt + relativedelta(days=2 * 30 + 2 * 7)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.deletion_warning)
                self.check_flow_item(self.uid, InactiveUsersFlowState.deletion_warning_1)
                assert User(self.uid).is_blocked() == 1

        with time_machine(now_dt + relativedelta(days=3 * 30 + 7)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.deletion_warning)
                self.check_flow_item(self.uid, InactiveUsersFlowState.deletion_warning_2)
                assert User(self.uid).is_blocked() == 1

        with time_machine(now_dt + relativedelta(days=3 * 30 + 2 * 7)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.deleted)
                self.check_flow_item(self.uid, InactiveUsersFlowState.deleted)
                assert User(self.uid).is_blocked() == 1

    def test_unblock_after_block_state(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        self.create_flow_item(self.uid)

        with mocks() as send_email:
            FlowManager.process()
            self.check_send_one_email(send_email, EmailCampaign.blocked_flow_blocked)
            self.check_flow_item(self.uid, InactiveUsersFlowState.blocked_flow_blocked)
            assert User(self.uid).is_blocked() == 1

        now_dt = datetime.now()
        User(self.uid).set_block(0)
        with time_machine(now_dt + relativedelta(days=60)):
            with mocks():
                FlowManager.process()
                self.check_flow_item(self.uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.not_blocked)
                assert User(self.uid).is_blocked() == 0

    def test_success_blocked_flow(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        self.create_flow_item(self.uid)

        with mocks() as send_email:
            FlowManager.process()
            self.check_send_one_email(send_email, EmailCampaign.blocked_flow_blocked)
            self.check_flow_item(self.uid, InactiveUsersFlowState.blocked_flow_blocked)
            assert User(self.uid).is_blocked() == 1

        now_dt = datetime.now()
        with time_machine(now_dt + relativedelta(days=60)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.deletion_warning)
                self.check_flow_item(self.uid, InactiveUsersFlowState.blocked_flow_deletion_warning_1)
                assert User(self.uid).is_blocked() == 1

        with time_machine(now_dt + relativedelta(days=83)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.deletion_warning)
                self.check_flow_item(self.uid, InactiveUsersFlowState.blocked_flow_deletion_warning_2)
                assert User(self.uid).is_blocked() == 1

        with time_machine(now_dt + relativedelta(days=90)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_send_one_email(send_email, EmailCampaign.deleted)
                self.check_flow_item(self.uid, InactiveUsersFlowState.deleted)
                assert User(self.uid).is_blocked() == 1

    def test_success_no_email_flow(self):
        uid = self.user_2.uid
        self.json_ok('user_init', {'uid': uid})
        self.create_flow_item(uid)

        with mocks() as send_email:
            FlowManager.process()
            self.check_flow_item(uid, InactiveUsersFlowState.no_email_blocked)
            assert User(uid).is_blocked() == 1

        now_dt = datetime.now()
        with time_machine(now_dt + relativedelta(days=60)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_flow_item(uid, InactiveUsersFlowState.no_email_deletion_warning_1)
                assert User(uid).is_blocked() == 1

        with time_machine(now_dt + relativedelta(days=83)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_flow_item(uid, InactiveUsersFlowState.no_email_deletion_warning_2)
                assert User(uid).is_blocked() == 1

        with time_machine(now_dt + relativedelta(days=90)):
            with mocks() as send_email:
                FlowManager.process()
                self.check_flow_item(uid, InactiveUsersFlowState.deleted)
                assert User(uid).is_blocked() == 1

    def test_active_user_excluded(self):
        self.create_flow_item(self.uid)
        with mocks(last_active_dt=datetime.now()):
            FlowManager.process()
            self.check_flow_item(self.uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.active)

    def test_staff_user_excluded(self):
        PassportStub.update_info_by_uid(self.uid, has_staff=True)
        self.create_flow_item(self.uid)
        with mocks():
            FlowManager.process()
            self.check_flow_item(self.uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.has_staff)

    def test_not_exists_user_excluded(self):
        uid = '12344321'
        self.create_flow_item(uid)
        with mocks():
            FlowManager.process()
            self.check_flow_item(uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.user_not_found)

    def test_already_deleted_excluded(self):
        User(self.uid).set_deleted()
        self.create_flow_item(self.uid)
        with mocks():
            FlowManager.process()
            self.check_flow_item(self.uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.already_deleted)

    def test_overdrawn_user_excluded(self):
        resp = self.billing_ok(
            'service_create',
            {
                'uid': self.uid,
                'line': 'partner',
                'pid': 'yandex_disk_for_business',
                'ip': '127.0.0.1',
                'product.amount': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 2
            }
        )
        self.upload_file(
            self.uid, '/disk/1.txt', file_data={'size': USER_OVERDRAFT_RESTRICTIONS_THRESHOLD + 1024 ** 3 * 10 + 1}
        )
        self.billing_ok(
            'service_delete', {'uid': self.uid, 'sid': resp['sid'], 'disable': True, 'ip': '127.0.0.1', 'send_email': 0}
        )
        self.create_flow_item(self.uid)
        with mocks():
            FlowManager.process()
            self.check_flow_item(self.uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.overdrawn)

    def test_not_send_at_time(self):
        self.create_flow_item(self.uid)
        with mocks() as send_email:
            FlowManager.process()
            self.check_send_one_email(send_email, EmailCampaign.block_warning)
            self.check_flow_item(self.uid, InactiveUsersFlowState.block_warning_1)
            assert User(self.uid).is_blocked() == 0

        now_dt = datetime.now()
        with time_machine(now_dt + relativedelta(days=11)):
            with mocks():
                FlowManager.process()
                self.check_flow_item(self.uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.stage_skipped)

    def test_add_handle(self):
        self.service_ok('inactive_users_flow_add', {'uid': self.uid})
        self.check_flow_item(self.uid, InactiveUsersFlowState.new)
        self.service_ok('inactive_users_flow_add', {'uid': self.uid})
        self.check_flow_item(self.uid, InactiveUsersFlowState.new)

    def test_update_handle(self):
        dao = InactiveUsersFlowDAO()
        self.service_ok('inactive_users_flow_add', {'uid': self.uid})
        flow_item = dao.get_by_uid(self.uid)
        assert flow_item.state == InactiveUsersFlowState.new
        assert flow_item.start_time is None

        raw_dt = '2021-01-10 00:00:00'
        self.service_ok('inactive_users_flow_update', {'uid': self.uid, 'state': 'excluded', 'start_time': raw_dt})
        flow_item = dao.get_by_uid(self.uid)
        assert flow_item.state == InactiveUsersFlowState.excluded
        assert flow_item.start_time.strftime('%Y-%m-%d %H:%M:%S') == raw_dt

        self.service_ok('inactive_users_flow_update', {'uid': self.uid, 'state': 'new', 'delta_days': -7})
        flow_item = dao.get_by_uid(self.uid)
        assert flow_item.state == InactiveUsersFlowState.new
        assert flow_item.start_time.strftime('%Y-%m-%d %H:%M:%S') == '2021-01-03 00:00:00'

    def test_add_from_yt(self):
        yt_data = [{'uid': i} for i in range(1, 6)]
        with mock.patch('mpfs.core.inactive_users_flow.interface._fetch_table_from_yt', return_value=yt_data) as stub:
            self.service_ok('inactive_users_flow_add_from_yt', {'table_path': '/home/test', 'start_index': 0, 'end_index': 10})
            args, kwargs = stub.call_args
            assert args[0] == '/home/test'
            assert kwargs == {'start_index': 0, 'end_index': 10}

        for uid in range(1, 6):
            self.check_flow_item(uid, InactiveUsersFlowState.new)

    def test_check_manager_border(self):
        uid = '12344321'
        self.create_flow_item(uid)
        with mocks():
            with QuellerStub(tasks_count=100000):
                FlowManager.process()
                self.check_flow_item(uid, InactiveUsersFlowState.new)
            with QuellerStub(tasks_count=100):
                FlowManager.process()
                self.check_flow_item(uid, InactiveUsersFlowState.excluded, exclude_reason=ExcludeReason.user_not_found)


@mock.patch('mpfs.core.inactive_users_flow.logic.INACTIVE_USERS_FLOW_ENABLED', True)
class InactiveUsersFlowSupportTestCase(DiskTestCase):
    def test_get_last_comment(self):
        user = User(self.uid)
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment 1',
        }
        self.support_ok('block_user', opts)
        comment_obj = get_last_block_comment(user)
        assert comment_obj['comment'] == opts['comment']

        opts['comment'] = 'comment 2'
        self.support_ok('block_user', opts)
        comment_obj = get_last_block_comment(user)
        assert comment_obj['comment'] == opts['comment']

        opts['comment'] = 'comment 3'
        self.support_ok('block_user', opts)
        comment_obj = get_last_block_comment(user)
        assert comment_obj['comment'] == opts['comment']

    def test_is_blocked_by_inactive_flow(self):
        user = User(self.uid)
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment 1',
        }
        self.support_ok('block_user', opts)
        assert is_blocked_by_inactive_flow(user) is False

        opts['comment'] = BLOCK_COMMENT_TEXT
        self.support_ok('block_user', opts)
        assert is_blocked_by_inactive_flow(user) is True

        self.support_ok('unblock_user', opts)
        assert is_blocked_by_inactive_flow(user) is False

    def test_can_be_unblock_inactive_flow(self):
        user = User(self.uid)
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': BLOCK_COMMENT_TEXT,
        }
        self.support_ok('block_user', opts)
        assert is_blocked_by_inactive_flow(user) is True
        assert can_be_unblock_inactive_flow(user) is False
        assert self.support_ok('inactive_users_flow_allow_unblock', {'uid': self.uid})['allowed'] is False

        flow_item = InactiveUsersFlowTestCase.create_flow_item(self.uid)
        assert can_be_unblock_inactive_flow(user) is True
        assert self.support_ok('inactive_users_flow_allow_unblock', {'uid': self.uid})['allowed'] is True

        flow_item.state = InactiveUsersFlowState.deleted
        InactiveUsersFlowDAO().update_by_uid(flow_item)
        assert can_be_unblock_inactive_flow(user) is False
        assert self.support_ok('inactive_users_flow_allow_unblock', {'uid': self.uid})['allowed'] is False

    def test_unblock(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': BLOCK_COMMENT_TEXT,
        }
        self.support_ok('block_user', opts)
        InactiveUsersFlowTestCase.create_flow_item(self.uid)
        user = User(self.uid)

        assert user.is_blocked() == 1
        assert self.support_ok('inactive_users_flow_allow_unblock', {'uid': self.uid})['allowed'] is True
        self.support_ok('inactive_users_flow_unblock', {'uid': self.uid})
        assert user.is_blocked() == 0

        assert self.support_ok('inactive_users_flow_allow_unblock', {'uid': self.uid})['allowed'] is False
        self.support_error('inactive_users_flow_unblock', {'uid': self.uid})
        assert user.is_blocked() == 0
