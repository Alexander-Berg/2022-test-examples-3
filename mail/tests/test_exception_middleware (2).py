
from mock import MagicMock
from pymail.web_tools.exception_middleware import ExceptionMiddleware


def test_returns_app_data_when_no_exceptions():
    def good_app(environ, start_response):
        return 'abc'

    wrapped_app = ExceptionMiddleware(good_app)

    response_str = ''.join(wrapped_app(MagicMock(), MagicMock()))

    assert response_str == 'abc'


def test_return_traceback_when_app_produce_exception():
    def buggy_app(environ, start_response):
        raise RuntimeError(
            'Something bad happens!!!'
        )

    wrapped_app = ExceptionMiddleware(buggy_app)

    start_response_mock = MagicMock()

    response_str = ''.join(wrapped_app(MagicMock(), start_response_mock))

    assert 'RuntimeError: Something bad happens!!!' in response_str
    start_response_mock.assert_called_once_with(
        '500 INTERNAL SERVER ERROR', [
            ('Content-Type', 'application/json')])
