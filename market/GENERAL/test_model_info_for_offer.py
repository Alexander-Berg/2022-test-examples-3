#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    DeliveryBucket,
    MarketSku,
    MnPlace,
    Model,
    ModelGroup,
    Opinion,
    Shop,
    Tax,
    VCluster,
)


SHOP_ID = 1
FEED_ID = 1
VIRTUAL_SHOP_ID = 2
VIRTUAL_FEED_ID = 2
DELIVERY_BUCKET_ID = 1000
DC_DELIVERY_BUCKET_ID = 1100
VISUAL_CLUSTER_ID = 1000000001

# Models
MODEL = Model(
    hyperid=1,
    hid=1,
    title="Тестовая синяя модель",
    rgb_type=Model.RGB_BLUE,
    has_blue_offers=True,
    opinion=Opinion(total_count=100, rating=4.5, precise_rating=4.67, rating_count=200, reviews=5),
)

# Group models
GROUP_MODEL = ModelGroup(
    hyperid=2,
    hid=2,
    title="Тестовая синяя групповая модель",
    opinion=Opinion(total_count=110, rating=4.6, rating_count=210, reviews=6),
)
FIRST_MODIFICATION = Model(
    hyperid=21, hid=GROUP_MODEL.hid, title="Первая тестовая синяя модификация", group_hyperid=GROUP_MODEL.hyper
)
SECOND_MODIFICATION = Model(
    hyperid=22, hid=GROUP_MODEL.hid, title="Вторая тестовая синяя модификация", group_hyperid=GROUP_MODEL.hyper
)

# Visual clusters
VISUAL_CLUSTER = VCluster(
    vclusterid=VISUAL_CLUSTER_ID,
    hid=3,
    title="Тестовый синий визуальный кластер",
    opinion=Opinion(total_count=120, rating=4.7, rating_count=220, reviews=7),
)

# Blue offers
CHEAP_FIRST_OFFER = BlueOffer(
    price=5000,
    offerid='Shop1_sku10_1',
    waremd5='Sku10Price05k-vm1Goleg',
    feedid=FEED_ID,
    ts=501,
)
EXPENSIVE_FIRST_OFFER = BlueOffer(
    price=15000,
    offerid='Shop1_sku10_1',
    waremd5='Sku10Price15k-vm1Goleg',
    feedid=FEED_ID,
    ts=502,
)
SECOND_OFFER = BlueOffer(
    price=4000,
    offerid='Shop1_sku11_1',
    waremd5='Sku11Price04k-vm1Goleg',
    feedid=FEED_ID,
    ts=503,
)
FIRST_MODIFICATION_OFFER = BlueOffer(
    price=11000, offerid='Shop1_sku12_1', waremd5='Sku12Price11k-vm1Goleg', feedid=FEED_ID
)
VISUAL_CLUSTER_OFFER = BlueOffer(price=12000, offerid='Shop1_sku13_1', waremd5='Sku13Price12k-vm1Goleg', feedid=FEED_ID)

# Market SKUs
FIRST_MSKU = MarketSku(
    title="Первый тестовый синий оффер",
    hid=MODEL.hid,
    hyperid=MODEL.hyper,
    sku='10',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[CHEAP_FIRST_OFFER, EXPENSIVE_FIRST_OFFER],
)
SECOND_MSKU = MarketSku(
    title="Второй тестовый синий оффер",
    hid=MODEL.hid,
    hyperid=MODEL.hyper,
    sku='11',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[SECOND_OFFER],
)
FIRST_MODIFICATION_MSKU = MarketSku(
    title="Тестовый синий оффер от первой модификации",
    hid=FIRST_MODIFICATION.hid,
    hyperid=FIRST_MODIFICATION.hyper,
    sku='12',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[FIRST_MODIFICATION_OFFER],
)
VISUAL_CLUSTER_MSKU = MarketSku(
    title="Тестовый синий оффер от визуальной модели",
    hid=VISUAL_CLUSTER.hid,
    vclusterid=VISUAL_CLUSTER_ID,
    sku='13',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[VISUAL_CLUSTER_OFFER],
)

REQUEST_MODEL = 'place=prime' '&rgb={color}' '&pp=18' '&hyperid={model}'

REQUEST_VCLUSTER = 'place=prime' '&rgb={color}' '&pp=18' '&hid={category}'


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.settings.rgb_blue_is_cpa = True

    @classmethod
    def prepare_delivery(cls):
        cls.index.shops += [
            Shop(
                fesh=SHOP_ID,
                datafeed_id=FEED_ID,
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_FEED_ID,
                name="Тестовый виртуальный магазин",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=DELIVERY_BUCKET_ID,
                dc_bucket_id=DC_DELIVERY_BUCKET_ID,
                fesh=SHOP_ID,
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.model_groups += [GROUP_MODEL]
        cls.index.models += [MODEL, FIRST_MODIFICATION, SECOND_MODIFICATION]
        cls.index.vclusters += [VISUAL_CLUSTER]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [FIRST_MSKU, SECOND_MSKU, FIRST_MODIFICATION_MSKU, VISUAL_CLUSTER_MSKU]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.8)  # CHEAP_FIRST_OFFER
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.9)  # EXPENSIVE_FIRST_OFFER
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 503).respond(0.7)  # SECOND_OFFER

    @staticmethod
    def __get_skus_stats_fragment(total_count, before_filters_count, after_filters_count):
        return {
            'totalCount': total_count,
            'beforeFiltersCount': before_filters_count,
            'afterFiltersCount': after_filters_count,
        }

    @staticmethod
    def __get_model_info_fragment(
        model, opinion, total_count=None, before_filters_count=None, after_filters_count=None
    ):
        model_id = None
        parent_id = None
        model_type = None
        sku_stats = T.__get_skus_stats_fragment(total_count, before_filters_count, after_filters_count)
        if isinstance(model, Model):
            model_id = model.hyper
            parent_id = model.group_hyperid
            model_type = 'modification' if parent_id is not None else 'model'
        elif isinstance(model, VCluster):
            model_id = int(model.vclusterid)
            model_type = 'cluster'
        return {
            'id': model_id,
            'parentId': parent_id if parent_id is not None else Absent(),
            'type': model_type,
            'opinions': opinion.total_count,
            'rating': opinion.rating,
            'preciseRating': opinion.precise_rating,
            'ratingCount': opinion.rating_count,
            'reviews': opinion.reviewes,
            'skuStats': Absent() if total_count is None else sku_stats,
        }

    def test_model_info(self):
        """
        Проверяем, что офферы в выдаче на Беру содержат блок 'model' с информаицей о своей модели
        """
        # С "сжатием" выдачи до моделей (параметр '&allow-collapsing' не задан)
        model_info_fragment = T.__get_model_info_fragment(
            model=MODEL, opinion=MODEL.opinion, total_count=2, before_filters_count=2, after_filters_count=2
        )

        response = self.report.request_json(REQUEST_MODEL.format(color='blue', model=MODEL.hyper))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'type': 'model',
                        'id': MODEL.hyper,
                        'offers': {
                            'count': 2,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': CHEAP_FIRST_OFFER.waremd5,  # Для FIRST_MSKU buybox выигрывает CHEAP_FIRST_OFFER
                                    # Для SECOND_MSKU buybox выигрывает SECOND_OFFER
                                    # В итоге выбираем CHEAP_FIRST_OFFER, так как он релевантнее
                                    'model': model_info_fragment,
                                }
                            ],
                        },
                        'skuStats': T.__get_skus_stats_fragment(2, 2, 2),
                    }
                ]
            },
        )

        # С пооферной выдачей - '&allow-collapsing=0'
        request = REQUEST_MODEL + '&allow-collapsing=0'
        response = self.report.request_json(request.format(color='blue', model=MODEL.hyper))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': offer.waremd5, 'model': model_info_fragment}
                    for offer in [CHEAP_FIRST_OFFER, SECOND_OFFER]
                ]
            },
        )

    def test_model_info_in_model(self):
        """Переносим данные из productInfo в блок model"""

        for color in ['green', 'blue']:
            model_info_fragment = T.__get_model_info_fragment(
                model=MODEL, opinion=MODEL.opinion, total_count=2, before_filters_count=2, after_filters_count=2
            )
            model_info_fragment_without_sku_stats = T.__get_model_info_fragment(model=MODEL, opinion=MODEL.opinion)

            response = self.report.request_json(
                REQUEST_MODEL.format(color=color, model=MODEL.hyper)
                + '&cpa=real&allow-collapsing=1&use-default-offers=1&rearr-factors=market_drop_product_info=1'
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'type': 'model',
                            'id': MODEL.hyper,
                            'offers': {
                                'count': 2,
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'model': model_info_fragment,
                                    }
                                ],
                            },
                            'skuStats': T.__get_skus_stats_fragment(2, 2, 2),
                        }
                    ]
                },
            )

            # С пооферной выдачей - '&allow-collapsing=0'
            request = REQUEST_MODEL + '&allow-collapsing=0&cpa=real&rearr-factors=market_drop_product_info=1'
            response = self.report.request_json(request.format(color=color, model=MODEL.hyper))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'model': model_info_fragment if color == 'blue' else model_info_fragment_without_sku_stats,
                        }
                        for offer in [CHEAP_FIRST_OFFER, SECOND_OFFER]
                    ]
                },
            )

    def test_model_group_info(self):
        """
        Аналогично test_model_info, но для офферов от групповой модели
        """
        response = self.report.request_json(REQUEST_MODEL.format(color='blue', model=FIRST_MODIFICATION.hyper))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'type': 'modification',
                        'id': FIRST_MODIFICATION.hyper,
                        'offers': {
                            'count': 1,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': FIRST_MODIFICATION_OFFER.waremd5,
                                    'model': T.__get_model_info_fragment(
                                        model=FIRST_MODIFICATION,
                                        opinion=GROUP_MODEL.opinion,
                                        total_count=1,
                                        before_filters_count=1,
                                        after_filters_count=1,
                                    ),
                                }
                            ],
                        },
                        'skuStats': T.__get_skus_stats_fragment(1, 1, 1),
                    }
                ]
            },
        )

    def test_visual_cluster_info(self):
        """
        Аналогично test_model_info, но для офферов от визуального кластера
        """
        response = self.report.request_json(REQUEST_VCLUSTER.format(color='blue', category=VISUAL_CLUSTER.hid))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'type': 'cluster',
                        'id': VISUAL_CLUSTER_ID,
                        'offers': {
                            'count': 1,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': VISUAL_CLUSTER_OFFER.waremd5,
                                    'model': T.__get_model_info_fragment(
                                        model=VISUAL_CLUSTER,
                                        opinion=VISUAL_CLUSTER.opinion,
                                        total_count=1,
                                        before_filters_count=1,
                                        after_filters_count=1,
                                    ),
                                }
                            ],
                        },
                        'skuStats': T.__get_skus_stats_fragment(1, 1, 1),
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
