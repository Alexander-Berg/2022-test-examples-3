#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

'''
Тестирование гиперлокальных функций для оферов Еды
'''

from core.types import (
    Offer,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import EmptyList
from core.types.express_partners import EatsWarehousesEncoder
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

HID = EATS_CATEG_ID


class _Shops:
    shop_eats_1 = Shop(
        business_fesh=1,
        fesh=11,
        datafeed_id=111,
        warehouse_id=111,
        cpa=Shop.CPA_REAL,
        is_eats=True,
    )

    shop_eats_2 = Shop(
        business_fesh=1,
        fesh=12,
        datafeed_id=112,
        warehouse_id=112,
        cpa=Shop.CPA_REAL,
        is_eats=True,
    )

    shop_lavka = Shop(
        business_fesh=2,
        fesh=21,
        datafeed_id=121,
        warehouse_id=121,
        cpa=Shop.CPA_REAL,
        is_lavka=True,
    )

    shop_dsbs = Shop(
        business_fesh=3,
        fesh=31,
        datafeed_id=131,
        warehouse_id=131,
        cpa=Shop.CPA_REAL,
    )

    shop_without_warehouse = Shop(
        business_fesh=4,
        fesh=41,
        is_eats=True,
        cpa=Shop.CPA_REAL,
        disable_auto_warehouse_id=True,
    )


class _Offers:
    eats_offer_1 = Offer(
        waremd5=Offer.generate_waremd5('eats_offer_1'),
        hid=HID,
        shop=_Shops.shop_eats_1,
        is_eda_retail=True,
        is_express=True,
    )

    eats_offer_2 = Offer(
        waremd5=Offer.generate_waremd5('eats_offer_2'),
        hid=HID,
        shop=_Shops.shop_eats_2,
        is_eda_retail=True,
        is_express=True,
    )

    lavka_offer = Offer(
        waremd5=Offer.generate_waremd5('lavka_offer'),
        hid=HID,
        shop=_Shops.shop_lavka,
        is_lavka=True,
        is_express=True,
    )

    dsbs_offer = Offer(
        waremd5=Offer.generate_waremd5('dsbs_offer'),
        hid=HID,
        shop=_Shops.shop_dsbs,
        is_express=True,  # Пока что в репорте нет фильтрации по экспрессу для ДСБС оферов
    )

    without_warehouse_offer = Offer(
        hid=HID,
        waremd5=Offer.generate_waremd5('without_warehouse'),
        shop=_Shops.shop_without_warehouse,
        has_delivery_options=False,
        is_express=True,
        is_eda_retail=True,
        cpa=Offer.CPA_REAL,
    )


class T(TestCase):
    @classmethod
    def prepare_shops(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(HID, Stream.FMCG.value),
        ]
        cls.index.shops += [
            _Shops.shop_eats_1,
            _Shops.shop_eats_2,
            _Shops.shop_lavka,
            _Shops.shop_dsbs,
            _Shops.shop_without_warehouse,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers.eats_offer_1,
            _Offers.eats_offer_2,
            _Offers.lavka_offer,
            _Offers.dsbs_offer,
            _Offers.without_warehouse_offer,
        ]

    def test_eats_hyperlocality(self):
        """
        Проверяем скрытие товаров Еды на маркете по списку гиперлокальных складов.
        Не действует на клиентов Еды и Лавки, чтобы не испортить их выдачу (они список складов не передают)
        """
        eats_wh_request = "&eats-warehouses-compressed={}"

        eats_client = "&client=eats"
        lavka_client = "&client=lavka"
        other_client = "&client=other"

        def check(flags, offers):
            request_prime = "place=prime&hid={hid}&enable-foodtech-offers=eda_retail,lavka".format(hid=HID)

            response = self.report.request_json(request_prime + flags)
            self.assertFragmentIn(
                response, [{'entity': 'offer', 'wareId': offer.waremd5} for offer in offers], allow_different_len=False
            )

        all_offers = [
            _Offers.eats_offer_1,
            _Offers.eats_offer_2,
            _Offers.lavka_offer,
            _Offers.dsbs_offer,
            _Offers.without_warehouse_offer,
        ]

        # Клиенты лавки, еды не фильтруют офера по складам
        wh = EatsWarehousesEncoder().add_warehouse(wh_id=_Shops.shop_eats_1.warehouse_id).encode()
        check(eats_wh_request.format(wh) + lavka_client, all_offers)
        check(eats_wh_request.format(wh) + eats_client, all_offers)

        # Любой другой клиент видит только офера, с запрошенных складов.
        # ДСБС офер не фильтруется по гиперлокальности
        wh = EatsWarehousesEncoder().add_warehouse(wh_id=_Shops.shop_eats_1.warehouse_id).encode()
        check(eats_wh_request.format(wh) + other_client, [_Offers.eats_offer_1, _Offers.dsbs_offer])

        wh = EatsWarehousesEncoder().add_warehouse(wh_id=_Shops.shop_lavka.warehouse_id).encode()
        check(eats_wh_request.format(wh) + other_client, [_Offers.lavka_offer, _Offers.dsbs_offer])

        wh = (
            EatsWarehousesEncoder()
            .add_warehouse(wh_id=_Shops.shop_eats_1.warehouse_id)
            .add_warehouse(wh_id=_Shops.shop_lavka.warehouse_id)
            .encode()
        )
        check(
            eats_wh_request.format(wh) + other_client, [_Offers.eats_offer_1, _Offers.lavka_offer, _Offers.dsbs_offer]
        )

        wh = (
            EatsWarehousesEncoder()
            .add_warehouse(wh_id=_Shops.shop_eats_1.warehouse_id)
            .add_warehouse(wh_id=_Shops.shop_eats_2.warehouse_id)
            .encode()
        )
        check(
            eats_wh_request.format(wh) + other_client, [_Offers.eats_offer_1, _Offers.eats_offer_2, _Offers.dsbs_offer]
        )

        wh = EatsWarehousesEncoder().add_warehouse(wh_id=_Shops.shop_dsbs.warehouse_id).encode()
        check(eats_wh_request.format(wh) + other_client, [_Offers.dsbs_offer])

    def test_eats_offer_without_warehouse_id(self):
        """
        Если нет склада, то не показываем офер на маркете
        """
        offerid = _Offers.without_warehouse_offer.waremd5
        wh = (
            EatsWarehousesEncoder()
            .add_warehouse(wh_id=_Shops.shop_eats_1.warehouse_id)
            .add_warehouse(wh_id=_Shops.shop_eats_2.warehouse_id)
            .encode()
        )

        request = 'place=offerinfo&rids=0&regset=2&show-urls=&offerid={}&debug=1&enable-foodtech-offers=1&eats-warehouses-compressed={}'.format(
            offerid, wh
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': EmptyList(),
                },
                'debug': {'brief': {'filters': {'HYPERLOCAL_WAREHOUSE_MISSED': 1}}},
            },
            allow_different_len=False,
        )

        # Для клиентов Еды и Лавки это не распространяется
        for client in ['eats', 'lavka']:
            response = self.report.request_json(request + '&client={}'.format(client))
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': offerid,
                    }
                ],
                allow_different_len=False,
            )

    def test_express_flag(self):
        """
        Проверяем, что оферы Еды и лавки отмечены признаком экспресс
        """
        request_prime = (
            "place=prime&hid={hid}&enable-foodtech-offers=eda_retail,lavka&filter-express-delivery=1".format(hid=HID)
        )
        eats_wh_request = "&eats-warehouses-compressed={}"

        wh = (
            EatsWarehousesEncoder()
            .add_warehouse(wh_id=_Shops.shop_eats_1.warehouse_id)
            .add_warehouse(wh_id=_Shops.shop_eats_2.warehouse_id)
            .add_warehouse(wh_id=_Shops.shop_lavka.warehouse_id)
            .encode()
        )

        response = self.report.request_json(request_prime + eats_wh_request.format(wh))
        for offer in [
            _Offers.eats_offer_1,
            _Offers.eats_offer_2,
            _Offers.lavka_offer,
        ]:
            self.assertFragmentIn(
                response, [{'entity': 'offer', 'wareId': offer.waremd5, 'delivery': {'isExpress': True}}]
            )


if __name__ == '__main__':
    main()
