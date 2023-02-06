#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import CategoryTransition, HyperCategory, Model, NavCategory, RecipeFilterValue
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()

        cls.index.models += [
            Model(hyperid=111, hid=1001),
            Model(hyperid=222, hid=1002),
            Model(hyperid=333, hid=1003),
            Model(hyperid=444, hid=1004),
        ]

        cls.index.navtree += [
            NavCategory(nid=1, hid=1001, uniq_name="Новая категория"),
            NavCategory(nid=2, hid=1002, uniq_name="C новыми gl-фильтрами"),
            NavCategory(nid=3, hid=1003, uniq_name="Без старых gl-фильтров"),
            NavCategory(nid=4, hid=1004, uniq_name="Без старых с новыми gl-фильтрами"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=101, exist=False),
            HyperCategory(hid=202, exist=False),
            HyperCategory(hid=303, exist=False),
            HyperCategory(hid=404, exist=False),
        ]

        cls.index.category_transitions += [
            CategoryTransition(old_category_id=101, new_category_id=1001),
            CategoryTransition(
                old_category_id=202,
                new_category_id=1002,
                new_recipe_filters=[RecipeFilterValue(1, "11"), RecipeFilterValue(2, "22")],
            ),
            CategoryTransition(
                old_category_id=303,
                new_category_id=1003,
                old_recipe_filters=[RecipeFilterValue(3, "33"), RecipeFilterValue(4, "44")],
            ),
            CategoryTransition(
                old_category_id=404,
                new_category_id=1004,
                old_recipe_filters=[RecipeFilterValue(5, "55"), RecipeFilterValue(6, "66")],
                new_recipe_filters=[RecipeFilterValue(7, "77"), RecipeFilterValue(8, "88")],
            ),
        ]

    def test_hid_transition(self):
        # Проверяем что для старой категории будет редирект на новую

        requests = ['place=prime&hid=101', 'place=prime&hid=101&rgb=blue']
        for r in requests:
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "params": {
                            "slug": ["novaia-kategoriia"],
                            "was_redir": ["1"],
                            "permanent": ["1"],
                            "nid": ["1"],
                            "hid": ["1001"],
                            "rt": ["9"],
                        },
                        "target": "search",
                    }
                },
                allow_different_len=False,
            )

    def test_hid_transition_new_filters(self):
        # Проверяем что для старой категории будет редирект на новую с добавлением gl-фильтров

        requests = [
            'place=prime&hid=202',
        ]
        for r in requests:
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "params": {
                            "glfilter": ["1:11", "2:22"],
                            "hid": ["1002"],
                            "nid": ["2"],
                            "permanent": ["1"],
                            "rt": ["9"],
                            "slug": ["c-novymi-gl-filtrami"],
                            "was_redir": ["1"],
                        },
                        "target": "search",
                    }
                },
                allow_different_len=False,
            )

    def test_hid_transition_old_filters(self):
        # Проверяем что для старой категории будет редирект на новую без старых фильтров

        response = self.report.request_json('place=prime&hid=303&glfilter=3:33&glfilter=4:44')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "hid": ["1003"],
                        "nid": ["3"],
                        "permanent": ["1"],
                        "rt": ["9"],
                        "slug": ["bez-starykh-gl-filtrov"],
                        "was_redir": ["1"],
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )

    def test_hid_transition_old_new_filters(self):
        # Проверяем что для старой категории будет редирект на новую без старых, но с новыми фильтрами

        response = self.report.request_json('place=prime&hid=404&glfilter=5:55&glfilter=6:66')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "glfilter": ["7:77", "8:88"],
                        "hid": ["1004"],
                        "nid": ["4"],
                        "permanent": ["1"],
                        "rt": ["9"],
                        "slug": ["bez-starykh-s-novymi-gl-filtrami"],
                        "was_redir": ["1"],
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )

    def test_hid_transition_incomplete_filters(self):
        # Проверяем что для старой категории не будет редиректа если не хватает какого-то фильтра
        # или значение параметра отличается от того что в transition'е

        requests = [
            'place=prime&hid=303&glfilter=4:44',
            'place=prime&hid=303&glfilter=3:44',
            'place=prime&hid=303&glfilter=4:33',
        ]
        for r in requests:
            response = self.report.request_json(r, strict=False)
            self.assertEqual(response.code, 400)
            self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST"}})
        self.error_log.expect(code=3036).times(len(requests))
        self.error_log.expect(code=3621).times(len(requests))


if __name__ == '__main__':
    main()
