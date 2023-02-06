import asyncio

import pytest

from hamcrest import assert_that, has_entries, has_items

from mail.payments.payments.taskq.workers.moderation import FastModerationProcessor, ModerationReader, ModerationWriter
from mail.payments.payments.tests.utils import dummy_async_generator, dummy_coro


class BaseTestModerationReader:
    @pytest.fixture
    def worker_cls(self):
        raise NotImplementedError

    @pytest.fixture
    def action_cls(self):
        raise NotImplementedError

    @pytest.fixture
    def consumer_path(self):
        raise NotImplementedError

    @pytest.fixture
    async def worker(self, moderation_app, worker_cls, test_logger):
        worker = worker_cls(logger=test_logger)
        worker._pause_period = 1
        await worker.initialize_worker(moderation_app)
        yield worker
        worker.heartbeat_task.cancel()

    @pytest.fixture(autouse=True)
    def action_mock(self, action_cls, mock_action):
        return mock_action(action_cls)

    @pytest.fixture
    def consumer_read_results(self):
        return [1, 2, 3]

    @pytest.fixture(autouse=True)
    def consumer_mock(self, mocker, consumer_path, consumer_read_results):
        mock = mocker.Mock()
        mock.run.return_value = run_coro = dummy_coro()
        mock.close.return_value = dummy_coro()
        mock.read = dummy_async_generator(consumer_read_results)
        mocker.patch(
            consumer_path,
            mocker.Mock(return_value=mock)
        )
        yield mock
        run_coro.close()
        mock.close.return_value.close()

    @pytest.mark.asyncio
    async def test_worker_starts_consumer(self, consumer_mock, worker):
        await worker.process_task()
        consumer_mock.run.assert_called_once()

    @pytest.mark.asyncio
    async def test_worker_closes_consumer(self, consumer_mock, worker):
        await worker.process_task()
        consumer_mock.close.assert_called_once()

    @pytest.mark.asyncio
    async def test_worker_restarts(self, consumer_mock, moderation_app, worker):
        run_calls = 0
        expected_calls = 3

        async def failing_run(*args):
            nonlocal run_calls
            run_calls += 1
            if run_calls >= expected_calls:
                raise asyncio.CancelledError
            raise ValueError

        consumer_mock.run = failing_run
        await worker(moderation_app)
        assert run_calls == expected_calls


class TestModerationReader(BaseTestModerationReader):
    @pytest.fixture
    def worker_cls(self):
        return ModerationReader

    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.worker.moderation import UpdateModerationAction
        return UpdateModerationAction

    @pytest.fixture
    def consumer_path(self):
        return 'mail.payments.payments.taskq.workers.moderation.ModerationConsumer'

    @pytest.mark.asyncio
    async def test_reader_calls_action(self, worker, action_mock, consumer_read_results):
        await worker.process_task()
        assert_that(
            [call[1] for call in action_mock.call_args_list],
            has_items(*[
                has_entries({'moderation_result': moderation_result})
                for moderation_result in consumer_read_results
            ]),
        )


class TestFastModerationProcessor(BaseTestModerationReader):
    @pytest.fixture
    def worker_cls(self):
        return FastModerationProcessor

    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.worker.moderation import ProcessFastModerationRequestAction
        return ProcessFastModerationRequestAction

    @pytest.fixture
    def consumer_path(self):
        return 'mail.payments.payments.taskq.workers.moderation.FastModerationRequestConsumer'

    @pytest.mark.asyncio
    async def test_processor_calls_action(self, worker, action_mock, consumer_read_results):
        await worker.process_task()
        assert_that(
            [call[1] for call in action_mock.call_args_list],
            has_items(*[
                has_entries({'request': request})
                for request in consumer_read_results
            ]),
        )


class TestModerationWriter:
    @pytest.fixture
    async def moderation_writer(self, moderation_app, test_logger):
        writer = ModerationWriter(logger=test_logger)
        await writer.initialize_worker(moderation_app)
        yield writer
        writer.heartbeat_task.cancel()
