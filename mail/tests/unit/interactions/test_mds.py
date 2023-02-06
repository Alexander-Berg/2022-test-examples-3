import pytest
from aiohttp import web

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.interactions.mds.exceptions import MDSForbiddenError, MDSInsufficientStorageError, MDSNotFoundError


@pytest.fixture
def mds_client(clients, mock_mds, ipa_settings):
    mds = clients.mds
    mds.EXPIRE = ipa_settings.MDS_EXPIRE
    mds.NAMESPACE = ipa_settings.MDS_NAMESPACE
    return mds


@pytest.fixture
def mds_namespace():
    return 'test-mds-namespace'


@pytest.fixture
def mds_expire():
    return 'test-mds-expire'


@pytest.fixture
def ipa_settings(ipa_settings, mds_expire, mds_namespace):
    ipa_settings.MDS_EXPIRE = mds_expire
    ipa_settings.MDS_NAMESPACE = mds_namespace
    return ipa_settings


class TestMDSUpload:
    @pytest.fixture
    def filepath(self):
        return 'test-mds-upload-filepath'

    @pytest.fixture
    def expected_request_path(self, mds_namespace, filepath, uuid):
        return f'/upload-{mds_namespace}/{filepath}.{uuid}'

    @pytest.fixture
    def mock_mds(self, mock_mds_write, expected_request_path, response_data):
        mock_mds_write(expected_request_path, web.Response(body=response_data))

    @pytest.fixture
    def key(self):
        return 'test-mds-upload-key'

    @pytest.fixture
    def data(self):
        return b'sample,data'

    @pytest.fixture
    def uuid(self):
        return 'test-mds-upload-uuid4-hex'

    @pytest.fixture
    def response_data(self, key):
        return f'''
            <?xml version="1.0" encoding="utf-8"?>
            <post
                obj="namespace.filename"
                id="81d8ba78...666dd3d1"
                groups="3"
                size="100"
                key="{key}">
            </post>
        '''.strip()

    @pytest.fixture
    def request_coro(self, mds_client, filepath, data):
        return mds_client.upload(filepath, data)

    @pytest.fixture
    async def returned(self, request_coro):
        return await request_coro

    @pytest.fixture(autouse=True)
    def uuid_mock(self, mocker, uuid):
        uuid4_mock = mocker.Mock()
        uuid4_mock.hex = uuid
        return mocker.patch(
            'mail.ipa.ipa.interactions.mds.uuid.uuid4',
            mocker.Mock(return_value=uuid4_mock)
        )

    def test_request_url(self,
                         get_last_mds_write_request,
                         ipa_settings,
                         mds_namespace,
                         mds_client,
                         uuid,
                         filepath,
                         data,
                         returned):
        url = str(get_last_mds_write_request().url.with_query({}).with_scheme('https'))
        assert url.endswith(f'{ipa_settings.MDS_WRITE_URL}/upload-{mds_namespace}/{filepath}.{uuid}')

    def test_request_query(self, returned, get_last_mds_write_request, mds_expire):
        assert_that(
            dict(get_last_mds_write_request().query),
            equal_to({'expire': mds_expire}),
        )

    def test_returns_key(self, key, returned):
        assert_that(returned, equal_to(key))

    class TestRetries:
        @pytest.fixture
        def statuses(self):
            return [403, 200]

        @pytest.fixture
        def mock_mds(self, mock_mds_write, expected_request_path, response_data, statuses):
            for status in statuses:
                mock_mds_write(expected_request_path, web.Response(body=response_data, status=status))

        def test_retries(self, get_mds_write_requests, returned, statuses):
            assert len(get_mds_write_requests()) == len(statuses)

        def test_retries_uuid_calls(self, uuid_mock, returned, statuses):
            assert uuid_mock.call_count == len(statuses)

    @pytest.mark.parametrize('status,expected_exc', (
        (507, MDSInsufficientStorageError),
        (403, MDSForbiddenError),
        (404, MDSNotFoundError),
    ))
    class TestException:
        @pytest.fixture
        def mock_mds(self, mock_mds_write, expected_request_path, status):
            async def handler(*args, **kwargs):
                mock_mds_write(expected_request_path, handler)

                return web.Response(status=status)
            mock_mds_write(expected_request_path, handler)

        @pytest.mark.asyncio
        async def test_raises_exc_on_status(self, request_coro, expected_exc):
            with pytest.raises(expected_exc):
                await request_coro


class TestMDSDownload:
    @pytest.fixture
    def filepath(self):
        return 'download/path'

    @pytest.fixture
    def expected_request_path(self, mds_namespace, filepath):
        return f'/get-{mds_namespace}/{filepath}'

    @pytest.fixture
    def response_data(self):
        return b'csv,data'

    @pytest.fixture
    def mock_mds(self, mock_mds_read, expected_request_path, response_data):
        mock_mds_read(expected_request_path, web.Response(body=response_data, status=200))
        mock_mds_read('/get-test-mds-namespace/download/path', web.Response(body=response_data, status=200))

    @pytest.fixture
    async def returned(self, mds_client, filepath):
        return await mds_client.download(filepath)

    def test_request_url(self,
                         get_last_mds_read_request,
                         ipa_settings,
                         mds_namespace,
                         mds_client,
                         filepath,
                         returned):
        url = str(get_last_mds_read_request().url.with_query({}).with_scheme('https'))
        assert url.endswith(f'{ipa_settings.MDS_READ_URL}/get-{mds_namespace}/{filepath}')

    @pytest.mark.asyncio
    async def test_returned(self,
                            response_data,
                            returned):
        assert_that(
            await returned.readline(),
            equal_to(response_data)
        )
