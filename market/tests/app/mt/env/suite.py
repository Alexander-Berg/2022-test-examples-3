from . import __classic_import     # noqa
from market.library.shiny.lite.suite import ShinySuite
from market.library.shiny.server.gen.lib.tests.app.beam.service import AppServer


class AppSuite(ShinySuite):
    svc_cls = AppServer
