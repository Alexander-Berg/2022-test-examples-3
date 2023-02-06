#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CpaCategory,
    CpaCategoryType,
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NoKey
from unittest import skip


# нужно сделать так чтобы CPA_NON_GURU работало так же как CPC_AND_CPA
# заодно потестим и CPC_AND_CPA
class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=3,
                name="Центральный федеральный округ",
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name="Москва"),
                ],
            ),
            Region(rid=2, name="Санкт-Петербург"),
        ]

        cls.index.shops += [
            Shop(fesh=2, priority_region=213, regions=[225], name='MoscowShop WoCPA'),
            Shop(fesh=3, priority_region=213, regions=[225], name='MoscowShop WoCPA'),
            Shop(fesh=6, priority_region=2, regions=[225], name='PiterShop WoCPA'),
            Shop(fesh=8, priority_region=2, regions=[225], name='PiterShop WoCPA'),
        ]

    class Expect(object):
        @staticmethod
        def offer_title(hid, fesh):
            return 'Offer in HID{0} from SHOP-{1}'.format(hid, fesh)

        @staticmethod
        def offers_json(hid, order):
            return {"results": [{"titles": {"raw": T.Expect.offer_title(hid, fesh)}} for fesh in order]}

        @staticmethod
        def offers_xml(hid, order, tag='offers'):
            return '<{0}>{1}</{0}>'.format(
                tag,
                '\n'.join(
                    [
                        '<offer><raw-title>{}</raw-title></offer>'.format(T.Expect.offer_title(hid, fesh))
                        for fesh in order
                    ]
                ),
            )

    @classmethod
    def prepare_simple_test_data(cls):

        cls.index.hypertree += [
            HyperCategory(hid=100, name="CPC and CPA", output_type=HyperCategoryType.SIMPLE),
            HyperCategory(
                hid=200,
                name="CPA non-guru",
                children=[
                    HyperCategory(hid=201, name="CPA non-guru Moscow", output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=202, name="CPA non-guru Piter", output_type=HyperCategoryType.SIMPLE),
                ],
            ),
            HyperCategory(hid=300, name="CPA with CPC pessimization", output_type=HyperCategoryType.GURU),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=201, regions=[213], cpa_type=CpaCategoryType.CPA_NON_GURU),
            CpaCategory(hid=300, regions=[2, 213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
        ]

        # В каждой категории для каждого магазина заводим по офферу (cpa офферы для магазинов торгующих по CPA)

        for hid in [100, 201, 202, 300]:
            for fesh in [2, 3, 6, 8]:
                ts = hid * 10 + fesh
                bucket_id = hid * 2000 + fesh
                cls.index.delivery_buckets += [
                    DeliveryBucket(
                        bucket_id=bucket_id,
                        fesh=fesh,
                        regional_options=[
                            RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                            RegionalDelivery(rid=2, options=[DeliveryOption(price=10, day_from=1, day_to=2)]),
                        ],
                    )
                ]
                cls.index.offers += [
                    Offer(
                        hid=hid,
                        title=T.Expect.offer_title(hid, fesh),
                        ts=ts,
                        fesh=fesh,
                        bid=10 * fesh,
                        fee=100 * (fesh + 1),
                        delivery_buckets=[bucket_id],
                    )
                ]
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.5 + 0.00001 * fesh)

    def check_search_places(self, rid, hid, order):
        '''провка порядка офферов на prime'''
        search_request = '&text=HID{hid}&rids={rid}&rearr-factors=market_skip_cpa_category_settings=skip_none'.format(
            hid=hid, rid=rid
        )
        response = self.report.request_json('place=prime' + search_request)
        self.assertFragmentIn(response, T.Expect.offers_json(hid, order), preserve_order=True)

    def check_cpa_auction(self, rid, hid, count_non_cpa):
        response = self.report.request_json(
            'place=prime&text=HID{hid}&rids={rid}&debug=da&debug-doc-count=10&rearr-factors=market_skip_cpa_category_settings=skip_none;disable_panther_quorum=0'.format(
                hid=hid, rid=rid
            )
        )
        self.assertEqual(
            response.count(
                {
                    "debug": {
                        "properties": {"DOCUMENT_AUCTION_TYPE": "CPC", "IS_CPA": NoKey("IS_CPA"), "FEE": NoKey("FEE")}
                    }
                }
            ),
            count_non_cpa,
        )

    @skip('cpa аукциона больше нет, есть тикет на выпиливание cpa из репротра MARKETOUT-29596')
    def test_non_guru_in_moscow(self):
        # Проверяем что базовая функциональность prime, в Москве работает
        # в категории 201 работает следующим образом:
        # гибридный аукцион, cpc-офферы пессимизируются
        # ну и заодно так же как в категории cpa_with_cpc_pessimization (300)
        # productoffers - больше не завязаны на тип категории, не проверяем их
        for hid in [201, 300]:

            # В Москве
            # на prime используется cpa-аукцион (и до черты и после сначала идут cpa-офферы от магазинов 1, 4, 5, 7 ставки которых влияют на релевантность)
            search_request = '&text=HID{}&rids=213&rearr-factors=disable_panther_quorum=0'.format(hid)
            response = self.report.request_json('place=prime' + search_request)
            self.assertFragmentIn(response, T.Expect.offers_json(hid, order=[3, 2, 8, 6]), preserve_order=True)

            # 4 не cpa оффера с множителем аукциона 1
            self.check_cpa_auction(rid=213, hid=hid, count_non_cpa=4)

        # в категории 202 (не cpa_non_guru в Москве) как в обычной cpc_and_cpa категории (100)
        # все по CPC-аукциону
        for hid in [100, 202]:
            response = self.report.request_json(
                'place=prime&text=HID{hid}&rids=213&debug=da&debug-doc-count=10&rearr-factors=disable_panther_quorum=0'.format(
                    hid=hid
                )
            )
            self.assertEqual(response.count({"debug": {"properties": {"DOCUMENT_AUCTION_TYPE": "CPC"}}}), 4)

    @skip('cpa аукциона больше нет, есть тикет на выпиливание cpa из репротра MARKETOUT-29596')
    def test_non_guru_in_piter(self):

        # в Питере вот эта логика
        # не в Москве категория работает по гибридному аукциону, если в Москве она не cpc_and_cpa
        # https://arc.yandex-team.ru/wsvn/arc/trunk/arcadia/market/report/src/hybrid_auction.cpp?op=blame&rev=2502580&peg=2502580#l218
        # приводит к тому что категория 201 ранжируется по гибридному аукциону, а 202 нет (т.к. она не cpa_non_guru в москве)
        # productoffers - больше не завязаны на тип категории, не проверяем их
        for hid in [201, 300]:
            # на prime cpa-офферы вверху
            search_request = '&text=HID{}&rids=2&rearr-factors=market_skip_cpa_category_settings=skip_none;disable_panther_quorum=0'.format(
                hid
            )
            response = self.report.request_json('place=prime' + search_request)
            self.assertFragmentIn(response, T.Expect.offers_json(hid, order=[8, 6, 3, 2]), preserve_order=True)

            # 4 не cpa оффера с множителем аукциона 1
            self.check_cpa_auction(rid=2, hid=hid, count_non_cpa=4)

        # в категории 202 как в обычной cpc_and_cpa категории (100)  хотя она и указана как cpa_non_guru
        for hid in [100, 202]:
            response = self.report.request_json(
                'place=prime&text=HID{hid}&rids=213&debug=da&debug-doc-count=10&rearr-factors=disable_panther_quorum=0'.format(
                    hid=hid
                )
            )
            self.assertEqual(response.count({"debug": {"properties": {"DOCUMENT_AUCTION_TYPE": "CPC"}}}), 4)


if __name__ == '__main__':
    main()
