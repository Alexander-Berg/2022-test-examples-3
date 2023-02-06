#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import MarketSku, MnPlace, Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    shift_flags_doc_count = 10
    shift_flags_base_hyper_id = 14888841
    shift_flags_base_msku_id = 25999952
    shift_flags_base_offer_id = 36000063

    @classmethod
    def prepare_shift_flags(cls):

        cls.index.models = [
            Model(
                hyperid=cls.shift_flags_base_hyper_id + i,
                hid=8841,
                title='MODEL' + str(i),
                ts=cls.shift_flags_base_hyper_id + i,
            )
            for i in range(cls.shift_flags_doc_count)
        ]
        models_ts = [cls.shift_flags_base_hyper_id + i for i in range(cls.shift_flags_doc_count)]

        cls.index.mskus += [
            MarketSku(
                sku=cls.shift_flags_base_msku_id + i,
                hyperid=cls.shift_flags_base_hyper_id + i,
                hid=8841,
                title='SKU' + str(i),
                ts=cls.shift_flags_base_msku_id + i,
            )
            for i in range(cls.shift_flags_doc_count)
        ]
        mskus_ts = [cls.shift_flags_base_msku_id + i for i in range(cls.shift_flags_doc_count)]

        cls.index.offers += [
            Offer(
                sku=cls.shift_flags_base_msku_id + i,
                hid=8841,
                price=100500 + i,
                hyperid=cls.shift_flags_base_hyper_id + i,
                title='OFFER' + str(i),
                ts=cls.shift_flags_base_offer_id + i,
            )
            for i in range(cls.shift_flags_doc_count)
        ]
        offers_ts = [cls.shift_flags_base_offer_id + i for i in range(cls.shift_flags_doc_count)]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            for ts in models_ts + mskus_ts + offers_ts:
                cls.matrixnet.on_place(place, ts).respond(0.9)

    def shift_flags_response_frag(self, n=0, k=0):
        original_frag = {
            'search': {
                'results': [{'id': str(self.shift_flags_base_msku_id + i)} for i in range(self.shift_flags_doc_count)]
            }
        }
        original_frag['search']['results'][n], original_frag['search']['results'][k] = (
            original_frag['search']['results'][k],
            original_frag['search']['results'][n],
        )
        return original_frag

    def test_shift_flags(self):
        request = (
            'place=prime&hid=8841&rearr-factors=market_metadoc_search=skus&use-default-offers=1&text=SKU&how=aprice'
        )
        move_doc_from_pos_n_to_k_flag = '&rearr-factors=move_doc_from_pos_n_to_k=%d,%d'

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, self.shift_flags_response_frag(), preserve_order=True, allow_different_len=False
        )

        response = self.report.request_json(request + move_doc_from_pos_n_to_k_flag % (7, 3))
        self.assertFragmentIn(
            response, self.shift_flags_response_frag(7, 3), preserve_order=True, allow_different_len=False
        )


if __name__ == '__main__':
    main()
