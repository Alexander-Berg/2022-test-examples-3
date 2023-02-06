#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Less, GreaterEq
from core.types import Offer
from core.testcase import TestCase, main


def CreateMetaFormulas(testroot, formulas):
    import os
    from core.paths import safe_makedirs

    def Write(path, content):
        with open(path, 'w') as file:
            file.write(content)

    formulasPath = os.path.join(testroot, 'formulas')
    safe_makedirs(formulasPath)

    # write version
    Write(os.path.join(formulasPath, 'version.txt'), '0.LITE')

    # create empty folder for base formulas
    safe_makedirs(os.path.join(formulasPath, 'formulas'))

    # create empty folder for category formulas
    safe_makedirs(os.path.join(formulasPath, 'category_formulas'))

    # create empty folder for category formulas
    safe_makedirs(os.path.join(formulasPath, 'blender_formulas'))
    safe_makedirs(os.path.join(formulasPath, 'blender_bundles'))

    # create meta formulas
    metaFormulasPath = os.path.join(formulasPath, 'meta_formulas')
    safe_makedirs(metaFormulasPath)
    for (name, content) in formulas:
        Write(os.path.join(metaFormulasPath, name), content)

    return formulasPath


class T(TestCase):
    @classmethod
    def prepare(cls):
        def GetFormulaJson(formulaName, modelName):
            return (formulaName, '{"class":"model","model_type":"remote","model":"' + modelName + '"}')

        # SHOP_ID factor index (see src/factors/meta_factors.h)
        SHOP_ID__FID = 5

        # disable mocks
        cls.matrixnet.set_defaults = False

        # enable CalcService
        # 12.21 for any set of factors
        cls.calc_service.on_request("ExistingRemoteModel", {}, {}).respond(12.21)
        # 1 if SHOP_ID >= 5, 0 otherwise
        cls.calc_service.on_request("ByShopId", {SHOP_ID__FID: Less(5)}, {}).respond(0)
        cls.calc_service.on_request("ByShopId", {SHOP_ID__FID: GreaterEq(5)}, {}).respond(1)

        # deploy meta formulas for CalcService
        cls.settings.formulas_path = CreateMetaFormulas(
            cls.meta_paths.testroot,
            [
                GetFormulaJson("CalcOfExistingRemoteModel", "ExistingRemoteModel"),
                GetFormulaJson("CalcOfAbsentRemoteModel", "AbsentRemoteModel"),
                GetFormulaJson("CalcByShopId", "ByShopId"),
            ],
        )

        cls.index.offers += [
            Offer(title="test-offer", fesh=4),
            Offer(title="test-offer", fesh=5),
            Offer(title="test-offer", fesh=6),
        ]

    def requestWithMetaFormula(self, formulaName):
        return self.report.request_json(
            "debug=1&place=prime&text=test-offer&rearr-factors=market_relevance_formula_threshold=0;market_meta_formula_type="
            + formulaName
        )

    def test_calculator_for_existing_remote_model(self):
        """
        Calculation on simple remote model
        """
        response = self.requestWithMetaFormula("CalcOfExistingRemoteModel")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "fullFormulaInfo": [{"tag": "Meta", "name": "CalcOfExistingRemoteModel", "value": "12.21"}]
                        }
                    }
                ]
            },
        )

    def test_calculator_for_absent_remote_model(self):
        """
        Calculation on absent remote model
        """
        response = self.requestWithMetaFormula("CalcOfAbsentRemoteModel")
        self.assertFragmentIn(response, {"error": {}})

    def test_calculator_by_shop_id(self):
        """
        Calculation on 'ByShopId' remote model
        """
        response = self.requestWithMetaFormula("CalcByShopId")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "shop": {"id": 4},
                        "debug": {"fullFormulaInfo": [{"tag": "Meta", "name": "CalcByShopId", "value": "0"}]},
                    },
                    {
                        "shop": {"id": 5},
                        "debug": {"fullFormulaInfo": [{"tag": "Meta", "name": "CalcByShopId", "value": "1"}]},
                    },
                    {
                        "shop": {"id": 6},
                        "debug": {"fullFormulaInfo": [{"tag": "Meta", "name": "CalcByShopId", "value": "1"}]},
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()
