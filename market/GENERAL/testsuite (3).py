import __classic_import     # noqa
from market.library.shiny.lite.suite import ShinySuite
from market.recommender.beam.service import RecommenderServer


class RecommenderSuite(ShinySuite):
    svc_cls = RecommenderServer
