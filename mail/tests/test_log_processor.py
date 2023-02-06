import pytest
from stats.logbrocker_workaround.clickhouse.processor import LogProcessor


@pytest.fixture
def loger(mocker):
    class MockLogger:
        def debug(self, msg, *args, **kwargs):
            pass

    return mocker.patch(
        "stats.logbrocker_workaround.clickhouse.processor.loger",
        MockLogger,
    )


@pytest.fixture
def pusher(loger):
    class MockPusher:
        def __init__(self):
            self.buffer = []

        def push(self, records):
            if not records:
                return
            tskv_gen = (rec.tskv for rec in records)
            self.buffer += tskv_gen

    pusher = LogProcessor()
    pusher._pusher = MockPusher()
    return pusher


@pytest.fixture
def pusher_buffer():
    return [
        "tskv\tdate=2022-07-19\trecipient=a@b.ru\tevent=pixel\tcampaign=123\tuser_ip=127.0.0.1\n",
        "tskv\tdate=2022-07-19\trecipient=a@b.ru\tevent=pixel\tcampaign=123\tuser_ip=127.0.0.1\n",
        "tskv\tdate=2022-07-19\trecipient=a@b.ru\tevent=pixel\tcampaign=123\tuser_ip=127.0.0.1\n",
        "tskv\tdate=2022-07-19\trecipient=a@b.ru\tevent=pixel\tcampaign=122\tuser_ip=127.0.0.1\n",
    ]


def test_process_lines(pusher, fan_feedback_loglines):
    assert pusher.process_lines(fan_feedback_loglines) == 4


def test_last_processed_datetime(pusher, fan_feedback_loglines):
    pusher.process_lines(fan_feedback_loglines)
    assert pusher.last_processed_datetime() == "2022-07-19 00:00:00.000004"


def test_flush(pusher, fan_feedback_loglines, pusher_buffer):
    pusher.process_lines(fan_feedback_loglines)
    pusher.flush()
    assert pusher._pusher.buffer == pusher_buffer
