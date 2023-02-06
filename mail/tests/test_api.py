from collections import namedtuple
from contextlib import contextmanager
import json
from freezegun import freeze_time
from datetime import timedelta, datetime
from pytz import timezone, utc

import mock
import pytest
import psycopg2
from mail.pypg.pypg.query_handler import ExpectOneItemError
from mail.husky.husky.api import HuskyApiApp
from mail.husky.husky.api_queries import CloneAudit, TaskInfo
from mail.husky.husky.types import Task, Status
from mail.husky.husky.web_tools import jdumps


TEST_SOURCE_UID = 100
TEST_DEST_UID = 200
CLONE_USER_DEST_SHARD = 17
CLONE_USER_PRIORITY = 500
DELETE_USER_UID = 300
DELETE_MAIL_USER_PRIORITY = 1000
DELETE_SHARDS_USER_PRIORITY = 1500
SHARPEI = 'test://sharpei'
SHARDDB_DSN_SUFFIX = 'user=sharpei'
SHARDDB_DSN = 'host=master.sharddb.net port=6432 dbname=sharddb user=sharpei'
DELETE_USER_SHARD_ID = '18'
DELETE_USER_SHIFT = timedelta(days=1)
DELETE_USER_TZ = timezone('Europe/Moscow')
DELETE_USER_NOW = datetime(year=2016, month=10, day=10)


def make_clone_audit(complete=False):
    return CloneAudit(
        source_uid=TEST_SOURCE_UID,
        dest_uid=TEST_DEST_UID,
        transfer_id=100,
        request_info={},
        complete=complete,
        complete_at=None,
    )


def make_task_info(status='pending'):
    return TaskInfo(
        transfer_id=100,
        uid=TEST_DEST_UID,
        task='clone_user',
        task_args={},
        status=status,
        error_type=None,
        try_notices=[],
        tries=0,
        last_update=None,
        priority=0,
    )


@contextmanager
def mocked_queries():
    with mock.patch.object(HuskyApiApp, '_connection'), mock.patch('mail.husky.husky.api.ApiQueries') as ApiQueriesMock:
        yield ApiQueriesMock()


def make_app():
    return HuskyApiApp(
        mock.Mock(),
        dict(
            clone_user_dest_shard=CLONE_USER_DEST_SHARD,
            clone_user_priority=CLONE_USER_PRIORITY,
            delete_mail_user_priority=DELETE_MAIL_USER_PRIORITY,
            delete_shards_user_priority=DELETE_SHARDS_USER_PRIORITY,
            delete_user_shift=DELETE_USER_SHIFT,
        ),
    )


def make_request():
    request = mock.Mock()
    request.data = jdumps({}).encode('utf-8')
    request.user_agent.string = 'Test'
    return request


def load_response_and_status_code(response):
    return json.loads(response.response[0]), response.status_code


class UsingSharddbAdaptorMixin:
    def setup_method(self, _):
        self.app = make_app()
        self.app.app.args.sharpei = (
            SHARPEI
        )
        self.app.app.args.sharddb_dsn_suffix = (
            SHARDDB_DSN_SUFFIX
        )


class MocksMixin:
    Mocks = namedtuple(
        'Mocks',
        (
            'get_sharddb_master_host',
            'get_user_shard_id',
            'get_localzone',
            'api_queries',
        )
    )

    @classmethod
    @contextmanager
    def mocks(cls):
        with \
            mock.patch('ora2pg.tools.find_master_helpers.get_sharddb_master_host') as mock_find_master, \
            mock.patch('mail.husky.husky.api.SharddbAdaptor.get_user_shard_id') as mock_get_shard_id, \
            mock.patch('mail.husky.husky.api.get_localzone') as mock_get_localzone, \
            mocked_queries() as mock_queries \
        :
            yield cls.prepare_mocks(
                cls.Mocks(mock_find_master, mock_get_shard_id, mock_get_localzone, mock_queries)
            )

    @classmethod
    def prepare_mocks(cls, mocks):
        mocks.get_sharddb_master_host.return_value = SHARDDB_DSN
        return mocks


class TestAddClone(MocksMixin, UsingSharddbAdaptorMixin):
    def make_request(self):
        return load_response_and_status_code(
            self.app.on_add_clone_user_task(
                request=make_request(),
                source_uid=TEST_SOURCE_UID,
                dest_uid=TEST_DEST_UID,
            ))

    @pytest.mark.parametrize('with_deleted_box', [False, True])
    def test_return_ok(self, with_deleted_box):
        with self.mocks() as mocks:
            mq = mocks.api_queries
            mq.get_clone_audit.side_effect = ExpectOneItemError
            mq.add_task.side_effect = [make_task_info()]

            response, status_code = load_response_and_status_code(
                self.app.on_add_clone_user_task(
                    request=make_request(),
                    source_uid=TEST_SOURCE_UID,
                    dest_uid=TEST_DEST_UID,
                    with_deleted_box=with_deleted_box
                ))

            assert response['status'] == 'ok'
            mq.add_task.assert_called_once_with(
                uid=TEST_DEST_UID,
                task='clone_user',
                task_args=json.dumps(dict(
                    source_user_uid=TEST_SOURCE_UID,
                    dest_shard=CLONE_USER_DEST_SHARD,
                    restore_deleted_box=with_deleted_box,
                )),
                priority=CLONE_USER_PRIORITY,
                shard_id=17,
                status='pending',
            )
            assert status_code == 200

    def test_return_already_exists_when_try_add_same(self):
        with self.mocks() as mocks:
            mq = mocks.api_queries
            mq.get_clone_audit.side_effect = [
                make_clone_audit()
            ]
            mq.get_task.side_effect = [
                make_task_info()
            ]

            response, status_code = self.make_request()

            assert response['status'] == 'already_exists'
            assert status_code == 200

    def test_return_already_exists_when_try_add_same_but_task_queue_was_empty(self):
        with self.mocks() as mocks:
            mq = mocks.api_queries
            mq.get_clone_audit.side_effect = [
                make_clone_audit()
            ]
            mq.get_task.side_effect = [
                ExpectOneItemError('1')
            ]

            response, status_code = self.make_request()

            assert response['status'] == 'already_exists'
            assert status_code == 200


class TestGetCloneStatus(object):
    @staticmethod
    def make_request():
        response = make_app().on_clone_user_status(
            request=make_request(),
            source_uid=TEST_SOURCE_UID,
            dest_uid=TEST_DEST_UID,
        )
        return load_response_and_status_code(response)

    def test_return_not_found_when_there_is_no_audit(self):
        with mocked_queries() as mq:
            mq.get_clone_audit.side_effect = ExpectOneItemError

            response, status_code = self.make_request()

            assert response['status'] == 'not_found'
            assert status_code == 200

    def test_return_task_status_when_task_exists(self):
        with mocked_queries() as mq:
            mq.get_clone_audit.side_effect = [make_clone_audit()]
            mq.get_task.side_effect = [make_task_info('in_progress')]

            response, status_code = self.make_request()

            assert response['status'] == 'in_progress'
            assert status_code == 200

    def test_return_complete_when_there_is_no_task_and_audit_is_complete(self):
        with mocked_queries() as mq:
            mq.get_clone_audit.side_effect = [make_clone_audit(True)]
            mq.get_task.side_effect = ExpectOneItemError

            response, status_code = self.make_request()

            assert response['status'] == 'complete'
            assert status_code == 200

    def test_return_error_when_there_is_no_task_and_audit_is_not_complete(self):
        with mocked_queries() as mq:
            mq.get_clone_audit.side_effect = [make_clone_audit(False)]
            mq.get_task.side_effect = ExpectOneItemError

            response, status_code = self.make_request()

            assert response['status'] == 'error'
            assert status_code == 200


class TestDeleteUser(MocksMixin, UsingSharddbAdaptorMixin):
    @classmethod
    def prepare_mocks(cls, mocks):
        mocks = super().prepare_mocks(mocks)
        mocks.get_user_shard_id.return_value = DELETE_USER_SHARD_ID
        mocks.get_localzone.return_value = DELETE_USER_TZ
        mocks.api_queries.get_task_by_user_and_type.side_effect = [
            list(),
            list(),
        ]
        mocks.api_queries.add_task.side_effect = [
            TestDeleteUser.make_task_info(Task.DeleteMailUser),
            TestDeleteUser.make_task_info(Task.DeleteShardsUser),
        ]
        return mocks

    app = None

    @freeze_time(DELETE_USER_NOW)
    def test_call_should_return_status_ok(self):
        with self.mocks():
            result = self.app.on_delete_user(
                request=make_request(),
                uid=DELETE_USER_UID,
            )
            assert json.loads(result.response[0])['status'] == 'ok'
            assert result.status_code == 200

    @freeze_time(DELETE_USER_NOW)
    def test_call_should_call_get_sharddb_master_host(self):
        with self.mocks() as mocks:
            self.app.on_delete_user(
                request=make_request(),
                uid=DELETE_USER_UID,
            )
            mocks.get_sharddb_master_host.assert_called_once_with(SHARPEI, SHARDDB_DSN_SUFFIX)

    @freeze_time(DELETE_USER_NOW)
    def test_call_should_call_get_user_shard_id(self):
        with self.mocks() as mocks:
            self.app.on_delete_user(
                request=make_request(),
                uid=DELETE_USER_UID,
            )
            mocks.get_user_shard_id.assert_called_once_with(DELETE_USER_UID)

    @freeze_time(DELETE_USER_NOW)
    def test_call_should_call_get_localzone(self):
        with self.mocks() as mocks:
            self.app.on_delete_user(
                request=make_request(),
                uid=DELETE_USER_UID,
            )
            mocks.get_localzone.assert_called_once_with()

    @freeze_time(DELETE_USER_NOW)
    @pytest.mark.parametrize('right_now, deleted_date', [
        (False, (utc.localize(DELETE_USER_NOW).astimezone(DELETE_USER_TZ) + DELETE_USER_SHIFT).isoformat()),
        (True, (datetime.fromtimestamp(0, DELETE_USER_TZ)).isoformat()),
    ])
    def test_call_should_call_add_task_twice(self, right_now, deleted_date):
        with self.mocks() as mocks:
            self.app.on_delete_user(
                request=make_request(),
                uid=DELETE_USER_UID,
                right_now=right_now,
            )
            mocks.api_queries.add_task.assert_has_calls([
                mock.call(
                    uid=DELETE_USER_UID,
                    task=Task.DeleteShardsUser,
                    status=Status.Pending,
                    priority=DELETE_SHARDS_USER_PRIORITY,
                    task_args=json.dumps(dict()),
                    shard_id=DELETE_USER_SHARD_ID,
                ),
                mock.call(
                    uid=DELETE_USER_UID,
                    task=Task.DeleteMailUser,
                    status=Status.Pending,
                    priority=DELETE_MAIL_USER_PRIORITY,
                    task_args=json.dumps(dict(
                        deleted_date=deleted_date,
                    )),
                    shard_id=DELETE_USER_SHARD_ID,
                ),
            ])

    @staticmethod
    def make_task_info(task):
        return TaskInfo(
            transfer_id=100,
            uid=DELETE_USER_UID,
            task=task,
            task_args={},
            status=Status.Pending,
            error_type=None,
            try_notices=[],
            tries=0,
            last_update=None,
            priority=0,
        )


class TestHuskyDb(object):
    @staticmethod
    def make_request():
        return make_app().on_pingdb(request=make_request())

    @contextmanager
    def fake_connect(*args, **kwargs):
        raise psycopg2.OperationalError

    @contextmanager
    def not_fake_connect(*args, **kwargs):
        yield []

    def query_execute(*args, **kwargs):
        pass

    def test_return_status_code_500_when_huskydb_is_not_operational(self):
        with mock.patch.object(HuskyApiApp, '_connection') as mock_connect:
            mock_connect.side_effect = self.fake_connect
            response = self.make_request()
            assert response.status_code == 500

    def test_return_status_code_200_when_huskydb_is_operational(self):
        with \
            mock.patch.object(HuskyApiApp, '_connection') as mock_connect, \
            mock.patch.object(HuskyApiApp, '_ping_query') as mock_query \
        :
            mock_connect.side_effect = self.not_fake_connect
            mock_query.side_effect = self.query_execute
            response = self.make_request()
            assert response.status_code == 200


class TestAddTask(MocksMixin, UsingSharddbAdaptorMixin):
    @staticmethod
    def make_clone_request(restore_deleted_box):
        request = make_request()
        request.data = json.dumps({
            "uid" : TEST_DEST_UID,
            "task" : Task.CloneUser,
            "task_args" : {
                "source_user_uid" : TEST_SOURCE_UID,
                "dest_shard" : CLONE_USER_DEST_SHARD,
                "restore_deleted_box" : restore_deleted_box,
                'force': True,
            }
        }).encode("utf-8")
        return request

    @pytest.mark.parametrize('with_deleted_box', [False, True])
    def test_clone_user_using_add_task(self, with_deleted_box):
        with self.mocks() as mocks:
            mq = mocks.api_queries
            mq.get_clone_audit.side_effect = ExpectOneItemError
            mq.add_task.side_effect = [make_task_info()]

            response, status_code = load_response_and_status_code(
                self.app.on_add_task(
                    request=TestAddTask.make_clone_request(with_deleted_box),
                ))
            assert response['status'] == 'ok'
            mq.add_task.assert_called_once_with(
                uid=TEST_DEST_UID,
                task=Task.CloneUser,
                task_args=json.dumps(dict(
                    source_user_uid=TEST_SOURCE_UID,
                    dest_shard=CLONE_USER_DEST_SHARD,
                    restore_deleted_box=with_deleted_box,
                    force=True,
                )),
                priority=CLONE_USER_PRIORITY,
                shard_id=17,
                status='pending',
            )
            assert status_code == 200

    @staticmethod
    def make_unknown_task_type_request():
        request = make_request()
        request.data = json.dumps({
            "uid": TEST_DEST_UID,
            "task": "unknown_task_type",
            "task_args": {
                "arg1": "val1",
                "arg2": True,
                "arg3": 3,
            }
        }).encode('utf-8')
        return request

    def test_add_unknown_task(self):
        with self.mocks() as mocks:
            mq = mocks.api_queries
            mq.add_task.side_effect = [BaseException]

            response, status_code = load_response_and_status_code(
                self.app.on_add_task(
                    request=TestAddTask.make_unknown_task_type_request(),
                ))
            assert response['status'] == 'error'
            assert response['error'] == 'unknown task type'
            mq.add_task.assert_not_called()
            assert status_code == 200
