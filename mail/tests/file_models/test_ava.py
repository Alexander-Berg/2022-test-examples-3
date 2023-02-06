import pytest
from django.conf import settings
from rest_framework import status
import requests

from fan.file_models.avatars import AvatarsPublisher

pytestmark = pytest.mark.django_db


@pytest.fixture
def source_path():
    return "/get-sender/5199/test_image/orig"


@pytest.fixture
def source_url(source_path):
    return "https://avatars.mdst.yandex.net%s" % source_path


@pytest.fixture
def publish_image():
    return "dummy.png"


@pytest.fixture
def publish_path(publish_image):
    return "/get-unit_test/5199/%s/orig" % publish_image


@pytest.fixture
def avatars_response(publish_path):
    return {"sizes": {"orig": {"height": 1, "width": 1, "path": publish_path}}}


@pytest.fixture
def wrong_avatars_response(publish_path):
    return {
        "attrs": {
            "sizes": {
                "orig": {
                    "height": 1,
                    "width": 1,
                    "path": publish_path,
                }
            }
        }
    }


@pytest.mark.parametrize("_ssl", [True, False])
def test_read_url(_ssl):
    url = AvatarsPublisher.get_read_url("/get/test/path", use_ssl=_ssl)
    schema = "https" if _ssl else "http"

    assert url == schema + "://{host}/get/test/path".format(host=settings.AVATARS_HOSTS["read"])


def test_publish(mock_avatars_publish):
    AvatarsPublisher().publish(b"")
    assert len(mock_avatars_publish.published_images) == 1


def test_publish_url(mock_avatars_publish, source_url):
    AvatarsPublisher().publish_url(source_url)
    assert len(mock_avatars_publish.published_images) == 1
    assert mock_avatars_publish.published_images[0][1] == source_url


def test_clone(mock_avatars_publish, source_path, source_url):
    AvatarsPublisher().clone(source_path)
    assert len(mock_avatars_publish.published_images) == 1
    assert mock_avatars_publish.published_images[0][1] == source_url


def test_publish_file_parses_avatars_response(mock_avatars_publish, publish_path, avatars_response):
    mock_avatars_publish.resp_json = avatars_response
    publish_path = AvatarsPublisher().publish_file("")
    assert publish_path == publish_path


def test_publish_file_raises_on_wrong_avatars_response(
    mock_avatars_publish, wrong_avatars_response
):
    mock_avatars_publish.resp_json = wrong_avatars_response
    with pytest.raises(KeyError):
        AvatarsPublisher().publish_file("")


def test_publish_url_parses_avatars_response(
    mock_avatars_publish, publish_path, avatars_response, source_url
):
    mock_avatars_publish.resp_json = avatars_response
    path = AvatarsPublisher().publish_url(source_url)
    assert path == publish_path


def test_publish_url_raises_on_wrong_avatars_response(
    mock_avatars_publish, wrong_avatars_response, source_url
):
    mock_avatars_publish.resp_json = wrong_avatars_response
    with pytest.raises(KeyError):
        AvatarsPublisher().publish_url(source_url)


def test_unpublish(mock_avatars_unpublish, publish_image, publish_path):
    AvatarsPublisher().unpublish(publish_path)
    assert mock_avatars_unpublish.unpublished_images[0] == (publish_image,)


def test_publish_avatars_timeout(mock_avatars_publish):
    mock_avatars_publish.raise_timeout_on_call_number = 1
    with pytest.raises(requests.exceptions.Timeout):
        AvatarsPublisher().publish(b"")


def test_publish_avatars_error(mock_avatars_publish):
    mock_avatars_publish.resp_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    mock_avatars_publish.resp_code_on_call_number = 1
    with pytest.raises(requests.exceptions.HTTPError):
        AvatarsPublisher().publish(b"")


def test_publish_url_avatars_timeout(mock_avatars_publish, source_url):
    mock_avatars_publish.raise_timeout_on_call_number = 1
    with pytest.raises(requests.exceptions.Timeout):
        AvatarsPublisher().publish_url(source_url)


def test_publish_url_avatars_error(mock_avatars_publish, source_url):
    mock_avatars_publish.resp_code = status.HTTP_500_INTERNAL_SERVER_ERROR
    mock_avatars_publish.resp_code_on_call_number = 1
    with pytest.raises(requests.exceptions.HTTPError):
        AvatarsPublisher().publish_url(source_url)


def test_unpublish_avatars_timeout(mock_avatars_unpublish, publish_path):
    mock_avatars_unpublish.raise_timeout = True
    AvatarsPublisher().unpublish(publish_path)


def test_unpublish_avatars_error(mock_avatars_unpublish, publish_path):
    mock_avatars_unpublish.resp_code = status.HTTP_404_NOT_FOUND
    AvatarsPublisher().unpublish(publish_path)
