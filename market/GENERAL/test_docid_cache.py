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
from core.matcher import Absent

import itertools


def parse_qtree(response):
    return response['debug']['report']['context']['collections']['SHOP']['qtree'][0]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.docid_cache_size = 10000
        cls.settings.docid_cache_docs_limit = 10000
        cls.settings.memcache_enabled = True

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
        docs_limit = 5
        qtree_rearr = 'docid_cache_qtree_key=1'
        for sorting, qtree_key in itertools.product(('aprice', 'dprice'), (False, True)):
            key_request = 'place=prime&how={}&nid=1&rids=213&rearr-factors={}'.format(
                sorting, qtree_rearr + ';' if qtree_key else ''
            )
            ordered_results = self._select_offers('Moscow', sorting)

            # cache miss request
            response = self.report.request_json(key_request + 'enable_read_from_docid_cache=1' + '&debug=1')

            self.assertEqual(self._count_offers(response.root['search']['results']), len(ordered_results))
            self.assertFragmentIn(response, {"results": ordered_results}, preserve_order=True)

            # fill cache request
            if qtree_key:
                qtree = parse_qtree(response)
                request = 'place=prime&qtree={qtree}&how={sorting}&rids=213&fill-cache=1&docid_cache_request_type=category_no_text'.format(
                    qtree=qtree, sorting=sorting
                )
                self.report.request_plain(
                    request + '&rearr-factors=docid_cache_docs_limit={}'.format(docs_limit) + ';' + qtree_rearr
                )
            else:
                self.report.request_plain('fill-cache=1&' + key_request + 'docid_cache_docs_limit=' + str(docs_limit))

            # cache hit request
            response = self.report.request_json(key_request + 'enable_read_from_docid_cache=1')

            self.assertEqual(self._count_offers(response.root['search']['results']), docs_limit)
            self.assertFragmentIn(response, {"results": ordered_results[:docs_limit]}, preserve_order=True)
            self.access_log.expect(docid_cache_hits=1, basesearch_calls_with_full_groups=Absent())

            # cache hit, but read from it is disabled by rearr-flag
            response = self.report.request_json(key_request + 'enable_read_from_docid_cache=0')

            self.assertEqual(self._count_offers(response.root['search']['results']), len(ordered_results))
            self.assertFragmentIn(response, {"results": ordered_results}, preserve_order=True)

        response = self.report.request_json(
            'place=prime&how=aprice&nid=1&rids=213&nosearchresults=1&rearr-factors=enable_read_from_docid_cache=1'
        )
        self.access_log.expect(docid_cache_hits=1, basesearch_calls_with_full_groups=1)

        response = self.report.request_json(
            'place=prime&how=aprice&nid=1&rids=213&rearr-factors=enable_read_from_docid_cache=1;docid_cache_read_docs_limit='
            + str(docs_limit - 1)
        )
        self.access_log.expect(docid_cache_hits=1, basesearch_calls_with_full_groups=Absent())
        self.assertEqual(self._count_offers(response.root['search']['results']), docs_limit - 1)

    def test_category_doc_limit(self):
        results = self._select_offers('St.Petersburg', 'aprice')

        request = 'place=prime&how=aprice&nid=1&rids=2&rearr-factors=enable_read_from_docid_cache=1;docid_cache_docs_limit=8;docid_cache_category_doc_limits={"1":6}'
        self.report.request_plain(request + '&fill-cache=1')
        response = self.report.request_json(request)
        self.assertEqual(self._count_offers(response.root['search']['results']), 6)
        self.assertFragmentIn(response, {"results": results[:6]}, preserve_order=True)

        request = 'place=prime&how=aprice&nid=1&rids=2&rearr-factors=enable_read_from_docid_cache=1;docid_cache_docs_limit=8;docid_cache_category_doc_limits={"2":6}'
        self.report.request_plain(request + '&fill-cache=1')
        response = self.report.request_json(request)
        self.assertEqual(self._count_offers(response.root['search']['results']), 8)
        self.assertFragmentIn(response, {"results": results[:8]}, preserve_order=True)

    def _select_offers(self, city, sorting):
        results = []
        for offer in self.index.offers:
            if city not in offer.title:
                continue
            results.append((offer.price, {"entity": "offer", "wareId": offer.ware_md5}))
        return [offer for _, offer in sorted(results, key=lambda x: x[0], reverse=(sorting == 'dprice'))]

    @staticmethod
    def _count_offers(results):
        count = 0
        for result in results:
            if result['entity'] == 'offer':
                count += 1
        return count


if __name__ == '__main__':
    main()
