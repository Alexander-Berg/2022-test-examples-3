#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, Offer, OfferDimensions, Shop


DSBS_CIS_OFFER = 'dsbs_cis_offer'
DSBS_NO_CIS_OFFER_CHILD = 'dsbs_no_cis_offer_child'
DSBS_NO_CIS_OFFER_PARENT = 'dsbs_no_cis_offer_parent'

HIDDEN_CIS_CATEGORY_FOR_DSBS = 90669  # See for details https://a.yandex-team.ru/review/1680376/details


class T(TestCase):
    dsbs_no_cis_shop = Shop(
        fesh=42,
        datafeed_id=4240,
        priority_region=213,
        regions=[213],
        name='dsbs_no_cis_shop',
        client_id=11,
        cpa=Shop.CPA_REAL,
        cis=Shop.CIS_NO,
    )

    dsbs_no_cis_offer_child = Offer(
        title=DSBS_NO_CIS_OFFER_CHILD,
        offerid=DSBS_NO_CIS_OFFER_CHILD,
        hyperid=101,
        fesh=dsbs_no_cis_shop.fesh,
        waremd5='DsbsOfferChildNoCis__g',
        price=1100,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    dsbs_no_cis_offer_parent = Offer(
        title=DSBS_NO_CIS_OFFER_PARENT,
        offerid=DSBS_NO_CIS_OFFER_PARENT,
        hyperid=102,
        fesh=dsbs_no_cis_shop.fesh,
        waremd5='DsbsOfferParentNoCis_g',
        price=10530881,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    dsbs_cis_shop = Shop(
        fesh=43,
        datafeed_id=4241,
        priority_region=213,
        regions=[213],
        name='dsbs_cis_shop',
        client_id=12,
        cpa=Shop.CPA_REAL,
        cis=Shop.CIS_REAL,
    )

    dsbs_cis_offer = Offer(
        title=DSBS_CIS_OFFER,
        offerid=DSBS_CIS_OFFER,
        hyperid=103,
        fesh=dsbs_cis_shop.fesh,
        waremd5='DsbsOfferCis_________g',
        price=1100,
        cpa=Offer.CPA_REAL,
        dimensions=OfferDimensions(width=10, height=20, length=15),
    )

    offer_info_request = (
        'place=offerinfo'
        '&pp=18'
        '&rgb=green_with_blue'
        '&rids=213'
        '&regset=2'
        '&show-urls=cpa'
        '&rearr-factors=market_hide_dsbs_by_cis_category={hide_dsbs_by_cis_category}'
    )

    offer_info_cpa_request = 'place=offerinfo' '&pp=18' '&rgb=green_with_blue' '&rids=213' '&regset=2' '&show-urls=cpa'

    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=HIDDEN_CIS_CATEGORY_FOR_DSBS,
                name='Шины',
                children=[
                    HyperCategory(hid=10530881, name='Велосипедные шины'),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=101, hid=HIDDEN_CIS_CATEGORY_FOR_DSBS, title='hyperid_101'),
            Model(hyperid=102, hid=10530881, title='hyperid_102'),
            Model(hyperid=103, hid=HIDDEN_CIS_CATEGORY_FOR_DSBS, title='hyperid_103'),
        ]

        cls.index.shops += [T.dsbs_cis_shop, T.dsbs_no_cis_shop]

        cls.index.offers += [T.dsbs_cis_offer, T.dsbs_no_cis_offer_child, T.dsbs_no_cis_offer_parent]

    def test_cis_offers_not_hide_by_category(self):
        """
        Проверяем, что CIS и скрытые категории DSBS товаров включаются в выдачу
        независмо от rearr-флага 'market_hide_dsbs_by_cis_category'
        """
        for request in [
            T.offer_info_request.format(hide_dsbs_by_cis_category=0),
            T.offer_info_request.format(hide_dsbs_by_cis_category=1),
            T.offer_info_cpa_request,
        ]:
            response = self.report.request_json(request + '&offerid={}'.format(T.dsbs_cis_offer.ware_md5))
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': T.dsbs_cis_offer.ware_md5})

    def test_no_cis_offers_hide_by_category(self):
        """
        Проверяем, что не CIS и скрытые категории DSBS товаров включаются в выдачу
        только по rearr-флагу 'market_hide_dsbs_by_cis_category'
        """
        for offer in [T.dsbs_no_cis_offer_child, T.dsbs_no_cis_offer_parent]:
            base_request = T.offer_info_request.format(hide_dsbs_by_cis_category=0)
            response = self.report.request_json(base_request + '&offerid={}'.format(offer.ware_md5))
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.ware_md5})

            base_request = T.offer_info_request.format(hide_dsbs_by_cis_category=1)
            response = self.report.request_json(base_request + '&offerid={}'.format(offer.ware_md5))
            self.assertFragmentNotIn(response, {'entity': 'offer', 'wareId': offer.ware_md5})

            response = self.report.request_json(T.offer_info_cpa_request + '&offerid={}'.format(offer.ware_md5))
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.ware_md5})


if __name__ == '__main__':
    main()
