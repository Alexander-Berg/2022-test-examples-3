#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, Offer, Opinion, Shop
from core.matcher import NotEmptyList


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    # MARKETOUT-13450
    # инцидент с тем, что у офферного колдунщика показываются офферы с CPC-ссылкой от CPA-only магазинов
    @classmethod
    def prepare_13450(cls):
        """
        1345001..04 - CPA+CPC
        1345011..14 - CPA-only
        """
        cls.index.shops += [
            Shop(fesh=1345001, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=1345002, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=1345003, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=1345004, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=1345011, cpc=Shop.CPC_NO, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=1345012, cpc=Shop.CPC_NO, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=1345013, cpc=Shop.CPC_NO, cpa=Shop.CPA_REAL, priority_region=213),
            Shop(fesh=1345014, cpc=Shop.CPC_NO, cpa=Shop.CPA_REAL, priority_region=213),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1345001, visual=True),
        ]

        cls.index.models += [
            Model(hyperid=1345001, hid=1345001, title='strange 1'),
            Model(hyperid=1345002, hid=1345001, title='strange 2'),
            Model(hyperid=1345003, hid=1345001, title='strange 3'),
            Model(hyperid=1345011, hid=1345001, title='zelipopa 1'),
            Model(hyperid=1345012, hid=1345001, title='zelipopa 2'),
            Model(hyperid=1345013, hid=1345001, title='zelipopa 3'),
        ]

        # Offers for CPA+CPC shops
        cls.index.offers += [
            Offer(hyperid=1345001, hid=1345001, fesh=1345001, price=10000, title='strange 1'),
            Offer(hyperid=1345002, hid=1345001, fesh=1345001, price=11000, title='strange 2'),
            Offer(hyperid=1345003, hid=1345001, fesh=1345001, price=12000, title='strange 3'),
            Offer(hyperid=1345001, hid=1345001, fesh=1345002, price=10000, title='strange 1'),
            Offer(hyperid=1345002, hid=1345001, fesh=1345002, price=11000, title='strange 2'),
            Offer(hyperid=1345003, hid=1345001, fesh=1345002, price=12000, title='strange 3'),
            Offer(hyperid=1345001, hid=1345001, fesh=1345003, price=10000, title='strange 1'),
            Offer(hyperid=1345002, hid=1345001, fesh=1345003, price=11000, title='strange 2'),
            Offer(hyperid=1345003, hid=1345001, fesh=1345003, price=12000, title='strange 3'),
            Offer(hyperid=1345001, hid=1345001, fesh=1345004, price=10000, title='strange 1'),
            Offer(hyperid=1345002, hid=1345001, fesh=1345004, price=11000, title='strange 2'),
            Offer(hyperid=1345003, hid=1345001, fesh=1345004, price=12000, title='strange 3'),
        ]

        # Offers for CPA only shops
        cls.index.offers += [
            Offer(hyperid=1345011, hid=1345001, fesh=1345011, price=10000, title='zelipopa 1'),
            Offer(hyperid=1345012, hid=1345001, fesh=1345011, price=11000, title='zelipopa 2'),
            Offer(hyperid=1345013, hid=1345001, fesh=1345011, price=12000, title='zelipopa 3'),
            Offer(hyperid=1345011, hid=1345001, fesh=1345012, price=10000, title='zelipopa 1'),
            Offer(hyperid=1345012, hid=1345001, fesh=1345012, price=11000, title='zelipopa 2'),
            Offer(hyperid=1345013, hid=1345001, fesh=1345012, price=12000, title='zelipopa 3'),
            Offer(hyperid=1345011, hid=1345001, fesh=1345013, price=10000, title='zelipopa 1'),
            Offer(hyperid=1345012, hid=1345001, fesh=1345013, price=11000, title='zelipopa 2'),
            Offer(hyperid=1345013, hid=1345001, fesh=1345013, price=12000, title='zelipopa 3'),
            Offer(hyperid=1345011, hid=1345001, fesh=1345014, price=10000, title='zelipopa 1'),
            Offer(hyperid=1345012, hid=1345001, fesh=1345014, price=11000, title='zelipopa 2'),
            Offer(hyperid=1345013, hid=1345001, fesh=1345014, price=12000, title='zelipopa 3'),
        ]

    def test_show_market_offers_wizard_for_cpa_plus_cpc(self):
        """
        Для CPA+CPC магазинов оставляем офферный колдунщик
        """
        response = self.report.request_bs('place=parallel&text=strange')
        self.assertFragmentIn(
            response,
            {
                'market_offers_wizard': [
                    {
                        "showcase": {"items": NotEmptyList()},
                        "offer_count": 12,
                        "text": [{"__hl": {"text": "4 магазина. Выбор по параметрам.", "raw": True}}],
                    }
                ]
            },
        )

    def test_not_show_market_offers_wizard_for_cpa_only(self):
        """
        Для CPA-only магазинов убираем офферный колдунщик
        """
        response = self.report.request_bs('place=parallel&text=zelipopa')
        self.assertFragmentNotIn(response, {'market_offers_wizard': []})

    @classmethod
    def prepare_pp_404(cls):
        """
        pp=404 (market_offers_wizard)
        """
        cls.index.shops += [
            Shop(fesh=1345021, priority_region=213, cpa=Shop.CPA_REAL, name='OZON.ru'),
            Shop(
                fesh=1345022, priority_region=213, cpa=Shop.CPA_REAL, name='Rumall.com', is_global=True, cpc=Shop.CPC_NO
            ),
            Shop(fesh=1345023, priority_region=213, cpa=Shop.CPA_REAL, name='Armashop'),
            Shop(fesh=1345024, priority_region=213, cpa=Shop.CPA_REAL, name='RusMarta'),
            Shop(fesh=1345025, priority_region=213, cpa=Shop.CPA_REAL, name='VIDEOTOY'),
            Shop(fesh=1345026, priority_region=213, cpa=Shop.CPA_REAL, name='ipCam24.ru'),
            Shop(fesh=1345027, priority_region=213, cpa=Shop.CPA_REAL, name='DigiPulse.ru'),
            Shop(fesh=1345028, priority_region=213, cpa=Shop.CPA_REAL, name='Mnogo'),
            Shop(fesh=1345029, priority_region=213, cpa=Shop.CPA_REAL, name='Karlson'),
            Shop(fesh=1345030, priority_region=213, cpa=Shop.CPA_REAL, name='ABVGD'),
            Shop(fesh=1345031, priority_region=213, cpa=Shop.CPA_REAL, name='EYoZhZI'),
            Shop(fesh=1345032, priority_region=213, cpa=Shop.CPA_REAL, name='KLMNO'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1345021, name='Umnye chasy i braslety', visual=True),
        ]

        cls.index.offers += [
            Offer(hyperid=1345021, fesh=1345021, hid=1345021, price=2973, title='detskiy gps 1', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345022, fesh=1345022, hid=1345021, price=1518, title='detskiy gps 2', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345023, fesh=1345023, hid=1345021, price=3770, title='detskiy gps 3', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345024, fesh=1345024, hid=1345021, price=2550, title='detskiy gps 4', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345025, fesh=1345025, hid=1345021, price=2450, title='detskiy gps 5', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345026, fesh=1345026, hid=1345021, price=2290, title='detskiy gps 6', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345027, fesh=1345027, hid=1345021, price=6950, title='detskiy gps 7', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345028, fesh=1345028, hid=1345021, price=4490, title='detskiy gps 8', cpa=Offer.CPA_REAL),
            Offer(hyperid=1345029, fesh=1345029, hid=1345021, price=1990, title='detskiy gps 9', cpa=Offer.CPA_REAL),
        ]

    def test_cpa_only_shop_is_not_shown_in_market_offers_wizard_size_tile_incut(self):
        """
        Проверим, что оффер от CPA-only магазина не показывается в боковой врезке офферного колдунщика
        """
        response = self.report.request_bs("place=parallel&text=detskiy")
        self.assertFragmentNotIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "offers": [
                            {
                                "shop_name": "Rumall.com",
                            }
                        ]
                    }
                ]
            },
        )

    @classmethod
    def prepare_pp_405(cls):
        """
        pp=405 (market_model -> offers)
        """
        cls.index.shops += [
            Shop(fesh=1345041, priority_region=213, cpa=Shop.CPA_REAL, name='Magazin 41'),
            Shop(fesh=1345042, priority_region=213, cpa=Shop.CPA_REAL, name='Magazin 42'),
            Shop(fesh=1345043, priority_region=213, cpa=Shop.CPA_REAL, name='Magazin 43', cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(hyperid=1345041, hid=1345041, title="samosval 2000", opinion=Opinion(rating=2.5)),
        ]

        cls.index.offers += [
            Offer(hyperid=1345041, price=888, title="samosval 2000", fesh=1345041, bid=500, cpa=Offer.CPA_REAL),
            Offer(hyperid=1345041, price=888, title="samosval 2000", fesh=1345042, bid=500, cpa=Offer.CPA_REAL),
            Offer(hyperid=1345041, price=888, title="samosval 2000", fesh=1345043, bid=500, cpa=Offer.CPA_REAL),
        ]

    def test_cpa_only_shop_is_not_shown_in_offer_incut_in_model_wizard_snippet(self):
        """
        Проверим, что оффер от CPA-only магазина не показывается в офферной врезке в сниппете модельного колдунщика
        """
        response = self.report.request_bs("place=parallel&text=samosval+2000")
        # проверяем, что собственно модельный колдунщик есть и офферы тоже
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "greenUrl": {
                                        "text": "Magazin 41",
                                    }
                                },
                                {
                                    "greenUrl": {
                                        "text": "Magazin 42",
                                    }
                                },
                            ]
                        }
                    }
                ]
            },
        )
        # а вот магазина CPA-only быть не должно
        self.assertFragmentNotIn(
            response,
            {
                "market_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "greenUrl": {
                                        "text": "Magazin 43",
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
