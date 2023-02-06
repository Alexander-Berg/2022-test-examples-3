from future.moves.urllib.error import HTTPError, URLError
from io import BytesIO as SIO
from collections import namedtuple
from contextlib import contextmanager
import socket
from ora2pg.tools import http
import mock
import pytest

# pylint: disable=R0201

TEST_URL = 'test://with.py.test'


class FakeHTTPError(HTTPError):
    @staticmethod
    def make(code):
        return FakeHTTPError(
            url=TEST_URL,
            code=code,
            msg='some test http error',
            hdrs=[],
            fp=SIO(b'some error file')
        )


class RaiseNTimesSideEffect(object):
    def __init__(self, times_to_raise, code):
        self.times_to_raise = times_to_raise
        self.counter = 0
        self.code = code
        self.response = SIO(b'test response')

    def __call__(self, *args, **kwargs):
        self.counter += 1
        if self.counter < (self.times_to_raise + 1):
            raise FakeHTTPError.make(self.code)
        return self.response


class TestURLJoin(object):

    def test_with_port(self):
        assert http.url_join(host='http://foo.bar', port=9090) == 'http://foo.bar:9090'

    def test_with_port_and_method(self):
        assert http.url_join(
            host='http://foo.bar',
            port=9090,
            method='baz'
        ) == 'http://foo.bar:9090/baz'

    def test_add_slash(self):
        assert http.url_join(host='http://foo.bar', method='baz') == 'http://foo.bar/baz'

    def test_with_one_argument(self):
        assert http.url_join(
            host='http://foo.bar/',
            method='baz',
            args=[('x', 'y')],
        ) == 'http://foo.bar/baz?x=y'

    def test_with_two_arguments(self):
        assert http.url_join(
            host='http://foo.bar/',
            method='baz',
            args=[('a', 'b'), ('c', 'd')],
        ) == 'http://foo.bar/baz?a=b&c=d'

    def test_with_only_host(self):
        assert http.url_join(host='http://foo.bar') == 'http://foo.bar'

    def test_with_host_ends_with_slash_and_method(self):
        assert http.url_join(host='http://foo.bar/', method='baz') == 'http://foo.bar/baz'


class TestRequest(object):

    RequestMocks = namedtuple('RequestMocks', ('sleep', 'urlopen'))

    @contextmanager
    def patch(self):
        with \
            mock.patch('ora2pg.tools.http.sleep', autospec=True) as sleep_mock, \
            mock.patch('ora2pg.tools.http.urlopen', autospec=True) as urlopen_mock \
        :
            yield self.RequestMocks(sleep_mock, urlopen_mock)

    def test_return_read_value(self):
        with self.patch() as mocks:
            mocks.urlopen.return_value = SIO(b'Foo Bar')
            with http.request(TEST_URL, timeout=10) as resp:
                assert resp.read() == b'Foo Bar'

    @pytest.mark.parametrize('code', [
        500, 501, 502, 503, 504
    ])
    def test_retries_with_timeout_on_http_errors_with_50x_codes(self, code):
        with self.patch() as mocks:
            side_effect = RaiseNTimesSideEffect(3, code)
            mocks.urlopen.side_effect = side_effect
            with http.request(TEST_URL, timeout=10):
                pass
            mocks.sleep.assert_has_calls([
                mock.call(1.), mock.call(3.), mock.call(10.)
            ])

    @pytest.mark.parametrize('timeout_error', [
        socket.timeout(''),
        URLError(socket.timeout('')),
        URLError(socket.gaierror('Name or service not known')),
        socket.error(104, 'Connection reset by peer'),
    ])
    def test_retries_with_timeout_on_transport_errors(self, timeout_error):
        with self.patch() as mocks:
            response = SIO(b'sometimes servers answer.')
            mocks.urlopen.side_effect = [
                timeout_error for _ in range(3)
            ] + [response]
            with http.request(
                TEST_URL, timeout=10, do_retries=True
            ):
                pass
            mocks.sleep.assert_has_calls([
                mock.call(1.), mock.call(3.), mock.call(10.)
            ])

    @pytest.mark.parametrize('timeout_error', [
        socket.timeout(''),
        URLError(socket.timeout('')),
        URLError(socket.gaierror('Name or service not known')),
        socket.error(104, 'Connection reset by peer'),
    ])
    def test_do_retries_when_read_produce_error(self, timeout_error):
        with self.patch() as mocks:
            response = b'sometimes servers answer.'
            mocks.urlopen.return_value.read.side_effect = [timeout_error for _ in range(3)] + [response]
            with http.request(TEST_URL, timeout=10, do_retries=True):
                pass
            mocks.sleep.assert_has_calls([
                mock.call(1.), mock.call(3.), mock.call(10.)
            ])

    def test_raise_when_no_more_retries(self):
        with self.patch() as mocks:
            mocks.urlopen.side_effect = RaiseNTimesSideEffect(42, 500)
            with pytest.raises(http.HTTPErrorWithBody):
                with http.request(TEST_URL, timeout=10):
                    pass
            mocks.sleep.assert_has_calls([
                mock.call(1.), mock.call(3.), mock.call(10.)
            ])

    @pytest.mark.parametrize('code', [
        100, 200, 300, 400, 404
    ])
    def test_raise_when_get_non_50x_error(self, code):
        with self.patch() as mocks:
            mocks.urlopen.side_effect = RaiseNTimesSideEffect(1, code)
            with pytest.raises(http.HTTPErrorWithBody):
                with http.request(TEST_URL, timeout=10):
                    pass

    @pytest.mark.parametrize('code', [
        100, 200, 300, 400, 404
    ])
    def test_do_retries_on_non_50x_error_when_ask_for_retries(self, code):
        with self.patch() as mocks:
            side_effect = RaiseNTimesSideEffect(1, code)
            mocks.urlopen.side_effect = side_effect
            with http.request(
                'http://foo.bar/baz', timeout=10, do_retries=True
            ):
                pass
            mocks.sleep.assert_called_once_with(1.)

    @pytest.mark.parametrize('code', [
        404, 505
    ])
    def test_no_retries_on_http_errors_in_skip_retry_codes(self, code):
        with self.patch() as mocks:
            mocks.urlopen.side_effect = RaiseNTimesSideEffect(1, code)
            with pytest.raises(http.HTTPErrorWithBody):
                with http.request(TEST_URL, timeout=10, do_retries=True,
                                  skip_retry_codes=[404, 505]):
                    pass

    @pytest.mark.parametrize('Error', [
        RuntimeError, IOError, Exception,
        URLError,  # we retiries only URLError.reason == timeout
    ])
    def test_no_retries_when_get_non_http_error(self, Error):
        with self.patch() as mocks:
            mocks.urlopen.side_effect = Error('foo')
            with pytest.raises(Error):
                with http.request(TEST_URL):
                    pass
