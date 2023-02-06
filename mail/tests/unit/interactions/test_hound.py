import pytest

from hamcrest import assert_that, has_entries, only_contains

from mail.beagle.beagle.interactions.hound import Envelope


class BaseHoundTest:
    @pytest.fixture
    def envelope_dict(self, randn):
        return {
            "mid": str(randn()),
            "fid": str(randn()),
            "threadId": str(randn()),
            "revision": randn(),
        }

    @pytest.fixture
    def envelope(self, envelope_dict):
        return Envelope(
            mid=int(envelope_dict['mid']),
            fid=int(envelope_dict['fid']),
            tid=int(envelope_dict['threadId']),
            revision=envelope_dict['revision'],
        )

    @pytest.fixture
    def returned_func(self, params, method, response_json, clients):
        async def _inner():
            return await getattr(clients.hound, method)(**params)

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()


@pytest.mark.asyncio
class TestGetThreadsByFolder(BaseHoundTest):
    @pytest.fixture
    def method(self):
        return 'get_threads_by_folder'

    @pytest.fixture
    def response(self, envelope_dict):
        return {
            'threads_by_folder': {
                'envelopes': [
                    envelope_dict
                ]
            }
        }

    @pytest.fixture(autouse=True)
    def setup(self, mock_hound, response):
        mock_hound('/threads_by_folder', response)

    @pytest.fixture
    def params(self, randn):
        return dict(uid=randn(), fid=randn(), limit=randn(), offset=randn())

    async def test_query_params(self, returned, params, last_hound_request):
        request = last_hound_request()
        assert_that(
            request.query,
            has_entries({
                'uid': str(params['uid']),
                'fid': str(params['fid']),
                'first': str(params['offset']),
                'count': str(params['limit']),
            })
        )

    async def test_result(self, envelope, returned):
        assert returned == [envelope]


@pytest.mark.asyncio
class TestGetMessagesByThread(BaseHoundTest):
    @pytest.fixture
    def method(self):
        return 'get_messages_by_thread'

    @pytest.fixture
    def response(self, envelope_dict):
        return {
            'envelopes': [envelope_dict]
        }

    @pytest.fixture(autouse=True)
    def setup(self, mock_hound, response):
        mock_hound('/messages_by_thread', response)

    @pytest.fixture
    def params(self, randn, rands):
        return dict(uid=randn(), tid=randn(), limit=randn(), offset=randn(), sort_type=rands())

    async def test_query_params(self, returned, params, last_hound_request):
        request = last_hound_request()
        assert_that(
            request.query,
            has_entries({
                'uid': str(params['uid']),
                'tid': str(params['tid']),
                'first': str(params['offset']),
                'count': str(params['limit']),
                'sort_type': params['sort_type'],
            })
        )

    async def test_result(self, envelope, returned):
        assert returned == [envelope]


@pytest.mark.asyncio
class TestGetAttachmentSid(BaseHoundTest):
    @pytest.fixture
    def method(self):
        return 'get_attachment_sid'

    @pytest.fixture
    def response(self, rands):
        return {
            'result': [{'sids': [rands()]}, {'sids': [rands()]}]
        }

    @pytest.fixture(autouse=True)
    def setup(self, mock_hound, response):
        mock_hound('/attach_sid', response)

    @pytest.fixture
    def params(self, randn, rands):
        return dict(uid=randn(), mid=randn(), hid=rands())

    async def test_query_params(self, returned, params, last_hound_request):
        request = last_hound_request()
        assert_that(
            await request.json(),
            only_contains(
                has_entries({
                    'uid': params['uid'],
                    'downloads': [{'mid': params['mid'], 'hids': [params['hid']]}]
                })
            )
        )

    async def test_result(self, response, returned):
        assert returned == response['result'][0]['sids'][0]
