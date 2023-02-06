import __classic_import     # noqa
from market.library.shiny.lite.suite import ShinySuite
from market.seo.experiments.beam.service import ExperimentsServer


class ExperimentsSuite(ShinySuite):
    svc_cls = ExperimentsServer
