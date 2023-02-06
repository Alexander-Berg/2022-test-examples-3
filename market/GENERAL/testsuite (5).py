import __classic_import     # noqa
from market.library.shiny.lite.suite import ShinySuite
from market.ugc.daemon.beam.service import UgcServer


class UgcSuite(ShinySuite):
    svc_cls = UgcServer
