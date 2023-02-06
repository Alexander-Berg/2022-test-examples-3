import io

import pytest

import asyncio

from sendr_utils import temp_set

from mail.ipa.ipa.core.actions.report import WriteCSVReportAction


class AsyncBytesIO:
    def __init__(self, buf: io.BytesIO):
        self.buf = buf

    async def write(self, chunk: bytes) -> None:
        self.buf.write(chunk)


class StorageSetterResultMock:
    def __aenter__(self):
        return self._do_nothing_task()

    def __aexit__(self, exc_type, exc_val, exc_tb):
        return self._do_nothing_task()

    @staticmethod
    def _do_nothing_task():
        return asyncio.create_task(asyncio.sleep(0))


class TestWriteCSVReportAction:
    @pytest.fixture(autouse=True)
    def setup(self, setup_report_data):
        pass

    @pytest.fixture
    def buf(self):
        return io.BytesIO()

    @pytest.fixture
    def returned_func(self, org_id, buf):
        async def _inner():
            return await WriteCSVReportAction(org_id=org_id, output=AsyncBytesIO(buf)).run()

        return _inner

    def test_csv(self, expected_report_response, buf, returned):
        buf.seek(0)
        assert buf.read() == expected_report_response.encode('utf-8')

    @pytest.mark.asyncio
    async def test_batch_size(self, mocker, randn, storage, returned_func, org_id):
        spy = mocker.spy(storage.user, 'get_all_with_collectors')
        mocker.patch('mail.ipa.ipa.core.actions.report.WriteCSVReportAction.storage_setter',
                     mocker.Mock(return_value=StorageSetterResultMock()))
        batch_size = randn()
        with temp_set(WriteCSVReportAction.context, 'storage', storage), \
             temp_set(WriteCSVReportAction, 'BATCH_SIZE', batch_size):
            await returned_func()
        spy.assert_called_once_with(org_id=org_id, batch_size=batch_size, only_errors=True)
