#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Offer, Shop, VCluster

# description length erased up to 512 symbols, so let's create description with bigger length
OFFER_FULL_DESCRIPTION = 'Ъ' * 504 + ' 0123456789'
NEW_CUT_DESCRIPTION = 'Ъ' * 504
MODEL_FULL_DESCRIPTION = 'Ъ' * 504 + ' 0123456789'
OLD_CUT_DESCRIPTION = 'Ъ' * 504 + '...'


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.shops += [Shop(fesh=1, regions=[1])]
        cls.index.offers += [
            Offer(
                title="offer_length_cut",
                descr=OFFER_FULL_DESCRIPTION,
                waremd5="wgrU12_pd1mqJ6DJm_9nEA",
                fesh=1,
                hyperid=1,
            )
        ]

        cls.index.models += [Model(title='model_length_cut', description=MODEL_FULL_DESCRIPTION, hyperid=1)]

        cls.index.vclusters += [
            VCluster(title="vcluster_length_cut", description=MODEL_FULL_DESCRIPTION, vclusterid=1000000001)
        ]

        cls.index.offers += [Offer(title="vcluster_length_cut offer", vclusterid=1000000001)]

    def test_offer_length_cut(self):
        # new places like prime cut description
        response = self.report.request_json('place=prime&text=offer_length_cut')

        self.assertFragmentIn(response, {"results": [{'description': NEW_CUT_DESCRIPTION}]})

        offers = self.report.request_images('place=images&offerid=wgrU12_pd1mqJ6DJm_9nEA')
        self.assertEqual(offers[0].Description, OLD_CUT_DESCRIPTION)

        # place offerinfo is the exception
        response = self.report.request_json(
            'place=offerinfo&offerid=wgrU12_pd1mqJ6DJm_9nEA&show-urls=external&rids=1&regset=1'
        )
        self.assertFragmentIn(response, {"results": [{'description': OFFER_FULL_DESCRIPTION}]})

    def test_model_length_cut(self):
        # new places like prime cut description
        response = self.report.request_json('place=prime&text=model_length_cut')

        self.assertFragmentIn(response, {"results": [{'description': NEW_CUT_DESCRIPTION}]})

        # place modelinfo is the exception
        response = self.report.request_json('place=modelinfo&hyperid=1&rids=1')
        self.assertFragmentIn(response, {"results": [{'description': MODEL_FULL_DESCRIPTION}]})

    def test_vcluster_length_cut(self):
        # new places like prime cut description
        response = self.report.request_json('place=prime&text=vcluster_length_cut')

        self.assertFragmentIn(response, {"results": [{'description': NEW_CUT_DESCRIPTION}]})

        # place modelinfo is the exception
        response = self.report.request_json('place=modelinfo&hyperid=1000000001&rids=1')
        self.assertFragmentIn(response, {"results": [{'description': MODEL_FULL_DESCRIPTION}]})


if __name__ == '__main__':
    main()
