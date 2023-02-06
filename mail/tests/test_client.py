from datetime import datetime, timezone

import pytest
from aiobotocore.endpoint import ClientResponseProxy
from aioresponses import aioresponses

from sendr_filestore import S3Storage


class ClientResponse(ClientResponseProxy):
    @property
    def content(self):
        return self._self_content

    @content.setter
    def content(self, value):
        self.__wrapped__.content = self._self_content = value

    async def read(self):
        if isinstance(self._self_content, bytes):
            return self._self_content
        self._self_content = await self.content.read()
        return self.content


@pytest.fixture
async def client():
    storage = S3Storage(
        host='http://s3.test',
        bucket='sendr-bucket',
        access_key_id='access_key_id',
        secret_access_key='secret_access_key',
        user_agent='UASender',
        connect_timeout=1,
        read_timeout=10,
        maxsize=1,
        retries=2,
    )
    async with storage.acquire() as client:
        yield client


@pytest.mark.asyncio
async def test_download_file(client):
    with aioresponses() as m:
        # TODO: втащить pytest-httpserver
        m.get(
            'http://s3.test/sendr-bucket/foo/bar',
            headers={
                'content-type': 'application/octet-stream',
                'etag': 'etag',
                'last-modified': 'Thu, 21 Jun 2021 11:29:06 GMT',
                'content-length': '3',
            },
            body='123',
            response_class=ClientResponse,
        )

        reader = await client.download('foo/bar')

        assert await reader.content.read() == b'123'
        assert reader.etag == 'etag'
        assert reader.mtime == datetime(2021, 6, 21, 11, 29, 6, tzinfo=timezone.utc)
        assert reader.size == 3
