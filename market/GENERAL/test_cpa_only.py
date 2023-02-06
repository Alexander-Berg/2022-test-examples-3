#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MarketSku, Model, Offer, Region, RegionalModel, Shop
from core.testcase import TestCase, main


class _C:

    rid_unknown = 3


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regional_models += [
            RegionalModel(hyperid=175941311, rids=[54, 194], has_good_cpa=False, has_cpa=True),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=11),
            Model(hyperid=2, hid=22),
            Model(hyperid=3, hid=33),
            Model(hyperid=175941311, hid=44),
            Model(hyperid=5, hid=91308),
            Model(hyperid=10557849, hid=66),
            Model(hyperid=12345, hid=91408, title='cpc-модель'),
            Model(hyperid=10, hid=44),
        ]

        cls.index.regiontree = [
            Region(rid=194, name='Саратов'),
            Region(
                rid=54,
                name='Екб',
                children=[
                    Region(rid=100500, name='улица в ЕКб'),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], cpa=Shop.CPA_REAL, name='CPA Магазин в Москве'),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[213],
                cpa=Shop.CPA_NO,
                cpc=Shop.CPC_REAL,
                name='CPC Магазин в Москве',
            ),
            Shop(
                fesh=3,
                priority_region=213,
                regions=[157],
                home_region=149,
                cpa=Shop.CPA_REAL,
                name='CPA Магазин в Минске',
            ),
            Shop(fesh=4, priority_region=54, regions=[54, 194], cpa=Shop.CPA_REAL, name='СPA Екб'),
            Shop(fesh=5, priority_region=54, regions=[54, 194], cpa=Shop.CPA_NO, cpc=Shop.CPC_REAL, name='CPC Екб'),
            Shop(fesh=6, priority_region=43, regions=[43], cpa=Shop.CPA_REAL, name='СPA Казань'),
            Shop(
                fesh=1017176,
                priority_region=43,
                regions=[43],
                cpa=Shop.CPA_NO,
                cpc=Shop.CPC_REAL,
                name='CPC Metro - Казань',
            ),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=1, hid=11, sku=1),
            MarketSku(hyperid=2, hid=22, sku=2),
            MarketSku(hyperid=3, hid=33, sku=3),
            MarketSku(hyperid=665306170, hid=44, sku=4),
            MarketSku(hyperid=5, hid=91308, sku=5),
            MarketSku(hyperid=10557849, hid=66, sku=6),
            MarketSku(hyperid=10, hid=44, sku=1010),
        ]

        cls.index.offers += [
            Offer(fesh=1, title="CPA офер", cpa=Offer.CPA_REAL, hyperid=1, hid=11),
            # TODO: для test_cpa_no_skip_by_sku, этому оферу нужно задать sku из конфига, когда появятся соответствующие данные
            Offer(fesh=2, title="CPA_NO офер", cpa=Offer.CPA_NO, hyperid=1, hid=11),
            Offer(fesh=2, title="CPA_NO офер, единственный в модели", cpa=Offer.CPA_NO, hyperid=2, hid=22),
            Offer(fesh=3, title="CPA офер в Минске", cpa=Offer.CPA_REAL, hyperid=3, hid=33),
            Offer(fesh=4, title="CPA офер #2", cpa=Offer.CPA_REAL, hyperid=175941311, price=200, hid=44),
            Offer(
                fesh=5, title="CPA_NO офер, модель из конфига", cpa=Offer.CPA_NO, price=100, hyperid=175941311, hid=44
            ),
            Offer(fesh=6, title="CPA офер в Казани", cpa=Offer.CPA_REAL, hyperid=5, hid=91308),
            Offer(fesh=6, title="CPA_NO офер в Казани", cpa=Offer.CPA_NO, hyperid=5, hid=91308),
            Offer(fesh=1017176, title="CPA_NO офер в Метро - Казань", cpa=Offer.CPA_NO, hyperid=5, hid=91308),
            Offer(fesh=4, title="CPA офер #3", cpa=Offer.CPA_REAL, hyperid=10557849, hid=66),
            Offer(fesh=5, title="CPA_NO офер, доп. модель из чанка", cpa=Offer.CPA_NO, hyperid=10557849, hid=66),
            Offer(fesh=4, title="CPA_NO офер из HardHID", cpa=Offer.CPA_NO, hid=91408, hyperid=8),
            Offer(
                fesh=4,
                title="CPA офер из HardHID чтобы первому было не одиноко",
                cpa=Offer.CPA_REAL,
                hid=91408,
                hyperid=9,
            ),
            Offer(fesh=11, title="CPA_NO одинокий офер из HardHID", cpa=Offer.CPA_NO, hid=91340),
            Offer(fesh=4, title="CPA_NO офер", cpa=Offer.CPA_NO, hid=91408),
            Offer(fesh=5, title="CPA_NO 10", cpa=Offer.CPA_NO, hyperid=10, hid=44),
        ]

    def test_cpa_no_allowed_by_model(self):
        # Не фильтруем CPC-офер: в модели больше нет других CPA
        response = self.report.request_json(
            'place=productoffers&rids=213&hyperid=2&hid=22&rearr-factors=market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {'results': [{'titles': {'raw': 'CPA_NO офер, единственный в модели'}}]},
            allow_different_len=False,
        )

    def test_hard_cpc_models(self):
        # Не фильтруем CPC-офер: регион не из списка EnabledInRegions
        response = self.report.request_json('place=prime&text=cpc-model&rearr-factors=market_cpa_only_by_index=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "cpaCount": 0,
                    "results": [
                        {"titles": {"raw": "cpc-модель"}},
                        {"titles": {"raw": "CPC офер 3-1"}},
                        {"titles": {"raw": "CPC офер 4-2"}},
                    ],
                }
            },
        )

    # def test_cpa_no_skip_by_sku(self):
    #     # Фильтруем CPC-офер: SKU из конфига cpa_only
    #     response = self.report.request_json('place=productoffers&hyperid=1&hid=11')
    #     self.assertFragmentIn(response, {
    #         "search": {
    #             "cpaCount": 1,
    #             "results": [
    #                 {
    #                     'titles': {'raw': 'CPA офер'}
    #                 }
    #             ]
    #         }
    #     })

    def test_disable_cpa_only_filtration(self):
        # Должны быть все оферы даже те что есть в конфиге
        response = self.report.request_json(
            'place=productoffers&hyperid=1&hid=11&rearr-factors=market_cpa_only_enabled=0;market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 2, "results": [{'titles': {'raw': 'CPA офер'}}, {'titles': {'raw': 'CPA_NO офер'}}]}},
        )
        self.assertFragmentIn(response, {"filters": [{"id": "cpa"}]})

    def test_filter_by_country(self):
        # Фильтруем по коду страны
        response = self.report.request_json(
            'place=productoffers&rids=213&hyperid=3&hid=33&rearr-factors=market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "cpaCount": 0,
                }
            },
        )

    def test_filter_by_hard_hid(self):
        response = self.report.request_json(
            'place=prime&hyperid=8&rearr-factors=market_cpa_only_enabled=0;market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
        )

        # Проверка на одинокие cpc оффера (теперь и в трудноступных местах)
        response = self.report.request_json(
            'place=prime&hyperid=8&rids=100500&rearr-factors=market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
        )

        # У этого нет cpa в категории, ему повезло
        response = self.report.request_json('place=prime&hid=91340&rearr-factors=market_cpa_only_by_index=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
        )

    def test_no_good_cpa_offer(self):
        # Не фильтруем CPC-офер: model из конфига cpa_only, но cpa слишком дорогой
        response = self.report.request_json(
            'place=productoffers&rids=54&hyperid=175941311&hid=44&rearr-factors=market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                }
            },
        )

    def test_food_tags_disabled_by_default(self):
        # Ничего не фильтруется т.к. продуктовые категории отключены до особого распоряжения
        response = self.report.request_json(
            'place=productoffers&rids=43&hyperid=5&hid=91308&cpa=any&rearr-factors=market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 3,
                    "results": [
                        {'titles': {'raw': 'CPA офер в Казани'}},
                        {'titles': {'raw': 'CPA_NO офер в Казани'}},
                        {'titles': {'raw': 'CPA_NO офер в Метро - Казань'}},
                    ],
                }
            },
        )

    def test_chunk_model_filter_disabled(self):
        # Не фильтруем CPC-офер: model из дополнительного чанка,
        # явно отключен tag models-17-03-21
        response = self.report.request_json(
            'place=productoffers&rids=54&hyperid=10557849&cpa=any&rearr-factors=market_cpa_only_disabled_tags=models-17-03-21;market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {'titles': {'raw': 'CPA офер #3'}},
                        {'titles': {'raw': 'CPA_NO офер, доп. модель из чанка'}},
                    ],
                }
            },
        )

    @classmethod
    def prepare_brand_procucts_from_hardhids(cls):
        cls.index.models += [
            Model(hyperid=101, hid=16044621, vendor_id=404, title="coffee 101"),
            Model(hyperid=102, hid=16044621, vendor_id=404, title="coffee 102"),
            Model(hyperid=103, hid=16044621, vendor_id=404, title="coffee 103"),
            Model(hyperid=104, hid=16044621, vendor_id=404, title="coffee 104"),
        ]

        cls.index.shops += [
            Shop(fesh=101, priority_region=5, regions=[1], cpa=Shop.CPA_REAL, name='CPA Магазин 1'),
            Shop(fesh=102, priority_region=5, regions=[2], cpa=Shop.CPA_REAL, name='CPA Магазин 2'),
            Shop(fesh=103, priority_region=5, regions=[3], cpa=Shop.CPA_NO, cpc=Shop.CPC_REAL, name='CPC Магазин 3'),
            Shop(
                fesh=104, priority_region=5, regions=[4], cpa=Shop.CPA_REAL, cpc=Shop.CPC_REAL, name='CPA CPC Магазин 4'
            ),
        ]

        cls.index.offers += [
            Offer(fesh=101, title="CPA офер 1-1", cpa=Offer.CPA_REAL, hyperid=101, hid=16044621, vendor_id=404),
            Offer(
                fesh=103, title="CPC офер 3-1", cpa=Offer.CPA_NO, is_cpc=True, hyperid=103, hid=16044621, vendor_id=404
            ),
            Offer(fesh=104, title="CPA офер 4-1", cpa=Offer.CPA_REAL, hyperid=104, hid=16044621, vendor_id=404),
            Offer(
                fesh=104, title="CPC офер 4-2", cpa=Offer.CPA_NO, is_cpc=True, hyperid=104, hid=16044621, vendor_id=404
            ),
        ]

    def test_brand_procucts_from_hardhids(self):
        # выдаем все модели для вендоров
        response = self.report.request_json(
            'place=brand_products&vendor_id=404&pp=7&entities=product&rearr-factors=market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "results": [
                        {'titles': {'raw': 'coffee 101'}},
                        {'titles': {'raw': 'coffee 102'}},
                        {'titles': {'raw': 'coffee 103'}},
                        {'titles': {'raw': 'coffee 104'}},
                    ],
                }
            },
        )

    # для случая, когда market_cpa_only_by_index отключен, а мета не передает has_cpa,
    # проверим, что выдаются модели из HardHIDs, для которых есть CPA офферы
    def test_cpa_only_for_models(self):
        response = self.report.request_json(
            'place=prime&vendor_id=404&pp=7&entities=product&rids=5'
            '&rearr-factors=market_cpa_only_enabled=1;market_cpa_only_by_index=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {'titles': {'raw': 'coffee 101'}},
                        {'titles': {'raw': 'coffee 104'}},
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()
