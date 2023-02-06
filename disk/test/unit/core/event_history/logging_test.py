import logging
import mock

from test.unit.base import NoDBTestCase

from mpfs.core.event_history.logger import CatchHistoryLogging, log_raw_event_message


class LoggingTestCase(NoDBTestCase):
    MESSAGE = 'test'

    def test_disable_logging_and_catching(self):
        with CatchHistoryLogging(disable_logging=True, catch_messages=False) as catcher:
            with mock.patch.object(logging.Logger, 'info') as logger_mock:
                log_raw_event_message(self.MESSAGE)
                logger_mock.assert_not_called()
                assert len(catcher.get_messages()) == 0

    def test_enable_logging_and_catching(self):
        with CatchHistoryLogging(disable_logging=False, catch_messages=True) as catcher:
            with mock.patch.object(logging.Logger, 'info') as logger_mock:
                log_raw_event_message(self.MESSAGE)
                logger_mock.assert_called_once_with(self.MESSAGE)
                msgs = catcher.get_messages()
                assert len(msgs) == 1
                assert msgs[0] == self.MESSAGE

    def test_enabled_logging_and_disable_catching(self):
        with CatchHistoryLogging(catch_messages=False) as catcher:
            with mock.patch.object(logging.Logger, 'info') as logger_mock:
                log_raw_event_message(self.MESSAGE)
                logger_mock.assert_called_once_with(self.MESSAGE)
                msgs = catcher.get_messages()
                assert len(msgs) == 0

    def test_disbale_logging_and_enable_catching(self):
        with CatchHistoryLogging(disable_logging=True, catch_messages=True) as catcher:
            with mock.patch.object(logging.Logger, 'info') as logger_mock:
                log_raw_event_message(self.MESSAGE)
                logger_mock.assert_not_called()
                msgs = catcher.get_messages()
                assert len(msgs) == 1
                assert msgs[0] == self.MESSAGE

    def test_default_behaviour(self):
        with mock.patch.object(logging.Logger, 'info') as logger_mock:
            log_raw_event_message(self.MESSAGE)
            logger_mock.assert_called_once_with(self.MESSAGE)
            msgs = CatchHistoryLogging.get_messages()
            assert len(msgs) == 0

            with CatchHistoryLogging() as catcher:
                    log_raw_event_message(self.MESSAGE)
                    assert len(logger_mock.mock_calls) == 2
                    msgs = catcher.get_messages()
                    assert len(msgs) == 0

    def test_catching_cleanup(self):
        with CatchHistoryLogging(catch_messages=True) as catcher:
            log_raw_event_message(self.MESSAGE)
            msgs = catcher.get_messages()
            assert len(msgs) == 1
            assert msgs[0] == self.MESSAGE
        assert len(CatchHistoryLogging.get_messages()) == 0
