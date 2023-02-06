import pytest
import json

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import (
    ThumbInfoMaker
)

from .utills import source_path


@pytest.fixture(scope='session')
def thumb_key() -> str:
    return '2a0000017491b5e761a53010b8e1e9283a31'


@pytest.fixture(scope='session')
def thumb_key_404():
    return '2a0000017491b5e761a53010b404404404'


@pytest.fixture(scope='session')
def thumb_group_id() -> int:
    return 4072575


@pytest.fixture(scope='session')
def thumb_url(thumb_group_id, thumb_key) -> str:
    return f'https://avatars.mds.yandex.net/get-vh/{thumb_group_id}/{thumb_key}/orig'


@pytest.fixture(scope='session')
def thumb_url_404(thumb_group_id, thumb_key_404) -> str:
    return f'https://avatars.mds.yandex.net/get-vh/{thumb_group_id}/{thumb_key_404}/orig'


@pytest.fixture(scope='session')
def thumb_info_maker(session):
    return ThumbInfoMaker(session)


@pytest.fixture(scope='session')
def thumb_data_info() -> dict:
    with open(source_path(r'resources/thumb/thumbnaildata_.json'), r'rb') as f:
        return json.load(f)
