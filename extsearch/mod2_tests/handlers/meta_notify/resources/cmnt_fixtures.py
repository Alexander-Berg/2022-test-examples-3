import pytest
import json

from extsearch.video.ugc.sqs_moderation.clients.db_client.model import VideoResource
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import CmntDataGetter


@pytest.fixture
def cmnt_data_getter():
    return CmntDataGetter()


@pytest.fixture(scope='session')
def cmnt_data() -> dict:
    return {
        'cmnt_id': 2,
        'with_cmnt': False
    }


@pytest.fixture
def wrong_cmnt_data() -> dict:
    return {'test_2': 2}


@pytest.fixture(scope='session')
def valid_cmnt_res(cmnt_data) -> VideoResource:
    return VideoResource(video_meta_id='1', name='cmnt_data', value=json.dumps(cmnt_data))


@pytest.fixture
def wrong_cmnt_res(wrong_cmnt_data) -> VideoResource:
    return VideoResource(video_meta_id='1', name='cmnt_data', value=json.dumps(wrong_cmnt_data))


@pytest.fixture
def no_data_cmnt_res(wrong_cmnt_data) -> VideoResource:
    return VideoResource(video_meta_id='1', name='cmnt_1', value=json.dumps(wrong_cmnt_data))
