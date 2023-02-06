import pytest
from requests import Response
from unittest import mock

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import SignatureMaker


@pytest.fixture(scope='session')
def signature_maker(session):
    return SignatureMaker(session=session)


@pytest.fixture(scope='session')
def signature_response(mock_signatures_text_data):
    resp = Response()
    resp.status_code = 200
    resp._content = mock_signatures_text_data
    return resp


@pytest.fixture(scope='session')
def signature_response_wrong_content():
    resp = Response()
    resp.status_code = 200
    resp._content = b'"\\u'
    return resp


@pytest.fixture(scope='session')
def signature_mock_video_info():
    mock_video = mock.Mock()
    mock_video.transcoder_info = {}
    return mock_video


@pytest.fixture(scope='session')
def signature_error_response():
    resp = Response()
    resp.status_code = 404
    return resp
