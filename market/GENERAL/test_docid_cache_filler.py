#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    NavCategory,
    Offer,
    Region,
    Shop,
)
from core.testcase import TestCase, main
import time
import itertools


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.docid_cache_size = 10000
        cls.settings.docid_cache_docs_limit = 5

    @classmethod
    def prepare_data_categories(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=2, name='Санкт-Петербург'),
                ],
            )
        ]

        cls.index.shops += [
            Shop(fesh=1, name='shop', regions=[213]),
            Shop(fesh=2, name='store', regions=[2]),
        ]

        cls.index.navtree += [
            NavCategory(nid=1, hid=1),
        ]

        for price in range(10, 20):
            cls.index.offers += [
                Offer(hid=1, price=price, title='Moscow', fesh=1),
                Offer(hid=1, price=price, title='St.Petersburg', fesh=2),
            ]

    def test_fill_and_read(self):
        results = []
        docs_limit = self.settings.docid_cache_docs_limit

        for offer in self.index.offers:
            if 'Moscow' not in offer.title:
                continue

            results.append((offer.price, {"entity": "offer", "wareId": offer.ware_md5}))

        def count_offers(results):
            count = 0
            for result in results:
                if result['entity'] == 'offer':
                    count += 1
            return count

        qtree_rearr = 'docid_cache_qtree_key=1'
        for sorting, qtree_key in itertools.product(('aprice', 'dprice'), (False, True)):
            key_request = 'place=prime&how={}&nid=1&rids=213&rearr-factors={}'.format(
                sorting, qtree_rearr + ';' if qtree_key else ''
            )

            if sorting == 'aprice':
                ordered_results = sorted(results, key=lambda x: x[0])
            elif sorting == 'dprice':
                ordered_results = sorted(results, key=lambda x: -x[0])

            ordered_results = [result for price, result in ordered_results]

            # cache miss request
            response = self.report.request_json(
                key_request + 'enable_read_from_docid_cache=1;'
                'docid_cache_filler_barrier=1;' + 'docid_cache_docs_limit=' + str(docs_limit)
            )

            self.assertEqual(count_offers(response.root['search']['results']), len(ordered_results))
            self.assertFragmentIn(response, {"results": ordered_results}, preserve_order=True)

            # wait for cache filler request to fill cache
            time.sleep(2)

            # cache hit request
            response = self.report.request_json(key_request + 'enable_read_from_docid_cache=1')

            self.assertEqual(count_offers(response.root['search']['results']), docs_limit)
            self.assertFragmentIn(response, {"results": ordered_results[:docs_limit]}, preserve_order=True)

            # cache hit, but read from it is disabled by rearr-flag
            response = self.report.request_json(key_request + 'enable_read_from_docid_cache=0')

            self.assertEqual(count_offers(response.root['search']['results']), len(ordered_results))
            self.assertFragmentIn(response, {"results": ordered_results}, preserve_order=True)


if __name__ == '__main__':
    main()
