#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import market.idx.datacamp.proto.offer.OfferMeta_pb2 as OfferMeta

from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Offer,
    RegionalDelivery,
    RtyOffer,
    Shop,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import Absent


def build_offer_disabled(value_bits, mask_bits):
    """
    Построение значения битовой маски скрытия офера в RTY индексе.
    Отсюда: https://a.yandex-team.ru/arc/trunk/arcadia/market/report/lite/test_rty_qpipe.py?rev=r7600582#L1122
    """
    result = 0
    for b in value_bits:
        result |= 1 << b
    for b in mask_bits:
        result |= 1 << (b + 16)
    return result


MARKET_STOCK_DISABLED = build_offer_disabled([OfferMeta.MARKET_STOCK], [OfferMeta.MARKET_STOCK])


class T(TestCase):

    dsbs_shop = Shop(
        fesh=10,
        cpa=Shop.CPA_REAL,  # DSBS
        priority_region=213,
        warehouse_id=41234,
    )

    blue_shop = Shop(
        fesh=20,
        cpa=Shop.CPA_REAL,
        blue=Shop.BLUE_REAL,  # Blue
        priority_region=213,
        warehouse_id=145,
    )

    dsbs_offer = Offer(
        fesh=10,
        cpa=Offer.CPA_REAL,  # DSBS
        waremd5='DsbsWithoutMsku______g',
        title="DSBS Offer",
        hyperid=100,
        delivery_options=[
            DeliveryOption(price=350, day_from=1, day_to=7, order_before=10),
        ],
    )

    dummy_white_offer = Offer(
        fesh=10,
        waremd5='DummyWhite___________g',
        title="Dummy White Offer",
        hyperid=100,
    )

    dummy_blue_offer = BlueOffer(
        fesh=20,
        waremd5='DummyBlue____________g',
        title="Dummy Blue Offer",
        hyperid=100,
        delivery_buckets=[9000],
        vat=Vat.VAT_10,
        price=1000,
    )

    msku = MarketSku(
        sku=1,
        hyperid=100,
        blue_offers=[
            dummy_blue_offer,
        ],
    )

    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True  # Включить RTY
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            T.dsbs_shop,
            T.blue_shop,
        ]

        cls.index.offers += [
            T.dsbs_offer,
            T.dummy_white_offer,
        ]

        cls.index.mskus += [
            T.msku,
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=9000,
                fesh=20,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=400, day_from=3, day_to=4)],
                    ),
                ],
            ),
        ]

    @staticmethod
    def dsbs_offer_output(ware_md5, warehouse_id=None):
        result = {
            'entity': "offer",
            'offerColor': "white",
            'cpa': "real",
            'wareId': ware_md5,
        }
        if warehouse_id is not None:
            result['supplier'] = {
                'entity': "shop",
                'warehouseId': warehouse_id,
            }
        return result

    @staticmethod
    def white_offer_output(ware_md5):
        return {
            'entity': "offer",
            'offerColor': "white",
            'wareId': ware_md5,
            'supplier': Absent(),
        }

    @staticmethod
    def blue_offer_output(ware_md5, warehouse_id=None):
        result = {
            'entity': "offer",
            'offerColor': "blue",
            'wareId': ware_md5,
        }
        if warehouse_id is not None:
            result['supplier'] = {
                'entity': "shop",
                'warehouseId': warehouse_id,
            }
        return result

    @staticmethod
    def make_rearr(all_rty_dynamics=False, enable_dsbs_warehouse=None):
        rearr = (
            [
                # Нужно включить: 1) RTY динамики вообще и 2) скрытие по стокам через RTY динамики
                "rty_dynamics=1",
                "rty_stock_dynamics=2",
            ]
            if all_rty_dynamics
            else []
        ) + (
            [
                "market_enable_dsbs_warehouse={}".format(1 if enable_dsbs_warehouse else 0),
            ]
            if enable_dsbs_warehouse is not None
            else []
        )

        return ";".join(rearr)

    REQUEST_TEMPLATE = "place=prime&hyperid=100&rids=213&allow-collapsing=0&rgb={rgb}&rearr-factors={rearr}"

    def test_rty_stocks_for_dsbs_on_white(self):
        """
        Проверка работы скрытия DSBS оферов через RTY источник MARKET_STOCK.
        На белом маркете. Дополнительно проверяется, что не аффектим другие белые оферы
        """

        request = T.REQUEST_TEMPLATE.format(
            rgb="white",
            rearr=T.make_rearr(all_rty_dynamics=True),
        )

        # На белом маркете видим DSBS и обычный белый оферы
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                T.dsbs_offer_output(T.dsbs_offer.ware_md5),
                T.white_offer_output(T.dummy_white_offer.ware_md5),
            ],
        )

        # Скрываем DSBS офер через RTY индекс
        self.rty.offers += [
            RtyOffer(
                disabled=MARKET_STOCK_DISABLED,
                feedid=T.dsbs_offer.feed_id(),
                offerid=T.dsbs_offer.offer_id(),
                disabled_ts=1,
            ),
        ]

        # DSBS скрыт, обычный белый остался
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            [
                T.dsbs_offer_output(T.dsbs_offer.ware_md5),
            ],
        )
        self.assertFragmentIn(
            response,
            [
                T.white_offer_output(T.dummy_white_offer.ware_md5),
            ],
        )

    def test_rty_stocks_for_dsbs_on_blue(self):
        """
        Проверка работы скрытия DSBS оферов через RTY источник MARKET_STOCK.
        На синем маркете. Дополнительно проверяется, что не аффектим другие синие оферы
        """

        request = T.REQUEST_TEMPLATE.format(
            rgb="blue",
            rearr=T.make_rearr(all_rty_dynamics=True),
        )

        # На синем маркете видим DSBS и обычный синий оферы
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                T.dsbs_offer_output(T.dsbs_offer.ware_md5),
                T.blue_offer_output(T.dummy_blue_offer.waremd5),
            ],
        )

        # Скрываем DSBS офер через RTY индекс
        self.rty.offers += [
            RtyOffer(
                disabled=MARKET_STOCK_DISABLED,
                feedid=T.dsbs_offer.feed_id(),
                offerid=T.dsbs_offer.offer_id(),
                disabled_ts=1,
            ),
        ]

        # DSBS скрыт, обычный синий остался
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            [
                T.dsbs_offer_output(T.dsbs_offer.ware_md5),
            ],
        )
        self.assertFragmentIn(
            response,
            [
                T.blue_offer_output(T.dummy_blue_offer.waremd5),
            ],
        )

    def test_supplier_warehouse_for_dsbs(self):
        """
        Проверяем, что для DSBS оферов под флагом market_enable_dsbs_warehouse в выдаче в supplier появляется warehouse_id.
        Так же как для синих. Но для белых — никогда.  См. подробнее:
        https://a.yandex-team.ru/review/1572817/files/2#file-0-51643192:R2829
        https://st.yandex-team.ru/MARKETOUT-35417
        """

        for enable_dsbs_warehouse in [False, True]:
            request = T.REQUEST_TEMPLATE.format(
                rgb="white",
                rearr=T.make_rearr(enable_dsbs_warehouse=enable_dsbs_warehouse),
            )

            # На белом склад присутствует у оферов:  белого — никогда, синего и DSBS — всегда
            response = self.report.request_json(request + '&rearr-factors=market_metadoc_search=no')
            self.assertFragmentIn(
                response,
                [
                    T.dsbs_offer_output(
                        T.dsbs_offer.ware_md5, T.dsbs_shop.warehouse_id if enable_dsbs_warehouse else Absent()
                    ),
                    T.white_offer_output(T.dummy_white_offer.ware_md5),
                    T.blue_offer_output(T.dummy_blue_offer.waremd5, T.blue_shop.warehouse_id),
                ],
            )

            request = T.REQUEST_TEMPLATE.format(
                rgb="blue",
                rearr=T.make_rearr(enable_dsbs_warehouse=enable_dsbs_warehouse),
            )

            # На синем склад присутствует у: DSBS офера под флагом, синего — всегда
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    T.dsbs_offer_output(
                        T.dsbs_offer.ware_md5, T.dsbs_shop.warehouse_id if enable_dsbs_warehouse else Absent()
                    ),
                    T.blue_offer_output(T.dummy_blue_offer.waremd5, T.blue_shop.warehouse_id),
                ],
            )


if __name__ == '__main__':
    main()
