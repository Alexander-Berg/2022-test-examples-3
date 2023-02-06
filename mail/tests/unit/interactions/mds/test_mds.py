import pytest


async def async_iter(iterable):
    for i in iterable:
        yield i


@pytest.fixture
def filepath():
    return 'test-mds-download-filepath'


class TestMDSDownload:
    @pytest.fixture
    def content_type(self):
        return 'test-mds-download-content-type'

    @pytest.fixture
    def response_data(self):
        return [
            (b'chunk1', None),
            (b'chunk2', None),
            (b'chunk3', None),
        ]

    @pytest.fixture
    def response_mock(self, mocker, content_type, response_data):
        mock = mocker.Mock()
        mock.status = 200
        mock.headers = {'Content-Type': content_type}
        mock.content.iter_chunks.return_value = async_iter(response_data)

        return mock

    @pytest.fixture
    async def returned(self, mds_client, filepath):
        content_type, data = await mds_client.download(filepath)
        return content_type, [chunk async for chunk in data]

    def test_request(self, payments_settings, mds_namespace, mds_client, filepath, returned):
        assert mds_client.call_args == (
            'download',
            'GET',
            f'{payments_settings.MDS_READ_URL}/get-{mds_namespace}/{filepath}'
        )

    def test_chunk_size(self, response_mock, returned):
        response_mock.content.iter_chunks.assert_called_once()

    def test_returns_chunks(self, content_type, response_data, returned):
        assert returned == (content_type, [chunk for chunk, _ in response_data])


class TestMDSUpload:
    @pytest.fixture
    def mds_expire(self):
        return None

    @pytest.fixture
    def key(self):
        return 'test-mds-upload-key'

    @pytest.fixture
    def data(self):
        return b'somepicture'

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
    async def returned(self, mds_client, filepath, data):
        return await mds_client.upload(filepath, data)

    @pytest.fixture(autouse=True)
    def uuid_mock(self, mocker, uuid):
        uuid4_mock = mocker.Mock()
        uuid4_mock.hex = uuid
        return mocker.patch(
            'mail.payments.payments.interactions.mds.uuid.uuid4',
            mocker.Mock(return_value=uuid4_mock)
        )

    def test_request(self, payments_settings, mds_namespace, mds_client, uuid, filepath, data, returned):
        assert all([
            mds_client.call_args == (
                'upload',
                'POST',
                f'{payments_settings.MDS_WRITE_URL}/upload-{mds_namespace}/{filepath}.{uuid}',
            ),
            mds_client.call_kwargs == {'params': None, 'data': data},
        ])

    def test_returns_key(self, key, returned):
        assert key == returned

    class TestExpire:
        @pytest.fixture
        def mds_expire(self):
            return 'test-mds-expire'

        def test_params(self, mds_expire, mds_client, returned):
            assert mds_client.call_kwargs.get('params') == {'expire': mds_expire}

    class TestRetries:
        @pytest.fixture
        def statuses(self):
            return [403, 200]

        @pytest.fixture(autouse=True)
        def setup(self, mds_client, response_mock, statuses):
            """
            Makes response_mock.status a property depending on mds_client call count
            """
            type(response_mock).status = property(lambda self: statuses[len(mds_client.calls) - 1])

        @pytest.mark.asyncio
        async def test_retries(self, mds_client, returned, statuses):
            assert len(mds_client.calls) == len(statuses)

        @pytest.mark.asyncio
        async def test_uuid_calls(self, uuid_mock, returned, statuses):
            assert uuid_mock.call_count == len(statuses)


class TestMDSRemove:
    @pytest.fixture
    async def returned(self, mds_client, filepath):
        return await mds_client.remove(filepath)

    def test_request(self, payments_settings, mds_namespace, mds_client, filepath, returned):
        assert mds_client.call_args == (
            'remove',
            'GET',
            f'{payments_settings.MDS_WRITE_URL}/delete-{mds_namespace}/{filepath}',
        )
