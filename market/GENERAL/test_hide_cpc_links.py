#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicShop, Offer, Outlet, Shop


class T(TestCase):
    @classmethod
    def prepare_offers_must_be_rejected(cls):
        """
        st:MARKETOUT-11653
        """
        # Магазины не-CPA и CPA, попарно с hide_cpc_links - False и True
        cls.index.shops += [
            Shop(fesh=1165301, priority_region=213, cpa=Shop.CPA_NO),
            Shop(fesh=1165302, priority_region=213, cpa=Shop.CPA_NO),
            Shop(fesh=1165303, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=1165304, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        # Офферы
        # 1165301 - CPA, где возможно
        # 1165302 - все CPC
        cls.index.offers += [
            Offer(hyperid=1165301, fesh=1165301, price=100, cpa=Offer.CPA_NO),
            Offer(hyperid=1165301, fesh=1165302, price=100, cpa=Offer.CPA_NO),
            Offer(hyperid=1165301, fesh=1165303, price=100, cpa=Offer.CPA_REAL),
            Offer(hyperid=1165301, fesh=1165304, price=100, cpa=Offer.CPA_REAL),
            Offer(hyperid=1165302, fesh=1165301, price=100, cpa=Offer.CPA_NO),
            Offer(hyperid=1165302, fesh=1165302, price=100, cpa=Offer.CPA_NO),
            Offer(hyperid=1165302, fesh=1165303, price=100, cpa=Offer.CPA_NO),
            Offer(hyperid=1165302, fesh=1165304, price=100, cpa=Offer.CPA_NO),
        ]

    def test_cpc_are_not_hidden(self):
        """
        Запрашиваем офферы для модели 1165301

        В результат <s>не</s> должен попасть магазин 1165302 (см. MARKETOUT-11796)
        А магазин 1165304 должен быть, т.к. оффер CPA-ный
        """
        response = self.report.request_json('place=productoffers&hyperid=1165301')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "totalOffers": 4,
                    "results": [
                        {"entity": "offer", "model": {"id": 1165301}, "shop": {"id": 1165301}},
                        {"entity": "offer", "model": {"id": 1165301}, "shop": {"id": 1165303}},
                        {"entity": "offer", "model": {"id": 1165301}, "shop": {"id": 1165304}},
                    ],
                }
            },
        )

        self.assertFragmentIn(response, {"shop": {"id": 1165302}})

    def test_cpc_only_hidden(self):
        """
        Запрашиваем офферы для модели 1165301

        В результат <s>не</s> (см. MARKETOUT-11796) должен попасть магазин 1165302
        и магазин 1165304 - т.к. оффер CPC-шный
        """
        response = self.report.request_json('place=productoffers&hyperid=1165302')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 4,
                    "totalOffers": 4,
                    "results": [
                        {"entity": "offer", "model": {"id": 1165302}, "shop": {"id": 1165301}},
                        {"entity": "offer", "model": {"id": 1165302}, "shop": {"id": 1165303}},
                    ],
                }
            },
        )

        self.assertFragmentIn(response, {"shop": {"id": 1165302}})

        self.assertFragmentIn(response, {"shop": {"id": 1165304}})

    # MARKETOUT-12111
    @classmethod
    def prepare_for_hide_cpc_offers(cls):
        cls.index.shops += [
            Shop(fesh=1211101, cpa=Shop.CPA_REAL, regions=[213]),
            Shop(fesh=1211102, cpa=Shop.CPA_REAL, regions=[213]),
        ]

        cls.index.offers += [
            Offer(hyperid=1211101, fesh=1211101, price=10000, cpa=Offer.CPA_REAL, has_url=False),
            # for place=images
            Offer(title='offer_1211101_with_image', fesh=1211101, waremd5='offer_images_1211101aa', has_url=False),
            Offer(title='offer_1211102_with_image', fesh=1211102, waremd5='offer_images_1211102aa'),
        ]

        cls.index.outlets += [
            Outlet(point_id=1211101, fesh=1211101, region=213),
            Outlet(point_id=1211102, fesh=1211102, region=213),
        ]

    def test_cpc_offers_are_not_hidden(self):
        """
        Проверяем, что оффер есть для обычных запросов
        """
        response = self.report.request_json('place=productoffers&hyperid=1211101')
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1211101}})

    def test_cpc_offers_are_not_hidden_for_sovetnik(self):
        """
        Проверяем, что оффер есть, если client=sovetnik
        """
        response = self.report.request_json('place=productoffers&hyperid=1211101&client=sovetnik')
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1211101}})

    def test_cpc_offers_are_hidden_for_sovetnik_with_flag(self):
        """
        Проверяем, что оффера нет, если client=sovetnik с флагом &hide-offers-without-cpc-link=1
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=1211101&client=sovetnik&hide-offers-without-cpc-link=1'
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "model": {"id": 1211101}})

    def test_cpc_offers_are_not_hidden_for_content_api(self):
        """
        Проверяем, что оффер есть, если api=content
        """
        response = self.report.request_json('place=productoffers&hyperid=1211101&api=content')
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1211101}})

    def test_cpc_offers_are_hidden_for_content_api_with_flag(self):
        """
        Проверяем, что оффера нет, если api=content с флагом &hide-offers-without-cpc-link=1
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=1211101&api=content&hide-offers-without-cpc-link=1'
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "model": {"id": 1211101}})

    def test_images_hidden(self):
        """
        Проверяем что оффера нет в place=images
        """
        offers = self.report.request_images('place=images&offerid=offer_images_1211101aa')
        self.assertEqual(0, len(offers))

    def test_images_not_hidden(self):
        """
        Проверяем что оффер есть в place=images
        """
        offers = self.report.request_images('place=images&offerid=offer_images_1211102aa')
        self.assertEqual(1, len(offers))
        self.assertEqual(offers[0].Title, 'offer_1211102_with_image')

    def check_attributes(self, report, attributes):
        for grouping in report.Grouping:
            for group in grouping.Group:
                for document in group.Document:
                    for attribut in document.ArchiveInfo.GtaRelatedAttribute:
                        if attribut.Key in attributes:
                            self.assertEqual(attributes[attribut.Key], attribut.Value)

    # MARKETOUT-12218
    @classmethod
    def prepare_test_cpc_links(cls):
        """
        Создаём магазин и три оффера с разными настройками CPC
        """
        cls.index.shops += [
            Shop(fesh=1221801, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(hyperid=1221801, fesh=1221801, price=10000, cpa=Offer.CPA_REAL),
            Offer(hyperid=1221803, fesh=1221801, price=10000, cpa=Offer.CPA_NO),
        ]

    def test_cpc_links_are_hidden_for_real_cpa_offer(self):
        """
        Ссылка вырезается для CPA=real оффера
        """
        _ = self.report.request_json('place=productoffers&hyperid=1221801&show-urls=encrypted')
        self.show_log.expect().times(0)

    # MARKETOUT-12260
    @classmethod
    def prepare_cpa_reject_test(cls):
        cls.index.shops += [
            Shop(fesh=1226001, cpa=Shop.CPA_NO),
        ]

        cls.index.offers += [
            Offer(
                hyperid=122600101, fesh=1226001, price=10000, cpa=Offer.CPA_REAL, has_url=False, override_cpa_check=True
            ),
        ]

    # тесты для offer.cpa=real, shop.cpa=no
    def test_offer_cpa_real_and_shop_cpa_no(self):
        """
        Оффер: CPA=real (122600101)
        Магазин: CPA=no (1226001)
        Клиент: неизвестен

        Оффер должен скрываться в общем случае
        """
        response = self.report.request_json('place=productoffers&hyperid=122600101')
        self.assertFragmentNotIn(response, {'entity': 'offer', 'model': {'id': 122600101}})

    def test_offer_cpa_real_and_shop_cpa_no_client_abo(self):
        """
        Оффер: CPA=real (122600101)
        Магазин: CPA=no (1226001)
        Клиент: ABO

        Оффер должны показать
        """
        response = self.report.request_json('place=productoffers&hyperid=122600101&client=abo')
        self.assertFragmentIn(response, {'entity': 'offer', 'model': {'id': 122600101}})

    def test_offer_cpa_real_and_shop_cpa_no_client_partnerinterfacesandbox_via_client(self):
        """
        Оффер: CPA=real (122600101)
        Магазин: CPA=no (1226001)
        Клиент: партнерский интерфейс (песочница) - &client=partnerinterface&pi-from=sandbox&cpa=-no

        Оффер нужно скрыть
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=122600101&client=partnerinterface&pi-from=sandbox&cpa=-no'
        )
        self.assertFragmentNotIn(response, {'entity': 'offer', 'model': {'id': 122600101}})

    def test_offer_cpa_real_and_shop_cpa_no_client_partnerinterfacesandbox_via_cpa_minus_no(self):
        """
        Оффер: CPA=real (122600101)
        Магазин: CPA=no (1226001)
        Клиент: партнерский интерфейс (песочница) - &cpa=-no

        Оффер нужно скрыть
        """
        response = self.report.request_json('place=productoffers&hyperid=122600101&cpa=-no')
        self.assertFragmentNotIn(response, {'entity': 'offer', 'model': {'id': 122600101}})

    @classmethod
    def prepare_no_cpc_no_cpa_offers(cls):
        """Создаем магазин с CPA_NO и офферы без CPC в нем"""
        cls.index.shops += [
            Shop(fesh=1641602, regions=[213], cpa=Shop.CPA_NO),
        ]

        cls.index.offers += [
            Offer(fesh=1641602, cpa=Offer.CPA_NO, has_url=False),
        ]

    def test_no_cpa_no_cpc_offers_are_hidden(self):
        """Что тестируем: офферы без CPC и без CPA не показываются на выдаче
        и не вызывают ошибок в логах
        Проверяем, что ошибок нет и при выключении магазина динамиком
        """
        shop_id = 1641602
        response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
        self.assertFragmentNotIn(response, {"entity": "offer"})

        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(shop_id)]

        response = self.report.request_json('place=prime&rids=213&fesh={}&show-urls=external,cpa'.format(shop_id))
        self.assertFragmentNotIn(response, {"entity": "offer"})


if __name__ == '__main__':
    main()
