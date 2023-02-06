#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Absent, Contains


class T(TestCase):
    class URL_TYPE:
        ENCRYPTED = 0

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.models += [
            Model(hyperid=1001, hid=1000, title="Model 1001"),
        ]
        cls.index.shops += [Shop(fesh=1100, priority_region=213, regions=[213])]
        cls.index.offers += [
            Offer(
                title="Offer for 1001", hid=1000, hyperid=1001, fesh=1100, price=100, waremd5="2b0-iAnHLZST2Ekoq4xElr"
            )
        ]

    def request(self, req):
        return self.report.request_json(req, add_defaults=0)

    def check_request_with_regular_pp(self, place, pp, params=""):
        response = self.request("place={}&pp={}&show-urls=encrypted&".format(place, pp) + params)

        self.assertFragmentIn(
            response, {"entity": "offer", "urls": {"encrypted": Contains("/pp={}/".format(pp))}, "urlsByPp": Absent()}
        )
        self.show_log_tskv.expect(pp=pp, hyper_id=1001, shop_id=1100, url_type=self.URL_TYPE.ENCRYPTED).times(1)

    def check_request_with_pp_list(self, place, pps, params=""):
        pp_list = ",".join(str(pp) for pp in pps)
        response = self.request("place={}&pp-list={}&show-urls=encrypted&clid=545&".format(place, pp_list) + params)

        urls_by_pp = {}
        for pp in pps:
            urls_by_pp[str(pp)] = {"encrypted": Contains("/pp={}/".format(pp))}

        self.assertFragmentIn(
            response,
            {"entity": "offer", "urls": {"encrypted": Contains("/pp={}/".format(pps[0]))}, "urlsByPp": urls_by_pp},
        )

        for pp in pps:
            urls_by_pp[str(pp)] = {"encrypted": Contains("clid%3D545")}

        self.assertFragmentIn(
            response, {"entity": "offer", "urls": {"encrypted": Contains("clid%3D545")}, "urlsByPp": urls_by_pp}
        )

        self.show_log_tskv.expect(
            pp=pps[0], hyper_id=1001, shop_id=1100, url_type=self.URL_TYPE.ENCRYPTED, pp_list=pp_list
        ).times(1)

    def test_productoffers_regular_pp(self):
        self.check_request_with_regular_pp(place="productoffers", pp=18, params="hyperid=1001")

    def test_productoffers_single_pp(self):
        self.check_request_with_pp_list(place="productoffers", pps=[18], params="hyperid=1001")

    def test_productoffers_multiple_pp(self):
        self.check_request_with_pp_list(place="productoffers", pps=[18, 19, 20], params="hyperid=1001")

    def test_prime_regular_pp(self):
        self.check_request_with_regular_pp(place="prime", pp=18, params="hyperid=1001")

    def test_prime_single_pp(self):
        self.check_request_with_pp_list(place="prime", pps=[18], params="hyperid=1001")

    def test_prime_multiple_pp(self):
        self.check_request_with_pp_list(place="prime", pps=[18, 19, 20], params="hyperid=1001")

    def test_prime_regular_pp_by_text(self):
        self.check_request_with_regular_pp(place="prime", pp=18, params="text=Model 1001")

    def test_prime_single_pp_by_text(self):
        self.check_request_with_pp_list(place="prime", pps=[18], params="text=Model 1001")

    def test_prime_multiple_pp_by_text(self):
        self.check_request_with_pp_list(place="prime", pps=[18, 19, 20], params="text=Model 1001")

    def test_offerinfo_regular_pp(self):
        self.check_request_with_regular_pp(place="prime", pp=18, params="hyperid=1001")

    def test_offerinfo_single_pp(self):
        self.check_request_with_pp_list(
            place="offerinfo", pps=[18], params="rids=213&regset=0&offerid=2b0-iAnHLZST2Ekoq4xElr"
        )

    def test_offerinfo_multiple_pp(self):
        self.check_request_with_pp_list(
            place="offerinfo", pps=[18, 19, 20], params="rids=213&regset=0&offerid=2b0-iAnHLZST2Ekoq4xElr"
        )


if __name__ == '__main__':
    main()
