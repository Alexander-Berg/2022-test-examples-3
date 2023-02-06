#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, RtyOffer
from core.testcase import TestCase, main


class T(TestCase):

    offer_ids = ['aaa', 'bbb', 'ccc', 'ddd', 'eee']

    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.settings.index_sort = 'feed-offer'
        cls.index.creation_time = 10000000
        cls.index.offers += [
            Offer(title=offer_id, feedid=26, offerid=offer_id, price=400, fesh=21) for offer_id in cls.offer_ids
        ]

    def _docs_order(self, docids):
        map = zip(docids, range(len(docids)))
        map.sort(key=lambda x: x[0])
        return [x[1] for x in map]

    def _get_docids(self):
        rty = []
        report = []
        for offer_id in self.offer_ids:
            response = self.report.request_json(
                'place=print_doc&feed_shoffer_id=26-{offer_id}&rearr-factors=rty_qpipe=1&debug=1'.format(
                    offer_id=offer_id
                )
            )
            document = response.root['documents'][0]
            debug = document['properties']['qdata']['debug']
            report_docid = debug['report docid']
            rty_docid = debug['segments']['index_0000000000_0000000000']['rty docid']
            rty.append(rty_docid)
            report.append(report_docid)
        return self._docs_order(rty), self._docs_order(report)

    def test_sort(self):
        self.rty.offers += [
            RtyOffer(feedid=26, offerid=offer_id, price=500, modification_time=11000000)
            for offer_id in reversed(self.offer_ids)
        ]
        self.rty_controller.reopen_indexes()

        # Проверяем, что документы отсортированы одинково в репорте и rty
        rty_docids, report_docids = self._get_docids()
        self.assertEqual(rty_docids, report_docids)


if __name__ == '__main__':
    main()
