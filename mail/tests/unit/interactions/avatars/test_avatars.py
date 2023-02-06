import re

import pytest
from aiohttp import MultipartReader

from mail.payments.payments.interactions.avatars import (
    AvatarsClient, BaseAvatarsError, ForbiddenError, RateLimitError, UnknownAvatarsError, UnsupportedMediaTypeError
)


class TestUpload:
    @pytest.fixture
    def imagename(self):
        return 'imagename'

    @pytest.fixture
    def group(self):
        return 'group'

    @pytest.fixture
    def image(self):
        return b'image'

    @pytest.fixture(autouse=True)
    def mock_upload(
        self,
        mocker,
        aioresponses_mocker,
        avatars_client,
        image,
        group,
        imagename,
    ):
        return aioresponses_mocker.post(
            re.compile(f'.*/put-{AvatarsClient.NAMESPACE}/$'),
            payload={
                'group-id': group,
                'imagename': imagename,
            }
        )

    @pytest.fixture
    def returned_coro(self, avatars_client, image):
        return avatars_client.upload(image)

    @pytest.fixture
    async def returned(self, returned_coro):
        return await returned_coro

    def test_returned(self, returned, group, imagename, payments_settings):
        assert all((
            returned.namespace == payments_settings.AVATARS_PAYMENTS_NAMESPACE,
            returned.group_id == group,
            returned.image_name == imagename,
            returned.url == f'get-{payments_settings.AVATARS_PAYMENTS_NAMESPACE}/{group}/{imagename}',
        ))

    @pytest.mark.asyncio
    async def test_data(self, returned, mock_upload, image):
        class ReadWriter:
            def __init__(self):
                self._data = b''

            async def write(self, data):
                self._data += data

            async def readline(self):
                parts = self._data.split(b'\r\n', 1)
                part = parts[0]
                self._data = parts[1] if len(parts) > 1 else b''
                return part + b'\r\n'

            async def read(self, size):
                data = self._data
                self._data = data[size:]
                return data[:size]

        mpwriter = mock_upload.call_args[1]['data']
        rw = ReadWriter()
        await mpwriter.write(rw)
        reader = MultipartReader(
            headers={'Content-Type': f'multipart/form-data; boundary={mpwriter.boundary}'},
            content=rw
        )

        parts = [await part.read() async for part in reader]
        assert [image] == parts

    @pytest.mark.parametrize('code, exc_cls', (
        (403, ForbiddenError),
        (415, UnsupportedMediaTypeError),
        (429, RateLimitError),
        (400, UnknownAvatarsError),
    ))
    class TestError:
        @pytest.fixture(autouse=True)
        def mock_upload(
            self,
            mocker,
            aioresponses_mocker,
            avatars_client,
            image,
            group,
            imagename,
            code,
            exc_cls,
        ):
            return aioresponses_mocker.post(
                re.compile(f'.*/put-{AvatarsClient.NAMESPACE}/$'),
                status=code,
                payload={
                    'description': 'desc'
                },
            )

        @pytest.mark.asyncio
        async def test_raises(self, returned_coro, exc_cls):
            with pytest.raises(exc_cls):
                await returned_coro

    @pytest.mark.parametrize('body, expected', (
        ('Backend unavailable', 'Unable to decode error json'),
        ('{"status": "error"}', 'No description present'),
        ('{"status": "error", "description": "description"}', 'description'),
    ))
    class TestErrorDescription:
        @pytest.fixture(autouse=True)
        def mock_upload(
            self,
            mocker,
            aioresponses_mocker,
            avatars_client,
            image,
            group,
            imagename,
            body,
        ):
            return aioresponses_mocker.post(
                re.compile(f'.*/put-{AvatarsClient.NAMESPACE}/$'),
                status=400,
                body=body,
            )

        @pytest.mark.asyncio
        async def test_description(self, returned_coro, expected):
            with pytest.raises(BaseAvatarsError) as exc_info:
                await returned_coro

            assert exc_info.value.message == expected
