import pytest

from extsearch.video.ugc.sqs_moderation.clients.db_client.api_schema import tags_schema

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import MessageDataExtender
from .utills import load_data


@pytest.fixture(scope='session')
def tags():
    return load_data('resources/video_data/tags.json', tags_schema)


@pytest.fixture(scope='session')
def data_extender(notify_configs, session, db_client):
    return MessageDataExtender(
        notify_configs=notify_configs,
        session=session,
        db_client=db_client
    )
