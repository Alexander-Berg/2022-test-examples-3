import __classic_import     # noqa
from market.library.shiny.lite.suite import ShinySuite
from market.quoter.beam.service import QuoterServer


class QuoterSuite(ShinySuite):
    svc_cls = QuoterServer
