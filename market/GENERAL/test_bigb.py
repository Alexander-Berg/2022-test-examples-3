import market.mars.lite.env as env
from market.library.shiny.lite.log import Severity
from market.pylibrary.lite.matcher import Contains


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {"bigb": cls.mars.bigb}

    @classmethod
    def prepare(cls):
        T.bigb_profiles = cls.mars.bigb.bigb_profiles()
        T.bigb_profiles.add(puid=123)
        T.bigb_profiles.add_code(puid=1, code=500)

    def test_error_handling(self):
        """Проверяем, что при ошибке в запросе, клиент возвращает сообщение со status=error"""
        with self.assertRaisesRegex(RuntimeError, "Server error: 500"):
            self.mars.request_json("bigb?puid=1")
        self.common_log.expect(message=Contains("returned error (500):"), severity=Severity.ERROR)

    def test_bigb_request(self):
        response = self.mars.request_json("bigb?puid=123")
        self.assertFragmentIn(
            response,
            {"marketLoyaltyCoins": [self.bigb_profiles.get_stub_coin()]},
        )

    def test_trace_log_contains_out_request(self):
        self.mars.request_json("bigb?puid=123")
        self.trace_log.expect(request_method="/bigb", http_code=200, type="OUT")


if __name__ == "__main__":
    env.main()
