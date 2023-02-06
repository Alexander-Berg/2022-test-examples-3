from market.mars.yql.proto.yt_content_pb2 import (
    THead,
    TCategory,
    TCategoryIndexValue,
    TModel,
    TCategoryMeta,
    TModelsBlock,
    TIndexStructure,
    TSegmentModelsBlock,
    TIndexValue,
)
from market.mars.lite.env import TestSuite, main

from market.pylibrary.lite.matcher import NotEmptyList, EmptyList
from market.mars.lite.core.report import ModelItem

ENTRY_POINT_IMAGES = [
    "//avatars.mds.yandex.net/get-mpic/4322107/img_id8962600163296697049.jpeg/orig",
    "//avatars.mds.yandex.net/get-mpic/4489193/img_id7820413109299165800.jpeg/orig",
    "//avatars.mds.yandex.net/get-mpic/3765589/img_id6658168663792038917.jpeg/orig",
    "//avatars.mds.yandex.net/get-mpic/4561793/img_id6270739844950654824.jpeg/orig",
]


class _Sentiel:
    """Singleton class that differ Developer None from User Defined None"""


_sentiel = _Sentiel()


# Минимальая заглушка вместо offer-а для проверки наличия его в ответе
def create_stub_offer(model_id, price_value=100, cpa=_sentiel):
    return {
        "wareId": "1{model_id}".format(model_id=model_id),
        "prices": {"value": str(price_value)},
        "model": {"id": model_id},
        "cpa": "real" if cpa is _sentiel else cpa,
    }


# Полный offer для проверки полного формата ответа
def create_offer(model_id, price_value=100, cashback=10, promocode_discount=10, cpa=_sentiel, as_str=False):
    offer_id = "1{model_id}".format(model_id=model_id)
    sku = "2{model_id}".format(model_id=model_id)
    cashback_stub = {
        "type": "blue-cashback",
        "key": "3ctFadzaB9ghRQ06IAvYmg",
        "description": "Повышенный кэшбэк 2% на CEHAC (цифровые товары) - Back to School",
        "shopPromoId": "Ggd6XL2fCucyjYUVenhSbQ",
        "startDate": "2021-08-01T21:00:00Z",
        "endDate": "2021-08-31T21:00:00Z",
        "tags": ["extra-cashback"],
        "share": 0.02,
        "version": 1,
        "priority": -88,
        "promoBucketName": "default",
        "value": cashback,
    }
    promocode_stub = {
        "type": "promo-code",
        "key": "zWpf-E7jNUs-1t4Qt0aX5Q",
        "description": "No description provided",
        "url": "https://market.yandex.ru/special/promocode-MARKETSALE",
        "landingUrl": "https://market.yandex.ru/special/autumn-sale",
        "shopPromoId": "L167426",
        "startDate": "2021-10-17T21:00:00Z",
        "endDate": "2021-10-24T20:59:59Z",
        "conditions": "на 1 заказ от 4 000 ₽ и на скидку не более 1 000 ₽",
        "promoCode": "MARKETSALE",
        "mechanicsPaymentType": "CPA",
        "discount": {"value": 10},
        "itemsInfo": {
            "promoCode": "MARKETSALE",
            "discountType": "percent",
            "conditions": "на 1 заказ от 4 000 ₽ и на скидку не более 1 000 ₽",
            "orderMinPrice": {"currency": "RUR", "value": "4000"},
            "orderMaxPrice": {"currency": "RUR", "value": "10000"},
            "promoPrice": {
                "currency": "RUR",
                "value": str(price_value - promocode_discount),
                "discount": {
                    "oldMin": str(price_value),
                    "percent": (promocode_discount * 100) // price_value,
                    "absolute": str(promocode_discount),
                },
            },
        },
    }
    cpa = "real" if cpa is _sentiel else cpa
    market_model_offer = "//market.yandex.ru/product/{sku}?offerid={offer_id}".format(sku=sku, offer_id=offer_id)
    promos = []
    if cashback:
        promos.append(cashback_stub)
    if promocode_discount:
        promos.append(promocode_stub)
    result = {
        "prices": {
            "value": str(price_value),
        },
        "model": {"id": str(model_id) if as_str else model_id},
        "sku": sku,
        "marketSku": sku,
        "wareId": offer_id,
        "titles": {"raw": "offer title with vendor for {offer_id}".format(offer_id=offer_id)},
        "titlesWithoutVendor": {"raw": "offer title for {offer_id}".format(offer_id=offer_id)},
        "pictures": [
            {
                "original": {
                    "groupId": 1337,
                    "width": 640,
                    "height": 480,
                    "namespace": "mpic",
                    "key": "img_{}".format(offer_id),
                }
            }
        ],
        "urls": {"direct": market_model_offer, "marketModelOffer": market_model_offer},
        "promo": cashback_stub if cashback else {},
        "promos": promos,
    }
    if cpa is not None:
        result.update({"cpa": cpa})
    return result


def create_models(model_ids) -> list[TModel]:
    return [TModel(base_price=110, model_id=model_id, price_benefit_multiplier=1) for model_id in model_ids]


def create_models_block(model_ids):
    return TModelsBlock(models=create_models(model_ids))


def create_head(staring_id: int, starting_image_id: int, names: list[str]) -> THead:
    top_categories: list[TCategoryMeta] = list()
    for num, name in enumerate(names):
        meta = TCategoryMeta(
            hid=staring_id * 10 + num,
            name=name,
            image=ENTRY_POINT_IMAGES[(starting_image_id + num) % len(ENTRY_POINT_IMAGES)],
            imageHd=ENTRY_POINT_IMAGES[(starting_image_id + num) % len(ENTRY_POINT_IMAGES)],
        )
        top_categories.append(meta)
    return THead(
        index=7.05,
        prev_index=6.5,
        cashback_impact=2.1,
        discount_impact=2.1,
        promo_impact=2.2,
        price_impact=1.2,
        top_categories=top_categories,
    )


def get_debug_stat(
    dyno_models_count: int = 0,
    negative_profit_index_models_count: int = 0,
    report_models_after_filtration_count: int = 0,
    report_models_count: int = 0,
    showed_models_count: int = 0,
) -> dict:
    # protobuf serializes uint64 as strings
    return {
        "debugStat": {
            "dynoModelsCount": str(dyno_models_count),
            "negativeProfitIndexModelsCount": str(negative_profit_index_models_count),
            "reportModelsAfterFiltrationCount": str(report_models_after_filtration_count),
            "reportModelsCount": str(report_models_count),
            "showedModelsCount": str(showed_models_count),
        }
    }


thumbnails = [{"thumbnails": [{"height": 100, "width": 100, "name": "100x100"}], "namespace": "mpic"}]


class T(TestSuite):
    @classmethod
    def connect(cls):
        return {"api_report": cls.mars.api_report, "dyno": cls.mars.dyno}

    @classmethod
    def prepare(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(
            ModelItem(model_id=1, item_dicts=[create_stub_offer(model_id=1, price_value=70)]),
            ModelItem(model_id=2, item_dicts=[create_stub_offer(model_id=2, price_value=60)]),
            known_thumbnails=thumbnails,
        )

    @classmethod
    def prepare_category_models(cls):
        cls.mars.dyno.add_to_profit_index(
            TCategory(
                hid=111,
                category_index=7.5,
                index_structure=TIndexStructure(
                    cashback_impact=2.1, discount_impact=2.2, price_impact=2.1, promo_impact=1.5
                ),
                name="Товары для дома",
                models=create_models([1, 2]),
            )
        )

    def test_category_models(self):
        """Проверяем выдачу ручки категории"""
        response = self.mars.request_json('profit-index/category?hid=111&items-min-count=2&debug')
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=1, price_value=70),
                    create_stub_offer(model_id=2, price_value=60),
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"knownThumbnails": thumbnails})
        self.assertFragmentIn(
            response,
            {
                "id": "111",
                "name": "Товары для дома",
                "currentValue": 7.5,
                "indexStructure": {"cashbackValue": 2.1, "discountValue": 2.2, "priceValue": 2.1, "promoValue": 1.5},
            },
        )
        self.assertFragmentIn(
            response,
            get_debug_stat(
                dyno_models_count=2,
                negative_profit_index_models_count=0,
                report_models_after_filtration_count=2,
                report_models_count=2,
                showed_models_count=2,
            ),
        )

    def test_category_not_enough_models_from_report(self):
        """Проверяем, что мы получаем информацию о том, что с репорта пришло мало моделей"""
        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_category_not_enough_models_from_report_dmmm", 0])
        with self.assertRaisesRegex(RuntimeError, "Server error: 404"):
            self.mars.request_json('profit-index/category?hid=111&items-min-count=3')

        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_category_not_enough_models_from_report_dmmm", 1])

    @classmethod
    def prepare_products(cls):
        cls.mars.dyno.add_to_profit_index(create_models_block([1, 2]))

    def test_products(self):
        """Проверяем выдачу ручки выгодных товаров"""
        response = self.mars.request_json('profit-index/products?items-min-count=2&debug')
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=1, price_value=70),
                    create_stub_offer(model_id=2, price_value=60),
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"knownThumbnails": thumbnails})
        self.assertFragmentIn(
            response,
            get_debug_stat(
                dyno_models_count=2,
                negative_profit_index_models_count=0,
                report_models_after_filtration_count=2,
                report_models_count=2,
                showed_models_count=2,
            ),
        )

    @classmethod
    def prepare_products_filter_out_non_profit_products(cls):
        recom_models = cls.mars.api_report.recom_models()
        # ИВ = (base_price - price_value + cashback + promodiscount) * price_benefit_multiplier
        # Модель 3  - нулевой ИВ:       (90 - 110 + 10 + 10) * 1 = 0
        # Модель 4  - отрицательный ИВ: (90 - 120 + 10 + 10) * 1 = -10
        # Модель 41 - ПРОХОДИТ по ИВ:   (90 - 100 + 10 + 10) * 1 = 10 -- только эта модель будет в ответе
        # Модель 42 - нулевой ИВ:       (90 - 100 + 10 + 10) * 0 = 0
        # Модель 43 - нулевой ИВ:       (90 - 100 + 10 + 0)  * 1 = 0
        # Модель 44 - нулевой ИВ:       (90 - 100 + 0  + 10) * 1 = 0
        recom_models.add(
            ModelItem(model_id=3, item_dicts=[create_offer(model_id=3, price_value=110)]),
            ModelItem(model_id=4, item_dicts=[create_offer(model_id=4, price_value=120)]),
            ModelItem(model_id=41, item_dicts=[create_offer(model_id=41, price_value=100)]),
            ModelItem(model_id=42, item_dicts=[create_offer(model_id=42, price_value=100)]),
            ModelItem(model_id=43, item_dicts=[create_offer(model_id=43, price_value=100, promocode_discount=0)]),
            ModelItem(model_id=44, item_dicts=[create_offer(model_id=44, price_value=100, cashback=0)]),
        )
        cls.mars.dyno.add_to_profit_index(
            TCategory(
                hid=112,
                category_index=2.5,
                name="Почти пустая категория",
                models=[
                    TModel(base_price=90, model_id=3, price_benefit_multiplier=1),
                    TModel(base_price=90, model_id=4, price_benefit_multiplier=1),
                    TModel(base_price=90, model_id=41, price_benefit_multiplier=1),
                    TModel(base_price=90, model_id=42, price_benefit_multiplier=0),
                    TModel(base_price=90, model_id=43, price_benefit_multiplier=1),
                    TModel(base_price=90, model_id=44, price_benefit_multiplier=1),
                ],
            )
        )

    def test_products_filter_out_non_profit_products(self):
        """Проверяем, что модели, у которых индекс выгодности (ИВ) меньше 0 выфильтровываются"""
        response = self.mars.request_json('profit-index/category?hid=112&items-min-count=1&items-multiplier=3')
        self.assertFragmentIn(response, {"products": [create_stub_offer(model_id=41)]}, allow_different_len=False)

    def test_base_price_multiplier(self):
        """Проверяем, что при включении 5% увеличения базовой цены, ИВ поддрастёт и модели не будут выфильтровываться"""
        response = self.mars.request_json(
            'profit-index/category?hid=112&items-min-count=1&items-multiplier=3&base-price-multiplier=1.05'
        )
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=41),
                    create_stub_offer(model_id=3, price_value=110),
                    create_stub_offer(model_id=43),
                    create_stub_offer(model_id=44),
                ]
            },
            allow_different_len=False,
        )

    def test_category_not_enough_items_after_filtration_metric(self):
        """Проверяем, что получаем информацию о том, что после фильтрования недостаточно айтемов"""
        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_category_not_enough_items_after_filtration_dmmm", 0])

        with self.assertRaisesRegex(RuntimeError, "Server error: 404"):
            self.mars.request_json('profit-index/category?hid=112&items-min-count=2')

        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_category_not_enough_items_after_filtration_dmmm", 1])

    @classmethod
    def prepare_products_order_profit_products(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(
            ModelItem(model_id=51, item_dicts=[create_offer(model_id=51)]),
            ModelItem(model_id=5, item_dicts=[create_offer(model_id=5, cashback=0)]),
            ModelItem(model_id=6, item_dicts=[create_offer(model_id=6)]),
            ModelItem(model_id=7, item_dicts=[create_offer(model_id=7, promocode_discount=20)]),
            ModelItem(model_id=71, item_dicts=[create_offer(model_id=71)]),
        )
        cls.mars.dyno.add_to_profit_index(
            # Модель 51 : ИВ = (90 - 100 + 10 + 10) * 1  = 10
            # Модель 5  : ИВ = (90 - 100 + 10 + 0)  * 1  = 1
            # Модель 6  : ИВ = (90 - 100 + 10 + 10) * 10 = 100
            # Модель 7  : ИВ = (81 - 100 + 10 + 20) * 5  = 55
            # Модель 71 : ИВ = (81 - 100 + 10 + 10) * 5  = 5
            TCategory(
                hid=113,
                category_index=9.5,
                name="Всё для плавания",
                models=[
                    TModel(base_price=90, model_id=51, price_benefit_multiplier=1),
                    TModel(base_price=91, model_id=5, price_benefit_multiplier=1),
                    TModel(base_price=90, model_id=6, price_benefit_multiplier=10),
                    TModel(base_price=81, model_id=7, price_benefit_multiplier=5),
                    TModel(base_price=81, model_id=71, price_benefit_multiplier=5),
                ],
            )
        )

    def test_products_order_profit_products(self):
        """Проверяем, что продукты сортируются в порядке убывания их индекса выгодности (ИВ): 6, 7, 51, 71, 8"""
        response = self.mars.request_json('profit-index/category?hid=113&items-min-count=1')
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=6),
                    create_stub_offer(model_id=7),
                    create_stub_offer(model_id=51),
                    create_stub_offer(model_id=71),
                    create_stub_offer(model_id=5),
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_products_filter_out_missing_products(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(ModelItem(model_id=8, item_dicts=[create_stub_offer(model_id=8, price_value=70)]))
        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=114, category_index=4.5, name="Природа рядом", models=create_models([8, 9]))
        )

    def test_products_filter_out_missing_products(self):
        """
        Проверяем, что модели, которые есть в динамической таблице
        выфильтровываются, если их нет в репорте
        """
        response = self.mars.request_json('profit-index/category?hid=114&items-min-count=1&items-multiplier=2')
        # Благодаря allow_different_len=False проверяем, что осталась только одна модель из двух, заданных в prepare
        self.assertFragmentIn(
            response, {"products": [create_stub_offer(model_id=8, price_value=70)]}, allow_different_len=False
        )

    @classmethod
    def prepare_products_format(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(ModelItem(model_id=10, item_dicts=[create_offer(model_id=10)]))
        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=115, category_index=4.5, name="Аниме обои", models=create_models([10]))
        )

    def test_products_format(self):
        """Проверяем что в продуктах приходят:
        Названия:
        - titles.raw
        - titlesWithoutVendor.raw

        Картинки:
        - pictures

        Цены с кэшбеком, скидками:
        - prices
            - value

        Id сущностей:
        - model.id
        - sku|marketSku
        - wareId (offerId)

        Ссылки для перехода на маркет:
        - urls
            - direct
        """
        response = self.mars.request_json('profit-index/category?hid=115')
        self.assertFragmentIn(response, {"products": [create_offer(model_id=10)]})

    @classmethod
    def prepare_not_enough_products(cls):
        recom_models = cls.mars.api_report.recom_models()
        # В этой категории мы не добавили ModelItem в репорт
        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=116, category_index=4.5, name="Ароматизированные свечи 1", models=create_models([11]))
        )

        recom_models.add(ModelItem(model_id=12, item_dicts=[create_offer(model_id=12, price_value=100)]))
        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=117, category_index=6.5, name="Ароматизированные свечи 2", models=create_models([12]))
        )

    def test_not_enough_products(self):
        """
        Проверяем, что сервис возвращает ответ со статусом 404, когда продуктов не достаточно.
        Несмотря на отсутствие флага debug, выдача должна содержать debugStat
        """
        # Нет данных в dyno
        response = self.mars.request_json('profit-index/category?hid=0', fail_on_error=False)
        self.assertFragmentIn(response, {'errorMessage': 'Error while dyno request'})
        self.assertFragmentIn(response, get_debug_stat())
        self.assertEqual(response.code, 404)

        # Нет данных в репорте
        response = self.mars.request_json('profit-index/category?hid=116', fail_on_error=False)
        self.assertFragmentIn(response, {'errorMessage': 'Error while report request'})
        self.assertFragmentIn(response, get_debug_stat(dyno_models_count=1))
        self.assertEqual(response.code, 404)

        # Не проходим по количеству
        response = self.mars.request_json('profit-index/category?hid=117&items-min-count=2', fail_on_error=False)
        self.assertFragmentIn(response, {'errorMessage': 'Not enough items from report'})
        self.assertFragmentIn(response, get_debug_stat(dyno_models_count=1, report_models_count=1))
        self.assertEqual(response.code, 404)

    @classmethod
    def prepare_category_empty_dyno_responses(cls):
        cls.mars.dyno.add_empty_response_to_profit_index(TCategory(hid=0))

    def test_category_empty_dyno_responses(self):
        """
        Проверяем, что получаем информацию, если каких-то данных нет:
            1. При пустом ответе увеличиваем profit-index_category_empty_dyno_responses_dmmm
        """
        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_category_empty_dyno_responses_dmmm", 0])
        with self.assertRaisesRegex(RuntimeError, "Server error: 404"):
            self.mars.request_json('profit-index/category?hid=0')

        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_category_empty_dyno_responses_dmmm", 1])

    def test_category_bad_dyno_responses(self):
        """
        Проверяем, что получаем информацию, если каких-то данных нет:
            2. При ошибке клиента увеличиваем marketDyno_4xx_dmmm
        """
        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["shiny_component=client;marketDyno_4xx_dmmm", 0])
        with self.assertRaisesRegex(RuntimeError, "Server error: 404"):
            self.mars.request_json('profit-index/category?hid=1')

        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["shiny_component=client;marketDyno_4xx_dmmm", 1])

    @classmethod
    def prepare_header(cls):
        cls.mars.dyno.add_to_profit_index(
            create_head(staring_id=1, starting_image_id=0, names=["Бакалейные товары", "Косметика, парфюмерия и уход"])
        )

    def test_header(self):
        response = self.mars.request_json('profit-index/header')
        self.assertFragmentIn(
            response,
            {
                'indexValue': 7.05,
                'yesterdayIndexDiff': 7.05 - 6.5,
                'indexStructure': {
                    "cashbackValue": 2.1,
                    "promoValue": 2.2,
                    "discountValue": 2.1,
                    "priceValue": 1.2,
                },
                'categories': [
                    {'id': '10', 'name': "Бакалейные товары"},
                    {'id': '11', 'name': "Косметика, парфюмерия и уход"},
                ],
            },
        )

    def test_entrance_point(self):
        """Проверяем формат точки входа индекса выгодности"""
        response = self.mars.request_json('profit-index/entry')
        self.assertFragmentIn(
            response,
            {
                "indexValue": 7.05,
                "yesterdayIndexDiff": 7.05 - 6.5,
                "indexStructure": {
                    "cashbackValue": 2.1,
                    "promoValue": 2.2,
                    "discountValue": 2.1,
                    "priceValue": 1.2,
                },
                "categories": [
                    {"image": image, "imageHd": image, "hid": str(10 + num)}
                    for num, image in enumerate(ENTRY_POINT_IMAGES[:2])
                ],
            },
        )

    def test_trace_log_contains_out_request(self):
        self.mars.request_json('profit-index/entry')
        self.trace_log.expect(request_method='/get', http_code=200, type='OUT')

    @classmethod
    def prepare_experiment_data(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(
            ModelItem(model_id=13, item_dicts=[create_stub_offer(model_id=13, price_value=90)]),
            ModelItem(model_id=14, item_dicts=[create_stub_offer(model_id=14, price_value=100)]),
        )

        cls.mars.dyno.add_to_profit_index(
            create_head(
                staring_id=2,
                starting_image_id=2,
                names=['[exp-1] Бакалейные товары', '[exp-1] Косметика, парфюмерия и уход'],
            ),
            experiment="exp-1",
        )

        cls.mars.dyno.add_to_profit_index(create_models_block([13, 14]), experiment="exp-2")

        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=13, category_index=7.5, name="[exp-3] Бакалейные товары", models=create_models([13, 14])),
            experiment="exp-3",
        )

    def test_experiment_data_header(self):
        """
        Проверяем, что при передаче значения эксперимента, выбираются записи с нужным суффиксом ([exp-1])
        для шапки
        """
        response = self.mars.request_json("profit-index/header?experiment=exp-1")
        self.assertEqual(len(response['categories']), 2)  # проверяем что в ответе только два товара
        self.assertFragmentIn(response, {'name': '[exp-1] Бакалейные товары'})  # первый товар с [exp-1]
        self.assertFragmentIn(response, {'name': '[exp-1] Косметика, парфюмерия и уход'})  # второй товар с [exp-1]

    def test_experiment_data_entry_point(self):
        """Проверяем, что при передаче значения эксперимента, выбираются записи с нужным суффиксом
        для точки входа
        """
        response = self.mars.request_json("profit-index/entry?experiment=exp-1")
        self.assertFragmentIn(
            response, {'categories': [{"image": image, "imageHd": image} for image in ENTRY_POINT_IMAGES[2:]]}
        )

    def test_experiment_data_products(self):
        """Проверяем, что при передаче значения эксперимента, выбираются записи с нужным суффиксом
        для списка выгодных товаров
        """
        response = self.mars.request_json("profit-index/products?experiment=exp-2&items-min-count=2")
        self.assertEqual(len(response['products']), 2)  # проверяем что в ответе только два товара
        self.assertFragmentIn(response, {"id": 13})  # товар с id 13
        self.assertFragmentIn(response, {"id": 14})  # товар с id 14

    def test_experiment_data_category(self):
        """Проверяем, что при передаче значения эксперимента, выбираются записи с нужным суффиксом
        для категории
        """
        response = self.mars.request_json("profit-index/category?hid=13&experiment=exp-3&items-min-count=2")
        self.assertFragmentIn(response, {'name': '[exp-3] Бакалейные товары'})
        self.assertEqual(len(response['products']), 2)  # проверяем что в ответе только два товара
        self.assertFragmentIn(response, {"id": 13})  # товар с id 13
        self.assertFragmentIn(response, {"id": 14})  # товар с id 14

    @classmethod
    def prepare_min_max_category_count_cgi_params(cls):
        cls.mars.dyno.add_to_profit_index(
            create_head(
                staring_id=3,
                starting_image_id=2,
                names=[
                    "[min-max-exp] Бакалейные товары",
                    "[min-max-exp] Косметика, парфюмерия и уход",
                    "[min-max-exp] Ножи из КС ГО",
                ],
            ),
            experiment="min-max-exp",
        )

    def test_header_max_category_count(self):
        """Проверяем, что возвращаем не больше максимума категорий при запросе шапки"""
        response = self.mars.request_json("profit-index/header?max-category-count=2&experiment=min-max-exp")
        self.assertEqual(len(response['categories']), 2)  # проверяем что в ответе только два товара
        self.assertFragmentIn(response, {'name': '[min-max-exp] Бакалейные товары'})  # первый товар
        self.assertFragmentIn(response, {'name': '[min-max-exp] Косметика, парфюмерия и уход'})  # второй товар

    def test_header_min_category_count(self):
        """Проверяем, что не возвращаем меньше минимума категорий при запросе шапки"""
        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_header_not_enough_categories_dmmm", 0])

        with self.assertRaisesRegex(RuntimeError, "Server error: 404"):
            self.mars.request_json("profit-index/header?min-category-count=4&experiment=min-max-exp")

        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_header_not_enough_categories_dmmm", 1])

    def test_entrance_point_max_category_count(self):
        """Проверяем, что возвращаем не больше максимума категорий при запросе точки входа"""
        response = self.mars.request_json("profit-index/entry?max-category-count=2&experiment=min-max-exp")
        self.assertLessEqual(len(response['categories']), 2)

    def test_entrance_point_min_category_count(self):
        """Проверяем, что не возвращаем меньше минимума категорий для точки входа"""
        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_entrance-point_not_enough_categories_dmmm", 0])

        with self.assertRaisesRegex(RuntimeError, "Server error: 404"):
            self.mars.request_json("profit-index/entry?min-category-count=4&experiment=min-max-exp")

        stat_response = self.mars.request_json('stat')
        self.assertFragmentIn(stat_response, ["profit-index_entrance-point_not_enough_categories_dmmm", 1])

    @classmethod
    def prepare_not_filter_out_products_by_cpa(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(
            ModelItem(model_id=17, item_dicts=[create_stub_offer(model_id=17)]),
            ModelItem(model_id=18, item_dicts=[create_stub_offer(model_id=18)]),
        )
        cls.mars.dyno.add_to_profit_index(create_models_block([17, 18]), experiment="cpa-ok")

    def test_not_filter_out_products_by_cpa(self):
        """
        Проверяем, что в выдачу попадают только офферы с правильным полем cpa
        """
        response = self.mars.request_json(
            "profit-index/products?experiment=cpa-ok&items-min-count=2&items-multiplier=1"
        )
        self.assertFragmentIn(response, {"products": [create_stub_offer(model_id=17), create_stub_offer(model_id=18)]})

    @classmethod
    def prepare_arranging_categories(cls):
        cls.mars.dyno.add_to_profit_index(
            THead(
                top_categories=[
                    TCategoryMeta(index=0.5, image="0.5", imageHd="0.5"),
                    TCategoryMeta(index=0.1, image="0.1", imageHd="0.1"),
                    TCategoryMeta(index=2.4, image="2.4", imageHd="2.4"),
                ],
            ),
            experiment="header-arranging",
        )

    def test_arranging_categories(self):
        """Проверяем, что категории для шапки индекса выгодности сортируются по убыванию current value"""
        response = self.mars.request_json("profit-index/header?experiment=header-arranging")
        self.assertFragmentIn(
            response,
            {"categories": [{"currentValue": 2.4}, {"currentValue": 0.5}, {"currentValue": 0.1}]},
            preserve_order=True,
        )

    @classmethod
    def prepare_index_date(cls):
        recom_models = cls.mars.api_report.recom_models()

        recom_models.add(
            ModelItem(model_id=313, item_dicts=[create_stub_offer(model_id=313, price_value=90)]),
            ModelItem(model_id=314, item_dicts=[create_stub_offer(model_id=314, price_value=80)]),
        )

        recom_models.add(
            ModelItem(model_id=323, item_dicts=[create_stub_offer(model_id=323, price_value=70)]),
            ModelItem(model_id=324, item_dicts=[create_stub_offer(model_id=324, price_value=60)]),
        )

        cls.mars.dyno.add_to_profit_index(THead(index=10), experiment="index_date")

        cls.mars.dyno.add_to_profit_index(create_models_block([313, 314]), experiment="index_date")
        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=13, category_index=5.5, models=create_models([313, 314])), experiment="index_date"
        )

        cls.mars.dyno.add_to_profit_index(THead(index=0), experiment="index_date", index_date="0000-1-1")
        cls.mars.dyno.add_to_profit_index(
            create_models_block([323, 324]), experiment="index_date", index_date="0000-1-1"
        )
        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=13, category_index=7.5, models=create_models([313, 314])),
            experiment="index_date",
            index_date="0000-1-1",
        )

    def test_index_date_header(self):
        """
        Проверяем в ручке шапки, что если index_date не None, то мы ходим в таблицу за index_date, иначе в recent
        """
        response = self.mars.request_json("profit-index/header?experiment=index_date")
        self.assertFragmentIn(response, {"indexValue": 10})

        response = self.mars.request_json("profit-index/header?experiment=index_date&index-date=0000-1-1")
        self.assertFragmentIn(response, {"indexValue": 0})

    def test_index_date_entrance_point(self):
        """
        Проверяем в точке входа ИВ, что если index_date не None, то мы ходим в таблицу за index_date, иначе в recent
        """
        response = self.mars.request_json("profit-index/entry?experiment=index_date")
        self.assertFragmentIn(response, {"indexValue": 10})

        response = self.mars.request_json("profit-index/entry?experiment=index_date&index-date=0000-1-1")
        self.assertFragmentIn(response, {"indexValue": 0})

    def test_index_date_category(self):
        """
        Проверяем в ручке категорий, что если index_date не None, то мы ходим в таблицу за index_date, иначе в recent
        """
        response = self.mars.request_json("profit-index/category?hid=13&experiment=index_date")
        self.assertFragmentIn(response, {"currentValue": 5.5})

        response = self.mars.request_json("profit-index/category?hid=13&experiment=index_date&index-date=0000-1-1")
        self.assertFragmentIn(response, {"currentValue": 7.5})

    def test_index_date_products(self):
        """
        Проверяем в ручке продуктов, что если index_date не None, то мы ходим в таблицу за index_date, иначе в recent
        """
        response = self.mars.request_json("profit-index/products?experiment=index_date&items-min-count=2")
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=313, price_value=90),
                    create_stub_offer(model_id=314, price_value=80),
                ]
            },
        )

        response = self.mars.request_json(
            "profit-index/products?experiment=index_date&index-date=0000-1-1&items-min-count=2"
        )
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=323, price_value=70),
                    create_stub_offer(model_id=324, price_value=60),
                ]
            },
        )

    @classmethod
    def prepare_report_region_and_gps(cls):
        recom_models = cls.mars.api_report.recom_models()

        recom_models.add(
            ModelItem(model_id=513, item_dicts=[create_stub_offer(model_id=513, price_value=40)]),
            ModelItem(model_id=514, item_dicts=[create_stub_offer(model_id=514, price_value=30)]),
            region=1,
            gps="region_and_gps",
        )

        cls.mars.dyno.add_to_profit_index(create_models_block([513, 514]), experiment="region_and_gps")

        cls.mars.dyno.add_to_profit_index(
            TCategory(hid=13, models=create_models([513, 514])), experiment="region_and_gps"
        )

    def test_report_region_and_gps_products(self):
        """
        Проверяем, что параметры region и gps прокидываются в репорт в ручке продуктов
        """
        response = self.mars.request_json(
            "profit-index/products?experiment=region_and_gps&region=1&gps=region_and_gps&items-min-count=2"
        )
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=513, price_value=40),
                    create_stub_offer(model_id=514, price_value=30),
                ]
            },
        )

    def test_report_region_and_gps_categories(self):
        """
        Проверяем, что параметры region и gps прокидываются в репорт в ручке категорий
        """
        response = self.mars.request_json(
            "profit-index/category?hid=13&experiment=region_and_gps&region=1&gps=region_and_gps&items-min-count=2"
        )
        self.assertFragmentIn(
            response,
            {
                "id": "13",
                "products": [
                    create_stub_offer(model_id=513, price_value=40),
                    create_stub_offer(model_id=514, price_value=30),
                ],
            },
        )

    @classmethod
    def prepare_segments(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(
            ModelItem(model_id=15, item_dicts=[create_stub_offer(model_id=15, price_value=45)]),
            ModelItem(model_id=16, item_dicts=[create_stub_offer(model_id=16, price_value=46)]),
            ModelItem(model_id=100, item_dicts=[create_stub_offer(model_id=100, price_value=500)]),
            known_thumbnails=thumbnails,
        )
        recom_models.add(
            ModelItem(model_id=101, item_dicts=[create_stub_offer(model_id=101, price_value=55)]),
            ModelItem(model_id=102, item_dicts=[create_stub_offer(model_id=102, price_value=500)]),
            known_thumbnails=thumbnails,
        )
        cls.mars.dyno.add_to_profit_index(
            TSegmentModelsBlock(models=create_models([15, 16, 100]), name="Всё для дома", id=1)
        )
        cls.mars.dyno.add_to_profit_index(create_models_block([15, 16, 100]))
        cls.mars.dyno.add_to_profit_index(
            TSegmentModelsBlock(
                models=[
                    TModel(base_price=56, model_id=101, price_benefit_multiplier=1),
                    TModel(base_price=501, model_id=102, price_benefit_multiplier=1),
                ],
                name="Товары для красоты",
                id=2,
            )
        )
        cls.mars.dyno.add_to_profit_index(create_models_block([101, 102]))

    def test_segments_format(self):
        """
        Проверяем формат fmcg товаров
        """
        response = self.mars.request_json('profit-index/segments?segment=1&items-min-count=2&debug')
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=15, price_value=45),
                    create_stub_offer(model_id=16, price_value=46),
                ],
                "knownThumbnails": NotEmptyList(),
                "name": "Всё для дома",
                "id": 1,
            },
        )
        self.assertFragmentIn(
            response,
            get_debug_stat(
                dyno_models_count=3,
                negative_profit_index_models_count=1,
                report_models_after_filtration_count=2,
                report_models_count=3,
                showed_models_count=2,
            ),
        )

    def test_segments_filter(self):
        """
        Проверяем Фильтр для сегмента 1 (не дороже 499)
        Проверяем что сегмент 2 не фильтруется (не задана максимальная цена)
        """
        response = self.mars.request_json('profit-index/segments?segment=1&items-min-count=2')
        self.assertFragmentNotIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=100, price_value=500),
                ],
                "knownThumbnails": NotEmptyList(),
                "name": "Всё для дома",
                "id": 1,
            },
        )
        response = self.mars.request_json('profit-index/segments?segment=2&items-min-count=2')
        self.assertFragmentIn(
            response,
            {
                "products": [
                    create_stub_offer(model_id=101, price_value=55),
                    create_stub_offer(model_id=102, price_value=500),
                ],
                "knownThumbnails": NotEmptyList(),
                "name": "Товары для красоты",
                "id": 2,
            },
        )

    @classmethod
    def prepare_page_exhaust_logic(cls):
        """Готовим модели, которые лежат в dyno в убывающем порядке их индекса выгодности.
        При этом каждая модель имеет положительный индекс выгодности.
        """
        recom_models = cls.mars.api_report.recom_models()
        recom_model_items = []
        dyno_models = []
        for i in range(200, 210):
            # ИВ: (2000 - 1000 - i)
            recom_model_items.append(
                ModelItem(
                    model_id=i,
                    item_dicts=[create_offer(model_id=i, price_value=1000 + i, cashback=0, promocode_discount=0)],
                )
            )
            dyno_models.append(TModel(base_price=2000, model_id=i, price_benefit_multiplier=1))
        for i in range(0, len(recom_model_items), 2):
            recom_models.add(*recom_model_items[i : i + 4], known_thumbnails=thumbnails)
        cls.mars.dyno.add_to_profit_index(TModelsBlock(models=dyno_models), experiment="page_exhaust_logic")

    def test_page_exhaust_logic(self):
        """Проверяем, что если передавать тот PageState, который к нам приходит, то мы пройдём по всем моделям
        При этом все модели из dyno должны иметь положительный индекс выгодности после обогащения
        """
        page_state = ""
        response_products = []
        for _ in range(20):
            response = self.mars.request_json(
                "/profit-index/products?items-multiplier=2&items-max-count=2&items-min-count=2"
                f"&experiment=page_exhaust_logic&page-state={page_state}"
            )
            pageState = response["responsePageState"]
            if pageState["isExhausted"]:
                break
            current_products = response["products"]
            response_products += current_products
            page_state = pageState["pageState"]
        self.assertFragmentIn(
            response_products,
            [{"model": {"id": i}} for i in range(200, 210)],
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_page_last_page_with_same_min_item_logic(cls):
        recom_models = cls.mars.api_report.recom_models()
        recom_models.add(
            *[
                ModelItem(model_id=model_id, item_dicts=[create_offer(model_id=model_id)])
                for model_id in range(220, 222 + 1)
            ]
        )
        cls.mars.dyno.add_to_profit_index(
            TModelsBlock(
                models=[
                    TModel(base_price=300, model_id=model_id, price_benefit_multiplier=1)
                    for model_id in range(220, 222 + 1)
                ]
            ),
            experiment="last_page_logic",
        )

    def test_page_last_page_with_same_min_item_logic(self):
        """Проверяем, что если передавать PageState от пустого ответа, то ответ будет пустым"""
        response_page_state = None
        page_state = ""

        # получаем пустой pageState
        for _ in range(2):
            response = self.mars.request_json(
                "/profit-index/products?items-multiplier=1&items-max-count=3&items-min-count=3"
                f"&experiment=last_page_logic&page-state={page_state}"
            )
            response_page_state = response["responsePageState"]
            page_state = response_page_state["pageState"]
            if not response_page_state["isExhausted"]:
                self.assertFragmentIn(response, {"products": NotEmptyList()})

        # проверяем результат на последнюю страницу
        self.assertFragmentIn(response, {"products": EmptyList()})
        self.assertTrue(response_page_state["isExhausted"])

        # передаём pageState от последней страницы
        response = self.mars.request_json(
            "/profit-index/products?items-multiplier=1&items-max-count=3&items-min-count=3"
            f"&experiment=last_page_logic&page-state={page_state}"
        )

        # проверяем результат на пустоту
        self.assertFragmentIn(response, {"products": EmptyList()})
        self.assertTrue(response["responsePageState"]["isExhausted"])

    @classmethod
    def prepare_page_are_invalidates_on_scroll(cls):
        recom_models = cls.mars.api_report.recom_models()

        # готовим для 2021-11-11 числа
        recom_models.add(
            *[
                ModelItem(model_id=model_id, item_dicts=[create_offer(model_id=model_id)])
                for model_id in range(225, 227 + 1)
            ]
        )
        cls.mars.dyno.add_to_profit_index(
            TModelsBlock(
                models=[
                    TModel(base_price=1000 - model_id, model_id=model_id, price_benefit_multiplier=1)
                    for model_id in range(225, 227 + 1)
                ],
                today_str="2021-11-11",
            ),
            experiment="invalidates_on_scroll_2021_11_11",
        )

        # готовим для 2021-11-12 числа
        recom_models.add(
            *[
                ModelItem(model_id=model_id, item_dicts=[create_offer(model_id=model_id)])
                for model_id in range(230, 232 + 1)
            ]
        )
        cls.mars.dyno.add_to_profit_index(
            TModelsBlock(
                models=[
                    TModel(base_price=1000 - model_id, model_id=model_id, price_benefit_multiplier=1)
                    for model_id in range(230, 232 + 1)
                ],
                today_str="2021-11-12",
            ),
            experiment="invalidates_on_scroll_2021_11_12",
        )

    def test_page_are_invalidates_on_scroll(self):
        """Проверяем, что если индекс обновился, когда пользователь скролил страницу, то PageState инвалидируется"""
        page_state = ""

        # делаем запрос до изменения индекса
        response = self.mars.request_json(
            "/profit-index/products?items-multiplier=1&items-max-count=2&items-min-count=2"
            f"&experiment=invalidates_on_scroll_2021_11_11&page-state={page_state}"
        )
        page_state = response["responsePageState"]["pageState"]

        # проверяем, что выдались модели за 2011-11-11 число
        self.assertFragmentIn(
            response,
            {
                "products": [
                    {"model": {"id": 225}},
                    {"model": {"id": 226}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFalse(response["responsePageState"]["isExhausted"])

        # делаем запрос после изменения индекса
        response = self.mars.request_json(
            "/profit-index/products?items-multiplier=1&items-max-count=2&items-min-count=2"
            f"&experiment=invalidates_on_scroll_2021_11_12&page-state={page_state}"
        )

        # проверяем, что выдались модели за 2011-11-12 число
        self.assertFragmentIn(
            response,
            {
                "products": [
                    {"model": {"id": 230}},
                    {"model": {"id": 231}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFalse(response["responsePageState"]["isExhausted"])

    @classmethod
    def prepare_page_are_invalidates_on_today_str(cls):
        recom_models = cls.mars.api_report.recom_models()

        # готовим для 2021-11-11 числа
        recom_models.add(
            *[
                ModelItem(model_id=model_id, item_dicts=[create_offer(model_id=model_id)])
                for model_id in range(235, 237 + 1)
            ]
        )
        cls.mars.dyno.add_to_profit_index(
            TModelsBlock(
                models=[
                    TModel(base_price=300, model_id=model_id, price_benefit_multiplier=1)
                    for model_id in range(235, 237 + 1)
                ],
                today_str="2021-11-13",
            ),
            experiment="invalidates_on_today_str",
            index_date="2021-11-13",
        )

        # готовим для 2021-11-12 числа
        recom_models.add(
            *[
                ModelItem(model_id=model_id, item_dicts=[create_offer(model_id=model_id)])
                for model_id in range(240, 242 + 1)
            ]
        )
        cls.mars.dyno.add_to_profit_index(
            TModelsBlock(
                models=[
                    TModel(base_price=300, model_id=model_id, price_benefit_multiplier=1)
                    for model_id in range(240, 242 + 1)
                ],
                today_str="2021-11-14",
            ),
            experiment="invalidates_on_today_str",
            index_date="2021-11-14",
        )

    def test_page_are_invalidates_on_today_str(self):
        """Проверяем, что дата в PageState не совпадает с датов переданной в качестве index-date, то PageState инвалидируется"""
        page_state = ""

        # делаем запрос до изменения индекса
        response = self.mars.request_json(
            "/profit-index/products?items-multiplier=2&items-max-count=1&items-min-count=1"
            f"&experiment=invalidates_on_today_str&index-date=2021-11-13&page-state={page_state}"
        )
        page_state = response["responsePageState"]["pageState"]

        # проверяем, что выдались модели за 2011-11-13 число
        self.assertFragmentIn(
            response,
            {
                "products": [
                    {"model": {"id": 235}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFalse(response["responsePageState"]["isExhausted"])

        # делаем запрос с другим index-date
        response = self.mars.request_json(
            "/profit-index/products?items-multiplier=1.5&items-max-count=2&items-min-count=2"
            f"&experiment=invalidates_on_today_str&index-date=2021-11-14&page-state={page_state}"
        )

        # проверяем, что выдались модели за 2011-11-14 число
        self.assertFragmentIn(
            response,
            {
                "products": [
                    {"model": {"id": 240}},
                    {"model": {"id": 241}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFalse(response["responsePageState"]["isExhausted"])

    @classmethod
    def prepare_page_not_show_if_items_not_enough(cls):
        recom_models = cls.mars.api_report.recom_models()

        # готовим для 2021-11-11 числа
        recom_models.add(
            *[
                ModelItem(model_id=model_id, item_dicts=[create_offer(model_id=model_id)])
                for model_id in range(245, 248 + 1)
            ]
        )
        cls.mars.dyno.add_to_profit_index(
            TModelsBlock(
                models=[
                    TModel(base_price=300, model_id=model_id, price_benefit_multiplier=1)
                    for model_id in range(245, 248 + 1)
                ]
            ),
            experiment="not_show_if_items_not_enough",
        )

    def test_page_not_show_if_items_not_enough(self):
        """Проверяем, что последняя страница не возвращается, если ей не хватает элементов для показа"""
        # делаем два запроса, так как "страничным" считается только тот запрос, при котором был передан непустой page_state

        # делаем первый запрос на 3 модели из 4, сохраняем page_state ответа
        response = self.mars.request_json(
            "/profit-index/products?items-max-count=3&items-min-count=2" "&experiment=not_show_if_items_not_enough"
        )
        self.assertFragmentIn(
            response,
            {
                "products": [
                    {"model": {"id": 245}},
                    {"model": {"id": 246}},
                    {"model": {"id": 247}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFalse(response["responsePageState"]["isExhausted"])

        page_state = response["responsePageState"]["pageState"]

        # делаем следующий запрос на 1 модель из 4, при этом для формировани ответа надо 2 модели
        response = self.mars.request_json(
            "/profit-index/products?items-max-count=3&items-min-count=2"
            f"&experiment=not_show_if_items_not_enough&page-state={page_state}"
        )
        self.assertTrue(response["responsePageState"]["isExhausted"])
        self.assertFragmentIn(response, {"products": EmptyList()})

    @classmethod
    def prepare_profit_index_value_format(cls):
        cls.mars.dyno.add_to_profit_index(
            TIndexValue(
                index=10,
                index_structure=TIndexStructure(
                    cashback_impact=3.02, discount_impact=2.03, price_impact=1.05, promo_impact=0.9
                ),
            ),
            experiment="index_value_endpoint",
        )

    def test_profit_index_value_format(self):
        response = self.mars.request_json("/profit-index/value?&experiment=index_value_endpoint")
        self.assertFragmentIn(
            response,
            {
                "indexValue": 10,
                "indexStructure": {"cashbackValue": 3.02, "discountValue": 2.03, "priceValue": 1.05, "promoValue": 0.9},
            },
        )

    @classmethod
    def prepare_profit_index_category_index_value_format(cls):
        """Ресурс, который подкладывается под mars с маппингом категорий находится в mars/beam/bin/resources"""
        cls.mars.dyno.add_to_profit_index(
            TCategoryIndexValue(
                hid=1,
                name="Категория 3 уровня для hid=31",
                index=10,
                index_structure=TIndexStructure(
                    cashback_impact=3.02, discount_impact=2.03, price_impact=1.05, promo_impact=0.9
                ),
            ),
            experiment="category_index_value_endpoint",
        )

    def test_profit_index_category_index_value_format(self):
        """Проверяем, что category_index_value возвращается в нужном формате."""
        response = self.mars.request_json(
            "/profit-index/category-index-value?hid=31&experiment=category_index_value_endpoint"
        )
        self.assertFragmentIn(
            response,
            {
                "id": "1",
                "name": "Категория 3 уровня для hid=31",
                "currentValue": 10,
                "indexStructure": {"cashbackValue": 3.02, "discountValue": 2.03, "priceValue": 1.05, "promoValue": 0.9},
            },
        )


if __name__ == '__main__':
    main()
