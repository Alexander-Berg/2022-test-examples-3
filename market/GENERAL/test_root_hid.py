#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [Offer(title='MARKETOUT-7890', hid=91461)]

    def test_root_hid(self):
        response = self.report.request_json('place=prime&hid=90401', strict=False)  # root hid (90401) is forbidden
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI", "message": "Requested root hid"}})
        self.error_log.expect(code=3043)

    def test_no_hid(self):
        response = self.report.request_json('place=prime&text=MARKETOUT-7890')  # text and no hid is OK
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {
                    "highlighted": [
                        {"value": "MARKETOUT", "highlight": True},
                        {"value": "-"},
                        {"value": "7890", "highlight": True},
                    ]
                },
            },
            preserve_order=True,
        )

    def test_root_hid_with_text(self):
        response = self.report.request_json('place=prime&hid=90401&text=MARKETOUT-7890')  # root hid + text is OK
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {
                    "highlighted": [
                        {"value": "MARKETOUT", "highlight": True},
                        {"value": "-"},
                        {"value": "7890", "highlight": True},
                    ]
                },
            },
            preserve_order=True,
        )

    def test_nonroot_hid(self):
        response = self.report.request_json('place=prime&hid=91461')  # nonroot hid is OK
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "MARKETOUT-7890"}}, preserve_order=True)

    def test_nonroot_hid_with_text(self):
        response = self.report.request_json('place=prime&hid=91461&text=MARKETOUT-7890')  # nonroot hid + text is OK
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {
                    "highlighted": [
                        {"value": "MARKETOUT", "highlight": True},
                        {"value": "-"},
                        {"value": "7890", "highlight": True},
                    ]
                },
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
