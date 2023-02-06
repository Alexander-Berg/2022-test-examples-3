#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, HyperCategory, MarketSku, MnPlace, Model, Offer, Opinion, Shop, Tax
from core.testcase import TestCase, main
from core.matcher import LikeUrl, Wildcard, Not, Contains


def expected_url(ssku, feedid, hid):
    url_params = {"utm_source": "market", "utm_medium": "cpc", "utm_content": hid}
    if (ssku is not None) and (feedid is not None):
        url_params["utm_term"] = "{}.{}".format(feedid, ssku)
    return LikeUrl(url_params=url_params)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1000, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Blue model",
                hyperid=100,
                hid=1,
                sku=100,
                blue_offers=[BlueOffer(price=1000, feedid=1, offerid=100100)],
            )
        ]
        cls.index.offers += [Offer(title="White model", hyperid=101, hid=2, fesh=2, price=10000)]

    def test_productoffers(self):
        request = "place=productoffers&hyperid={}&offers-set=default&show-urls=external&rgb={}"

        # White on White
        for rgb in ("green", "", "green_with_blue"):
            response = self.report.request_json(request.format(101, rgb))
            self.assertFragmentNotIn(response, {"results": [{"urls": {"direct": Wildcard("*&utm_*")}}]})

        # Blue on Blue
        response = self.report.request_json(request.format(100, "blue"))
        self.assertFragmentNotIn(response, {"results": [{"urls": {"direct": Wildcard("*&utm_*")}}]})

    def test_prime(self):
        request = "place=prime&text={}&rgb={}"

        # White on White
        for rgb in ("green", "", "green_with_blue"):
            response = self.report.request_json(request.format("White offer", rgb))
            self.assertFragmentNotIn(response, {"results": [{"urls": {"direct": Wildcard("*&utm_*")}}]})

        # Blue on Blue
        response = self.report.request_json(request.format("Blue offer", "blue"))
        self.assertFragmentNotIn(response, {"results": [{"urls": {"direct": Wildcard("*&utm_*")}}]})

    def test_parallel(self):
        request = "place=parallel&text={}&rgb={}"

        # White on White
        response = self.report.request_bs(request.format("White offer", "green"))
        self.assertFragmentNotIn(
            response, {"market_model": [{"showcase": {"items": [{"title": {"url": Wildcard("*&utm_*")}}]}}]}
        )

        # Blue on Blue
        response = self.report.request_bs(request.format("Blue offer", "blue"))
        self.assertFragmentNotIn(
            response, {"market_model": [{"showcase": {"items": [{"title": {"url": Wildcard("*&utm_*")}}]}}]}
        )

    @classmethod
    def prepare_blue_implicit_model_wizard(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=19863002,
                name='Оптика',
                children=[
                    HyperCategory(hid=19863006, name='Контактные линзы 12'),
                    HyperCategory(hid=19863008, name='Контактные линзы 24'),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=198632,
                hid=19863006,
                ts=198632000,
                title="Acuvue OASYS with Hydraclear Plus (12 линз)",
                has_blue_offers=True,
                opinion=Opinion(rating=3.5, rating_count=17, total_count=19),
            ),
            Model(
                hyperid=198633,
                hid=19863008,
                ts=198633000,
                title="Acuvue OASYS with Hydraclear Plus (24 линзы)",
                has_blue_offers=True,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=1986320, title="Acuvue OASYS 12", hyperid=198632, blue_offers=[BlueOffer(feedid=3, offerid=1001)]
            ),
            MarketSku(
                sku=1986330, title="Acuvue OASYS 24", hyperid=198633, blue_offers=[BlueOffer(feedid=3, offerid=1002)]
            ),
        ]

    @classmethod
    def prepare_parallel_blue_base_search(cls):
        cls.index.offers += [
            Offer(title="ya yandeksofon white 1", ts=226641),
            Offer(title="ya yandeksofon white 2", ts=226642),
            Offer(title="ya yandeksofon white 3", ts=226643),
            Offer(title="ya yandeksofon white 4", ts=226644),
            Offer(title="ya yandeksofon white 5", ts=226645),
        ]
        cls.index.mskus += [
            MarketSku(
                title="ya yandeksofon blue", sku=226640001, blue_offers=[BlueOffer(ts=226646, feedid=3, offerid=1003)]
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226641).respond(0.99)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226642).respond(0.97)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226643).respond(0.95)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226644).respond(0.93)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226645).respond(0.91)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 226646).respond(0.94)

    def test_wizards_offer_incut(self):
        response = self.report.request_bs_pb(
            "place=parallel&text=yandeksofon&rearr-factors=market_offers_wizard_incut_url_type=External;market_adg_offer_url_type=External"
        )
        _ = "utm_source%3Dmarket%26utm_medium%3Dcpc%26utm_term%3D3.1003%26utm_content%3D"
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": "ya yandeksofon white 1", "raw": True}},
                                    "urlForCounter": Not(Contains("utm")),
                                    "offercardUrl": Not(Contains("utm")),
                                },
                                "thumb": {
                                    "urlForCounter": Not(Contains("utm")),
                                    "offercardUrl": Not(Contains("utm")),
                                },
                            }
                        ]
                    }
                }
            },
        )

    def test_wizards_model_incut(self):
        response = self.report.request_bs_pb('place=parallel&text=acuvue')
        self.assertFragmentNotIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "url": Wildcard(
                                            "*utm_source%3Dmarket%26utm_medium%3Dcpc%26utm_term%3D3.100*%26utm_content%3D*"
                                        )
                                    }
                                }
                            ]
                        }
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
