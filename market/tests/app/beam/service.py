#!/usr/bin/env python
if __name__ == '__main__':
    import __classic_import  # noqa
else:
    from . import __classic_import  # noqa
from market.library.shiny.server.gen.lib.tests.app.proto.config_pb2 import TConfig as Config
from market.library.shiny.server.beam.service import ShinyServer
from market.pylibrary.lite.beam import run


class AppServer(ShinyServer):
    name = 'app'

    def __init__(self, ctx, **kwargs):
        ShinyServer.__init__(
            self,
            ctx=ctx,
            bin_path='market/library/shiny/server/gen/lib/tests/app/bin/bin',
            config_cls=Config,
            **kwargs
        )

    def prepare(self, yamake, rebuild=None, **kwargs):
        return ShinyServer.prepare(self, yamake, rebuild, **kwargs)

    def start(self):
        return ShinyServer.start(self)

    def stop(self):
        return ShinyServer.stop(self)


if __name__ == '__main__':
    run(AppServer)
