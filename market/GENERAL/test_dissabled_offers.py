#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(hyperid=1, fesh=1, title="enabled"),
            Offer(hyperid=1, fesh=2, has_gone=True, title="disabled"),
        ]

    def test_disabled_offer_is_not_shown(self):
        response = self.report.request_json('place=productoffers&hyperid=1&pp=21')
        self.assertEqual(
            1,
            response.count(
                {
                    "entity": "offer",
                }
            ),
        )
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "enabled"}})


if __name__ == '__main__':
    main()
