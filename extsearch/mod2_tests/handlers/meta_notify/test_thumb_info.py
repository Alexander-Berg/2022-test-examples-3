import pytest
from unittest import mock

from requests import RequestException

from vh.lib.sqs_watcher import HandlerError


def test_get_thumb_info(thumb_info_maker, thumb_key, thumb_group_id, thumb_url):
    data = thumb_info_maker.get_thumb_info(thumb_url)
    assert thumb_key == data.get('ThumbKey'), 'Wrong thumb key!'
    assert thumb_group_id == data.get('GroupId'), 'Wrong group id!'


def test_get_thumb_info_none_url(thumb_info_maker):
    data = thumb_info_maker.get_thumb_info(None)
    assert data == {}, 'None thumb url must return empty dict'


def test_get_thumb_info_empty_url(thumb_info_maker):
    data = thumb_info_maker.get_thumb_info('')
    assert data == {}, 'Empty thumb url must return empty dict'


def test_get_thumb_info_wrong_url(thumb_info_maker):
    with pytest.raises(HandlerError):
        thumb_info_maker.get_thumb_info('test_test')


def test_get_thumb_info_request_error(thumb_info_maker, thumb_url):
    with mock.patch('requests.Session.get', side_effect=RequestException('Err!')):
        with pytest.raises(HandlerError):
            thumb_info_maker.get_thumb_info(thumb_url)


def test_get_thumb_info_request_404(thumb_info_maker, thumb_url_404):
    data = thumb_info_maker.get_thumb_info(thumb_url_404)
    assert data == {}
