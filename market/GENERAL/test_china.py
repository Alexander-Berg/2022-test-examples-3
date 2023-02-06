#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, HyperCategoryType, Offer, Region, Shop, VCluster


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    # MARKETOUT-12117
    @classmethod
    def prepare_vcluster_collapsing(cls):
        cls.index.regiontree += [
            Region(
                rid=134,
                name='Китай',
                region_type=Region.COUNTRY,
                children=[Region(rid=10590, name='Пекин', region_type=Region.CITY)],
            )
        ]

        cls.index.vclusters += [VCluster(title='puhovik', hid=1211701, vclusterid=1001211701)]

        cls.index.hypertree += [HyperCategory(hid=1211701, output_type=HyperCategoryType.CLUSTERS, visual=True)]

        cls.index.shops += [Shop(fesh=1211701, home_region=134, cpc=Shop.CPC_NO, cpa=Shop.CPA_REAL, is_global=True)]

        cls.index.offers += [
            Offer(vclusterid=1001211701, fesh=1211701, cpa=Offer.CPA_REAL, title='puhovik 1'),
            Offer(vclusterid=1001211701, fesh=1211701, cpa=Offer.CPA_REAL, title='puhovik 2'),
            Offer(fesh=1211701, cpa=Offer.CPA_REAL, title='puhovik 3', offerid=121170103, hid=1211701),
        ]

    def test_without_collapsing(self):
        """
        Делаем обычный запрос, проверяем, что в выдаче нет офферов, схлопнутых в кластер,
        но есть сами оффера по отдельности
        и есть оффер, не сматченный в кластер
        """
        response = self.report.request_json('place=prime&hid=1211701&fesh=1211701')
        self.assertFragmentNotIn(response, {'entity': 'product', 'type': 'cluster', 'id': 1001211701})

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'puhovik 1',
                },
                'model': {'id': 1001211701},
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'puhovik 2',
                },
                'model': {'id': 1001211701},
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'puhovik 3',
                },
            },
        )

    def test_with_collapsing_via_home_region(self):
        """
        Делаем запрос с фильтром страны,
        проверяем, что в выдаче есть схлопнутый кластер,
        но нет офферов с этим кластером,
        а есть оффер без кластера
        """
        response = self.report.request_json('place=prime&hid=1211701&fesh=1211701&home_region_filter=134')
        self.assertFragmentIn(response, {'entity': 'product', 'type': 'cluster', 'id': 1001211701})

        self.assertFragmentNotIn(response, {'entity': 'offer', 'model': {'id': 1001211701}})

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'puhovik 3',
                },
            },
        )

    def test_with_collapsing_via_flag(self):
        """
        Делаем запрос с флагом схлопывания,
        проверяем, что в выдаче есть схлопнутый кластер,
        но нет офферов
        """
        response = self.report.request_json('place=prime&hid=1211701&fesh=1211701&allow-collapsing=1')
        self.assertFragmentIn(response, {'entity': 'product', 'type': 'cluster', 'id': 1001211701})

        self.assertFragmentNotIn(response, {'entity': 'offer', 'model': {'id': 1001211701}})

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'puhovik 3',
                },
            },
        )

    def test_with_collapsing_via_touch(self):
        """
        Делаем запрос, как будто мы - тач,
        проверяем, что в выдаче есть схлопнутый кластер,
        но нет офферов
        """
        response = self.report.request_json('place=prime&hid=1211701&fesh=1211701&touch=1')
        self.assertFragmentIn(response, {'entity': 'product', 'type': 'cluster', 'id': 1001211701})

        self.assertFragmentNotIn(response, {'entity': 'offer', 'model': {'id': 1001211701}})

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'puhovik 3',
                },
            },
        )

    # see MARKETOUT-12422
    @classmethod
    def prepare_12422(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1242201, output_type=HyperCategoryType.SIMPLE),
            HyperCategory(hid=1242202, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=1242204, output_type=HyperCategoryType.CLUSTERS, visual=True),
        ]

        cls.index.vclusters += [
            VCluster(title='kurtka', hid=1242204, vclusterid=1001242204),
        ]

        cls.index.offers += [
            # оффер в simple категории
            Offer(fesh=1242201, hid=1242201, title="pesok"),
            # оффер в gurulight категории
            Offer(fesh=1242201, hid=1242202, title="lustra"),
        ]

        # офферы в кластерной категории
        cls.index.offers += [
            Offer(vclusterid=1001242204, fesh=1242201, title='kurtka 1'),
            Offer(vclusterid=1001242204, fesh=1242201, title='kurtka 2'),
            Offer(fesh=1242201, title='kurtka 3', offerid=124220103, hid=1242204),
        ]

    def test_offers_are_not_collapsing_in_touch_in_simple_category(self):
        """
        Делаем запрос в категорию, не помеченную как кластерная,
        проверяем, что есть несхлопнутый оффер
        """
        response = self.report.request_json('place=prime&fesh=1242201&touch=1&hid=1242201')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'pesok',
                },
            },
        )

    def test_offers_are_not_collapsing_in_touch_in_gurulight_category(self):
        """
        Делаем запрос в категорию, не помеченную как кластерная,
        проверяем, что есть несхлопнутый оффер
        """
        response = self.report.request_json('place=prime&fesh=1242201&touch=1&hid=1242202')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'lustra',
                },
            },
        )

    def test_offers_are_collapsing_in_touch_in_cluster_category(self):
        """
        Делаем запрос в категорию, помеченную как кластерная,
        проверяем, что НЕТ несхлопнутых офферов с моделями (куртка 1 и 2),
        но ЕСТЬ несхлопнутый оффер без модели (куртка 3)
        """
        response = self.report.request_json('place=prime&fesh=1242201&touch=1&hid=1242204')
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'kurtka 1',
                },
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'kurtka 2',
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'titles': {
                    'raw': 'kurtka 3',
                },
            },
        )


if __name__ == '__main__':
    main()
