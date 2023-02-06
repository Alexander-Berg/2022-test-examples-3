#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import ClothesIndex, Offer, Picture, PictureSignature, VCluster

from core.testcase import TestCase, main

resp_json_au = {"error": {"code": "INVALID_USER_CGI", "message": "Requested root hid"}}


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.vclusters += [
            VCluster(
                hid=1,
                vclusterid=1000000101,
                title='visual cluster 1-1',
                clothes_index=[ClothesIndex([179], [179], [179])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=10)])],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000102,
                title='visual cluster 1-2',
                clothes_index=[ClothesIndex([22], [22], [22])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=11)])],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000103,
                title='visual cluster 1-3',
                clothes_index=[ClothesIndex([174], [174], [174])],
                pictures=[Picture(width=100, height=100, group_id=1234, signatures=[PictureSignature(similar=12)])],
            ),
        ]
        cls.index.offers += [
            Offer(vclusterid=1000000101),
            Offer(vclusterid=1000000102),
            Offer(vclusterid=1000000103),
        ]

        cls.index.offers += [Offer(title='kiyanka 103-3-1', bid=300)]

        # JSON places

    def test_error_format_visualanalog(self):
        response = self.report.request_json('place=visualanalog&vclusterid=1000000101&hid=90401&bsformat=2')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_defaultoffer(self):
        response = self.report.request_json('place=defaultoffer&hid=90401&bsformat=2')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_recipe_by_glfilters(self):
        response = self.report.request_json('place=recipe_by_glfilters&hid=90401')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_productoffers(self):
        response = self.report.request_json('place=productoffers&hid=90401')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_book_now_incut(self):
        response = self.report.request_json('place=book_now_incut&hid=90401&bsformat=2')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_recipes_contain_glfilters(self):
        response = self.report.request_json('place=recipes_contain_glfilters&hid=90401')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_top_categories(self):
        response = self.report.request_json('place=top_categories&numdoc=1&hid=90401')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_vendor_offers_models(self):
        response = self.report.request_json('place=vendor_offers_models&hid=90401')
        self.assertFragmentIn(response, resp_json_au)
        self.error_log.expect(code=3043)

    def test_error_format_modelinfo(self):
        response = self.report.request_json('place=modelinfo&bsformat=2')
        self.assertFragmentIn(
            response, {"error": {"code": "INVALID_USER_CGI", "message": "Model ID or mSKU should be specified"}}
        )
        self.error_log.expect(code=3043)


if __name__ == '__main__':
    main()
