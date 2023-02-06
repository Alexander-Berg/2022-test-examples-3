#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, GLParam, GLType, GLValue, HyperCategory
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [HyperCategory(hid=898601), HyperCategory(hid=898602)]

        cls.index.gltypes += [
            GLType(
                param_id=70001,
                hid=898601,
                gltype=GLType.ENUM,
                cluster_filter=True,
                values=[
                    GLValue(100, text="Nazvanie"),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=898601,
                title="BuffaloBill-1",
                glparams=[GLParam(param_id=70001, value=100)],
                model_medicine_form_param=70001,
                model_medicine_form_option=100,
                fesh=100,
            ),
            Offer(hid=898601, title="BuffaloBill-2", fesh=200),
        ]

    def test_model_medicine_form(self):
        response = self.report.request_json('place=prime&text=BuffaloBill')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {
                        "entity": "offer",
                        "shop": {"id": 100},
                        "modelMedicineForm": {"key": 70001, "value": 100, "name": "Nazvanie"},
                    },
                    {"entity": "offer", "shop": {"id": 200}, "modelMedicineForm": Absent()},
                ],
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
