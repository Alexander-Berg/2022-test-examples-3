import pytest
from unittest import mock

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import (
    VideoEffectsMaker
)
from extsearch.video.ugc.sqs_moderation.clients.db_client.api_schema import (
    spell_templates_schema, instanced_spells_schema
)

from .utills import load_data


@pytest.fixture(scope='session')
def effect_getter(db_client):
    return VideoEffectsMaker(db_client)


@pytest.fixture(scope='session')
def valid_meta_id(video_meta_id):
    return video_meta_id


@pytest.fixture(scope='session')
def effect_mock_meta(valid_meta_id):
    mock_meta = mock.Mock()
    mock_meta.id = valid_meta_id
    return mock_meta


@pytest.fixture
def effect_mock_channel():
    channel_id = '3137396078705328615'
    mock_channel = mock.Mock()
    mock_channel.id = channel_id
    return mock_channel


@pytest.fixture(scope='session')
def channel_spells():
    return load_data('resources/spells/channel_spells.json', instanced_spells_schema)


@pytest.fixture(scope='session')
def meta_spells():
    return load_data('resources/spells/meta_spells.json', instanced_spells_schema)


@pytest.fixture(scope='session')
def spell_templates():
    return load_data('resources/spells/spell_templates.json', spell_templates_schema)
