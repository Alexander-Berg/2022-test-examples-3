from market.pylibrary.lite.log import TskvLogBackend
from market.pylibrary.lite.matcher import ustr
from market.pylibrary.lite.structure_matching import contains_fragment
from market.library.shiny.lite.suite import ShinySuite
from market.mars.beam.service import MarsServer
from market.mars.lite.core.logs import TraceLogFrontend, PromoTriggersLog, BinaryLogBackend, RecommendationLog


class TestSuite(ShinySuite):
    svc_cls = MarsServer
    bigb_pg = None
    trace_log = TraceLogFrontend('trace log')
    trace_log_backend = None
    promo_triggers_log = PromoTriggersLog('promo triggers log')
    promo_triggers_log_backend = None
    recommendation_log = RecommendationLog('recommendation log')
    recommendation_log_backend = None

    @classmethod
    def setUpClass(cls):
        super(TestSuite, cls).setUpClass()
        cls.trace_log_backend = TskvLogBackend(cls.mars.config.Core.TraceLog.Target.FilePath)
        cls.trace_log.bind(cls.trace_log_backend)
        cls.promo_triggers_log_backend = BinaryLogBackend(cls.mars.config.PromoTriggersLogConfig.Target.FilePath)
        cls.promo_triggers_log.bind(cls.promo_triggers_log_backend)
        cls.recommendation_log_backend = BinaryLogBackend(cls.mars.config.RecommendationLogConfig.Target.FilePath)
        cls.recommendation_log.bind(cls.recommendation_log_backend)

    def assertFragmentIn(self, response, fragment, preserve_order=False, allow_different_len=True, use_regex=False):
        if isinstance(response, list) or isinstance(response, dict):
            contains, reasons = contains_fragment(
                fragment,
                response,
                preserve_order=preserve_order,
                allow_different_len=allow_different_len,
                use_regex=use_regex,
            )
            if not contains:
                self.fail_verbose(
                    'Response does not contain {}\n'
                    'Strict order: {}\n'
                    'Original response: {}'.format(ustr(fragment), preserve_order, response),
                    reasons,
                    '',
                )
        else:
            super(TestSuite, self).assertFragmentIn(
                response, fragment, preserve_order=preserve_order, allow_different_len=allow_different_len
            )

    def assertFragmentNotIn(self, response, fragment, preserve_order=False, allow_different_len=True, use_regex=False):
        if isinstance(response, list) or isinstance(response, dict):
            contains, reasons = contains_fragment(
                fragment,
                response,
                preserve_order=preserve_order,
                allow_different_len=allow_different_len,
                use_regex=use_regex,
            )
            if contains:
                self.fail_verbose(
                    'Response contains unexpected {}\n'
                    'Strict order: {}\n'
                    'Original response: {}'.format(ustr(fragment), preserve_order, response),
                    reasons,
                    '',
                )
        else:
            super(TestSuite, self).assertFragmentNotIn(response, fragment, preserve_order=preserve_order)

    def setUp(self):
        super(TestSuite, self).setUp()
        self.mars.request_text("stat/reset", "POST")

    def _teardown_check(self, server):
        try:
            error, reasons = self.trace_log.check()
            if error is not None:
                return error, reasons
            error, reasons = self.promo_triggers_log.check()
            if error is not None:
                return error, reasons
            error, reasons = self.recommendation_log.check()
            if error is not None:
                return error, reasons
            return super(TestSuite, self)._teardown_check(server)
        finally:
            self.trace_log_backend.reopen()
            self.promo_triggers_log_backend.reopen()
            self.recommendation_log_backend.reopen()
