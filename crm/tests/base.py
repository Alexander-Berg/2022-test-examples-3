from aiohttp.test_utils import AioHTTPTestCase
from aiohttp import web


class BaseTestCase(AioHTTPTestCase):
    middleware = None

    async def get_application(self):
        async def ok(request):
            return web.Response(text='Ok')

        app = web.Application(middlewares=[self.middleware])
        app.router.add_view('/', ok)
        app.router.add_get('/ping', ok, name='ping')
        return app
