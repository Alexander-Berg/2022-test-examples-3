#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, Offer, OfferDimensions, Shop, Region
from core.types.sku import MarketSku, BlueOffer
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

ROOT_EATS_HID = EATS_CATEG_ID
FMCG_MODEL = 111
FMCG_DSBS_MODEL = 112
FMCG_DSBS_RETAIL_MODEL = 113

FMCG_BLUE_MODEL = 121
FMCG_DSBS_BLUE_MODEL = 122

FMCG_CATEGORY = 13360738
# Специально для теста сделал эту категорию вложенной чтобы показать,
# что родительская категория не влияет на вложенную, разрешенную категорию
FMCG_ALLOWED_FOR_DSBS_CATEGORY = 1111

MOSCOW_AND_MOSCOW_REGION = 1
MOSCOW = 213
ZELENOGRAD = 216
NOT_EXP_REGION = 880
NOT_EXP_CHILD_REGION = 15555


class T(TestCase):
    dsbs_shop = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        regions=[213],
        name='dsbs_shop',
        client_id=11,
        cpa=Shop.CPA_REAL,
    )

    disabled_dsbs_fmcg_offer = Offer(
        title="dsbs_fmcg_without_dsbs",
        hyperid=FMCG_MODEL,
        shop=dsbs_shop,
        price=1100,
        cpa=Offer.CPA_REAL,
        waremd5=Offer.generate_waremd5("dsbs_in_fmcg"),
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    allowed_dsbs_fmcg_offer = Offer(
        title="dsbs_fmcg_with_dsbs",
        hyperid=FMCG_DSBS_MODEL,
        shop=dsbs_shop,
        price=1100,
        cpa=Offer.CPA_REAL,
        waremd5=Offer.generate_waremd5("dsbs_in_non_fmcg"),
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    alowed_dsbs_fmcg_eda_retail_offer = Offer(
        title="dsbs_fmcg_eda_retail_offer",
        hyperid=FMCG_DSBS_RETAIL_MODEL,
        shop=dsbs_shop,
        price=1100,
        cpa=Offer.CPA_REAL,
        waremd5=Offer.generate_waremd5("dsbs_in_fmcg_retail"),
        dimensions=OfferDimensions(width=10, height=20, length=15),
        is_eda_retail=True,
    )

    blue_shop = Shop(
        fesh=43,
        datafeed_id=4241,
        priority_region=213,
        regions=[213],
        name='blue_shop',
        client_id=12,
        cpa=Shop.CPA_REAL,
        blue=Shop.BLUE_REAL,
    )

    blue_fmcg_offer = BlueOffer(
        title="blue_fmcg_without_dsbs",
        hyperid=FMCG_BLUE_MODEL,
        shop=blue_shop,
        price=1100,
        waremd5=Offer.generate_waremd5("blue_in_fmcg"),
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    blue_fmcg_with_dsbs_offer = BlueOffer(
        title="blue_fmcg_without_dsbs",
        hyperid=FMCG_DSBS_BLUE_MODEL,
        shop=blue_shop,
        price=1100,
        waremd5=Offer.generate_waremd5("blue_in_non_fmcg"),
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(ROOT_EATS_HID, Stream.FMCG.value),
            CategoryStreamRecord(FMCG_CATEGORY, Stream.FMCG.value),
            CategoryStreamRecord(FMCG_ALLOWED_FOR_DSBS_CATEGORY, Stream.FMCG.value),
        ]
        cls.index.hypertree += [
            HyperCategory(
                hid=ROOT_EATS_HID,
                name='ROOT_EATS_HID',
                children=[
                    HyperCategory(
                        hid=FMCG_CATEGORY,
                        name='FMCG',
                        children=[
                            HyperCategory(hid=FMCG_ALLOWED_FOR_DSBS_CATEGORY, name='FMCG FOR DSBS'),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.models += [
            # for dsbs
            Model(hyperid=FMCG_MODEL, hid=FMCG_CATEGORY, title='fmcg model'),
            Model(hyperid=FMCG_DSBS_MODEL, hid=FMCG_ALLOWED_FOR_DSBS_CATEGORY, title='fmcg + dsbs model'),
            Model(hyperid=FMCG_DSBS_RETAIL_MODEL, hid=FMCG_CATEGORY, title='fmcg + dsbs + retail model'),
            # for blue offers
            Model(hyperid=FMCG_BLUE_MODEL, hid=FMCG_CATEGORY, title='fmcg model blue'),
            Model(hyperid=FMCG_DSBS_BLUE_MODEL, hid=FMCG_ALLOWED_FOR_DSBS_CATEGORY, title='fmcg + dsbs model blue'),
        ]

        cls.index.shops += [T.dsbs_shop, T.blue_shop]

        cls.index.offers += [T.disabled_dsbs_fmcg_offer, T.allowed_dsbs_fmcg_offer, T.alowed_dsbs_fmcg_eda_retail_offer]
        cls.index.mskus += [
            MarketSku(
                hyperid=FMCG_BLUE_MODEL,
                sku=FMCG_BLUE_MODEL,
                blue_offers=[T.blue_fmcg_offer],
            ),
            MarketSku(
                hyperid=FMCG_DSBS_BLUE_MODEL,
                sku=FMCG_DSBS_BLUE_MODEL,
                blue_offers=[T.blue_fmcg_with_dsbs_offer],
            ),
        ]
        cls.index.regiontree += [
            Region(
                rid=3,
                children=[
                    Region(
                        rid=MOSCOW_AND_MOSCOW_REGION,
                        children=[
                            Region(
                                rid=MOSCOW,
                                children=[
                                    Region(rid=ZELENOGRAD, children=[]),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
            Region(
                rid=110,
                children=[
                    Region(
                        rid=NOT_EXP_REGION,
                        children=[
                            Region(rid=NOT_EXP_CHILD_REGION, children=[]),
                        ],
                    ),
                ],
            ),
        ]
        cls.index.fmcg_parameters_config_records = {
            'dbs_pessimie_categories': [FMCG_CATEGORY],
            'fbs_pessimie_categories': [],
        }

    def test_fmcg_categories_with_dsbs_offers(self):
        """
        Оферы ДСБС скрываются в категориях FMCG при появлении реарр флага market_fmcg_disable_dsbs_offers
        """
        prime_request = "place=prime&hid={}&regset=1&use-default-offers=1&enable-foodtech-offers=eda_retail".format(
            FMCG_CATEGORY
        )
        product_offers_request = "place=productoffers&regset=1&hyperid={}&enable-foodtech-offers=eda_retail"

        rearr_enabled = "&rearr-factors=market_fmcg_disable_dsbs_offers=1"
        rearr_disabled = "&rearr-factors=market_fmcg_disable_dsbs_offers=0"

        exp_region = "&rids={}".format(ZELENOGRAD)
        not_exp_region = "&rids={}".format(NOT_EXP_CHILD_REGION)

        disabled_offers = [T.disabled_dsbs_fmcg_offer]

        allowed_offers = [
            T.allowed_dsbs_fmcg_offer,
            T.blue_fmcg_offer,
            T.blue_fmcg_with_dsbs_offer,
            T.alowed_dsbs_fmcg_eda_retail_offer,
        ]

        # Без реарр флага оферы ДСБС доступны
        for region in [exp_region, not_exp_region]:
            response = self.report.request_json(prime_request + region + rearr_disabled)
            for offer in disabled_offers + allowed_offers:
                self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.waremd5})

        # С флагом ДСБС доступны только в разрешенных категориях
        response = self.report.request_json(prime_request + rearr_enabled + exp_region)
        for offer in disabled_offers:
            self.assertFragmentNotIn(response, {'entity': 'offer', 'wareId': offer.waremd5})
        for offer in allowed_offers:
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.waremd5})

        # С флагом ДСБС, но не в регионе эксперимента оферы ДСБС доступны
        response = self.report.request_json(prime_request + rearr_enabled + not_exp_region)
        for offer in disabled_offers + allowed_offers:
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.waremd5})

        # Аналогично проверяем для product_offers
        for offer in disabled_offers + allowed_offers:
            response = self.report.request_json(
                product_offers_request.format(offer.hyperid) + rearr_disabled + exp_region
            )
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.waremd5})
        # C флагом офер ДСБС скрывается
        for offer in disabled_offers:
            response = self.report.request_json(
                product_offers_request.format(offer.hyperid) + rearr_enabled + exp_region
            )
            self.assertFragmentNotIn(response, {'entity': 'offer', 'wareId': offer.waremd5})
        # C флагом но не в регионе эксперимента офер ДСБС не скрывается
        for offer in disabled_offers:
            response = self.report.request_json(
                product_offers_request.format(offer.hyperid) + rearr_enabled + not_exp_region
            )
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.waremd5})
        for offer in allowed_offers:
            response = self.report.request_json(
                product_offers_request.format(offer.hyperid) + rearr_enabled + exp_region
            )
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.waremd5})


if __name__ == '__main__':
    main()
