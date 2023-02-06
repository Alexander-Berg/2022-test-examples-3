import base64
import json

from market.mars.lite.env import TestSuite, main
from market.mars.lite.core.dsp import create_dsp_body
from market.pylibrary.lite.matcher import Capture, NotEmpty, LikeUrl, Wildcard

from market.mars.lite.core.report import ModelItem


class T(TestSuite):
    @classmethod
    def prepare(cls):
        cls.bigb = cls.mars.bigb_pg

    @classmethod
    def connect(cls):
        return {"api_report": cls.mars.api_report}

    @classmethod
    def prepare_auction_win_notification(cls):
        dsp_models = cls.mars.api_report.dsp_models()
        dsp_models.add(ModelItem(1))
        dsp_models.add()

    @staticmethod
    def get_url(notification_type, puid, offer_id=None):
        url_params = dict(type=notification_type, puid=puid, block_id=Wildcard("{puid}:*".format(puid=puid)))
        if offer_id is not None:
            url_params["offer_id"] = offer_id
        return LikeUrl(url_scheme="http", url_host="testing-mars", url_path="/dsp/notification", url_params=url_params)

    def test_auction_win_notification(self):
        """Проверяем что модели, которые есть в бигб и есть в репорте появляются в выдаче"""
        puid = "969798"
        body = create_dsp_body(self.bigb.gen_profile([1]), puid=puid)
        response = self.mars.request_json('dsp/offline?recommendation-items-count-threshold=0', 'POST', body)

        native = Capture()
        self.assertFragmentIn(
            response,
            {
                "seatbid": [
                    {
                        "bid": [
                            {
                                "adm": NotEmpty(capture=native),
                                "nurl": self.get_url(notification_type="auction_win", puid=puid),
                                "burl": self.get_url(notification_type="billing", puid=puid),
                            }
                        ]
                    }
                ]
            },
        )
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(
            native_block,
            {
                "ads": [
                    {
                        "showNotice": {
                            "url": self.get_url(notification_type="show", puid=puid, offer_id=1),
                            "delay": 2000,
                            "visibilityPercent": 30,
                        },
                        "link": {
                            "trackingUrl": self.get_url(notification_type="click", puid=puid, offer_id=1),
                            "falseClickUrl": self.get_url(notification_type="misclick", puid=puid, offer_id=1),
                            "falseClickInterval": 360000,
                        },
                        "id": "1",
                    }
                ]
            },
        )

    def test_trace_log_contains_out_request(self):
        puid = "969798"
        body = create_dsp_body(self.bigb.gen_profile([1]), puid=puid)
        self.mars.request_json('dsp/offline?recommendation-items-count-threshold=0', 'POST', body)
        self.trace_log.expect(request_method='/yandsearch', http_code=200, type='OUT')


if __name__ == '__main__':
    main()
