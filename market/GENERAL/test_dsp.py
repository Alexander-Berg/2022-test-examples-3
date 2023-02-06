import base64
import json

from market.mars.lite.env import TestSuite, main
from market.mars.lite.core.dsp import create_dsp_body
from market.mars.lite.core.report import ModelItem
from market.pylibrary.lite.matcher import Capture, NotEmpty
from market.mars.yql.proto.recommendation_pb2 import TRecommendedModel


class T(TestSuite):
    def get_item_info(self, item):
        result = {
            "bannerID": str(item.model_id),
            "targetUrl": item.target_url,
            "legal": "ООО \"Яндекс.Маркет\"",
            "price": item.price,
        }
        if item.old_price and item.old_price != item.price:
            result["oldprice"] = item.old_price
        return result

    def get_item_fragment(self, item, puid=1):
        return {
            "ads": [
                {
                    "link": {"url": item.direct_url},
                    "assets": [
                        {"name": "title", "value": item.title},
                        {
                            "name": "body",
                            "value": 'Купить за ' + str(item.price) + ' ₽.',
                        },
                        {"name": "domain", "value": "market.yandex.ru"},
                        {
                            "name": "media",
                            "value": {
                                "image": {
                                    "h": item.picture.height,
                                    "w": item.picture.width,
                                    "url": 'https:' + item.picture.url,
                                }
                            },
                        },
                        # По просьбе @mellanore
                        {
                            "name": "favicon",
                            "value": {
                                "h": 96,
                                "w": 96,
                                "url": "https://yastatic.net/market-export/_/i/favicon/ymnew/96.png",
                            },
                        },
                        {
                            "name": "sponsored",
                            "link": {"url": "https://market.yandex.ru/"},
                            "value": "реклама",
                            "type": "string",
                        },
                    ],
                    "id": str(item.model_id),
                    "info": json.dumps(
                        self.get_item_info(item), ensure_ascii=False, separators=(',', ':'), sort_keys=True
                    ),
                }
            ]
        }

    @classmethod
    def prepare(cls):
        cls.bigb = cls.mars.bigb_pg

    @classmethod
    def connect(cls):
        return {"api_report": cls.mars.api_report, "dj": cls.mars.dj}

    @classmethod
    def prepare_test_has_bigb_models_has_report_models(cls):
        dsp_models = cls.mars.api_report.dsp_models()
        dsp_models.add(
            ModelItem(1),
            ModelItem(2),
            ModelItem(3),
        )
        dsp_models.add()

    def check_media_format(self, media):
        self.assertTrue(media.contains("image"))
        self.assertTrue(media["image"].contains("h"))
        self.assertTrue(media["image"].contains("w"))
        self.assertTrue(media["image"].contains("url"))
        self.assertTrue(media["image"].contains("sizeType"))

    def check_info_format(self, info):
        self.assertTrue(info.contains("bannerId"))
        self.assertTrue(info.contains("targetUrl"))
        self.assertTrue(info.contains("legal"))
        self.assertTrue(info.contains("price"))

    def check_native_format(self, native):
        self.assertTrue(native.contains("ver"))
        self.assertTrue(native.contains("ads"))
        ads = native["ads"]
        self.assertTrue(len(ads) > 0)
        for ad in ads:
            self.assertTrue(ad.contains("link"))
            self.assertTrue(ad.contains["link"].contains("url"))
            self.assertTrue(ad.contains("adType"))
            self.assertTrue(ad.contains("id"))
            self.assertTrue(ad.contains("assets"))
            self.assertTrue(ad.contains("info"))
            self.check_info_format(ad["info"])
            assets = ad["assets"]
            self.assertTrue(len(assets) > 0)
            types = {}
            for asset in assets:
                self.assertTrue(asset.contains("name"))
                self.assertTrue(asset.contains("clickable"))
                self.assertTrue(asset.contains("value"))
                self.assertTrue(asset.contains("required"))
                self.assertTrue(asset.contains("type"))
                self.assertTrue(asset.contains("id"))
                if asset["type"] == "media":
                    self.check_media_format(asset["value"])
                types.add(asset["name"])
                if asset["type"] == "sponsored":
                    self.assertTrue(asset.contains("link"))
                    self.assertTrue(asset["link"].contains("url"))
            self.assertTrue(types.contains("title"))
            self.assertTrue(types.contains("body"))
            self.assertTrue(types.contains("domain"))
            self.assertTrue(types.contains("media"))
            self.assertTrue(types.contains("favicon"))

    def check_native_base64_format(self, native_base64):
        self.check_native_format(json.loads(base64.b64decode(native_base64)))

    def check_dsp_response_format(self, response):
        self.assertTrue(response.contains(b"id"))
        self.assertTrue(response.contains(b"cur"))
        self.assertTrue(response.contains(b"seatbid"))
        seatbids = response["seatbid"]
        self.assertTrue(len(seatbids) > 0)
        for seatbid in seatbids:
            self.assertTrue("seat" in seatbid)
            self.assertTrue("bid" in seatbid)
            bids = seatbid["bid"]
            self.assertTrue(len(bids) > 0)
            for bid in bids:
                self.assertTrue("impid" in bid)
                self.assertTrue("id" in bid)
                self.assertTrue("price" in bid)
                self.assertTrue("adid" in bid)
                self.assertTrue("adomain" in bid)
                self.assertTrue("cid" in bid)
                self.assertTrue("adm" in bid)
                self.assertTrue("w" in bid)
                self.assertTrue("h" in bid)
                self.assertTrue("ad_type" in bid)
                assert bid["ad_type"] == 1

    def test_response_format(self):
        """
        Проверяем формат выдачи
        """
        body = create_dsp_body(self.bigb.gen_profile([1, 2, 3]))
        response = self.mars.request_json('dsp/offline?recommendation-items-count-threshold=0', 'POST', body)
        self.check_dsp_response_format(response)

    def test_has_bigb_models_has_report_models(self):
        """Проверяем что модели, которые есть в бигб и есть в репорте появляются в выдаче"""
        body = create_dsp_body(self.bigb.gen_profile([1]))
        response = self.mars.request_json('dsp/offline', 'POST', body)

        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(1)))

    def test_has_bigb_models_hasnt_report_models(self):
        """Проверяем что модели, которые есть в бигб, но которых нет в репорте не появляются в выдаче"""
        body = create_dsp_body(self.bigb.gen_profile([1, 4]))
        response = self.mars.request_json('dsp/offline', 'POST', body)

        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(1)))
        self.assertFragmentNotIn(native_block, self.get_item_fragment(ModelItem(4)))

    def test_hasnt_bigb_models_response_with_204(self):
        """
        Проверяем, что при пустом количестве моделей из bigb ответ не формируется, а возвращается ответ с 204 кодом
        """
        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            body = create_dsp_body(self.bigb.gen_profile([]))
            self.mars.request_json('dsp/offline', 'POST', body)

    def test_profile_models_stats(self):
        """
        Проверяем, что считаются статистики has_profile_models и no_profile_models
        """

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(
            stat_respones, [["offline-dsp_has_profile_models_dmmm", 0], ["offline-dsp_no_profile_models_dmmm", 0]]
        )

        body = create_dsp_body(self.bigb.gen_profile([1]))
        self.mars.request_json('dsp/offline', 'POST', body)

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(
            stat_respones, [["offline-dsp_has_profile_models_dmmm", 1], ["offline-dsp_no_profile_models_dmmm", 0]]
        )

        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            body = create_dsp_body(self.bigb.gen_profile([]))
            self.mars.request_json('dsp/offline', 'POST', body)

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(
            stat_respones, [["offline-dsp_has_profile_models_dmmm", 1], ["offline-dsp_no_profile_models_dmmm", 1]]
        )

    def test_not_enough_report_models_response_with_204(self):
        """
        Проверяем, что при количестве моделей в репорте, меньшем чем recommendation-items-count-threshold,
        возвращяется ответ с 204 кодом
        """
        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            body = create_dsp_body(self.bigb.gen_profile([1, 2, 3]))
            self.mars.request_json('dsp/offline?recommendation-items-count-threshold=4', 'POST', body)

    def test_hasnt_report_models_response_with_204(self):
        """Проверяем, что при отсутствии моделей в репорте ответ не формируется, а возвращается ответ с 204 кодом"""
        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            body = create_dsp_body(self.bigb.gen_profile([4]))
            self.mars.request_json('dsp/offline', 'POST', body)

    def test_report_stats(self):
        """Проверяем, что считаются статистики ответа репорта not_enough_recommendations и all_models_filtered_out"""

        stat_respones = self.mars.request_json('stat')

        self.assertFragmentIn(stat_respones, ["offline-dsp_not_enough_recommendations_dmmm", 0])

        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            body = create_dsp_body(self.bigb.gen_profile([4]))
            self.mars.request_json('dsp/offline', 'POST', body)

        stat_respones = self.mars.request_json('stat')

        self.assertFragmentIn(stat_respones, ["offline-dsp_not_enough_recommendations_dmmm", 1])

    def test_recommendation_items_max_count(self):
        """Проверяем, что возвращаемое количество моделей не больше, чем recommendation-items-max-count"""

        body = create_dsp_body(self.bigb.gen_profile([1, 2]))
        response = self.mars.request_json('dsp/offline?recommendation-items-max-count=1', 'POST', body)

        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(1)))
        self.assertFragmentNotIn(native_block, self.get_item_fragment(ModelItem(2)))

    def test_offline_dsp_json(self):
        body = create_dsp_body(self.bigb.gen_profile([1, 2, 3]))
        response = self.mars.request_json('dsp/offline', 'POST', body)
        self.assertFragmentIn(
            response,
            {
                "id": "92374572431",
                "cur": "RUB",
                "seatbid": [
                    {
                        "seat": "dsp-market",
                        "bid": [{"id": "92374572431", "impid": "103", "cid": "1", "adomain": ["market.yandex.ru"]}],
                    }
                ],
            },
        )

    def test_trace_log_contains_out_request_offline(self):
        body = create_dsp_body(self.bigb.gen_profile([1, 2, 3]))
        self.mars.request_json('dsp/offline', 'POST', body)
        self.trace_log.expect(request_method='/yandsearch', http_code=200, type='OUT')

    @staticmethod
    def create_skip_token(yabs_ids: list[int], awaps_ids=None):
        """Создание скип-токена (https://wiki.yandex-team.ru/pcode/skip-token/#filtracijadirektavskip-token)"""
        if yabs_ids:
            yabs = "yabs." + base64.urlsafe_b64encode("\n".join(map(str, yabs_ids)).encode()).decode()
            if not awaps_ids:
                return yabs
        if awaps_ids:
            awaps = "awaps." + base64.urlsafe_b64encode("\n".join(map(str, yabs_ids)).encode()).decode()
            if not yabs_ids:
                return awaps
        return r"%0A".join([yabs, awaps])

    @classmethod
    def prepare_test_offline_dsp_skip_token(cls):
        dsp_models = cls.mars.api_report.dsp_models()
        dsp_models.add(
            ModelItem(10),
            ModelItem(20),
            ModelItem(30),
            ModelItem(40),
        )
        dsp_models.add()

    def test_offline_dsp_skip_token(self):
        """
        Проверяем фильтрацию по скип-токену (в скип-токене указаны id моделей, которые уже были показаны).
        В ответе должно содержаться не более 50% моделей из скип-токена, если рекомендаций не хватает.

        Заодно проверим, что в статистике содержится filtration_time.
        """
        skip_token = self.create_skip_token([30, 40])
        body = create_dsp_body(self.bigb.gen_profile([10, 20, 30, 40]), skip_token=skip_token)
        response = self.mars.request_json('dsp/offline?recommendation-items-count-threshold=3', 'POST', body)

        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(10)))
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(20)))
        # You can add no more than 50% of those previously shown, if not enough
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(30)))
        self.assertFragmentNotIn(native_block, self.get_item_fragment(ModelItem(40)))

        stat_respones = self.mars.request_json('stat')

        self.assertFragmentIn(stat_respones, ["offline-dsp_filtration_time_hgram"])

    def test_offline_dsp_skip_token_204(self):
        """
        Так как нельзя добавлять в выдачу больше 50% ранее показанных моделей,
        то в данном случае не получится вернуть 3 модели. Ошибка 204.

        Также проверим здесь и счетчик not_enough_recommendations_after_filtration.
        """
        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ["offline-dsp_not_enough_recommendations_after_filtration_dmmm", 0])

        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            skip_token = self.create_skip_token([20, 30, 40])
            body = create_dsp_body(self.bigb.gen_profile([10, 20, 30, 40]), skip_token=skip_token)
            self.mars.request_json('dsp/offline?recommendation-items-count-threshold=3', 'POST', body)

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ["offline-dsp_not_enough_recommendations_after_filtration_dmmm", 1])

    @classmethod
    def prepare_test_online_dsp(cls):
        recommendation = cls.mars.dj.models_recommendation()
        body = base64.b64decode(cls.bigb.gen_profile([1, 2, 3]))
        recommendation.add(
            puid=1337,
            models=[recommendation.create_model(1), recommendation.create_model(2), recommendation.create_model(3)],
            body=body,
        )
        recommendation.add_code(experiment="test", puid=1337, body=body)

    def test_online_dsp(self):
        """
        Проверяем, что cgi параметры парсятся, рекомендации от диджея доходят и при ошибке на диджее рекомендаций нет,
        остальная часть работы dsp покрывается другими тестами.
        """
        body = create_dsp_body(self.bigb.gen_profile([1, 2, 3]), puid=1337)
        ok_response = self.mars.request_json('dsp/online', 'POST', body)

        native = Capture()
        self.assertFragmentIn(ok_response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(1), 1337))
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(2), 1337))
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(3), 1337))

        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            self.mars.request_json('dsp/online?recommendation-items-count-threshold=100', 'POST', body)

        with self.assertRaisesRegex(RuntimeError, "Server error: 204"):
            self.mars.request_json('dsp/online?dj-experiment=test', 'POST', body)

    def test_trace_log_contains_out_request_online(self):
        body = create_dsp_body(self.bigb.gen_profile([1, 2, 3]), puid=1337)
        self.mars.request_json('dsp/online', 'POST', body)
        self.trace_log.expect(request_method='/recommend', http_code=200, type='OUT')

    def test_bid_price(self):
        body = create_dsp_body(self.bigb.gen_profile([TRecommendedModel(model_id=1, score=1, ctr_value=0.1)]))
        response = self.mars.request_json('dsp/offline', 'POST', body)
        native = Capture()
        assert response["seatbid"][0]["bid"][0]["price"] == 3000

        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(1)))

    @classmethod
    def prepare_prices(cls):
        dsp_models = cls.mars.api_report.dsp_models()
        dsp_models.add(
            ModelItem(model_id=71, price="975", old_price="1090", discount=11),
            ModelItem(model_id=72, price="1000"),
        )
        dsp_models.add()

    def test_prices(self):
        """
        Проверяем что в поле info доезжает price и oldprice
        Проверяем что oldprice отсутствует, если не задан (скидки нет)
        """
        body = create_dsp_body(self.bigb.gen_profile([71, 72]))
        response = self.mars.request_json('dsp/offline?', 'POST', body)

        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(
            native_block, self.get_item_fragment(ModelItem(71, price=975, old_price=1090, discount=11))
        )
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(72, price=1000)))

    @classmethod
    def prepare_title_length(cls):
        dsp_models = cls.mars.api_report.dsp_models()
        dsp_models.add(
            ModelItem(model_id=73, title="______________Модель 73. Длина заголовка 56_____________"),
            ModelItem(model_id=74, title="____________Модель 74. Длина заголовка 50_________"),
            ModelItem(model_id=75, title="______________Модель 75. Длина заголовка 57______________"),
        )
        dsp_models.add()

    def test_title_length(self):
        """
        Проверяем что длина заголовка не превышает 56 символов,
        а заголовки длиной свыше 56 символов обрезаются с "..." на конце
        """
        body = create_dsp_body(self.bigb.gen_profile([73, 74, 75]))
        response = self.mars.request_json('dsp/offline?', 'POST', body)

        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))
        normal_title_1 = "______________Модель 73. Длина заголовка 56_____________"
        normal_title_2 = "____________Модель 74. Длина заголовка 50_________"
        long_title_1 = "______________Модель 75. Длина заголовка 57__________..."
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(73, title=normal_title_1)))
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(74, title=normal_title_2)))
        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(75, title=long_title_1)))

    def test_cpm_stats(self):
        """Проверяем, что отправляются сигналы по выставленной ставке cpm"""

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, [["offline-dsp_non_zero_cpm_dmmm", 0], ["offline-dsp_zero_cpm_dmmm", 0]])

        body = create_dsp_body(self.bigb.gen_profile([1]))
        self.mars.request_json('dsp/offline?', 'POST', body)

        stat_respones = self.mars.request_json('stat')
        self.assertFragmentIn(stat_respones, ["offline-dsp_non_zero_cpm_dmmm", 1])

    @classmethod
    def prepare_price_source(cls):
        dsp_models = cls.mars.api_report.dsp_models()
        dsp_models.add(ModelItem(model_id=76))
        dsp_models.add(ModelItem(model_id=77), rearrs=[('mars_dsp_models_prices_source', 'regional_statistic')])
        dsp_models.add(ModelItem(model_id=78), rearrs=[('mars_dsp_models_prices_source', 'default_offer')])
        dsp_models.add()

    def test_price_source(self):
        """
        Проверяем что cgi параметр price-source устанавливает реарр-флаг mars_dsp_models_prices_source
        при запросах в репорт
        """
        response = self.mars.request_json('dsp/offline?', 'POST', create_dsp_body(self.bigb.gen_profile([76])))
        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(76)))

        response = self.mars.request_json(
            'dsp/offline?price-source=regional_statistic', 'POST', create_dsp_body(self.bigb.gen_profile([77]))
        )
        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(77)))

        response = self.mars.request_json(
            'dsp/offline?price-source=default_offer', 'POST', create_dsp_body(self.bigb.gen_profile([78]))
        )
        native = Capture()
        self.assertFragmentIn(response, {"seatbid": [{"bid": [{"adm": NotEmpty(capture=native)}]}]})
        native_block = json.loads(base64.b64decode(native.value))

        self.assertFragmentIn(native_block, self.get_item_fragment(ModelItem(78)))


if __name__ == '__main__':
    main()
