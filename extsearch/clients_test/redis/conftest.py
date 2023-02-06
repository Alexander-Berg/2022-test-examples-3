import fakeredis
import pytest

from extsearch.video.ugc.sqs_moderation.clients.redis.notification_status import NotificationClient
from extsearch.video.ugc.sqs_moderation.clients.db_client import TranscoderStatus


@pytest.fixture
def redis():
    client = fakeredis.FakeRedis()
    return client


@pytest.fixture
def prepared_redis(redis, done_task, transcoding_task, new_task):
    client = NotificationClient(redis)
    client.update_notification_status(new_task, TranscoderStatus.Empty)
    client.update_notification_status(done_task, TranscoderStatus.ETSTranscoding)
    client.update_notification_status(done_task, TranscoderStatus.ETSDone)
    client.update_notification_status(transcoding_task, TranscoderStatus.ETSTranscoding)
    return redis


@pytest.fixture
def new_task() -> str:
    return 'e302fcdf-bfe8-11ec-9f63-9cb6d0b904c9'


@pytest.fixture
def done_task() -> str:
    return '1b864ffc-bfe9-11ec-bd0b-9cb6d0b904c9'


@pytest.fixture
def transcoding_task() -> str:
    return '2f228d38-bfe9-11ec-a368-9cb6d0b904c9'


@pytest.fixture
def notification_client(prepared_redis):
    return NotificationClient(prepared_redis)
