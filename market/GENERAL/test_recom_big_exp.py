#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    Offer,
    Shop,
    Tax,
    Vat,
    YamarecCategoryPartition,
    YamarecFeaturePartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.bigb import WeightedValue, BigBKeyword
from core.dj import DjModel
from core.matcher import ElementCount


RANDOMIZED_PLACE_MODELS = [
    (265357245, 90626),
    (285916715, 90626),
    (115753065, 90626),
    (245461126, 90626),
    (245461138, 90626),
    (291024114, 90626),
    (291024126, 90626),
    (298088276, 90626),
    (344134477, 90626),
    (344135325, 90626),
    (33155020, 91529),
    (1717555647, 2724669),
    (382714113, 2724669),
    (267342154, 2724669),
    (1723592852, 2724669),
    (12918772, 2724669),
    (55276657, 2724669),
    (202605006, 2724669),
    (165767632, 2724669),
    (165767631, 2724669),
    (150334049, 2724669),
    (14178340, 15148752),
    (14052766, 15148752),
    (13055974, 15148752),
    (1626877, 15148752),
]

HIDSET = set()
RANDOMIZED_PLACE_DISTINCT_HIDS = []
for _, hid in RANDOMIZED_PLACE_MODELS:
    if hid not in HIDSET:
        RANDOMIZED_PLACE_DISTINCT_HIDS.append(hid)
        HIDSET.add(hid)

COLDSTART_CATEGORIES = [90799, 13360751]


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=5, vat=Vat.VAT_10, offerid='Shop1_sku1', feedid=228760011, waremd5='Sku1Price5-IiLVm1Goleg', discount=50
    )
    sku2_offer1 = BlueOffer(
        price=50, vat=Vat.VAT_0, offerid='Shop1_sku2', feedid=228760011, waremd5='Sku1Price50-iLVm1Goleg'
    )
    sku3_offer1 = BlueOffer(
        price=500, vat=Vat.VAT_0, offerid='Shop1_sku3', feedid=228760011, waremd5='Sku1Price500-LVm1Goleg'
    )


class T(TestCase):
    """
    MARKETOUT-19834: Большой обратный эксперимент рекомендаций
    """

    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False
        cls.settings.rgb_blue_is_cpa = True

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]
        cls.index.models += [
            Model(hyperid=1, hid=101),
            Model(hyperid=2, hid=201),
            Model(hyperid=99, hid=901),
        ]
        cls.index.offers += [
            Offer(hyperid=1, fesh=1),
            Offer(hyperid=2, fesh=1),
            Offer(hyperid=99, fesh=1),
        ]

        # coldstart
        cls.index.hypertree += [
            HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in COLDSTART_CATEGORIES
        ]
        cls.index.models += [
            Model(hyperid=11, hid=90799),
            Model(hyperid=12, hid=13360751),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=11),
            Offer(fesh=1, hyperid=12),
        ]

    @classmethod
    def prepare_popular_products(cls):
        cls.index.hypertree += [
            HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
            HyperCategory(
                hid=200,
                children=[
                    HyperCategory(hid=201, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.POPULAR_MODELS,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['model_id', 'category_id', 'position'],
                        feature_keys=['model_id', 'category_id'],
                        features=[
                            [99, 901, 0],
                        ],
                        splits=[{}],
                    ),
                ],
            ),
        ]
        # personal categories ordering
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1001').respond({'models': ['2']})

        # models for randomized place
        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in RANDOMIZED_PLACE_MODELS]
        cls.index.offers += [Offer(fesh=1, hyperid=hyperid) for hyperid, _ in RANDOMIZED_PLACE_MODELS]
        cls.index.mskus += [
            MarketSku(hyperid=hyperid, sku=hyperid * 10 + 1, blue_offers=[BlueOffer(feedid=228760011)])
            for hyperid, _ in RANDOMIZED_PLACE_MODELS
        ]

    @classmethod
    def _reg_ichwill_request(cls, user_id, models, item_count=40):
        cls.recommender.on_request_models_of_interest(
            user_id=user_id, item_count=item_count, with_timestamps=True, version=4
        ).respond({'models': map(str, models), 'timestamps': map(str, list(range(len(models), 0, -1)))})
        cls.bigb.on_request(yandexuid=user_id.replace('yandexuid:', ''), client='merch-machine').respond(counters=[])

    @classmethod
    def prepare_index_page_omm(cls):
        DEFAULT_PROFILE = [
            BigBKeyword(
                id=BigBKeyword.GENDER,
                weighted_uint_values=[
                    WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
                    WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
                ],
            ),
        ]
        cls.bigb.on_request(yandexuid='1001', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.dj.on_request(yandexuid='1001', exp='blue_attractive_models').respond(
            models=[
                DjModel(id=1, title='model'),
                DjModel(id=5001, title='model'),
                DjModel(id=5002, title='model'),
                DjModel(id=5003, title='model'),
            ]
        )

    @classmethod
    def prepare_accessories(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            )
        ]
        cls.index.models += [
            Model(hyperid=21, hid=301),
            Model(hyperid=22, hid=302),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=21),
            Offer(fesh=1, hyperid=22),
        ]
        cls.recommender.on_request_accessory_models(model_id=21, item_count=1000, version='1').respond(
            {'models': ['22']}
        )

    @classmethod
    def prepare_also_viewed(cls):
        cls.index.models += [
            Model(hyperid=31),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=31),
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='1').respond(
            {'models': ['31']}
        )

    @classmethod
    def prepare_deals(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=500,
                children=[
                    HyperCategory(hid=501, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]
        cls.index.models += [
            Model(hyperid=51, hid=501),
        ]
        cls.index.offers += [
            Offer(fesh=1, hyperid=51, discount=15),
        ]

    @classmethod
    def prepare_blue_experiment(cls):
        cls.index.shops += [
            Shop(
                fesh=22876000,
                datafeed_id=228760001,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=22876000,
                datafeed_id=228760011,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=2287610, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2287620, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=2287601, hid=2287610),
            Model(hyperid=2287602, hid=2287610),
            Model(hyperid=2287603, hid=2287620),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=2287601, sku=228760101, blue_offers=[_Offers.sku1_offer1]),
            MarketSku(hyperid=2287602, sku=228760201, blue_offers=[_Offers.sku2_offer1]),
            MarketSku(hyperid=2287603, sku=228760301, blue_offers=[_Offers.sku3_offer1]),
        ]

        for hyperid, hid in RANDOMIZED_PLACE_MODELS:
            sku_id = hyperid * 100 + 1
            blue_offer = BlueOffer(offerid='Shop1_sku' + str(sku_id), feedid=228760011)
            # skus for randomized place
            cls.index.mskus += [
                MarketSku(hyperid=hyperid, sku=sku_id, blue_offers=[blue_offer]),
            ]

    #        for hid in RANDOMIZED_PLACE_DISTINCT_HIDS:
    #            cls.index.blue_category_region_stat += [
    #                CategoryStatsRecord(hid, 213, n_offers=3, n_discounts=3),
    #            ]

    @classmethod
    def prepare_popular_products_and_history_blue(cls):
        # personal categories ordering
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:2287601').respond(
            {'models': ['2287601', '2287603']}
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:2287601', item_count=40, with_timestamps=True
        ).respond({'models': ['2287601', '2287603'], 'timestamps': ['1', '2']})
        cls.bigb.on_request(yandexuid='2287601', client='merch-machine').respond(counters=[])
        cls.dj.on_request(yandexuid='2287601').respond(
            models=[
                DjModel(id=2287601, title='model'),
                DjModel(id=2287602, title='model'),
                DjModel(id=2287603, title='model'),
                DjModel(id=2287604, title='model'),
            ]
        )
        cls.dj.on_request(yandexuid='1001').respond(
            models=[
                DjModel(id=5000, title='model'),
            ]
        )

    def test_popular_products_blue(self):
        """
        Проверяем сплиты для популярных:
        В дефолтном сплите обычные популярные,
        В эксперименте популярные рандомные на морде и отключены в департаментах
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            # default split
            # departments
            response = self.report.request_json(
                'place=popular_products&rids=213&yandexuid=2287601&hid=2287610&{}&numdoc=10'.format(suffix)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 2287601},
                        ]
                    }
                },
                preserve_order=False,
                allow_different_len=True,
            )

            # disable recommendations in departments in experiment
            rearr_factors = 'rearr-factors=market_recom_random_blue=1;switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0'
            response = self.report.request_json(
                'place=popular_products&{}&rids=213&yandexuid=2287601&hid=2287610&{}&numdoc=10'.format(
                    rearr_factors, suffix
                )
            )
            self.assertFragmentIn(
                response, {'search': {'total': 0, 'results': []}}, preserve_order=False, allow_different_len=False
            )

            # random recommendations in experiment
            response = self.report.request_json(
                'place=popular_products&{}&rids=213&yandexuid=2287601&{}&numdoc=10'.format(rearr_factors, suffix)
            )
            actual_hid_sequence = RANDOMIZED_PLACE_DISTINCT_HIDS[::2]
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': len(actual_hid_sequence),
                        'results': [{'entity': 'product', 'categories': [{'id': hid}]} for hid in actual_hid_sequence],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )
            self.assertFragmentNotIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 2287601},
                        ]
                    }
                },
            )

    def test_products_by_history_blue(self):
        """
        Проверка работы эксперимента с блоком истории на морде
        В дефолтном сплите обычная история,
        В эксперименте популярные история отключена
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            # default split
            response = self.report.request_json(
                'place=products_by_history&yandexuid=2287601&rids=213&{}'.format(suffix)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 2287601},
                            {'entity': 'product', 'id': 2287602},
                            {'entity': 'product', 'id': 2287603},
                        ]
                    }
                },
            )
            # disable recommendations in experiment
            response = self.report.request_json(
                'place=products_by_history&yandexuid=1001&rids=213&{}&rearr-factors=market_recom_random_blue=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(response, {'search': {'results': ElementCount(0)}})
            self.assertFragmentNotIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 2287601},
                        ]
                    }
                },
            )

    @classmethod
    def prepare_index_page_omm_blue(cls):
        DEFAULT_PROFILE = [
            BigBKeyword(
                id=BigBKeyword.GENDER,
                weighted_uint_values=[
                    WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
                    WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
                ],
            ),
        ]
        cls.bigb.on_request(yandexuid='2287602', client='merch-machine').respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(yandexuid='2287602', exp='blue_attractive_models').respond(
            models=[
                DjModel(id=2287601, title='model'),
                DjModel(id=2287602, title='model'),
                DjModel(id=2287603, title='model'),
                DjModel(id=2287604, title='model'),
            ]
        )

        cls.dj.on_request(yandexuid='2287602', exp='blue_omm_findings').respond(
            models=[
                DjModel(id=2287601, title='model'),
                DjModel(id=2287602, title='model'),
                DjModel(id=2287603, title='model'),
                DjModel(id=2287604, title='model'),
            ]
        )

    def test_index_page_omm_blue(self):
        """
        В дефолтном сплите обычные популярные,
        В эксперименте ОММ рандомный на морде и на серче
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            # default split
            response = self.report.request_json(
                'place=blue_attractive_models&yandexuid=2287602&rids=213&{}'.format(suffix)
            )
            self.assertFragmentIn(response, {'search': {'results': [{'entity': 'product', 'id': 2287601}]}})

            # random recommendations in experiment, empty with dj
            response = self.report.request_json(
                'place=blue_attractive_models&yandexuid=1001&rids=213&{}&rearr-factors=market_recom_random_blue=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(response, {'search': {'results': []}}, preserve_order=True, allow_different_len=False)

            self.error_log.ignore(code=3787)

    def test_index_page_omm_findings_blue(self):
        """
        В дефолтном сплите обычные популярные,
        В эксперименте ОММ рандомный на морде и на серче
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            # default split
            response = self.report.request_json('place=blue_omm_findings&yandexuid=2287602&rids=213&{}'.format(suffix))
            self.assertFragmentIn(response, {'search': {'results': [{'entity': 'product', 'id': 2287601}]}})

            # random recommendations in experiment, empty with dj
            response = self.report.request_json(
                'place=blue_omm_findings&yandexuid=1001&rids=213&{}&rearr-factors=market_recom_random_blue=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(response, {'search': {'results': []}}, preserve_order=True, allow_different_len=False)

            self.error_log.ignore(code=3787)

    @classmethod
    def prepare_accessories_blue(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            )
        ]

        cls.recommender.on_request_accessory_models(model_id=2287601, item_count=1000, version='1').respond(
            {'models': ['2287603']}
        )

    def test_accessories_blue(self):
        """
        В дефолтном сплите обычные аксы,
        В эксперименте аксы отключены
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            # default split
            response = self.report.request_json(
                'place=product_accessories&yandexuid=2287601&rids=213&{}&hyperid=2287601&rearr-factors=market_disable_product_accessories=0'.format(
                    suffix
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 2287603},
                        ]
                    }
                },
            )

            # disable recommendations in experiment
            response = self.report.request_json(
                'place=product_accessories&yandexuid=2287601&rids=213&hyperid=2287601&{}&rearr-factors=market_recom_random_blue=1;market_disable_product_accessories=0'.format(
                    suffix
                )
            )
            self.assertFragmentIn(response, {'search': {'results': ElementCount(0)}})

    @classmethod
    def prepare_also_viewed_blue(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=2287602, item_count=1000, version='1').respond(
            {'models': ['2287601']}
        )

    def test_also_viewed_blue(self):
        """
        В дефолтном сплите обычные видевшие,
        В эксперименте видевшие отключены
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            # default split
            response = self.report.request_json(
                'place=also_viewed&yandexuid=2287601&rids=213&{}&hyperid=2287602'.format(suffix)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {'entity': 'product', 'id': 2287601},
                        ],
                    }
                },
                preserve_order=False,
            )
            # disable recommendations in experiment
            response = self.report.request_json(
                'place=also_viewed&yandexuid=2287601&rids=213&{}&hyperid=2287602&rearr-factors=market_recom_random_blue=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(response, {'search': {'results': ElementCount(0)}})

    def test_deals_blue(self):
        """
        В дефолтном сплите обычные скидки,
        В эксперименте скидки отключены
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            # default split
            response = self.report.request_json('place=deals&numdoc=100&rids=213&yandexuid=2287601&{}'.format(suffix))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {'entity': 'product', 'id': 2287601},
                        ],
                    }
                },
            )
            # disable recommendations in experiment
            response = self.report.request_json(
                'place=deals&numdoc=100&rids=213&yandexuid=2287601&{}&rearr-factors=market_recom_random_blue=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(response, {'search': {'results': ElementCount(0)}})
            # disable recommendations in departments in experiment
            response = self.report.request_json(
                'place=deals&numdoc=100&rids=213&yandexuid=2287601&{}&hid=2287610&rearr-factors=market_recom_random_blue=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(response, {'search': {'results': ElementCount(0)}})

    @classmethod
    def prepare_commonly_purchased(cls):
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

        cls.index.hypertree += [
            HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)
            for hid in personal_category_filter + universal_categories
        ]
        cls.index.hypertree += [HyperCategory(202, output_type=HyperCategoryType.GURU)]

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
        ]

        categories = [
            90606,  # personal
            278374,  # univeral
            7683677,  # personal
            202,  # unknown
            13491643,  # universal
            91078,  # universal
        ]
        model_ids = [
            2628001,
            2628002,
            2628003,
            2628004,
            2628005,
            2628006,
        ]
        cls.index.models += [Model(hyperid=model_id, hid=category) for model_id, category in zip(model_ids, categories)]

        # market skus
        cls.index.mskus += [
            MarketSku(hyperid=hyperid, sku=hyperid * 10 + 1, blue_offers=[BlueOffer(feedid=228760011)])
            for hyperid in model_ids
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1001').respond(
            {'models': ['2628001', '2628003']}
        )

    def test_commonly_purchased(self):
        """
        В дефолтном сплите обычные частотные,
        В эксперименте частотные рандомные
        """
        # default split
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json(
                'place=commonly_purchased&yandexuid=1001&rids=213&{}&rearr-factors=turn_on_commonly_purchased=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 2628001},
                            {'entity': 'product', 'id': 2628003},
                            {'entity': 'product', 'id': 2628002},
                            {'entity': 'product', 'id': 2628005},
                            {'entity': 'product', 'id': 2628006},
                        ]
                    }
                },
            )
            # random recommendations in experiment
            response = self.report.request_json(
                'place=commonly_purchased&yandexuid=1001&rids=213&{}&rearr-factors=market_recom_random_blue=1;turn_on_commonly_purchased=1'.format(
                    suffix
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [{'entity': 'product', 'categories': [{'id': hid}]} for hid in [15148752, 91529]]
                    }
                },
            )
            self.assertFragmentNotIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 2628001},
                        ]
                    }
                },
            )


if __name__ == '__main__':
    main()
