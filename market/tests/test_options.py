# -*- coding: utf-8 -*-

from functools import cmp_to_key
from market.idx.pylibrary.regional_delivery.options import compare_options, DeliveryOption, OptionType, delivery_option_from_yml
from market.proto.delivery.delivery_calc import delivery_calc_pb2
from market.proto.common import common_pb2


def test_bool():
    expected = [False, True, True]
    opts = [
        DeliveryOption(OptionType.FORBIDDEN),
        DeliveryOption(OptionType.UNSPECIFIED),
        DeliveryOption(OptionType.NORMAL, delivery_calc_pb2.DeliveryOption())
    ]
    actual = list(map(bool, opts))
    assert actual == expected


def test_compare_options():
    args_list = [(100, 10, 20),
                 (50, 100, 20),
                 (100, 5, 21),
                 (100, 10, 18),
                 (100, 10, 20),
                 (200, 2, 10)]

    options = []
    for args in args_list:
        opt = delivery_calc_pb2.DeliveryOption()
        opt.delivery_cost = args[0]
        opt.max_days_count = args[1]
        opt.order_before = args[2]
        options.append(DeliveryOption(OptionType.NORMAL, opt))

    options.append(DeliveryOption(OptionType.FORBIDDEN))
    options.append(DeliveryOption(OptionType.UNSPECIFIED))

    actual_sorted = sorted(options, key=cmp_to_key(compare_options))
    expected_sorted = [options[1],  # cheapest
                       options[2],  # fastest in group of cost 100
                       options[0],  # two equal options ...
                       options[4],  # ... but sort is stable
                       options[3],  # worst in group of cost 100
                       options[5],  # most expensive
                       options[7],  # UNSPECIFIED
                       options[6]]  # FORBIDDEN

    assert actual_sorted == expected_sorted


def test_delivery_option_from_yml():
    yml_opt = common_pb2.TDeliveryOption(Cost=10, DaysMin=2, DaysMax=5, OrderBeforeHour=15)

    expected = {
        'price': {
            'value': 1000,
            'currency': 'RUR',
        },
        'dayTo': 5,
        'dayFrom': 2,
        'order_before': 15,
    }

    actual = delivery_option_from_yml(yml_opt, region=None, currency='RUR').to_dict()
    assert expected == actual
