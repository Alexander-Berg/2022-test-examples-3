#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
from core.testcase import TestCase


class TMadvModelCardTest(TestCase):
    """
    Тесты врезок от предыдущей ручки competitive_model_card, полуемых от новой madv_model_card
    """

    _rearr_factors = (
        "&rearr-factors="
        "market_competitive_model_card_do_not_bill=0"
        ";market_competitive_model_card_closeness_threshold=10"
        ";split=empty"
        ";market_competitive_model_card_disable_remove_empty_do=1"  # отключение функционала по запрету выдачи моделей без ДО
    )

    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        if params and rearr:
            return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))
        elif params:
            return dict_to_str(params, '&')
        else:  # only rearr
            return "rearr-factors={}".format(dict_to_str(rearr, ';'))
