#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    Offer,
    Picture,
    RegionalDelivery,
    Shop,
    VCluster,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=10, output_type=HyperCategoryType.GURU, show_offers=False),
            HyperCategory(hid=11, output_type=HyperCategoryType.CLUSTERS, show_offers=False, visual=True),
            HyperCategory(hid=20, output_type=HyperCategoryType.GURU, show_offers=True),
            HyperCategory(hid=21, output_type=HyperCategoryType.CLUSTERS, show_offers=True, visual=True),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=10),
            Model(hyperid=102, hid=10),
            Model(hyperid=103, hid=10),
            Model(hyperid=104, hid=10),
            Model(hyperid=105, hid=10),
            Model(hyperid=201, hid=20),
            Model(hyperid=202, hid=20),
            Model(hyperid=203, hid=20),
            Model(hyperid=204, hid=20),
            Model(hyperid=205, hid=20),
        ]

        cls.index.vclusters += [
            VCluster(vclusterid=1000000011, hid=11),
            VCluster(vclusterid=1000000012, hid=11),
            VCluster(vclusterid=1000000013, hid=11),
            VCluster(vclusterid=1000000014, hid=11),
            VCluster(vclusterid=1000000015, hid=11),
            VCluster(vclusterid=2000000011, hid=21),
            VCluster(vclusterid=2000000012, hid=21),
            VCluster(vclusterid=2000000013, hid=21),
            VCluster(vclusterid=2000000014, hid=21),
            VCluster(vclusterid=2000000015, hid=21),
        ]

        cls.index.offers += [
            # Hid 10
            Offer(title="Offer Model 101", hyperid=101, hid=10),
            Offer(title="Offer Model 102", hyperid=102, hid=10),
            Offer(title="Offer Model 103", hyperid=103, hid=10),
            Offer(title="Offer Model 104", hyperid=104, hid=10),
            Offer(title="Offer Model 105", hyperid=105, hid=10),
            Offer(title="Offer No Model hid10 1", hid=10),
            Offer(title="Offer No Model hid10 2", hid=10),
            Offer(title="Offer No Model hid10 3", hid=10),
            Offer(title="Offer No Model hid10 4", hid=10),
            Offer(title="Offer No Model hid10 5", hid=10),
            # Hid 11
            Offer(title="Offer VCluster 1000000011", vclusterid=1000000011, hid=11),
            Offer(title="Offer VCluster 1000000012", vclusterid=1000000012, hid=11),
            Offer(title="Offer VCluster 1000000013", vclusterid=1000000013, hid=11),
            Offer(title="Offer VCluster 1000000014", vclusterid=1000000014, hid=11),
            Offer(title="Offer VCluster 1000000015", vclusterid=1000000015, hid=11),
            Offer(title="Offer No VCluster hid11 1", hid=11),
            Offer(title="Offer No VCluster hid11 2", hid=11),
            Offer(title="Offer No VCluster hid11 3", hid=11),
            Offer(title="Offer No VCluster hid11 4", hid=11),
            Offer(title="Offer No VCluster hid11 5", hid=11),
            # Hid 20
            Offer(title="Offer Model 201", hyperid=201, hid=20),
            Offer(title="Offer Model 202", hyperid=202, hid=20),
            Offer(title="Offer Model 203", hyperid=203, hid=20),
            Offer(title="Offer Model 204", hyperid=204, hid=20),
            Offer(title="Offer Model 205", hyperid=205, hid=20),
            Offer(title="Offer No Model hid20 1", hid=20),
            Offer(title="Offer No Model hid20 2", hid=20),
            Offer(title="Offer No Model hid20 3", hid=20),
            Offer(title="Offer No Model hid20 4", hid=20),
            Offer(title="Offer No Model hid20 5", hid=20),
            # Hid 21
            Offer(title="Offer VCluster 2000000011", vclusterid=2000000011, hid=21),
            Offer(title="Offer VCluster 2000000012", vclusterid=2000000012, hid=21),
            Offer(title="Offer VCluster 2000000013", vclusterid=2000000013, hid=21),
            Offer(title="Offer VCluster 2000000014", vclusterid=2000000014, hid=21),
            Offer(title="Offer VCluster 2000000015", vclusterid=2000000015, hid=21),
            Offer(title="Offer No VCluster hid21 1", hid=21),
            Offer(title="Offer No VCluster hid21 2", hid=21),
            Offer(title="Offer No VCluster hid21 3", hid=21),
            Offer(title="Offer No VCluster hid21 4", hid=21),
            Offer(title="Offer No VCluster hid21 5", hid=21),
        ]

    def test_no_show_offers_guru(self):
        """Проверим, что в выдаче нет офферов, потому что у категории show_offers=False"""
        response = self.report.request_json('place=prime&allow-collapsing=1&hid=10&debug=da')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 101},
                        {"type": "model", "id": 102},
                        {"type": "model", "id": 103},
                        {"type": "model", "id": 104},
                        {"type": "model", "id": 105},
                    ]
                }
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, "Sorting: SF_CPM")
        self.assertFragmentIn(response, "enableRemovingWithoutHyper=1")

    def test_no_show_offers_guru__force_show(self):
        """Офферов быть не должно, но мы форсим их показ с помощью &force-show-offers-without-hyper=1"""

        for query in [
            'place=prime&allow-collapsing=1&hid=10&force-show-offers-without-hyper=1&debug=da',
            'place=prime&allow-collapsing=1&hid=10&rearr-factors=market_force_show_offers_without_hyper=1&debug=da',
        ]:
            response = self.report.request_json(query + '&rearr-factors=market_metadoc_search=no')
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"type": "model", "id": 101},
                            {"type": "model", "id": 102},
                            {"type": "model", "id": 103},
                            {"type": "model", "id": 104},
                            {"type": "model", "id": 105},
                            {"titles": {"raw": "Offer No Model hid10 1"}},
                            {"titles": {"raw": "Offer No Model hid10 2"}},
                            {"titles": {"raw": "Offer No Model hid10 3"}},
                            {"titles": {"raw": "Offer No Model hid10 4"}},
                            {"titles": {"raw": "Offer No Model hid10 5"}},
                        ]
                    }
                },
                allow_different_len=False,
            )

            self.assertFragmentIn(response, "Sorting: SF_CPM")
            self.assertFragmentIn(response, "enableRemovingWithoutHyper=0")

    def test_no_show_offers_guru__with_text(self):
        """Проверим, что в выдаче есть офферы, потому что у нас текстовый запрос"""
        response = self.report.request_json('place=prime&allow-collapsing=1&hid=10&debug=da&text=Offer')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 101},
                        {"type": "model", "id": 102},
                        {"type": "model", "id": 103},
                        {"type": "model", "id": 104},
                        {"type": "model", "id": 105},
                        {"titles": {"raw": "Offer No Model hid10 1"}},
                        {"titles": {"raw": "Offer No Model hid10 2"}},
                        {"titles": {"raw": "Offer No Model hid10 3"}},
                        {"titles": {"raw": "Offer No Model hid10 4"}},
                        {"titles": {"raw": "Offer No Model hid10 5"}},
                    ]
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, "Sorting: SF_CPM")
        self.assertFragmentIn(response, "enableRemovingWithoutHyper=0")

    def test_no_show_offers_visual(self):
        """Проверим, что в выдаче нет офферов, потому что у категории show_offers=False"""
        response = self.report.request_json('place=prime&allow-collapsing=1&hid=11&debug=da')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "cluster", "id": 1000000011},
                        {"type": "cluster", "id": 1000000012},
                        {"type": "cluster", "id": 1000000013},
                        {"type": "cluster", "id": 1000000014},
                        {"type": "cluster", "id": 1000000015},
                    ]
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, "Sorting: SF_CPM")
        self.assertFragmentIn(response, "enableRemovingWithoutHyper=1")

    def test_no_show_offers_visual__force_show(self):
        """Офферов быть не должно, но мы форсим их показ с помощью &force-show-offers-without-hyper=1"""
        for query in [
            'place=prime&allow-collapsing=1&hid=11&force-show-offers-without-hyper=1&debug=da',
            'place=prime&allow-collapsing=1&hid=11&&rearr-factors=market_force_show_offers_without_hyper=1&debug=da',
        ]:
            response = self.report.request_json(query + '&rearr-factors=market_metadoc_search=no')
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"type": "cluster", "id": 1000000011},
                            {"type": "cluster", "id": 1000000012},
                            {"type": "cluster", "id": 1000000013},
                            {"type": "cluster", "id": 1000000014},
                            {"type": "cluster", "id": 1000000015},
                            {"titles": {"raw": "Offer No VCluster hid11 1"}},
                            {"titles": {"raw": "Offer No VCluster hid11 2"}},
                            {"titles": {"raw": "Offer No VCluster hid11 3"}},
                            {"titles": {"raw": "Offer No VCluster hid11 4"}},
                            {"titles": {"raw": "Offer No VCluster hid11 5"}},
                        ]
                    }
                },
                allow_different_len=False,
            )

            self.assertFragmentIn(response, "Sorting: SF_CPM")
            self.assertFragmentIn(response, "enableRemovingWithoutHyper=0")

    def test_no_show_offers_visual__with_text(self):
        """Проверим, что в выдаче есть офферы, потому что у нас текстовый запрос"""
        response = self.report.request_json('place=prime&allow-collapsing=1&hid=11&debug=da&text=Offer')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "cluster", "id": 1000000011},
                        {"type": "cluster", "id": 1000000012},
                        {"type": "cluster", "id": 1000000013},
                        {"type": "cluster", "id": 1000000014},
                        {"type": "cluster", "id": 1000000015},
                        {"titles": {"raw": "Offer No VCluster hid11 1"}},
                        {"titles": {"raw": "Offer No VCluster hid11 2"}},
                        {"titles": {"raw": "Offer No VCluster hid11 3"}},
                        {"titles": {"raw": "Offer No VCluster hid11 4"}},
                        {"titles": {"raw": "Offer No VCluster hid11 5"}},
                    ]
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, "Sorting: SF_CPM")

    def test_show_offers_guru(self, aux=''):
        """Проверим, что в выдаче есть офферы, потому что у категории show_offers=True"""
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=20&debug=da&rearr-factors=market_metadoc_search=no&{}'.format(aux)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 201},
                        {"type": "model", "id": 202},
                        {"type": "model", "id": 203},
                        {"type": "model", "id": 204},
                        {"type": "model", "id": 205},
                        {"titles": {"raw": "Offer No Model hid20 1"}},
                        {"titles": {"raw": "Offer No Model hid20 2"}},
                        {"titles": {"raw": "Offer No Model hid20 3"}},
                        {"titles": {"raw": "Offer No Model hid20 4"}},
                        {"titles": {"raw": "Offer No Model hid20 5"}},
                    ]
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, "Sorting: SF_CPM")

    def test_show_offers_guru__with_text(self):
        return self.test_show_offers_guru(aux="&text=Offer")

    def test_show_offers_visual(self, aux=''):
        """Проверим, что в выдаче есть офферы, потому что у категории show_offers=True"""
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=21&debug=da&rearr-factors=market_metadoc_search=no&{}'.format(aux)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "cluster", "id": 2000000011},
                        {"type": "cluster", "id": 2000000012},
                        {"type": "cluster", "id": 2000000013},
                        {"type": "cluster", "id": 2000000014},
                        {"type": "cluster", "id": 2000000015},
                        {"titles": {"raw": "Offer No VCluster hid21 1"}},
                        {"titles": {"raw": "Offer No VCluster hid21 2"}},
                        {"titles": {"raw": "Offer No VCluster hid21 3"}},
                        {"titles": {"raw": "Offer No VCluster hid21 4"}},
                        {"titles": {"raw": "Offer No VCluster hid21 5"}},
                    ]
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, "Sorting: SF_CPM")
        self.assertFragmentIn(response, "enableRemovingWithoutHyper=0")

    def test_show_offers_visual__with_text(self):
        return self.test_show_offers_visual(aux="&text=Offer")

    def test_show_offers_2hids__show_offers_true(self, aux=''):
        """Проверим, что в выдаче есть офферы, потому что у всех категорий в запросе show_offers=True"""
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=20&hid=21&debug=da&numdoc=20&rearr-factors=market_metadoc_search=no&{}'.format(
                aux
            )
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 201},
                        {"type": "model", "id": 202},
                        {"type": "model", "id": 203},
                        {"type": "model", "id": 204},
                        {"type": "model", "id": 205},
                        {"type": "cluster", "id": 2000000011},
                        {"type": "cluster", "id": 2000000012},
                        {"type": "cluster", "id": 2000000013},
                        {"type": "cluster", "id": 2000000014},
                        {"type": "cluster", "id": 2000000015},
                        {"titles": {"raw": "Offer No Model hid20 1"}},
                        {"titles": {"raw": "Offer No Model hid20 2"}},
                        {"titles": {"raw": "Offer No Model hid20 3"}},
                        {"titles": {"raw": "Offer No Model hid20 4"}},
                        {"titles": {"raw": "Offer No Model hid20 5"}},
                        {"titles": {"raw": "Offer No VCluster hid21 1"}},
                        {"titles": {"raw": "Offer No VCluster hid21 2"}},
                        {"titles": {"raw": "Offer No VCluster hid21 3"}},
                        {"titles": {"raw": "Offer No VCluster hid21 4"}},
                        {"titles": {"raw": "Offer No VCluster hid21 5"}},
                    ]
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(response, "Sorting: SF_CPM")
        self.assertFragmentIn(response, "enableRemovingWithoutHyper=0")

    def test_show_offers_2hids__show_offers_true__with_text(self):
        return self.test_show_offers_2hids__show_offers_true(aux="&text=Offer")

    def test_show_offers_2hids__show_offers_false(self):
        """Проверим, что в выдаче нет офферов, потому что только у одного show_offers=True"""

        response = self.report.request_json('place=prime&allow-collapsing=1&hid=10&hid=20&debug=da')

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 101},
                        {"type": "model", "id": 102},
                        {"type": "model", "id": 103},
                        {"type": "model", "id": 104},
                        {"type": "model", "id": 105},
                        {"type": "model", "id": 201},
                        {"type": "model", "id": 202},
                        {"type": "model", "id": 203},
                        {"type": "model", "id": 204},
                        {"type": "model", "id": 205},
                    ]
                }
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, "enableRemovingWithoutHyper=1")

    @classmethod
    def prepare_pessimize(cls):
        cls.index.hypertree += [
            HyperCategory(hid=30, output_type=HyperCategoryType.GURU, show_offers=True, pessimize_offers=True),
            HyperCategory(hid=300, output_type=HyperCategoryType.GURU, show_offers=True, pessimize_offers=True),
            HyperCategory(hid=31, output_type=HyperCategoryType.GURU, show_offers=True, pessimize_offers=False),
            HyperCategory(hid=310, output_type=HyperCategoryType.GURU, show_offers=True, pessimize_offers=False),
            HyperCategory(hid=32, output_type=HyperCategoryType.GURU, show_offers=True),
            HyperCategory(hid=33, output_type=HyperCategoryType.GURU, show_offers=True),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225], name='Московская пепячечная "Доставляем"'),
        ]

        local_delivery = [DeliveryOption(price=100, day_from=0, day_to=2, order_before=24)]
        delivery_on_russia = [RegionalDelivery(rid=213, options=[DeliveryOption(price=100)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=1225, fesh=1, carriers=[99], regional_options=delivery_on_russia),
        ]

        cls.index.models += [
            Model(hyperid=301, hid=30, ts=301, title="Test model 301"),
            Model(hyperid=302, hid=30, ts=302, title="Test model 302"),
            Model(hyperid=303, hid=30, ts=303, title="Test model 303"),
            Model(hyperid=304, hid=30, ts=304, title="Test model 304"),
            Model(hyperid=305, hid=30, ts=305, title="Test model 305"),
            Model(hyperid=3001, hid=300, ts=3001, title="Test model 3001"),
            Model(hyperid=3002, hid=300, ts=3002, title="Test model 3002"),
            Model(hyperid=3003, hid=300, ts=3003, title="Test model 3003"),
            Model(hyperid=3004, hid=300, ts=3004, title="Test model 3004"),
            Model(hyperid=3005, hid=300, ts=3005, title="Test model 3005"),
            Model(hyperid=311, hid=31, ts=311, title="Test model 311"),
            Model(hyperid=312, hid=31, ts=312, title="Test model 312"),
            Model(hyperid=313, hid=31, ts=313, title="Test model 313"),
            Model(hyperid=314, hid=31, ts=314, title="Test model 314"),
            Model(hyperid=315, hid=31, ts=315, title="Test model 315"),
            Model(hyperid=3101, hid=310, ts=3101, title="Test model 3101"),
            Model(hyperid=3102, hid=310, ts=3102, title="Test model 3102"),
            Model(hyperid=3103, hid=310, ts=3103, title="Test model 3103"),
            Model(hyperid=3104, hid=310, ts=3104, title="Test model 3104"),
            Model(hyperid=3105, hid=310, ts=3105, title="Test model 3105"),
            Model(hyperid=321, hid=32, ts=321, title="Test model 321"),
            Model(hyperid=322, hid=32, ts=322, title="Test model 322"),
            Model(hyperid=323, hid=32, ts=323, title="Test model 323"),
            Model(hyperid=324, hid=32, ts=324, title="Test model 324"),
            Model(hyperid=325, hid=32, ts=325, title="Test model 325"),
            Model(hyperid=331, hid=33, ts=331, title="Test model 331"),  # has offer
            Model(hyperid=332, hid=33, ts=332, title="Test model 332"),  # has no offer
        ]

        cls.index.offers += [
            # Hid 30
            Offer(
                title="Offer Model 301",
                hyperid=301,
                hid=30,
                ts=3011,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3011', width=100, height=100),
            ),
            Offer(
                title="Offer Model 302",
                hyperid=302,
                hid=30,
                ts=3012,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3012', width=100, height=100),
            ),
            Offer(
                title="Offer Model 303",
                hyperid=303,
                hid=30,
                ts=3013,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3013', width=100, height=100),
            ),
            Offer(
                title="Offer Model 304",
                hyperid=304,
                hid=30,
                ts=3014,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3014', width=100, height=100),
            ),
            Offer(
                title="Offer Model 305",
                hyperid=305,
                hid=30,
                ts=3015,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3015', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid30 1",
                hid=30,
                ts=3016,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3016', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid30 2",
                hid=30,
                ts=3017,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3017', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid30 3",
                hid=30,
                ts=3018,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3018', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid30 4",
                hid=30,
                ts=3019,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3019', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid30 5",
                hid=30,
                ts=3020,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3020', width=100, height=100),
            ),
            # Hid 300
            Offer(
                title="Offer Model 3001",
                hyperid=3001,
                hid=300,
                ts=30011,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4011', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3002",
                hyperid=3002,
                hid=300,
                ts=30012,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4012', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3003",
                hyperid=3003,
                hid=300,
                ts=30013,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4013', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3004",
                hyperid=3004,
                hid=300,
                ts=30014,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4014', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3005",
                hyperid=3005,
                hid=300,
                ts=30015,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4015', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid300 1",
                hid=300,
                ts=30016,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4016', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid300 2",
                hid=300,
                ts=30017,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4017', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid300 3",
                hid=300,
                ts=30018,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4018', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid300 4",
                hid=300,
                ts=30019,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4019', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid300 5",
                hid=30,
                ts=30020,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4020', width=100, height=100),
            ),
            # Hid 31
            Offer(
                title="Offer Model 311",
                hyperid=311,
                hid=31,
                ts=3111,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3111', width=100, height=100),
            ),
            Offer(
                title="Offer Model 312",
                hyperid=312,
                hid=31,
                ts=3112,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3112', width=100, height=100),
            ),
            Offer(
                title="Offer Model 313",
                hyperid=313,
                hid=31,
                ts=3113,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3113', width=100, height=100),
            ),
            Offer(
                title="Offer Model 314",
                hyperid=314,
                hid=31,
                ts=3114,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3114', width=100, height=100),
            ),
            Offer(
                title="Offer Model 315",
                hyperid=315,
                hid=31,
                ts=3115,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3115', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid31 1",
                hid=31,
                ts=3116,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3116', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid31 2",
                hid=31,
                ts=3117,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3117', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid31 3",
                hid=31,
                ts=3118,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3118', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid31 4",
                hid=31,
                ts=3119,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3119', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid31 5",
                hid=31,
                ts=3120,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3120', width=100, height=100),
            ),
            # Hid 310
            Offer(
                title="Offer Model 3101",
                hyperid=3101,
                hid=310,
                ts=31011,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4111', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3102",
                hyperid=3102,
                hid=310,
                ts=31012,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4112', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3103",
                hyperid=3103,
                hid=310,
                ts=31013,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4113', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3104",
                hyperid=3104,
                hid=310,
                ts=31014,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4114', width=100, height=100),
            ),
            Offer(
                title="Offer Model 3105",
                hyperid=3105,
                hid=310,
                ts=31015,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4115', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid310 1",
                hid=310,
                ts=31016,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4116', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid310 2",
                hid=310,
                ts=31017,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4117', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid310 3",
                hid=310,
                ts=31018,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4118', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid310 4",
                hid=310,
                ts=31019,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4119', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid310 5",
                hid=310,
                ts=31020,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='4120', width=100, height=100),
            ),
            # Hid 32
            Offer(
                title="Offer Model 321",
                hyperid=321,
                hid=32,
                ts=3211,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer Model 322",
                hyperid=322,
                hid=32,
                ts=3212,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer Model 323",
                hyperid=323,
                hid=32,
                ts=3213,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer Model 324",
                hyperid=324,
                hid=32,
                ts=3214,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer Model 325",
                hyperid=325,
                hid=32,
                ts=3215,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid32 1",
                hid=32,
                ts=3216,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid32 2",
                hid=32,
                ts=3217,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid32 3",
                hid=32,
                ts=3218,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid32 4",
                hid=32,
                ts=3219,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid32 5",
                hid=32,
                ts=3220,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3211', width=100, height=100),
            ),
            # Hid 33
            Offer(
                title="Offer Model 331",
                hyperid=331,
                hid=33,
                ts=3311,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3311', width=100, height=100),
            ),
            Offer(
                title="Offer No Model hid33 1",
                hid=33,
                ts=3312,
                fesh=1,
                delivery_options=local_delivery,
                delivery_buckets=[1225],
                picture=Picture(picture_id='3311', width=100, height=100),
            ),
        ]

        # hid=30
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 301).respond(0.01)  # Models
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 302).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 303).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 304).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 305).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3011).respond(0.11)  # Offers ...
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3012).respond(0.12)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3013).respond(0.13)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3014).respond(0.14)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3015).respond(0.15)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3016).respond(0.16)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3017).respond(0.17)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3018).respond(0.18)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3019).respond(0.19)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3020).respond(0.20)

        # hid=300
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3001).respond(0.001)  # Models
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3002).respond(0.002)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3003).respond(0.003)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3004).respond(0.004)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3005).respond(0.005)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30011).respond(0.011)  # Offers ...
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30012).respond(0.012)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30013).respond(0.013)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30014).respond(0.014)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30015).respond(0.015)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30016).respond(0.016)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30017).respond(0.017)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30018).respond(0.018)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30019).respond(0.019)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30020).respond(0.020)

        # hid=31
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 311).respond(0.01)  # Models
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 312).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 313).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 314).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 315).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3111).respond(0.11)  # Offers ...
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3112).respond(0.12)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3113).respond(0.13)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3114).respond(0.14)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3115).respond(0.15)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3116).respond(0.16)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3117).respond(0.17)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3118).respond(0.18)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3119).respond(0.19)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3120).respond(0.20)

        # hid=310
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3101).respond(0.001)  # Models
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3102).respond(0.002)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3103).respond(0.003)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3104).respond(0.004)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3105).respond(0.005)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31011).respond(0.011)  # Offers ...
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31012).respond(0.012)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31013).respond(0.013)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31014).respond(0.014)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31015).respond(0.015)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31016).respond(0.016)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31017).respond(0.017)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31018).respond(0.018)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31019).respond(0.019)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31020).respond(0.020)

        # hid=32
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 321).respond(0.015)  # Models
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 322).respond(0.025)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 323).respond(0.035)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 324).respond(0.045)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 325).respond(0.055)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3211).respond(0.115)  # Offers ...
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3212).respond(0.125)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3213).respond(0.135)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3214).respond(0.145)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3215).respond(0.155)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3216).respond(0.165)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3217).respond(0.175)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3218).respond(0.185)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3219).respond(0.195)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3220).respond(0.205)

        # hid=33
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 331).respond(0.015)  # Models
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 332).respond(0.11)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3311).respond(0.10)  # Offers ...
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3312).respond(0.12)

        # ^^^ NB: reverse order 301 is less relevant than 302

    def test_pessimization_enabled(self):
        """Проверим, что офферы идут ниже моделей несмотря на то, что они более релевантны"""

        for text in ["", "&text=model"]:
            response = self.report.request_json(
                'place=prime&hid=30{}&allow-collapsing=1&rids=213&rearr-factors=market_search_enable_offer_pessimization=1&rearr-factors=market_metadoc_search=no'.format(
                    text
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"type": "model", "id": 305},
                            {"type": "model", "id": 304},
                            {"type": "model", "id": 303},
                            {"type": "model", "id": 302},
                            {"type": "model", "id": 301},
                            {"titles": {"raw": "Offer No Model hid30 5"}},
                            {"titles": {"raw": "Offer No Model hid30 4"}},
                            {"titles": {"raw": "Offer No Model hid30 3"}},
                            {"titles": {"raw": "Offer No Model hid30 2"}},
                            {"titles": {"raw": "Offer No Model hid30 1"}},
                        ]
                    }
                },
                allow_different_len=True,
                preserve_order=True,
            )

    def test_pessimization_enabled_2hids(self):
        """Проверим, что офферы идут ниже моделей несмотря на то, что они более релевантны"""

        for text in ["", "&text=model"]:
            response = self.report.request_json(
                'place=prime&hid=30&hid=300{}&allow-collapsing=1&rids=213&numdoc=20&rearr-factors=market_search_enable_offer_pessimization=1&rearr-factors=market_metadoc_search=no'.format(
                    text
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"type": "model", "id": 305},
                            {"type": "model", "id": 304},
                            {"type": "model", "id": 303},
                            {"type": "model", "id": 302},
                            {"type": "model", "id": 301},
                            {"type": "model", "id": 3005},
                            {"type": "model", "id": 3004},
                            {"type": "model", "id": 3003},
                            {"type": "model", "id": 3002},
                            {"type": "model", "id": 3001},
                            {"titles": {"raw": "Offer No Model hid30 5"}},
                            {"titles": {"raw": "Offer No Model hid30 4"}},
                            {"titles": {"raw": "Offer No Model hid30 3"}},
                            {"titles": {"raw": "Offer No Model hid30 2"}},
                            {"titles": {"raw": "Offer No Model hid30 1"}},
                            {"titles": {"raw": "Offer No Model hid300 5"}},
                            {"titles": {"raw": "Offer No Model hid300 4"}},
                            {"titles": {"raw": "Offer No Model hid300 3"}},
                            {"titles": {"raw": "Offer No Model hid300 2"}},
                            {"titles": {"raw": "Offer No Model hid300 1"}},
                        ]
                    }
                },
                allow_different_len=True,
                preserve_order=True,
            )

    def test_pessimization_disabled(self):
        """Проверим, что офферы идут выше моделей несмотря на то, что они более релевантны"""

        for text in ["", "&text=model"]:
            response = self.report.request_json(
                'place=prime&hid=31{}&allow-collapsing=1&rids=213&rearr-factors=market_search_enable_offer_pessimization=1'.format(
                    text
                )
            )

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"titles": {"raw": "Offer No Model hid31 5"}},
                            {"titles": {"raw": "Offer No Model hid31 4"}},
                            {"titles": {"raw": "Offer No Model hid31 3"}},
                            {"titles": {"raw": "Offer No Model hid31 2"}},
                            {"titles": {"raw": "Offer No Model hid31 1"}},
                            {"type": "model", "id": 315},
                            {"type": "model", "id": 314},
                            {"type": "model", "id": 313},
                            {"type": "model", "id": 312},
                            {"type": "model", "id": 311},
                        ]
                    }
                },
                allow_different_len=True,
                preserve_order=True,
            )

    def test_pessimization_disabled_2hids(self):
        """Проверим, что офферы идут выше моделей несмотря на то, что они более релевантны"""

        for text in ["", "&text=model"]:
            response = self.report.request_json(
                'place=prime&hid=31&hid=310{}&allow-collapsing=1&rids=213&numdoc=20'
                '&rearr-factors=market_search_enable_offer_pessimization=1;'
                'market_metadoc_search=no;market_max_offers_per_shop_count=5'.format(text)
            )

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"titles": {"raw": "Offer No Model hid31 5"}},
                            {"titles": {"raw": "Offer No Model hid31 4"}},
                            {"titles": {"raw": "Offer No Model hid31 3"}},
                            {"titles": {"raw": "Offer No Model hid31 2"}},
                            {"titles": {"raw": "Offer No Model hid31 1"}},
                            {"type": "model", "id": 315},
                            {"type": "model", "id": 314},
                            {"type": "model", "id": 313},
                            {"type": "model", "id": 312},
                            {"type": "model", "id": 311},
                            {"type": "model", "id": 3105},
                            {"type": "model", "id": 3104},
                            {"type": "model", "id": 3103},
                            {"type": "model", "id": 3102},
                            {"type": "model", "id": 3101},
                            {"titles": {"raw": "Offer No Model hid310 5"}},
                            {"titles": {"raw": "Offer No Model hid310 4"}},
                            {"titles": {"raw": "Offer No Model hid310 3"}},
                            {"titles": {"raw": "Offer No Model hid310 2"}},
                            {"titles": {"raw": "Offer No Model hid310 1"}},
                        ]
                    }
                },
                allow_different_len=True,
                preserve_order=True,
            )

    def test_no_pessimization_default(self):
        """Проверим, что офферы идут выше моделей если они более релевантны"""

        response = self.report.request_json(
            'place=prime&hid=32&allow-collapsing=1&rids=213&text=model&rearr-factors=market_search_enable_offer_pessimization=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"titles": {"raw": "Offer No Model hid32 5"}},
                        {"type": "model", "id": 325},
                        {"type": "model", "id": 324},
                        {"type": "model", "id": 323},
                        {"type": "model", "id": 322},
                        {"type": "model", "id": 321},
                        {"titles": {"raw": "Offer No Model hid32 4"}},
                        {"titles": {"raw": "Offer No Model hid32 3"}},
                        {"titles": {"raw": "Offer No Model hid32 2"}},
                        {"titles": {"raw": "Offer No Model hid32 1"}},
                    ]
                }
            },
            allow_different_len=True,
            preserve_order=True,
        )

    def test_pessimization_reverse_exp(self):
        """Проверим, что офферы в обратном эксперименте идут ниже моделей несмотря на то, что они более релевантны"""

        for text in ["", "&text=model"]:
            response = self.report.request_json(
                'place=prime&hid=32{}&allow-collapsing=1&rids=213&rearr-factors=market_no_offer_pessimization_reverse=1;market_search_enable_offer_pessimization=1&rearr-factors=market_metadoc_search=no'.format(  # noqa
                    text
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"type": "model", "id": 325},
                            {"type": "model", "id": 324},
                            {"type": "model", "id": 323},
                            {"type": "model", "id": 322},
                            {"type": "model", "id": 321},
                            {"titles": {"raw": "Offer No Model hid32 5"}},
                            {"titles": {"raw": "Offer No Model hid32 4"}},
                            {"titles": {"raw": "Offer No Model hid32 3"}},
                            {"titles": {"raw": "Offer No Model hid32 2"}},
                            {"titles": {"raw": "Offer No Model hid32 1"}},
                        ]
                    }
                },
                allow_different_len=True,
                preserve_order=True,
            )

    def test_pessimization_reverse_exp_2hids(self):
        """Проверим, что офферы в обратном эксперименте идут ниже моделей несмотря на то, что они более релевантны
        Мы проверяем 2 категории - в одной галочка включена, а в другой - неопределена, поэтому
        во всей выдаче используется cтандартное поведение"""

        response = self.report.request_json(
            'place=prime&hid=32&hid=30&allow-collapsing=1&rids=213&numdoc=20'
            '&rearr-factors=market_no_offer_pessimization_reverse=1;market_search_enable_offer_pessimization=1'
            ';rearr-factors=market_metadoc_search=no;market_max_offers_per_shop_count=5'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 325},
                        {"type": "model", "id": 305},
                        {"type": "model", "id": 324},
                        {"type": "model", "id": 304},
                        {"type": "model", "id": 323},
                        {"type": "model", "id": 303},
                        {"type": "model", "id": 322},
                        {"type": "model", "id": 302},
                        {"type": "model", "id": 321},
                        {"type": "model", "id": 301},
                        {"titles": {"raw": "Offer No Model hid32 5"}},
                        {"titles": {"raw": "Offer No Model hid30 5"}},
                        {"titles": {"raw": "Offer No Model hid32 4"}},
                        {"titles": {"raw": "Offer No Model hid30 4"}},
                        {"titles": {"raw": "Offer No Model hid32 3"}},
                        {"titles": {"raw": "Offer No Model hid30 3"}},
                        {"titles": {"raw": "Offer No Model hid32 2"}},
                        {"titles": {"raw": "Offer No Model hid30 2"}},
                        {"titles": {"raw": "Offer No Model hid32 1"}},
                        {"titles": {"raw": "Offer No Model hid30 1"}},
                    ]
                }
            },
            allow_different_len=True,
            preserve_order=True,
        )

    def test_pessimization_no_exp(self):
        """Проверим, что оффера пессимизировались, т.к. вне эксперимента мы не смотрим на pessimize_offers"""

        response = self.report.request_json('place=prime&hid=31&allow-collapsing=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 315},
                        {"type": "model", "id": 314},
                        {"type": "model", "id": 313},
                        {"type": "model", "id": 312},
                        {"type": "model", "id": 311},
                        {"titles": {"raw": "Offer No Model hid31 5"}},
                        {"titles": {"raw": "Offer No Model hid31 4"}},
                        {"titles": {"raw": "Offer No Model hid31 3"}},
                        {"titles": {"raw": "Offer No Model hid31 2"}},
                        {"titles": {"raw": "Offer No Model hid31 1"}},
                    ]
                }
            },
            preserve_order=True,
        )

    def test_pessimization_empty_models_in_guru_offer_category(self):
        """Проверим, что модели без офферов не бустятся и не пессимизирутся, а ранжируются вместе с офферами"""

        response = self.report.request_json('place=prime&hid=33&allow-collapsing=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"type": "model", "id": 331},
                        {"titles": {"raw": "Offer No Model hid33 1"}},
                        {"type": "model", "id": 332},
                    ]
                }
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
