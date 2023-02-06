from unittest.mock import MagicMock
import requests
from tractor.util.retrying import TemporaryError, retry


retry_base_delay: float = 0.0
retries: int = 3


def _make_http_error(status_code: int):
    response = requests.Response()
    response.status_code = status_code
    return requests.HTTPError(response=response)


def test_retry_should_handle_call_without_exception():
    f = MagicMock()
    retry(retry_base_delay, retries)(f)()
    assert f.call_count == 1


def test_retry_should_handle_4xx_status_code():
    f = MagicMock(side_effect=_make_http_error(400))
    try:
        retry(retry_base_delay, retries)(f)()
    except requests.HTTPError as e:
        pass
    assert f.call_count == 1


def test_retry_should_handle_5xx_status_code():
    f = MagicMock(side_effect=_make_http_error(500))
    try:
        retry(retry_base_delay, retries)(f)()
    except requests.HTTPError as e:
        pass
    assert f.call_count == retries + 1


def test_retry_should_handle_transient_error():
    f = MagicMock(side_effect=TemporaryError())
    try:
        retry(retry_base_delay, retries)(f)()
    except TemporaryError as e:
        pass
    assert f.call_count == retries + 1


def test_retry_should_handle_non_http_error():
    f = MagicMock(side_effect=Exception(""))
    try:
        retry(retry_base_delay, retries)(f)()
    except:
        pass
    assert f.call_count == 1
