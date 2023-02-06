#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, RtyOffer, Shop
from core.testcase import TestCase, main
import shutil
import os.path


class T(TestCase):
    OFFERS_COUNT = 3
    offers_to_index = None
    rty_offers_to_index = None
    complete_collections = []

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.rty_qpipe = True
        cls.index.shops += [Shop(fesh=1001)]
        cls.offers_to_index = [
            Offer(
                title='iphone %s' % i,
                feedid=27,
                offerid='fff_%s' % i,
                price=300,
                fesh=1001,
                hid=100,
                hyperid=1,
                vendor_id=100,
                datasource_id=100,
                vbid=100,
                is_recommended=False,
            )
            for i in range(cls.OFFERS_COUNT)
        ]

        collections_path = os.path.join(cls.meta_paths.search_root, 'collections')
        os.mkdir(collections_path)
        for i in range(3):
            cls.index.offers += [cls.offers_to_index[i]]
            cls._reindex_generation()
            cls.complete_collections.append(os.path.join(collections_path, str(i)))
            shutil.copytree(cls.meta_paths.shopindex, cls.complete_collections[i])
        cls._clear()
        cls.rty_offers_to_index = [
            RtyOffer(feedid=27, offerid='fff_%s' % i, price=400) for i in range(cls.OFFERS_COUNT)
        ]

    @classmethod
    def _clear(cls):
        shutil.rmtree(os.path.join(cls.meta_paths.reportdata, 'web_features_csv', 'erf'), ignore_errors=True)
        shutil.rmtree(cls.meta_paths.shopindex, ignore_errors=True)
        cls.index.navforest = []
        cls.meta_paths.create_all()

    @classmethod
    def _reindex_generation(cls):
        cls._clear()
        cls.index.commit(reset_fields=False, rty_reindex=True)

    @classmethod
    def _set_generation(cls, i):
        shutil.rmtree(cls.meta_paths.shopindex, ignore_errors=True)
        shutil.copytree(cls.complete_collections[i], cls.meta_paths.shopindex)

    def _update(self):
        response = self.base_search_client.request_xml('admin_action=updatedata&which=collection&id=basesearch16-0')
        self.assertFragmentIn(response, "<admin-action>ok</admin-action>")

    def _check_offers(self, count):
        response = self.report.request_json('place=prime&text=iphone&rearr-factors=rty_qpipe=1')
        for i in range(self.OFFERS_COUNT):
            if i < count:
                self.assertFragmentIn(
                    response,
                    {
                        'entity': 'offer',
                        'shop': {'feed': {'id': '27', 'offerId': 'fff_%s' % i}},
                        'prices': {'currency': 'RUR', 'value': '400'},
                    },
                )
            else:
                self.assertFragmentNotIn(
                    response, {'entity': 'offer', 'shop': {'feed': {'id': '27', 'offerId': 'fff_%s' % i}}}
                )

    def test_report_collection_restart(self):
        self._set_generation(0)
        self._update()
        """
        Check initial offer exists in index
        """
        self.rty.offers += self.rty_offers_to_index
        self._check_offers(1)

        """
        Rebuild index adding one more offer and check
        """
        self._set_generation(1)
        self._update()
        self._check_offers(2)

        """
        Flush rty index to final and check
        """
        self.rty_controller.reopen_indexes()
        self._check_offers(2)

        """
        Rebuild index adding one more offer and check
        """
        self.index.offers += [self.offers_to_index[2]]
        self._set_generation(2)
        self._update()
        self._check_offers(3)


if __name__ == '__main__':
    main()
