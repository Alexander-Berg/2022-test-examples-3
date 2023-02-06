#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClothesIndex, HyperCategory, Offer, Picture, PictureSignature, VCluster
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.is_multi_signatures = True
        cls.index.hypertree += [
            HyperCategory(hid=1, visual=True),
        ]
        cls.index.vclusters += [
            VCluster(
                hid=1,
                vclusterid=1000000101,
                title='visual cluster 1-1',
                clothes_index=[
                    ClothesIndex([22], [22], [22], 1),
                    ClothesIndex([22], [22], [22], 2),
                ],
                pictures=[
                    Picture(
                        width=100,
                        height=100,
                        group_id=1234,
                        signatures=[
                            PictureSignature(similar=10, version=1, clothes={22: 0.8}),
                            PictureSignature(similar=10, version=2, clothes={22: 0.8}),
                        ],
                    )
                ],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000102,
                title='visual cluster 1-2',
                clothes_index=[
                    ClothesIndex([22], [22], [22], 1),
                    ClothesIndex([22], [22], [66], 2),
                ],
                pictures=[
                    Picture(
                        width=100,
                        height=100,
                        group_id=1234,
                        signatures=[
                            PictureSignature(similar=11, version=1, clothes={22: 0.8}),
                            PictureSignature(similar=11, version=2, clothes={22: 0.6}),
                        ],
                    )
                ],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000103,
                title='visual cluster 1-3',
                clothes_index=[ClothesIndex([22], [22], [22], 1), ClothesIndex([22], [66], [66], 2)],
                pictures=[
                    Picture(
                        width=100,
                        height=100,
                        group_id=1234,
                        signatures=[
                            PictureSignature(similar=12, version=1, clothes={22: 0.8}),
                            PictureSignature(similar=12, version=2, clothes={22: 0.4}),
                        ],
                    )
                ],
            ),
            VCluster(
                hid=1,
                vclusterid=1000000104,
                title='visual cluster 1-4',
                clothes_index=[ClothesIndex([22], [22], [22], 1), ClothesIndex([66], [66], [66], 2)],
                pictures=[
                    Picture(
                        width=100,
                        height=100,
                        group_id=1234,
                        signatures=[
                            PictureSignature(similar=13, version=1, clothes={22: 0.8}),
                            PictureSignature(similar=13, version=2, clothes={22: 0.1}),
                        ],
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(vclusterid=1000000101),
            Offer(vclusterid=1000000102),
            Offer(vclusterid=1000000103),
            Offer(vclusterid=1000000104),
        ]

        # test_sort data
        cls.index.vclusters += [
            VCluster(
                hid=2,
                vclusterid=1000000201,
                title='visual cluster 2-1',
                clothes_index=[
                    ClothesIndex([22], [22], [22], 1),
                    ClothesIndex([22], [22], [22], 2),
                ],
                pictures=[
                    Picture(
                        width=100,
                        height=100,
                        group_id=1234,
                        signatures=[
                            PictureSignature(similar=10, version=1, clothes={22: 0.8}),
                            PictureSignature(similar=11, version=2, clothes={22: 0.8}),
                        ],
                    )
                ],
            ),
            VCluster(
                hid=2,
                vclusterid=1000000202,
                title='visual cluster 2-2',
                clothes_index=[
                    ClothesIndex([22], [22], [22], 1),
                    ClothesIndex([22], [22], [22], 2),
                ],
                pictures=[
                    Picture(
                        width=100,
                        height=100,
                        group_id=1234,
                        signatures=[
                            PictureSignature(similar=11, version=1, clothes={22: 0.8}),
                            PictureSignature(similar=10, version=2, clothes={22: 0.8}),
                        ],
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(vclusterid=1000000201),
            Offer(vclusterid=1000000202),
        ]

    def test_xml_output(self):
        # without version (must use first version in picture = 1)
        response = self.report.request_xml('place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1&debug=1')
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
                <model id="1000000103">
                    <name>visual cluster 1-3</name>
                </model>
                <model id="1000000104">
                    <name>visual cluster 1-4</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            '''
            Nothing has been found with given filtration. Relaxing filters
            ''',
            preserve_order=True,
        )

        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1&rearr-factors=market_picture_version=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
                <model id="1000000103">
                    <name>visual cluster 1-3</name>
                </model>
                <model id="1000000104">
                    <name>visual cluster 1-4</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            '''
            Nothing has been found with given filtration. Relaxing filters
            ''',
            preserve_order=True,
        )

        # for version 2 must work with other analog-filters
        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1&rearr-factors=market_picture_version=2&debug=1'
        )
        self.assertFragmentNotIn(
            response,
            '''
            Nothing has been found with given filtration. Relaxing filters
            ''',
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
                <model id="1000000103">
                    <name>visual cluster 1-3</name>
                </model>
                <model id="1000000104">
                    <name>visual cluster 1-4</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=2&rearr-factors=market_picture_version=1'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
                <model id="1000000103">
                    <name>visual cluster 1-3</name>
                </model>
                <model id="1000000104">
                    <name>visual cluster 1-4</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=2&rearr-factors=market_picture_version=2'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

        self.assertFragmentNotIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000103"/>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=3&rearr-factors=market_picture_version=1'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
                <model id="1000000103">
                    <name>visual cluster 1-3</name>
                </model>
                <model id="1000000104">
                    <name>visual cluster 1-4</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=3&rearr-factors=market_picture_version=2'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000102">
                    <name>visual cluster 1-2</name>
                </model>
                <model id="1000000103">
                    <name>visual cluster 1-3</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

        self.assertFragmentNotIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000104"/>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

    def test_xml_output_unexisted_version(self):
        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=1&analog-filter=1&rearr-factors=market_picture_version=10'
        )
        self.assertFragmentNotIn(
            response,
            '''
            <search_results>
              <offers>
                <model/>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

    def test_sort(self):
        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=2&analog-filter=1&rearr-factors=market_picture_version=1'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000201">
                    <name>visual cluster 2-1</name>
                </model>
                <model id="1000000202">
                    <name>visual cluster 2-2</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )

        # order for version = 2
        response = self.report.request_xml(
            'place=visualanalog&vclusterid=1000000101&hid=2&analog-filter=1&rearr-factors=market_picture_version=2'
        )
        self.assertFragmentIn(
            response,
            '''
            <search_results>
              <offers>
                <model id="1000000202">
                    <name>visual cluster 2-2</name>
                </model>
                <model id="1000000201">
                    <name>visual cluster 2-1</name>
                </model>
              </offers>
            </search_results>
            ''',
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
