#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import Offer

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(
                title='offer_100',
                classifier_category_confidence_for_filtering_stupids=1.00,
                hid=322,
                offerid=1,
                feedid=1,
            ),
            Offer(title='offer_005', classifier_category_confidence_for_filtering_stupids=0.05, hid=322),
            Offer(title='offer_099', classifier_category_confidence_for_filtering_stupids=0.99, hid=1337),
        ]

    def test_filter_offer_with_small_category_confidence(self):
        """
        Фильтруем оффер с низкой уверенностью в категории offer_005
        """
        request = 'place=prime&hid=322&rearr-factors=market_hide_offers_by_classifier_confidence_category_threshold=0.3'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer_100"},
                    }
                ],
            },
        )
        self.assertFragmentNotIn(response, {"titles": {"raw": "offer_005"}})
        self.assertFragmentNotIn(response, {"titles": {"raw": "offer_099"}})

    def test_print_doc(self):
        """
        print_doc выводит classifier_category_confidence
        """
        response = self.report.request_json('place=print_doc&feed_shoffer_id=1-1&debug=1')
        self.assertFragmentIn(response, {"classifierCategoryConfidenceForFilteringStupids": "1"})


if __name__ == '__main__':
    main()
