from collections import namedtuple

import pytest
import asynctest
import mock
from psycopg2 import IntegrityError
from psycopg2.errorcodes import UNIQUE_VIOLATION

from mail.husky.stages.worker.interactions.db.huskydb import HuskydbTransfer, retry_as_failed
from mail.husky.husky.types import Errors, ResultData
from mail.python.tvm_requests import Tvm

parametrize = pytest.mark.parametrize

Args = namedtuple('Args', (
    'huskydb',
    'blackbox',
    'mailhost',
    'max_tries',
    'fail_retry_wait_progression',
    'tvm',
    'bb_tvm_id',
))


def make_args(
        huskydb='huskydb dsn',
        blackbox='test://blackbox',
        mailhost='test://mailhost',
        max_tries={'default': 3, 'transfer': 5},
        fail_retry_wait_progression=[],
        tvm=Tvm(
            tvm_daemon_url='tvm_daemon_url',
            client_id='client_id',
            local_token=None,
        ),
        bb_tvm_id=None,
):
    return Args(**locals())


class FakeIntegrityError(IntegrityError):
    def __init__(self, fake_pgcode):
        self.fake_pgcode = fake_pgcode

    @property
    def pgcode(self):
        return self.fake_pgcode


class TestRetries(asynctest.TestCase):
    async def test_retry_as_failed_call_once_on_ok(self):
        mocked_func = asynctest.CoroutineMock()
        decorated = retry_as_failed(mocked_func)

        await decorated(42, foo='bar')
        mocked_func.assert_called_once_with(42, mark_failed=False, foo='bar')

    async def test_retry_as_failed_call_twice_on_uqinue_violation(self):
        mocked_func = asynctest.CoroutineMock()
        mocked_func.side_effect = [FakeIntegrityError(UNIQUE_VIOLATION), None]
        decorated = retry_as_failed(mocked_func)

        await decorated(42, foo='bar')
        assert mocked_func.call_args_list == [
            mock.call(42, mark_failed=False, foo='bar'),
            mock.call(42, mark_failed=True, foo='bar')
        ]


def make_huskydb_transfer(*mocks):
    return HuskydbTransfer(
        app_args=make_args(),
        shard_id=111,
        pg=None,
        husky_cluster='husky_worker'
    )

TRANSFER_ID = 42
ERR_MSG = 'The error has come!'
TASK_OUTPUT = dict(result=42)


class TestHuskydbTransfer(asynctest.TestCase):
    @asynctest.patch.object(HuskydbTransfer, '_mark_task_as_completed')
    async def test_update_user_status_without_error(self, mocked_mark_task_as_completed):
        mocked_mark_task_as_completed.return_value = ('completed', 1)

        huskydb = make_huskydb_transfer()
        await huskydb.update_user_status(
            ResultData(
                transfer_id=TRANSFER_ID,
                error=Errors.NoError,
                error_message=None,
                task_output=None)
        )

        mocked_mark_task_as_completed.assert_called_once_with(
            transfer_id=TRANSFER_ID,
            task_output=None,
        )

    @asynctest.patch.object(HuskydbTransfer, '_mark_task_as_failed')
    async def test_update_user_status_with_non_retryable_error(self, mocked_mark_task_as_failed):
        mocked_mark_task_as_failed.return_value = ('error', 1)

        huskydb = make_huskydb_transfer()
        await huskydb.update_user_status(
            ResultData(
                transfer_id=TRANSFER_ID,
                error=Errors.DeferredDueToMessageCountLimit,
                error_message=ERR_MSG,
                task_output=None)
        )

        mocked_mark_task_as_failed.assert_called_once_with(
            transfer_id=TRANSFER_ID,
            error=Errors.DeferredDueToMessageCountLimit,
            error_message=ERR_MSG,
            task_output=None,
        )

    @asynctest.patch.object(HuskydbTransfer, '_mark_task_as_failed')
    @asynctest.patch.object(HuskydbTransfer, '_get_task_info')
    async def test_update_user_status_with_retryable_error_and_exceeded_retries(
        self,
        mocked_get_task_info,
        mocked_mark_task_as_failed,
    ):
        mocked_get_task_info.return_value = ('transfer', 4)
        mocked_mark_task_as_failed.return_value = ('error', 5)

        huskydb = make_huskydb_transfer()
        await huskydb.update_user_status(
            ResultData(
                transfer_id=TRANSFER_ID,
                error=Errors.Unknown,
                error_message=ERR_MSG,
                task_output=None)
        )

        mocked_get_task_info.assert_called_once_with(
            transfer_id=TRANSFER_ID,
        )

        mocked_mark_task_as_failed.assert_called_once_with(
            transfer_id=TRANSFER_ID,
            error=Errors.Unknown,
            error_message=ERR_MSG,
            task_output=None,
        )

    @asynctest.patch.object(HuskydbTransfer, '_mark_task_as_to_be_retried')
    @asynctest.patch.object(HuskydbTransfer, '_get_task_info')
    async def test_update_user_status_with_retryable_error(
        self,
        mocked_get_task_info,
        mocked_mark_task_as_to_be_retried,
    ):
        mocked_get_task_info.return_value = ('transfer', 3)
        mocked_mark_task_as_to_be_retried.return_value = ('error', 4)

        huskydb = make_huskydb_transfer()
        await huskydb.update_user_status(
            ResultData(
                transfer_id=TRANSFER_ID,
                error=Errors.Unknown,
                error_message=ERR_MSG,
                task_output=None)
        )

        mocked_get_task_info.assert_called_once_with(
            transfer_id=TRANSFER_ID,
        )

        mocked_mark_task_as_to_be_retried.assert_called_once_with(
            transfer_id=TRANSFER_ID,
            delay=huskydb.tries_delays[4],
            error_message=ERR_MSG,
            task_output=None,
        )
