#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import WhiteSupplier


class T(TestCase):
    @classmethod
    def prepare_supplier_info(cls):
        cls.index.white_suppliers += [
            WhiteSupplier(ogrn="1235", jur_name="name1", jur_address="address1"),
            WhiteSupplier(ogrn="1238", jur_name="name2", jur_address="address2"),
            WhiteSupplier(ogrn="125100", jur_name="name3", jur_address="address3"),
        ]

    def test_supplier_info(self):
        response = self.report.request_json('place=supplier_info&ogrn=1235')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "white_suppliers": [
                        {
                            "ogrn": "1235",
                            "jur_name": "name1",
                            "jur_address": "address1",
                        },
                    ],
                }
            },
        )

        response = self.report.request_json('place=supplier_info&ogrn=1235,1236,1238')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "white_suppliers": [
                        {
                            "ogrn": "1235",
                            "jur_name": "name1",
                            "jur_address": "address1",
                        },
                        {
                            "ogrn": "1238",
                            "jur_name": "name2",
                            "jur_address": "address2",
                        },
                    ],
                }
            },
        )


if __name__ == '__main__':
    main()
