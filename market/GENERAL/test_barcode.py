#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, Shop
from core.testcase import TestCase, main


class T(TestCase):
    """
    Набор тестов для поиска по баркодам
    """

    offers_data = {
        "UPC-A correct barcode": ["012345678905"],
        "UPC-E correct barcode": ["01234565"],
        "EAN-13 correct barcode": ["0123456789012"],
        "EAN-8 correct barcode": ["91234568"],
        "EAN-8 and EAN-13 and UPC-A correct barcodes": [
            "12345670",  # <!-- EAN-8 -->
            "1234567890128",  # <!-- EAN-13 -->
            "123456789012",  # <!-- UPC-A -->
        ],
    }

    @classmethod
    def prepare(cls):
        """
        Для тестов поиска по баркодам требуется эмуляция reqwizard
        """

        # ReqWizard config
        reqwizard_data = {
            "012345678905": "cHic4_Li4uFgEmCSYFJg0GAyYBBikGJQYtBiMGKw4uFgEGCQYACKMxgwOLB6sAYwRDAkMGQwTmBkmMXIsIiRYRMjw15GBiA4wchwgTFFaiEjVwoXRBsH1DgeA0MjYxNTM3MLSwNTJKN5wEYzgo1m8GAAGZ0xZ9q9tUwww1E0ollkwGDB6KQLdDajFIoyJW4kjgajASPMxVUMCYwNjAxAs6WmMHIVQN3IqcAEdiN7UmJRcn5KKth5vErIJoLdyo4UDDC3QgIBpnMRI6o2bO4VA9kJsgLVYRBnKfFy1As0MAJjgRlkjQGjBdS9ABRkQuw,",  # noqa
            "01234565": "cHicbZCxjkFBGIXPmY0YwyY3CpFbya2salhEVLKVbKVUbIEluZ1EqeEBFNusQlR6jV6_pSfwLObOvYMVfzeTOd_5_lGfKieFJ4qihLLQyMNHgApqaOckPBRh7qHRSXVTPfQxQMgfYkPsiANxJMz8ESeO_V-qLxXHZIKTulp7rzeajTustFhaLLqIsOF2fd4LB76GHgo0Wvx4M7r0r0-CrDaTHMrUdJZzDLgkDNNfUYWJV6YkrFd6NJx9T8cTq5UJHMD6pe_Wdn7x0i614y3yzLEQdUXo_0KxTvAqF96S5sdfogrNVuJ5AQr-Plw,",  # noqa
            "0123456789012": "cHic4_Li4uFgEmCSYFJg0GAyYBBikGJQYtBiMGKw4uFgEGCQYACKMxgwOLB6sAYwRDAkMGQwTmBkmMXIsIiRYRMjw15GBiA4wchwgTFFaikjVxoXRBsH1DheA0MjYxNTM3MLSyALyWxesNmMYLMZPBhAZmfMmXZvLRPMdFSdaFYZMFgwOukDHc4ohapOiQeZp8FowAhzdRVDAmMDIwPQeKlpjFxFUHdyKjCB3cmelFiUnJ-SCnYhnxKKmWD3siOFBcy9kJCAaV3EiKYPm5vFQLaCLEF1GtRhExi5ctAcxpJZnJSH01UsOF0F1ke5k5R4OeoFGhiByYMZZAka14DRAhqmAFxGXbo,",  # noqa
            "91234568": "cHic4_Li4uFgEmCSYFJg0GAyYBBikGJQYtBiMGKw4uFgEGCQYACKMxgwOLB6sAYwRDAkMGQwTmBkmMXIsIiRYRMjw15GBiA4wchwgTFFajIjVwwXRBsH1DgOS0MjYxNTMwskYznAxjKCjWXwYAAZmzFn2r21TDCD4ZrQLDBgsGB00gQ6l0EKrkSJ28DAAMbRYDRghLmyiqGBkQFoolQPI1cG1FWcCkxgV7EnJRYl56ekgh3FqQTTDnYdO5KnYa6DeBmmaxEjQgs2F4qB7AIZjc05Srwc9QINjMDwZgZZYcBowQiRAAC3uj5A",  # noqa
            "12345670": "cHic4_Li4uFgEmCSYFJg0GAyYBBikGJQYtBiMGKw4uFgEGCQYACKMxgwOLB6sAYwRDAkMGQwTmBkmMXIsIiRYRMjw15GBiA4wchwgTFFajIjVwwXRBsH1DgOQyNjE1MzcwMkYznAxjKCjWXwYAAZmzFn2r21TDCD4ZrQLDBgsGB00gQ6l0EKrkSJ28DAAMbRYDRghLmyiqGBkQFoolQPI1cG1FWcCkxgV7EnJRYl56ekgh3FqQTTDnYdO5KnYa6DeBmmaxEjQgs2F4qB7AIZjc05Srwc9QINjMDwZgZZYcBowQiRAACLMj4E",  # noqa
            "1234567890128": "cHic4_Li4uFgEmCSYFJg0GAyYBBikGJQYtBiMGKw4uFgEGCQYACKMxgwOLB6sAYwRDAkMGQwTmBkmMXIsIiRYRMjw15GBiA4wchwgTFF6gQjVxoXRBsH1DheQyNjE1MzcwtLA0MjCySzecFmM4LNZvBgAJmdMWfavbVMMNNRdaJZZcBgwehkAHQ4gxSqOiVUrgajASPM2VUMDYwMXYyKXCwgFwpJcjBKiXKwiebactnL2rxXtlNg0AVZYMAAdIHUNEauIqhXOBWYwF5hT0osSs5PSQV7gk8JxR6wl9iRggvmJUhgwbQuYkTTh81bYiBbQZZgOB7ksAmMXDloDmPJLE7Kw-kqFpyuAuuj3ElKvBz1Ag2MwBTEDLIEjWvAaMEIUQcAJZViXg,,",  # noqa
            "123456789012": "cHic4_Li4uFgEmCSYFJg0GAyYBBikGJQYtBiMGKw4uFgEGCQYACKMxgwOLB6sAYwRDAkMGQwTmBkmMXIsIiRYRMjw15GBiA4wchwgTFFagEjVwoXRBsH1DgeQyNjE1MzcwtLA0MjJKN5wEYzgo1m8GAAGZ0xZ9q9tUwww1E0ollkwGDB6KQHdDaDFIoyJRSeBqMBI8zJVQwNjAxAo6WmMHIVQJ3IqcAEdiJ7UmJRcn5KKth1vErIRoCdyo4UCjCnQsIApnMRI6o2bM4VA9kJsgKbs5R4OeoFGhiBkcAMssaA0YIRIgEAxC9Crw,,",  # noqa
        }
        cls.reqwizard.on_default_request().respond()
        for barcode_str, qtree in reqwizard_data.items():
            cls.reqwizard.on_request(barcode_str).respond(qtree=qtree)

        # Offers collection
        cls.index.shops += [
            Shop(fesh=1, cpa=Shop.CPA_REAL, regions=[1001]),
        ]
        for descr, barcodes in cls.offers_data.items():
            cls.index.offers.append(Offer(fesh=1, descr=descr, barcodes=barcodes))

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_barcode_search(self):
        """
        MARKETOUT-11453
        Сделать запросы для place=prime c параметром text,
        в которые передавать разные значения баркодов и проверять,
        что каждый раз находится верный оффер, который
        соответствует указанному баркоду
        """

        for descr, barcodes in T.offers_data.items():
            for bcode in barcodes:
                for text_query in [bcode, "barcode:{barcode}".format(barcode=bcode)]:
                    response = self.report.request_json(
                        "pp=18&place=prime&text={text_query}&rids=1001".format(text_query=text_query)
                    )
                    self.assertFragmentIn(
                        response,
                        {
                            "search": {
                                "total": 1,
                                "results": [
                                    {
                                        "entity": "offer",
                                        "description": descr,
                                    },
                                ],
                            }
                        },
                        preserve_order=False,
                    )


if __name__ == '__main__':
    main()
