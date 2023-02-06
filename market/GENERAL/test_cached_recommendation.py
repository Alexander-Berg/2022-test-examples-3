import market.mars.lite.env as env

from market.mars.lite.core.report import ModelItem, RecomPictureNewFormat
from market.mars.lib.log.proto.recommendation_record_pb2 import TRecommendationItem
from market.pylibrary.lite.matcher import LikeUrl

thumbnails = [{"thumbnails": [{"height": 100, "width": 100, "name": "100x100"}], "namespace": "mpic"}]


# Минимальая заглушка вместо offer-а для проверки наличия его в ответе
def create_stub_offer(model_id, sku, price_value=100):
    return {
        "wareId": f"1{model_id}",
        "prices": {"value": str(price_value), "currency": "RUR"},
        "model": {"id": model_id},
        "sku": sku,
        "cpa": "real",
        "titles": {"raw": f"title model {model_id}"},
    }


class T(env.TestSuite):
    @classmethod
    def connect(cls):
        return {"main_report": cls.mars.main_report, "memcached": cls.mars.memcached}

    @classmethod
    def prepare(cls):
        offer_info = cls.mars.main_report.offer_info()
        offer_info.add_full_offers(
            offer_items=[
                create_stub_offer(model_id=1, sku="31", price_value=70),
                create_stub_offer(model_id=2, sku="32", price_value=60),
            ],
            known_thumbnails=thumbnails,
            platform="app",
            pp="18",
        )

        dj_models = cls.mars.main_report.dj_models()
        dj_models.add(
            ModelItem(
                model_id=1,
                picture=RecomPictureNewFormat(),
                item_dicts=[create_stub_offer(model_id=1, sku="31", price_value=70)],
            ),
            ModelItem(
                model_id=2,
                picture=RecomPictureNewFormat(),
                item_dicts=[create_stub_offer(model_id=2, sku="32", price_value=60)],
            ),
            djid="yandex_go_product_thematic_block",
            topic="kids",
            title="Гаджеты для детей",
        )

    def test_caching(self):
        """Проверяем кеширование данных"""
        self.mars.request_json(
            "recommendation/prepare?djid=yandex_go_product_thematic_block&topic=kids&service=go&platform=app"
        )
        assert int(self.memcached.get_stats()[0][1]['curr_items']) == 1

    def test_get_compact_format(self):
        """Проверяем ответ товарной ручки - формат ответа compact"""
        self.mars.request_json(
            "recommendation/prepare?djid=yandex_go_product_thematic_block&topic=kids&service=go&platform=app"
        )
        response = self.mars.request_json(
            "recommendation/take?djid=yandex_go_product_thematic_block&topic=kids&service=go&platform=app"
        )
        self.assertFragmentIn(
            response,
            {
                "items": [
                    {
                        "cachback": 0,
                        "currency": "RUR",
                        "id": "1",
                        "picture": "//avatars.mds.yandex.net/get-mpic/1337/img_12/orig",
                        "price": "70",
                        "title": "title model 1",
                        "type": "product",
                        "url": LikeUrl.of("https://1389598.redirect.appmetrica.yandex.com/product/31?"),
                    },
                ],
                "title": "Гаджеты для детей",
            },
        )
        self.trace_log.expect(http_code=200, type="OUT")
        recommendation = [
            TRecommendationItem(Model="1", Sku="31"),
            TRecommendationItem(Model="2", Sku="32"),
        ]
        self.recommendation_log.expect(handler="recommendation/take", service="go", recommendation=recommendation)


if __name__ == "__main__":
    env.main()
