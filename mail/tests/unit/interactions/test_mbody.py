import base64

import pytest
from aiohttp import web

from hamcrest import assert_that, has_entries

from mail.beagle.beagle.interactions.mbody import MessageEndpointFlag, MidNotFoundMBodyError, UidNotFoundMBodyError


class BaseMBodyTest:
    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture
    def mid(self, randn):
        return randn()

    @pytest.fixture
    def returned_func(self, params, method, response_json, clients, mock_mbody):
        async def _inner():
            return await getattr(clients.mbody, method)(**params)

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    class TestError:
        @pytest.fixture
        def response(self, error):
            return web.json_response(status=500, data={'error': error})

        @pytest.mark.parametrize('error,exception', (
            ('unknown mid', MidNotFoundMBodyError),
            ('returns records count not equal mids count', MidNotFoundMBodyError),
            ('uid not found', UidNotFoundMBodyError),
            ('Sharpei service responded with 404', UidNotFoundMBodyError),
        ))
        async def test_error(self, clients, method, params, exception):
            with pytest.raises(exception):
                await getattr(clients.mbody, method)(**params)


@pytest.mark.asyncio
class TestMessage(BaseMBodyTest):
    @pytest.fixture
    def message(self, rands):
        return {
            "bodies": [
                {
                    "isAttach": False,
                    "transformerResult": {
                        "textTransformerResult": {
                            "content": rands(),
                        }
                    }
                }
            ],
        }

    @pytest.fixture
    def flags(self, randslice):
        return randslice(list(MessageEndpointFlag))

    @pytest.fixture
    def response(self, message):
        return message

    @pytest.fixture(params=('message', 'message_content'))
    def method(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def setup(self, mock_mbody, response):
        mock_mbody('/message', response)

    @pytest.fixture
    def params(self, uid, mid, flags):
        return dict(uid=uid, mid=mid, flags=flags)

    async def test_query_params(self, returned, uid, mid, flags, last_mbody_request):
        request = last_mbody_request()
        assert_that(
            request.query,
            has_entries({
                'uid': str(uid),
                'mid': str(mid),
                'flags': ','.join((flag.value for flag in flags))
            })
        )

    async def test_result(self, method, message, returned):
        expected = {
            'message': message,
            'message_content': message['bodies'][0]['transformerResult']['textTransformerResult']['content'],
        }
        assert returned == expected[method]


@pytest.mark.asyncio
class TestMessageSource(BaseMBodyTest):
    @pytest.fixture
    def message(self, rands):
        return rands().encode('utf8')

    @pytest.fixture
    def response(self, message):
        return {
            'text': base64.b64encode(message)
        }

    @pytest.fixture(autouse=True)
    def setup(self, mock_mbody, response):
        mock_mbody('/message_source', response)

    @pytest.fixture
    def method(self):
        return 'message_source'

    @pytest.fixture
    def params(self, uid, mid):
        return dict(uid=uid, mid=mid)

    async def test_query_params(self, returned, uid, mid, last_mbody_request):
        request = last_mbody_request()
        assert_that(
            request.query,
            has_entries({'uid': str(uid), 'mid': str(mid)})
        )

    async def test_result(self, message, returned):
        assert returned == message
