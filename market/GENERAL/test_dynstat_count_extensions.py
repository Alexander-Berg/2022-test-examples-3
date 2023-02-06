#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, MnPlace, Model, Offer
from core.testcase import TestCase, main
from core.matcher import Contains


class T(TestCase):
    ts = 100500

    @classmethod
    def generate_ts(cls, doc_priority):
        result = cls.ts
        cls.ts += 1
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, result).respond(doc_priority / 1000.0)
        return result

    @classmethod
    def prepare_dynstat_for_collapsed(cls):
        m_count = 4
        offers_count = 4

        mskus_count = m_count
        models_count = m_count

        cls.index.mskus += [
            MarketSku(
                sku=101 + j,
                hid=800,
                hyperid=801 + j,
                blue_offers=[
                    BlueOffer(price=1 + 100 * j + i, title='blue_offer{}.{}'.format(j, i), ts=cls.generate_ts(i))
                    for i in range(offers_count)
                ],
            )
            for j in range(mskus_count)
        ]
        cls.index.models += [Model(hid=800, hyperid=801 + mskus_count + j) for j in range(models_count)]
        cls.index.offers += [
            Offer(
                title='white_offer{}.{}'.format(model_i, offer_i),
                hid=800,
                price=1 + 100 * (mskus_count + model_i) + offer_i,
                hyperid=801 + mskus_count + model_i,
                ts=cls.generate_ts(offer_i),
            )
            for model_i in range(models_count)
            for offer_i in range(offers_count)
        ]

    def test_dynstat_for_collapsed(self):
        '''
        Checks that "market_dynstat_non_cpa_slice_sz" numeric flag correctly increases amount of
        non-CPA documents (after default 60 or amount determined by max(market_dynstat_count,numdoc))
        to request default offers for.
        '''
        m_count = 4
        offers_count = 4

        response = self.report.request_json(
            "hid=800&"
            "place=prime&"
            "how=aprice&"
            "allow-collapsing=1&"
            "numdoc=" + str(m_count) + "&"
            "page=1&"
            "use-default-offers=1&"
            "rearr-factors=market_dynstat_count=" + str(m_count + 1) + "&"
            "rearr-factors=market_dynstat_non_cpa_slice_sz=" + str(m_count - 2)
        )
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'titles': {'raw': Contains('blue_offer')},
                        }
                    ],
                }
                for i in range(m_count)
            },
        )
        response = self.report.request_json(
            "hid=800&"
            "place=prime&"
            "how=aprice&"
            "allow-collapsing=1&"
            "numdoc=" + str(m_count) + "&"
            "page=2&"
            "use-default-offers=1&"
            "rearr-factors=market_dynstat_count=" + str(m_count + 1) + "&"
            "rearr-factors=market_dynstat_non_cpa_slice_sz=" + str(m_count - 2)
        )
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'titles': {'raw': 'white_offer' + str(i) + '.' + str(offers_count - 1)},
                        }
                    ],
                }
                for i in range(m_count - 1)
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'titles': {'raw': Contains('white_offer' + str(m_count - 1) + '.')},
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_dynstat_for_models(cls):
        m_count = 4
        offers_count = 4

        mskus_count = m_count
        models_count = m_count

        cls.index.mskus += [
            MarketSku(
                sku=201 + j,
                hid=700,
                hyperid=701 + j,
                title='sku 1.' + str(j),
                blue_offers=[
                    BlueOffer(price=1 + 100 * j + i, title='titled blue_offer{}.{}'.format(j, i), ts=cls.generate_ts(i))
                    for i in range(offers_count)
                ],
            )
            for j in range(mskus_count)
        ]
        cls.index.models += [
            Model(hid=700, hyperid=701 + mskus_count + j, title='titled model 2.' + str(j)) for j in range(models_count)
        ]
        cls.index.offers += [
            Offer(
                title='white_offer{}.{}'.format(model_i, offer_i),
                hid=700,
                price=1 + 100 * (mskus_count + model_i) + offer_i,
                hyperid=701 + mskus_count + model_i,
                ts=cls.generate_ts(offer_i),
            )
            for model_i in range(models_count)
            for offer_i in range(offers_count)
        ]

    def test_dynstat_for_models(self):
        '''
        Checks almost the same as "test_dynstat_for_collapsed", however in this test case we search
        by "text=..." defined only for models to ensure that we not drop any models found not by offer.
        '''
        m_count = 4
        offers_count = 4

        response = self.report.request_json(
            "text=titled&"
            "place=prime&"
            "how=aprice&"
            "allow-collapsing=1&"
            "numdoc=" + str(m_count) + "&"
            "page=2&"
            "use-default-offers=1&"
            "rearr-factors=market_dynstat_count=" + str(m_count + 1) + "&"
            "rearr-factors=market_dynstat_non_cpa_slice_sz=" + str(m_count - 2) + "&"
            "rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'titles': {'raw': 'white_offer' + str(i) + '.' + str(offers_count - 1)},
                        }
                    ],
                }
                for i in range(m_count - 1)
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'titles': {'raw': Contains('white_offer' + str(m_count - 1) + '.')},
                        }
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()
