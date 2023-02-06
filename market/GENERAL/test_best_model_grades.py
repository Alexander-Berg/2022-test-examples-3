#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, Model, ModelGrade
from core.testcase import TestCase, main
from core.types.hypercategory import ADULT_CATEG_ID
from core.matcher import ElementCount, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, name="ordinary_category"),
            HyperCategory(hid=ADULT_CATEG_ID, name="adult_category"),
        ]

        cls.index.best_model_grades += [
            ModelGrade(
                1, 2, 3, 0, 4, "pro_\\ntext", "contra_\\ntext", "short_\\ntext", 5, 6, 7, "cr_time_text", 8, 9.1
            ),
            ModelGrade(grade_id=11, model_id=3),
            ModelGrade(grade_id=21, model_id=4, rank=0.9615384615384615384615384615384615384616),
        ]

        cls.index.models += [
            Model(hyperid=2, title="non_adult_model_{}".format(1), hid=1),
            Model(hyperid=3, title="non_adult_model_{}".format(2), hid=ADULT_CATEG_ID),  # should be skipped
            Model(hyperid=4, title="non_adult_model_{}".format(3), hid=1),
        ]

    def test_output_correct(self):
        response = self.report.request_json('place=best_model_reviews&numdoc=1')
        product_json = {
            "entity": "product",
            "id": 2,
            "type": "model",
            "vendor": {"entity": "vendor"},
            "titles": {"raw": "non_adult_model_1"},
            "categories": [{"entity": "category", "name": NotEmpty(), "isLeaf": True}],
            "navnodes": [{"entity": "navnode", "name": NotEmpty(), "isLeaf": True}],
            "pictures": [{"entity": "picture", "original": NotEmpty(), "thumbnails": ElementCount(9)}],
        }

        self.assertFragmentIn(
            response,
            {
                "reviews": [
                    {
                        "entity": "review",
                        "id": 1,
                        "anonymous": 0,
                        "created": "cr_time_text",
                        "comment": "short_\ntext",
                        "pro": "pro_\ntext",
                        "contra": "contra_\ntext",
                        "averageGrade": 4,
                        "votes": {"agree": 5, "reject": 6, "total": 7},
                        "user": {"entity": "user", "uid": 3},
                        "region": {"entity": "region"},
                        "product": product_json,
                        "auxiliary": {"rank": 9.1},
                    }
                ]
            },
            preserve_order=True,
        )

    def test_adult_filtered(self):
        response = self.report.request_json('place=best_model_reviews&numdoc=3')
        self.assertEqual(2, response.count({"entity": "review"}))
        self.assertFragmentNotIn(
            response,
            {
                "entity": "review",
                "id": 11,
            },
        )


if __name__ == '__main__':
    main()
