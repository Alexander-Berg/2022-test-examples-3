import pytest
import mock

from mail.husky.husky.tasks.errors import MissingArgumentError, NotSupportedError
from mail.husky.husky.husky_subproc import Worker, error_result, success_result
from mail.husky.husky.types import UserData, Errors

from collections import namedtuple

parametrize = pytest.mark.parametrize


class FakeTransferApp(object):
    def __init__(self):
        self.args = namedtuple('Args', [])()

    def set_context(self, user):
        pass

    def unset_context(self):
        pass


class TestWorker(object):
    APP = FakeTransferApp()
    TRANSFER_ID = 42
    WORKER_ID = 666
    UID = 100

    def setup(self):
        self.worker = Worker(
            app=self.APP,
            worker_id=self.WORKER_ID,
            data_q=None,
            resp_q=None
        )

    def make_userdata(self):
        return UserData(self.TRANSFER_ID, self.UID, 'task', {})

    def call_dispatch(self):
        return self.worker.dispatch(
            self.TRANSFER_ID,
            self.make_userdata(),
        )

    @mock.patch('mail.husky.husky.husky_subproc.get_handler')
    def test_tasks_wrong_args_error_on_empty_args(self, mocked_get_handlers):
        mocked_get_handlers.side_effect = MissingArgumentError
        retval = self.call_dispatch()
        assert retval.error == Errors.WrongArgs

    @mock.patch('mail.husky.husky.husky_subproc.get_handler')
    def test_dispatch_not_supported_on_unexisted_task(self, mocked_get_handlers):
        mocked_get_handlers.side_effect = NotSupportedError
        retval = self.call_dispatch()
        assert retval.error == Errors.NotSupported

    @mock.patch('mail.husky.husky.husky_subproc.get_handler')
    def test_dispatch_task(self, mocked_get_handlers):
        mocked_run = mocked_get_handlers.return_value.return_value.run
        mocked_run.return_value = None
        retval = self.call_dispatch()
        assert mocked_get_handlers.called
        assert mocked_run.called
        assert retval == success_result(self.TRANSFER_ID)

    @mock.patch.object(Worker, 'dispatch')
    def test_process_ok(self, mocked_dispatch):
        with \
            mock.patch.object(self.worker, 'data_q', spec=['get']) as mocked_data_q, \
            mock.patch.object(self.worker, 'resp_q', spec=['put']) as mocked_resp_q \
        :
            mocked_data_q.get.return_value = self.make_userdata()

            self.worker.process()

            mocked_data_q.get.assert_called_once_with()
            mocked_resp_q.put.assert_called_once_with(
                mocked_dispatch.return_value
            )

    @mock.patch('mail.husky.husky.husky_subproc.error_info_from_tb')
    @mock.patch.object(Worker, 'dispatch')
    def test_process_error(self, mocked_dispatch, mocked_error_info):
        with \
            mock.patch.object(self.worker, 'data_q', spec=['get']) as mocked_data_q, \
            mock.patch.object(self.worker, 'resp_q', spec=['put']) as mocked_resp_q \
        :
            mocked_data_q.get.return_value = self.make_userdata()
            mocked_dispatch.side_effect = Exception
            mocked_error_info.side_effect = ['Fancy Error Text']

            self.worker.process()

            mocked_resp_q.put.assert_called_once_with(
                error_result(self.TRANSFER_ID, err_msg='Fancy Error Text')
            )
