#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, NavCategory
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.models += [
            Model(hyperid=111, hid=8475933, title="new_category"),
            Model(hyperid=222, hid=15646037, title="new_category_with_discard"),
        ]

        cls.index.navtree += [
            NavCategory(nid=123, hid=8475933, uniq_name="Новая категория"),
            NavCategory(nid=456, hid=15646037, uniq_name="Новая категория без фильтра"),
        ]

    def test_hid_redirect(self):
        """
        Проверяем что для старой категории будет редирект на новую с нужным фильтром
        """

        requests = ['place=prime&hid=8476110', 'place=prime&hid=8476110&rgb=blue']
        for r in requests:
            response = self.report.request_json(r)
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "params": {
                            "glfilter": ["17874771:7876781"],
                            "hid": ["8475933"],
                            "nid": ["123"],
                            "permanent": ["1"],
                            "rt": ["9"],
                            "slug": ["novaia-kategoriia"],
                            "was_redir": ["1"],
                        },
                        "target": "search",
                    }
                },
                allow_different_len=False,
            )

    def test_glfilters_append(self):
        """
        Проверяем что исходные фильтры запроса сохранятся при редиректе
        """

        response = self.report.request_json(
            'place=prime&hid=8476110&glfilter=8512706:15041354&glfilter=8512706:16363268'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "glfilter": ["17874771:7876781", "8512706:15041354", "8512706:16363268"],
                        "hid": ["8475933"],
                        "nid": ["123"],
                        "permanent": ["1"],
                        "rt": ["9"],
                        "slug": ["novaia-kategoriia"],
                        "was_redir": ["1"],
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )

    def test_glfilters_discard(self):
        """
        Для hid=90720 есть правило редиректа, которое удаляет из исходного зарпоса glfilter=4863051:12110576,
        проверяем что в получившемся редиректе останется только один glfilter
        """
        response = self.report.request_json('place=prime&glfilter=4863051:12110576&glfilter=7893318:1024139&hid=90720')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "glfilter": ["7893318:1024139"],
                        "hid": ["15646037"],
                        "nid": ["456"],
                        "permanent": ["1"],
                        "rt": ["9"],
                        "slug": ["novaia-kategoriia-bez-filtra"],
                        "was_redir": ["1"],
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
