
from aiohttp.test_utils import TestClient


class BaseTestClient(TestClient):

    async def get(self, path, **kwargs):
        return await self.request('GET', path, **kwargs)

    async def post(self, path, **kwargs):
        return await self.request('POST', path, **kwargs)

    async def put(self, path, **kwargs):
        return await self.request('PUT', path, **kwargs)

    async def patch(self, path, **kwargs):
        return await self.request('PATCH', path, **kwargs)

    async def delete(self, path, **kwargs):
        return await self.request('DELETE', path, **kwargs)

    async def request(self, method, path, **kwargs):
        expected_status = kwargs.pop('expected_status', None)
        as_response = kwargs.pop('as_response', False)

        auth_headers = kwargs.pop('auth_headers', {})
        if 'headers' in kwargs:
            kwargs['headers'].update(auth_headers)
        else:
            kwargs['headers'] = auth_headers

        response = await super().request(method, path, **kwargs)

        if expected_status:
            assert response.status == expected_status

        if as_response:
            result = response
        elif response.content_type == 'application/json':
            result = await response.json()
        else:
            result = await response.read()

        return result
