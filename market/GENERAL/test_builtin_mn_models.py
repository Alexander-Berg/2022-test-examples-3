#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    """
    Test whether all necessary MatrixNet formulas are builtin into report. Everyone
    is welcome to add its formulas.
    """

    @classmethod
    def prepare(cls):
        cls.matrixnet.set_defaults = False
        cls.index.offers += [Offer(hyperid=1)]

    def check_base_model(self, modelName):
        _ = self.report.request_json("place=productoffers&hyperid=1&rearr-factors=market_ha_cpc_formula=" + modelName)
        # check nothing here: if the model is absent we fail on unexpected message in error log

    def check_meta_model(self, modelName):
        _ = self.report.request_json(
            "place=productoffers&hyperid=1&rearr-factors=market_meta_formula_type=" + modelName
        )
        # check nothing here: if the model is absent we fail on unexpected message in error log
        # TSearchExperimentFlags constructor checks if model specified in 'market_meta_formula_type' exists

    def test_fail_on_absent_base_model(self):
        "Check itself - error message must be logged for absent base MN model"

        modelName = "__MN_DEFINITELY_ABSENT_MODEL_"
        self.check_base_model(modelName)
        self.base_logs_storage.error_log.expect("No calculator for mn algorithm " + modelName)

    def test_fail_on_absent_meta_model(self):
        "Check itself - error message must be logged for absent meta MN model"

        modelName = "__MN_DEFINITELY_ABSENT_MODEL_"
        self.check_meta_model(modelName)
        self.error_log.expect(
            "Can't parse experimental flag \"market_meta_formula_type\" "
            "Meta mn algorithm \"" + modelName + "\" doesn't exist"
        )

        self.base_logs_storage.error_log.expect(
            "Can't parse experimental flag \"market_meta_formula_type\" "
            "Meta mn algorithm \"" + modelName + "\" doesn't exist"
        )

    # BASE MODELS
    def test_MNA_HybridAuctionCpcCtr2430(self):
        self.check_base_model("MNA_HybridAuctionCpcCtr2430")

    def test_MNA_DefaultOffer_2680(self):
        self.check_base_model("MNA_DefaultOffer_2680")

    def test_MNA_DefaultOffer_20180718(self):
        self.check_base_model("MNA_DefaultOffer_20180718")

    def test_MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax(self):
        self.check_base_model("MNA_DO_20190325_simple_factors_6w_shops99m_QuerySoftMax")

    def test_MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter(self):
        self.check_base_model("MNA_DO_20190720_shift_goal_all_factors_shops90m_c0_QSM_1000iter")

    def test_MNA_P_Purchase_log_loss_full_factors_6w_20210311(self):
        self.check_base_model("MNA_P_Purchase_log_loss_full_factors_6w_20210311")

    def test_MNA_top6_formula_logloss_220718(self):
        self.check_base_model("MNA_top6_formula_logloss_220718")

    def test_MNA_top6_formula_softmax_220718(self):
        self.check_base_model("MNA_top6_formula_softmax_220718")

    def test_MNA_top6_formula_logloss_220725(self):
        self.check_base_model("MNA_top6_formula_logloss_220725")

    def test_MNA_top6_formula_softmax_220725(self):
        self.check_base_model("MNA_top6_formula_softmax_220725")

    # META MODELS
    def test_meta_fml_formula_268329(self):
        self.check_meta_model("meta_fml_formula_268329")


if __name__ == '__main__':
    main()
