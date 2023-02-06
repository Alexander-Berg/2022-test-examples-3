#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import NewShopRating, Shop
from core.testcase import TestCase, main
from core.matcher import NotEmpty, NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, name="aAc"),
            Shop(fesh=2, name="aBa", domain="aba.ru", shop_logo_url='www.aba.ru/logo', shop_logo_info='14:30:PNG'),
            Shop(fesh=3, name="Aaa"),
            Shop(fesh=7, name="abAc"),
            Shop(fesh=4, name="бвг"),
            Shop(fesh=5, name="Ббб"),
            Shop(fesh=6, name="бВВ"),
        ]

    def test_shops_list(self):
        response = self.report.request_json("place=shops_list&by-letter=a")
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {
                        "fesh": 3,
                        "name": "Aaa",
                        "slug": "aaa",
                    },
                    {
                        "fesh": 1,
                        "name": "aAc",
                        "slug": "aac",
                    },
                    {"fesh": 2, "name": "aBa", "slug": "aba", "domain": "aba.ru", "logo": NotEmpty()},
                    {"fesh": 7, "name": "abAc", "slug": "abac", "logo": NoKey("logo")},
                ],
            },
            preserve_order=False,
            allow_different_len=False,
        )

    def test_russian_letter(self):
        # Проверяем для русских букв
        response = self.report.request_json("place=shops_list&by-letter=б")
        self.assertFragmentIn(
            response,
            [
                {
                    "fesh": 5,
                    "name": "Ббб",
                    "slug": "bbb",
                },
                {
                    "fesh": 6,
                    "name": "бВВ",
                    "slug": "bvv",
                },
                {
                    "fesh": 4,
                    "name": "бвг",
                    "slug": "bvg",
                },
            ],
            preserve_order=False,
            allow_different_len=False,
        )

        # Проверяем заэнкоженные буквы в by-letter (б == "%D0%B1)
        response = self.report.request_json("place=shops_list&by-letter=%D0%B1")
        self.assertFragmentIn(
            response,
            [
                {
                    "fesh": 5,
                    "name": "Ббб",
                    "slug": "bbb",
                },
                {
                    "fesh": 6,
                    "name": "бВВ",
                    "slug": "bvv",
                },
                {
                    "fesh": 4,
                    "name": "бвг",
                    "slug": "bvg",
                },
            ],
            preserve_order=False,
            allow_different_len=False,
        )

    def test_upper_letter(self):
        # Если в by-letter - заглавная буква, то все равно отдаются магазины на эту букву
        response = self.report.request_json("place=shops_list&by-letter=Б")
        self.assertFragmentIn(
            response,
            [
                {
                    "fesh": 5,
                    "name": "Ббб",
                    "slug": "bbb",
                },
                {
                    "fesh": 6,
                    "name": "бВВ",
                    "slug": "bvv",
                },
                {
                    "fesh": 4,
                    "name": "бвг",
                    "slug": "bvg",
                },
            ],
            preserve_order=False,
            allow_different_len=False,
        )

    @classmethod
    def prepare_pager(cls):
        cls.index.shops += [Shop(name='c' + str(i), fesh=i) for i in range(19, 9, -1)]

    def test_pager(self):
        # Проверяем магазины на первой странице
        response = self.report.request_json("place=shops_list&by-letter=c&page=1&numdoc=4")
        self.assertFragmentIn(response, {"total": 10})
        self.assertFragmentIn(
            response,
            [
                {
                    "fesh": i,
                    "name": "c" + str(i),
                }
                for i in range(10, 14)
            ],
            allow_different_len=False,
            preserve_order=False,
        )

        # Проверяем на третьей странице
        response = self.report.request_json("place=shops_list&by-letter=c&page=3&numdoc=4")
        self.assertFragmentIn(
            response,
            [
                {
                    "fesh": i,
                    "name": "c" + str(i),
                }
                for i in range(18, 20)
            ],
            allow_different_len=False,
            preserve_order=False,
        )

    @classmethod
    def prepare_without_clones(cls):
        cls.index.shops += [
            Shop(name='ea d', fesh=101),
            Shop(name='ea', fesh=102, main_fesh=101),
            Shop(name='e c', fesh=103, main_fesh=101),
            Shop(name='Evvvv', fesh=104),
        ]

    def test_without_clones(self):
        # Проверяем, что с параметром without-clones=1 отдается только один магазин среди клонов, причем с самым коротким названием

        response = self.report.request_json("place=shops_list&by-letter=e&page=1&numdoc=10&without-clones=1")
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"fesh": 102, "shopClones": [101, 102, 103]},
                    {"fesh": 104},
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json("place=shops_list&by-letter=e&page=1&numdoc=10")
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "results": [
                    {"fesh": 101, "shopClones": [101, 102, 103]},
                    {"fesh": 102, "shopClones": [101, 102, 103]},
                    {"fesh": 103, "shopClones": [101, 102, 103]},
                    {"fesh": 104, "shopClones": []},
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_rating(cls):
        cls.index.shops += [
            Shop(
                name='Big Kahuna Burger',
                fesh=200,
                new_shop_rating=NewShopRating(
                    new_rating=4.5,
                    new_rating_total=3.9,
                    new_grades_count=456,
                    rec_and_nonrec_pub_count=789,
                ),
            ),
            Shop(
                name='Big Kahuna Pizza',
                fesh=201,
            ),
        ]

    def test_click_n_collect(self):
        """Test that rating info from dump renders correctly"""
        response = self.report.request_json("place=shops_list&by-letter=b")
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {
                        "fesh": 200,
                        "ratingToShow": 4.5,
                        "ratingType": 3,  # rating type RATING_3M
                        "overallGradesCount": 789,
                        "newGradesCount": 456,
                    },
                    {
                        "fesh": 201,
                        "ratingToShow": 0,
                        "ratingType": 6,  # default rating is NO_RATING
                        "overallGradesCount": NoKey("overallGradesCount"),
                        "newGradesCount": NoKey("newGradesCount"),
                    },
                ],
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
