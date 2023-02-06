#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryStatsRecord,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    ModelGroup,
    NewShopRating,
    Offer,
    RegionalModel,
    Shop,
    UrlType,
    YamarecCategoryPartition,
    YamarecFeaturePartition,
    YamarecMatchingPartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import main
from core.matcher import GreaterEq, NoKey, NotEmpty, Absent
from core.types.taxes import Vat, Tax
from core.dj import DjModel

from simple_testcase import SimpleTestCase, create_model_with_default_offer, create_model


def gen_waremd5(hyperid, waremd5, price):
    if waremd5 is not None:
        return waremd5
    return (str(hyperid) + str(int(price * 100000)) + "1" * 21)[:21] + "w"


def create_model_without_default_offer(model_id):
    m = create_model(hyperid=model_id)
    m["offers"] = {"items": NoKey("items")}
    return m


class T(SimpleTestCase):
    @classmethod
    def register_default_offer_for(cls, hyperid, hid=333, waremd5=None, model=None, cluster=None):
        if cluster is not None:
            cls.index.vclusters.append(cluster)
        else:
            m = model if model is not None else Model(hyperid=hyperid, hid=hid)
            cls.index.models.append(m)
        # main default offer
        cls.index.offers += [
            Offer(
                hyperid=hyperid,
                fesh=1,
                price=50,
                waremd5=gen_waremd5(hyperid=hyperid, waremd5=waremd5, price=50),
                cpa=Offer.CPA_NO,
                discount=5,
            ),
        ]
        # if expected waremd5 id not defined, we can make noisy offers
        if waremd5 is None:
            cls.index.offers += [
                Offer(
                    hyperid=hyperid,
                    fesh=1,
                    price=100,
                    waremd5=gen_waremd5(hyperid=hyperid, waremd5=None, price=100),
                    cpa=Offer.CPA_REAL,
                    discount=5,
                ),
                Offer(
                    hyperid=hyperid,
                    fesh=2,
                    price=300,
                    waremd5=gen_waremd5(hyperid=hyperid, waremd5=None, price=300),
                    cpa=Offer.CPA_REAL,
                    discount=5,
                ),
            ]

    @classmethod
    def register_models_without_default_offer(cls, ids, hids=None):
        h = hids if hids is not None else [None] * len(ids)
        cls.index.models += [Model(hyperid=x[0], hid=x[1]) for x in zip(ids, h)]
        cls.index.regional_models += [RegionalModel(hyperid=hyperid, offers=100500) for hyperid in ids]

    @classmethod
    def register_groups_with_default_offer(
        cls, modification_sets, group_ids, analog_lists=None, prices=None, discounts=None, hids=None
    ):
        """
        Добавляются в индекс группы и их модификации
        Для модификаций добавляется дефолтный оффер
        Несколько модификации групповой модели полезны для проверки выбора модификации
        при маппинге оффера к групповой модели
        Офферы модификаций для групп ранжируются с точки зрения ДО в порядке,
        в котором переданы их идентификаторы в modification_sets (за счет ts)
        """
        _prices = [100500] * len(group_ids) if prices is None else prices
        _hids = [None] * len(group_ids) if hids is None else hids
        _discounts = [None] * len(group_ids) if discounts is None else discounts
        _analog_lists = [[]] * len(group_ids) if analog_lists is None else analog_lists
        for modification_ids, group_id, analogs, hid, price, discount in zip(
            modification_sets, group_ids, _analog_lists, _hids, _prices, _discounts
        ):
            # model group
            cls.index.model_groups.append(ModelGroup(hyperid=group_id, hid=hid, analogs=analogs))
            # modifications with DO
            ids = [modification_ids] if type(modification_ids) == int else modification_ids
            cls.index.models += [
                Model(hyperid=modification_id, hid=hid, group_hyperid=group_id) for modification_id in ids
            ]
            cls.index.offers += [
                Offer(
                    randx=1000 - i % 1000,
                    fesh=11,
                    hyperid=modification_id,
                    price=price,
                    waremd5=gen_waremd5(hyperid=modification_id, waremd5=None, price=price),
                    discount=discount,
                )
                for i, modification_id in enumerate(ids)
            ]

    @classmethod
    def prepare(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.settings.rgb_blue_is_cpa = True

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=3.0), cpa=Shop.CPA_REAL),
            Shop(fesh=2, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=4.0), cpa=Shop.CPA_REAL),
            Shop(fesh=11, regions=[11]),
        ]
        cls.register_default_offer_for(hyperid=101)
        cls.crypta.on_request_profile(yandexuid="1001").respond(features=[])

    def check_default_offer_in(
        self, response, hyperid=101, waremd5=None, price=300, fesh=2, negate=False, test_show=True, test_urls=None
    ):
        factory = create_model_with_default_offer
        if negate:

            def factory(model_id, waremd5, price, fesh):
                return create_model_without_default_offer(hyperid)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    factory(
                        model_id=hyperid,
                        waremd5=gen_waremd5(hyperid=hyperid, waremd5=waremd5, price=price),
                        price=price,
                        fesh=fesh,
                    )
                ]
            },
        )
        if test_show and not negate:
            self.show_log.expect(
                hyper_id=hyperid, shop_id=99999999, ware_md5=gen_waremd5(hyperid=hyperid, waremd5=waremd5, price=price)
            )
        if (test_urls is not None) and (not negate):
            url_response = factory(
                model_id=hyperid,
                waremd5=gen_waremd5(hyperid=hyperid, waremd5=waremd5, price=price),
                price=price,
                fesh=fesh,
            )
            assert "offers" in url_response
            assert "items" in url_response["offers"]
            assert len(url_response["offers"]["items"]) == 1
            if test_urls:
                url_response["offers"]["items"][0]["urls"] = {
                    'direct': NotEmpty(),
                    'offercard': NotEmpty(),
                }
            else:
                url_response["offers"]["items"][0]["urls"] = {
                    'direct': NotEmpty(),
                    'offercard': Absent(),
                }
            self.assertFragmentIn(response, url_response)

    def check_default_offer(
        self, query, hyperid=101, waremd5=None, price=300, fesh=2, negate=False, test_show=True, test_urls=True
    ):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче
        """
        response = self.report.request_json(query)
        self.check_default_offer_in(
            response=response,
            hyperid=hyperid,
            waremd5=waremd5,
            price=price,
            fesh=fesh,
            negate=negate,
            test_show=test_show,
        )
        if test_urls and not negate:
            self.check_default_offer_in(
                response=response,
                hyperid=hyperid,
                waremd5=waremd5,
                price=price,
                fesh=fesh,
                negate=negate,
                test_show=False,
                test_urls=False,
            )
            url_query = query + '&show-urls=offercard'
            self.check_default_offer_in(
                response=self.report.request_json(url_query),
                hyperid=hyperid,
                waremd5=waremd5,
                price=price,
                fesh=fesh,
                negate=negate,
                test_show=False,
                test_urls=False,
            )
            url_query += '&rearr-factors=market_default_offer_keep_urls=1'
            self.check_default_offer_in(
                response=self.report.request_json(url_query),
                hyperid=hyperid,
                waremd5=waremd5,
                price=price,
                fesh=fesh,
                negate=negate,
                test_show=False,
                test_urls=True,
            )
        return response

    def check_default_offer_in_groups(self, query, modification_ids, group_ids, prices=None, negate=False):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче для группы
        """
        _prices = [100500] * len(group_ids) if prices is None else prices
        response = self.report.request_json(query + "&rids=11" + "&debug=da")
        if negate:
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "product",
                                "type": "group",
                                "id": group_id,  # searched model
                                "offers": {"items": NoKey("items")},  # no default offers
                            }
                            for modification_id, group_id, price in zip(modification_ids, group_ids, _prices)
                        ]
                    }
                },
                preserve_order=False,
            )
        else:
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "product",
                                "type": "group",
                                "id": group_id,  # searched model
                                "offers": {
                                    "items": [
                                        {  # default offer
                                            "entity": "offer",
                                            "wareId": gen_waremd5(hyperid=modification_id, waremd5=None, price=price),
                                            "prices": {"value": str(price)},
                                            "shop": {"id": 11},
                                            "model": {
                                                "id": modification_id,  # actual modification model with default offer
                                                "parentId": group_id,
                                            },
                                        },
                                    ]
                                },
                            }
                            for modification_id, group_id, price in zip(modification_ids, group_ids, _prices)
                        ]
                    }
                },
                preserve_order=False,
            )

        return response

    @classmethod
    def prepare_best_deals(cls):
        """
        Модель и офферы для неё
        Один из оферов очень дорогой, но только он - со скидкой
        """
        cls.index.models += [
            Model(hyperid=150, hid=335),
            Model(hyperid=158, hid=335),
            Model(hyperid=159, hid=335),
        ]
        cls.index.shops += [
            Shop(fesh=5, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=4.0), cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(hyperid=150, fesh=1, price=100, cpa=Offer.CPA_REAL, discount=5),
            Offer(hyperid=150, fesh=5, price=200, cpa=Offer.CPA_REAL, discount=None),
            Offer(hyperid=150, waremd5="RuDq59UhfHnD72tMISuMbw", fesh=5, price=1000, cpa=Offer.CPA_REAL, discount=5),
            Offer(hyperid=158, fesh=5, price=100, cpa=Offer.CPA_REAL, discount=None),
        ]
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:", item_count=1000).respond(
            {"models": ['150']}
        )
        cls.index.hypertree += [
            HyperCategory(hid=335, output_type=HyperCategoryType.GURU),
        ]
        """
        Случай групповой модели
        """
        # В текущей реализации bestdeals не выдаёт групповые модели (что неправильно)

    def test_best_deals(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче bestdeals
        и выбор из всех офферов в первую очередь того, что со скидкой:
        Также проверяем, что отфильтровываются модели, для которых нет дефолтного офера со скидкой
        """
        self.error_log.ignore('Personal category config is not available')
        self.check_default_offer(
            query="place=bestdeals&hid=335", hyperid=150, price=1000, waremd5="RuDq59UhfHnD72tMISuMbw", fesh=5
        )
        self.assertModelsNotInResponse(query="place=bestdeals&hid=335", ids=[158, 159])

        """
        Не проверяем вставку дефолтного оффера для групповой модели, поскольку
        bestdeals не работает с групповыми моделями
        """

    @classmethod
    def prepare_popular_products(cls):
        """
        Конфигурация для получения хотя бы одной модели на выдаче popular_products
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=9200,
                children=[
                    HyperCategory(hid=9201, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9202, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=91, hid=9201),
        ]

        cls.bigb.on_request(yandexuid='1009', client='merch-machine').respond(counters=[], keywords=[])
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1009').respond({'models': ['91', '92']})
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1009', item_count=40, with_timestamps=True
        ).respond({'models': ['91', '92'], 'timestamps': ['2', '1']})
        cls.index.offers += [
            Offer(waremd5=gen_waremd5(hyperid=91, waremd5=None, price=400), fesh=11, hyperid=91, price=400),
        ]

        """
        Случай групповой модели
        """
        cls.register_groups_with_default_offer(group_ids=[92], modification_sets=[[93]], hids=[9202])

    def test_popular_products(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче popular_products
        """
        self.check_default_offer(
            query="place=popular_products&yandexuid=1009&hid=9200&rearr-factors=switch_popular_products_to_dj_no_nid_check=0",
            hyperid=91,
            price=400,
            fesh=11,
        )
        self.check_default_offer_in_groups(
            query="place=popular_products&yandexuid=1009&hid=9200&rearr-factors=switch_popular_products_to_dj_no_nid_check=0",
            group_ids=[92],
            modification_ids=[93],
        )

    @classmethod
    def prepare_personal_category_models(cls):
        """
        Конфигурация для получения хотя бы одной модели на выдаче personalcategorymodels
        """
        cls.register_default_offer_for(hyperid=110, hid=334, model=Model(hyperid=110, hid=334, model_clicks=100))
        cls.index.regional_models += [
            RegionalModel(hyperid=110, offers=100),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=334, output_type=HyperCategoryType.GURU),
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [334, 1],
                        ],
                        splits=[{'split': 'personalcategorymodels'}],
                    ),
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[
                            [491, 1],
                        ],
                        splits=[{'split': 'personalcategorymodels_groups'}],
                    ),
                ],
            ),
        ]
        """
        Случай групповой модели не рассматриваем, поскольку плэйс не ищет групповые модели
        """

    def test_personal_category_models(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче personalcategorymodels
        """
        self.check_default_offer(
            query="place=personalcategorymodels&rearr-factors=split=personalcategorymodels", hyperid=110
        )

    @classmethod
    def _reg_ichwill_request(cls, user_id, models, item_count=40):
        cls.recommender.on_request_models_of_interest(user_id=user_id, item_count=item_count, version=4).respond(
            {'models': map(str, models), 'timestamps': map(str, reversed(list(range(len(models)))))}
        )
        cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(
            counters=[], keywords=[]
        )

    @classmethod
    def prepare_products_by_history(cls):
        """
        Конфигурация для получения хотя бы одной модели на выдаче products_by_history
        """
        cls._reg_ichwill_request("yandexuid:1002", [103, 104, 105, 111])
        cls.index.regional_models += [
            RegionalModel(hyperid=103, offers=123),
            RegionalModel(hyperid=104, offers=123),
            RegionalModel(hyperid=105, offers=123),
            RegionalModel(hyperid=111, offers=123),
            RegionalModel(hyperid=203, offers=123),
            RegionalModel(hyperid=204, offers=123),
            RegionalModel(hyperid=205, offers=123),
            RegionalModel(hyperid=211, offers=123),
        ]
        cls.register_default_offer_for(hyperid=103, hid=334, model=Model(hyperid=103, analogs=[104, 105]))
        cls.register_default_offer_for(hyperid=203, hid=334, model=Model(hyperid=203, analogs=[204, 205]))
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='5002').respond(
            [DjModel(id='203'), DjModel(id='204'), DjModel(id='205'), DjModel(id='211')]
        )

        """
        Случай групповых моделей
        """
        cls.index.regional_models += [
            RegionalModel(hyperid=106, offers=123, rids=[11]),
            RegionalModel(hyperid=107, offers=123, rids=[11]),
            RegionalModel(hyperid=108, offers=123, rids=[11]),
            RegionalModel(hyperid=109, offers=123, rids=[11]),
            RegionalModel(hyperid=206, offers=123, rids=[11]),
            RegionalModel(hyperid=207, offers=123, rids=[11]),
            RegionalModel(hyperid=208, offers=123, rids=[11]),
            RegionalModel(hyperid=209, offers=123, rids=[11]),
        ]
        cls._reg_ichwill_request("yandexuid:1003", list(range(106, 110)))
        cls.register_groups_with_default_offer(
            group_ids=[106], modification_sets=[[107, 108]], analog_lists=[[109]], hids=[334]
        )
        cls.register_groups_with_default_offer(
            group_ids=[206], modification_sets=[[207, 208]], analog_lists=[[209]], hids=[334]
        )
        dj_models = [DjModel(id=str(modelid)) for modelid in range(206, 210)]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='5003').respond(dj_models)

    def test_products_by_history(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче products_by_history
        """
        self.check_default_offer(
            query="place=products_by_history&yandexuid=1002&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            hyperid=103,
        )
        self.check_default_offer(query="place=products_by_history&yandexuid=5002", hyperid=203)
        self.check_default_offer_in_groups(
            "place=products_by_history&yandexuid=1003&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            group_ids=[106],
            modification_ids=[107],
        )
        self.check_default_offer_in_groups(
            "place=products_by_history&yandexuid=5003", group_ids=[206], modification_ids=[207]
        )

    @classmethod
    def prepare_better_price(cls):
        """
        Конфигурация для получения одной модели на выдаче better_price в режиме
            фильтрации по цене
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            "filter-by-price": "1",
                        },
                        splits=[{"split": "better_price"}],
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1001", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 161, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 162, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 163, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 164, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 165, "price": 50.1111, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )

        """
        Дефолтные офферы
        """
        cls.register_default_offer_for(hyperid=161, hid=666)
        waremd5s = {
            162: "bpQ3a9LXZAl_Kz34vaOpSg",
            163: "V5Y7eJkIdDh0sMeCecijqw",
            164: "xzFUFhFuAvI1sVcwDnxXPQ",
            165: "1GLQ3RgNgEoQ_6A1LRrQuQ",
        }
        for hyperid, waremd5 in waremd5s.items():
            cls.register_default_offer_for(hyperid=hyperid, waremd5=waremd5, hid=666)

        """
        Лучший магазин
        """
        Shop(fesh=9, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=4.5), cpa=Shop.CPA_REAL),
        """
        Оффер из лучшего магазина и с нелучшей ценой для 161й модели
        (но эта цена соответствует фильтру истории пользователя)
        И то же самое - для 162й
        """
        cls.index.offers += [
            Offer(hyperid=161, fesh=9, price=140, cpa=Offer.CPA_REAL, override_cpa_check=True, discount=5),
            Offer(hyperid=162, fesh=9, price=140, cpa=Offer.CPA_REAL, override_cpa_check=True, discount=5),
        ]

        """
        Лучшие офферы с копейками для некоторых моделей
        """
        cls.index.offers += [
            Offer(hyperid=162, waremd5="gpQxwKBuLtj5OIlRrvGwTw", fesh=1, price=49.0496, cpa=Offer.CPA_NO, discount=5),
            Offer(hyperid=163, waremd5="91t1fTRZw-k-mN2re5A5OA", fesh=1, price=49.19, cpa=Offer.CPA_NO, discount=5),
            Offer(hyperid=164, waremd5="qtZDmKlp7DGGgA1BL6erMQ", fesh=1, price=49.7, cpa=Offer.CPA_NO, discount=5),
            # Оффер с ценой чуть выше цены из истории и подходящий оффер
            Offer(hyperid=165, waremd5="AHZO1SOUQX-bEaFqMBPdOQ", fesh=1, price=50.112, cpa=Offer.CPA_NO, discount=5),
            Offer(hyperid=165, waremd5="rZt32gv6_zQKoq7OqTXqeQ", fesh=1, price=49, cpa=Offer.CPA_NO, discount=5),
        ]

    def test_better_price(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче better_price
        и работу фильтра по цене, требуемого в данном случае:
            для better_price в режиме фильтрации по цене
            требуется дефолтный оффер с ценой:
                меньше цены этой модели в истории пользователя
            необходимо, чтобы цена дефолтного оффера совпадала с ourPrice в блоке betterPrice
            также проверяем кейс с копейками в цене оффера
        """
        response = self.check_default_offer(
            query="place=better_price&rids=213&yandexuid=1001&rearr-factors=split=better_price&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0",
            hyperid=161,
            price=50,
            fesh=1,
        )

        self.check_default_offer_in(response=response, hyperid=162, waremd5="gpQxwKBuLtj5OIlRrvGwTw", price=49, fesh=1)
        self.check_default_offer_in(response=response, hyperid=163, waremd5="91t1fTRZw-k-mN2re5A5OA", price=49, fesh=1)
        self.check_default_offer_in(response=response, hyperid=164, waremd5="qtZDmKlp7DGGgA1BL6erMQ", price=50, fesh=1)
        self.check_default_offer_in(response=response, hyperid=165, waremd5="rZt32gv6_zQKoq7OqTXqeQ", price=49, fesh=1)

        """
        Блок "betterPrice"
        """
        self.assertFragmentIn(response, create_model_with_better_price(hyperid=162, ourPrice=49, theirPrice=150))
        self.assertFragmentIn(response, create_model_with_better_price(hyperid=163, ourPrice=49, theirPrice=150))
        self.assertFragmentIn(response, create_model_with_better_price(hyperid=164, ourPrice=50, theirPrice=150))
        self.assertFragmentIn(response, create_model_with_better_price(hyperid=165, ourPrice=49, theirPrice=50))

    @classmethod
    def prepare_better_price_groups(cls):
        """
        Данные для place=better_price. Случай групповых моделей
        """
        cls.recommender.on_request_we_have_cheaper(user_id="yandexuid:1004", item_count=100,).respond(
            {
                "we_have_cheaper": [
                    {"model_id": 220, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 221, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 222, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                    {"model_id": 223, "price": 150.0, "success_requests_share": 0.1, "timestamp": "1495206745"},
                ]
            }
        )
        cls.register_groups_with_default_offer(group_ids=[220], modification_sets=[[221, 222, 223]], prices=[100])

    def test_better_price_groups(self):
        """
        Проверка вставки дефолтного оффера для place=better_price. Случай групповых моделей
        """
        self.check_default_offer_in_groups(
            query="place=better_price&yandexuid=1004&rearr-factors=split=better_price&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0",
            group_ids=[220],
            modification_ids=[221],
            prices=[100],
        )

    @classmethod
    def prepare_product_accessories(cls):
        """
        Данные для непустой выдачи product_accessories и ДО
        """
        cls.register_default_offer_for(hyperid=170, hid=336, model=Model(hyperid=170, accessories=[171, 172, 173, 174]))
        cls.register_default_offer_for(hyperid=171, hid=337)
        cls.register_default_offer_for(hyperid=172, hid=338)
        cls.register_models_without_default_offer(ids=[173, 174], hids=[338, 338])
        """
        Случай групповой модели
        """
        cls.index.models.append(Model(hyperid=177, accessories=[175]))
        cls.register_groups_with_default_offer(group_ids=[175], modification_sets=[[176, 178, 179]], hids=[339])

    def test_product_accessories(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче product_accessories
        """
        response = self.report.request_json(
            "place=product_accessories&hyperid=170&rearr-factors=market_disable_product_accessories=0"
        )
        self.check_default_offer_in(response=response, hyperid=171)
        self.check_default_offer_in(response=response, hyperid=172)
        self.check_default_offer_in_groups(
            query="place=product_accessories&hyperid=177&rearr-factors=market_disable_product_accessories=0",
            group_ids=[175],
            modification_ids=[176],
        )

    @classmethod
    def prepare_modelinfo(cls):
        """
        Данные для выдачи modelinfo
        """
        cls.register_default_offer_for(hyperid=181, hid=341)
        """
        Случай групповой модели
        """
        cls.register_groups_with_default_offer(group_ids=[182], modification_sets=[183], hids=[341])

    def test_modelinfo(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче modelinfo при запросе с флагом use-default-offers
        """
        self.check_default_offer(
            query="place=modelinfo&rids=213&hyperid=181&use-default-offers=1", hyperid=181, test_show=False
        )
        self.check_default_offer_in_groups(
            query="place=modelinfo&hyperid=182&use-default-offers=1", group_ids=[182], modification_ids=[183]
        )
        """
        Флаг выключен
        """
        self.check_default_offer(
            query="place=modelinfo&rids=213&hyperid=181&use-default-offers=0", hyperid=181, negate=True, test_show=False
        )
        """
        Флаг выключен по умолчанию
        """
        self.check_default_offer(
            query="place=modelinfo&rids=213&hyperid=181", hyperid=181, negate=True, test_show=False
        )
        self.check_default_offer_in_groups(
            query="place=modelinfo&hyperid=182", group_ids=[182], modification_ids=[183], negate=True
        )

    def test_prime(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче prime при запросе с флагом use-default-offers
        """
        url = "place=prime&rids=213&hid=341"
        self.check_default_offer(query=url + "&use-default-offers=1", hyperid=181)
        """
        Флаг выключен
        """
        self.check_default_offer(query=url + "&use-default-offers=0", hyperid=181, negate=True)
        """
        Флаг выключен по умолчанию
        """
        self.check_default_offer(query=url, hyperid=181, negate=True)

    @classmethod
    def prepare_also_viewed(cls):
        """
        Данные для place=also_viewed
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{"split": "also_viewed"}]),
                    YamarecSettingPartition(params={'version': '2'}, splits=[{"split": "also_viewed_groups"}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=190, item_count=1000, version='1').respond(
            {'models': ['191', '192', '193']}
        )
        cls.register_default_offer_for(hyperid=191, hid=361)
        cls.register_models_without_default_offer(ids=[192])
        """
        Случай групповой модели
        """
        cls.recommender.on_request_accessory_models(model_id=190, item_count=1000, version='2').respond(
            {'models': ['194']}
        )
        cls.register_groups_with_default_offer(group_ids=[194], modification_sets=[[195, 196, 197]], hids=[361])

    def test_also_viewed(self):
        """
        Проверяем наличие вставки дефолтного оффера в выдаче also_viewed
        """
        response = self.report.request_json("place=also_viewed&rearr-factors=split=also_viewed&hyperid=190")
        self.check_default_offer_in(response=response, hyperid=191)
        self.check_default_offer_in_groups(
            query="place=also_viewed&rearr-factors=split=also_viewed_groups&hyperid=190",
            group_ids=[194],
            modification_ids=[195],
        )
        """
        В выдачу не должны попадать модели без ДО
        """
        self.assertModelsNotIn(response=response, ids=[173, 174])

    @classmethod
    def prepare_share_default_offer(cls):
        """
        Данные о групповых моделях и модификациях
        для проверки случая конфликтов при мапинге дефолтных офферов
        модификаций на групповые модели и их же на сами модификации
        на примере place=products_by_history
        Здесь рассчитываем на yamarec-конфигурацию из prepare_products_by_history
        """

        """
        place=products_by_history
        """
        cls.index.regional_models += [
            RegionalModel(hyperid=241, offers=123, rids=[11]),
            RegionalModel(hyperid=242, offers=123, rids=[11]),
        ]
        cls._reg_ichwill_request("yandexuid:1005", list(range(241, 246)))
        dj_models = [DjModel(id=str(modelid)) for modelid in range(241, 246)]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='2005').respond(dj_models)
        cls.register_groups_with_default_offer(
            group_ids=[241],
            modification_sets=[[242, 243, 244, 245]],
            analog_lists=[[242, 243, 244]],
            hids=[441],
            prices=[100],
        )
        """
        Еще один кейс place=products_by_history
        """
        cls.index.models += [Model(hyperid=256, analogs=[257, 258, 259])]
        cls.index.regional_models += [
            RegionalModel(hyperid=251, offers=123, rids=[11]),
            RegionalModel(hyperid=252, offers=123, rids=[11]),
            RegionalModel(hyperid=253, offers=123, rids=[11]),
            RegionalModel(hyperid=254, offers=123, rids=[11]),
            RegionalModel(hyperid=255, offers=123, rids=[11]),
            RegionalModel(hyperid=256, offers=123, rids=[11]),
            RegionalModel(hyperid=257, offers=123, rids=[11]),
            RegionalModel(hyperid=258, offers=123, rids=[11]),
            RegionalModel(hyperid=259, offers=123, rids=[11]),
        ]
        cls.index.offers += [
            Offer(hyperid=254, fesh=11, price=130, waremd5=gen_waremd5(waremd5=None, hyperid=254, price=130)),
            Offer(hyperid=256, fesh=11, price=130, waremd5=gen_waremd5(waremd5=None, hyperid=256, price=130)),
            Offer(hyperid=257, fesh=11, price=130, waremd5=gen_waremd5(waremd5=None, hyperid=257, price=130)),
            Offer(hyperid=258, fesh=11, price=130, waremd5=gen_waremd5(waremd5=None, hyperid=258, price=130)),
            Offer(hyperid=259, fesh=11, price=130, waremd5=gen_waremd5(waremd5=None, hyperid=259, price=130)),
        ]
        cls._reg_ichwill_request("yandexuid:1006", list(range(251, 260)))
        dj_models = [DjModel(id=str(modelid)) for modelid in range(251, 260)]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='2006').respond(dj_models)
        cls.register_groups_with_default_offer(
            group_ids=[251],
            modification_sets=[[252, 253, 255]],
            analog_lists=[[253, 252, 254]],
            hids=[451],
            prices=[120],
        )
        """
        Случай, когда в запросе на ДО на первом месте модификация, и только после неё - её группа
        на примере product_accessories
        """
        cls.index.models += [
            Model(hyperid=261, accessories=[264, 263, 262]),
        ]
        cls.register_groups_with_default_offer(
            group_ids=[263], modification_sets=[[262, 264]], hids=[461], prices=[140]
        )

    def test_share_default_offer(self):
        """
        Проверяем наличие вставки дефолтного оффера для групповой модели,
        когда подходящий дефолтный оффер есть дефолтный оффер модификации этой модели,
        также присутствующей в выдаче
        """

        """
        products_by_history
        В выдаче должны быть групповая модель 241 и её модификации 242, 243
        Дефолтный оффер для группы - оффер модели-модификации 242
        """
        response = self.check_default_offer_in_groups(
            query="place=products_by_history&yandexuid=1005&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            group_ids=[241],
            modification_ids=[242],
            prices=[100],
        )
        response = self.check_default_offer_in_groups(
            query="place=products_by_history&yandexuid=2005", group_ids=[241], modification_ids=[242], prices=[100]
        )
        """
        Модификация 242 также должна выводиться со своим дефолтным оффером
        """
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "type": "modification",
                            "id": 242,
                            "offers": {
                                "items": [
                                    {  # default offer
                                        "entity": "offer",
                                        "wareId": gen_waremd5(hyperid=242, waremd5=None, price=100),
                                        "prices": {"value": "100"},
                                        "shop": {"id": 11},
                                        "model": {"id": 242, "parentId": 241},
                                    },
                                ]
                            },
                        }
                    ]
                }
            },
            preserve_order=False,
        )

        """
        Похожий кейс с большим количеством модификаций,
        включая тех, которые должны быть найдены без родительской группы,
        а также с простой моделью
        Проверяем возможные проблемы при группировке офферов на базовом поиске дефолтного оффера

        Сначала групповая модель в результатах выдачи с дефолтным оффером к одной её модификации
        """
        response = self.check_default_offer_in_groups(
            query="place=products_by_history&yandexuid=1006&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            group_ids=[251],
            modification_ids=[252],
            prices=[120],
        )
        response = self.check_default_offer_in_groups(
            query="place=products_by_history&yandexuid=2006", group_ids=[251], modification_ids=[252], prices=[120]
        )
        """
        Немодификации
        """
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "type": "model",
                            "id": hyperid,
                            "offers": {
                                "items": [
                                    {  # default offer
                                        "entity": "offer",
                                        "wareId": gen_waremd5(hyperid=hyperid, waremd5=None, price=130),
                                        "prices": {"value": "130"},
                                        "shop": {"id": 11},
                                        "model": {"id": hyperid, "parentId": NoKey("parentId")},
                                    },
                                ]
                            },
                        }
                        for hyperid in [254, 256, 257, 258]
                    ]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )
        """
        Модификации в результатах выдачи
        """
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "type": "modification",
                            "id": hyperid,
                            "offers": {
                                "items": [
                                    {  # default offer
                                        "entity": "offer",
                                        "wareId": gen_waremd5(hyperid=hyperid, waremd5=None, price=120),
                                        "prices": {"value": "120"},
                                        "shop": {"id": 11},
                                        "model": {"id": hyperid, "parentId": 251},
                                    },
                                ]
                            },
                        }
                        for hyperid in [252, 253]
                    ]
                }
            },
            preserve_order=False,
        )

        """
        Проверяем, что мапинг групповой модели к офферу к её модификации, найденному для этой групповой модели,
        работает корректно даже если в запрос на ДО группа поступает по порядку после другой её модификации,
        также имеющей ДО
        """
        response = self.check_default_offer_in_groups(
            query="place=product_accessories&hyperid=261&rearr-factors=market_disable_product_accessories=0",
            group_ids=[263],
            modification_ids=[262],
            prices=[140],
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "product",
                            "type": "modification",
                            "id": hyperid,
                            "offers": {
                                "items": [
                                    {  # default offer
                                        "entity": "offer",
                                        "wareId": gen_waremd5(hyperid=hyperid, waremd5=None, price=140),
                                        "prices": {"value": "140"},
                                        "shop": {"id": 11},
                                        "model": {"id": hyperid, "parentId": 263},
                                    },
                                ]
                            },
                        }
                        for hyperid in [262, 264]
                    ]
                }
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_cpa_urls(cls):
        cls.index.hypertree += [
            HyperCategory(hid=9999, output_type=HyperCategoryType.GURU),
        ]
        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(hid=9999, region=213, n_offers=1, n_discounts=1),
        ]
        cls.index.models += [
            Model(hyperid=hyperid, hid=9999, randx=1000 - hyperid % 10)  # ts to make 9999001 ranked first
            for hyperid in range(9999001, 9999005)
        ]
        cls.index.models += [Model(hyperid=9999005, hid=9999, accessories=[9999001], analogs=[9999001], randx=1)]
        # shops
        cls.index.shops += [
            Shop(
                fesh=99,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                blue='REAL',
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=100,
                datafeed_id=2,
                priority_region=213,
                name='blue_shop_2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                warehouse_id=145,
            ),
        ]
        cls.index.offers += [
            Offer(hyperid=hyperid, fesh=2, price=234, price_old=300, randx=1000 - hyperid % 1000)
            for hyperid in range(9999001, 9999006)
        ]
        blue_offers = [
            BlueOffer(price=234, price_old=300, vat=Vat.VAT_10, offerid='shop_sku_{}'.format(i), feedid=1)
            for i in range(9999001, 9999006)
        ]

        cls.index.mskus += [
            MarketSku(
                title='blue_market_sku_1',
                hyperid=hyperid,
                sku=10000000 + hyperid,
                blue_offers=[blue_offer],
                randx=1000 - hyperid % 1000,
            )
            for hyperid, blue_offer in zip(list(range(9999001, 9999006)), blue_offers)
        ]
        # products_by_history and personal categories based
        cls._reg_ichwill_request("yandexuid:1099", list(range(9999001, 9999005)))
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='1099').respond([DjModel(id='9999001'), DjModel(id='9999005')])
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:1099").respond({"models": ["9999001"]})
        # accessories

        # commonly_purchased categories
        personal_category_filter = [
            90606,
            7683677,
            7683675,
            13462769,
            91330,
            13337703,
            15720046,
            15720050,
            91331,
            15720051,
            15720045,
            91329,
            15720037,
            15720039,
            16147683,
            91382,
            15714713,
            15714708,
            15714698,
            91388,
            14698852,
            15557928,
            15714682,
            15714680,
            15714675,
            15714671,
            15720042,
            91419,
            91420,
            14621180,
            91422,
            91430,
            91397,
            15726404,
            15726402,
            91352,
            15726412,
            15726410,
            15726408,
            15719803,
            15719820,
            15719828,
            15719799,
            91427,
            91344,
            15714127,
            15714122,
            91342,
            15714129,
            91340,
            91343,
            14706137,
            15714542,
            15714113,
            15714106,
            15727944,
            15728039,
            91345,
            15727473,
            15727886,
            15727888,
            15727896,
            15727878,
            15727884,
            15727954,
            91339,
            15727967,
            91421,
            91408,
            15697700,
            13041400,
            15934091,
            16088924,
            15770939,
            4922657,
            15770934,
            13518990,
            14245094,
            12718081,
            15959385,
            15963644,
            13212408,
            15685457,
            15685787,
            13212400,
            12718223,
            15999360,
            15963668,
            15999143,
            12718332,
            12714755,
            12718255,
            12766642,
            12704208,
            15971367,
            12714763,
            12704139,
            15962102,
            818945,
            8480736,
            13277104,
            13277088,
            13277108,
            13277089,
            13276918,
            13276920,
            14995813,
            14995788,
            4748066,
            4748064,
            4748062,
            8480752,
            8510396,
            14996541,
            4748057,
            14996686,
            8480754,
            13276669,
            4748072,
            4748074,
            13276667,
            14996659,
            4748078,
            14994593,
            14990285,
            8480738,
            13244155,
            13239550,
            13240862,
            13239503,
            13239527,
            4854062,
            14993426,
            13239477,
            13239479,
            14993540,
            91184,
            14993483,
            15011042,
            8476101,
            8476102,
            8476110,
            8476103,
            8476097,
            8476098,
            8476539,
            8476100,
            8476099,
            13239041,
            13238924,
            14994948,
            8478954,
            14989778,
            4748058,
            13239135,
            14990252,
            13238994,
            13239089,
            15350596,
            6470214,
            8475961,
            13357269,
            13314796,
            15019493,
            91179,
            91180,
            13314795,
            13314823,
            14993676,
            13314841,
            8475955,
            14994526,
            14989707,
            4852774,
            4852773,
            13314855,
            14994695,
        ]

        universal_categories = [
            278374,
            13491643,
            91078,
            91335,
            91327,
            91423,
            15368134,
            16044621,
            16044387,
            16044466,
            16044416,
            15714731,
            91392,
            15726400,
            91332,
            818944,
            91346,
            15714135,
            15714102,
            16011677,
            16011796,
            16011704,
            15714105,
            982439,
            15720388,
            16099944,
            15697667,
            15697685,
            15697659,
            15697691,
            90689,
            13041431,
            13196790,
            13041429,
            15696738,
            13041430,
            90691,
            90688,
            13041456,
            90690,
            13041460,
            13041507,
            13041512,
            13041511,
            13041314,
            13041252,
            13277094,
            8480725,
            8480722,
            8480713,
            15927546,
            13243353,
            13239358,
            91183,
            91167,
            91176,
            16042844,
            14989652,
            91173,
            7693914,
            14995755,
            13334231,
            13314877,
            91174,
            15002303,
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_UNIVERSAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=universal_categories, splits=['*']),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_PERSONAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=personal_category_filter, splits=['*']),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'use-external': '0',
                            'use-local': '1',
                        },
                        splits=[{}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.MODEL_CARD_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.MATCHING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecMatchingPartition(
                        name='accessory_blue_market_matching', matching={9999005: [9999001]}, splits=[{}]
                    )
                ],
            ),
        ]

    def test_cpa_urls(self):
        """
        MARKETOUT-20902
        Проверка наличия cpa-ссылок в офферах внутри моделей в выдаче рекомендательных ручек для синего маркета
        """
        self.error_log.ignore("Personal category config is not available for user 1099")
        self.error_log.ignore("Unknown category ID")

        for q in [
            "place=products_by_history",
            "place=product_accessories&hyperid=9999005&rearr-factors=market_disable_product_accessories=0",
            "place=deals",
            "place=commonly_purchased&rearr-factors=turn_on_commonly_purchased=1",
        ]:
            response = self.report.request_json("{}&yandexuid=1099&rgb=blue&rids=213&show-urls=cpa".format(q))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'id': 9999001,
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'urls': {
                                            'cpa': NotEmpty(),
                                            'encrypted': Absent(),
                                        },
                                    }
                                ]
                            },
                        }
                    ]
                },
            )
            self.show_log_tskv.expect(
                url_type=UrlType.CPA,
                price=234,
                hyper_cat_id=9999,
                msku=19999001,
                rgb="BLUE",
                shop_id=99,
                is_blue_offer=1,
            )

        for q in [
            "place=products_by_history",
            "place=product_accessories&hyperid=9999005&rearr-factors=market_disable_product_accessories=0",
            "place=deals&show-personal=1&use-default-offers=1",
            "place=commonly_purchased&turn_on_commonly_purchased=1",
        ]:
            response = self.report.request_json(
                "{}&yandexuid=1099&rids=213&rgb=green&show-urls=encryptedmodel".format(q)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'id': 9999001,
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'urls': {
                                            'cpa': Absent(),
                                            'encrypted': NotEmpty(),
                                        },
                                    }
                                ]
                            },
                        }
                    ]
                },
            )

    def test_popular_products_cpa_urls(self):
        """
        Выносим тест popular_products отдельно из-за дедупликации с историей
        """
        response = self.report.request_json("place=popular_products&yandexuid=1099&rgb=blue&rids=213&show-urls=cpa")
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 9999005,
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'urls': {
                                        'cpa': NotEmpty(),
                                        'encrypted': Absent(),
                                    },
                                }
                            ]
                        },
                    }
                ]
            },
        )
        self.show_log_tskv.expect(
            url_type=UrlType.CPA, price=234, hyper_cat_id=9999, msku=19999005, rgb="BLUE", shop_id=99, is_blue_offer=1
        )

        response = self.report.request_json(
            "place=popular_products&yandexuid=1099&rids=213&rgb=green&show-urls=encryptedmodel"
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 9999001,
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'urls': {
                                        'cpa': Absent(),
                                        'encrypted': NotEmpty(),
                                    },
                                }
                            ]
                        },
                    }
                ]
            },
        )
        self.error_log.ignore("Personal category config is not available for user 1099")
        self.error_log.ignore("Unknown category ID")


def create_model_with_better_price(hyperid, ourPrice, theirPrice):
    return {
        "type": "model",
        "id": hyperid,
        "prices": {
            "betterPrice": {
                "ourPrice": str(ourPrice),
                "defaultOfferPriceTo": GreaterEq(str(ourPrice)),
                "theirPrice": str(theirPrice),
            }
        },
    }


if __name__ == '__main__':
    main()
