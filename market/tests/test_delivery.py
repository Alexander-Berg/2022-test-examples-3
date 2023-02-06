# -*- coding: utf-8 -*-

from market.proto.delivery.delivery_calc import delivery_calc_pb2

from market.idx.pylibrary.regional_delivery.delivery import RegionalDeliveryBase, LocalDelivery
from market.idx.pylibrary.regional_delivery.options import OptionType
from market.idx.pylibrary. regional_delivery.regions import RegionContainer
from .common import GEO_C2P_FILE, COUNTRIES_UTF8_FILE
from .stubs import CurrencyExchangeStub, OfferDataStub, dc_bucket, dc_option, dc_option_group


def region(r_id, g_id, opt_type):
    return {
        'region': r_id,
        'group_id': g_id,
        'type': opt_type,
    }

BUCKETS = [
    dc_bucket(111, 'RUR', [
        region(2, -1, delivery_calc_pb2.UNSPECIFIC_OPTION),
        region(5, 111, delivery_calc_pb2.NORMAL_OPTION),
        region(7, -1, delivery_calc_pb2.FORBIDDEN_OPTION),
    ]),
    dc_bucket(222, 'RUR', [
        region(3, -1, delivery_calc_pb2.FORBIDDEN_OPTION),
        region(2, 222, delivery_calc_pb2.NORMAL_OPTION),
    ]),
    dc_bucket(333, 'RUR', [
        region(5, 333, delivery_calc_pb2.NORMAL_OPTION),
        region(6, -1, delivery_calc_pb2.UNSPECIFIC_OPTION),
        region(7, -1, delivery_calc_pb2.FORBIDDEN_OPTION),
    ]),
    dc_bucket(444, 'USD', [
        region(5, 444, delivery_calc_pb2.NORMAL_OPTION),
        region(1, -1, delivery_calc_pb2.FORBIDDEN_OPTION),
        region(7, -1, delivery_calc_pb2.FORBIDDEN_OPTION),
    ]),
]


DELIVERY_OPTION_GROUPS = [
    dc_option_group(111, [
        dc_option(10000, 1, 1, 24),
        dc_option(5000, 10, 10, 16),
    ]),
    dc_option_group(222, [
        dc_option(50000, 2, 5, 24),
        dc_option(50000, 1, 2, 24),
    ]),
    dc_option_group(333, [
        dc_option(30000, 1, 1, 20),
        dc_option(30000, 1, 1, 23),
    ]),
    dc_option_group(444, [
        dc_option(300, 1, 1, 15),
    ]),
]


ROOT_REGION = 10000
UNKNOWN_REGION = 100499


REGION_CONTAINER = RegionContainer(GEO_C2P_FILE, COUNTRIES_UTF8_FILE)
CEXCHANGE = CurrencyExchangeStub()
OFFER_STUB = {
    'data:offer': OfferDataStub(),
}


def test_parse():
    """ Проверяем правильность парсинга из сырых опций (протобуфов).
        Для распаршенных групп опций должно выполняться:
            * Опции внутри группы отсортированы по 1) цене ^, 2) сроку доставки ^, 3) часу перескока v
            * У опций внутри группы правильно заполнены валюта и регион доставки - из бакета, по тому,
              для какого региона указана ссылка на группу опций.
    """

    regional_delivery = RegionalDeliveryBase(REGION_CONTAINER, CEXCHANGE)
    regional_delivery.raw_buckets = BUCKETS
    regional_delivery.raw_option_groups = DELIVERY_OPTION_GROUPS
    regional_delivery.parse()

    actual = {}
    for g_id in [111, 222, 333, 444]:
        group = regional_delivery.parsed_option_groups.get(g_id)
        group_opts = []
        for opt in group.options:
            group_opts.append(
                (opt.currency, opt.delivery_region_id, opt.price, opt.dayTo, opt.order_before)
            )
        actual[g_id] = group_opts

    ''' Сортировка внутри списков слева направо:
                   возрастание | убывание
                         v   v | v
    '''
    expected = {
        111: [('RUR', 5, 5000, 10, 16), ('RUR', 5, 10000, 1, 24)],
        222: [('RUR', 2, 50000, 2, 24), ('RUR', 2, 50000, 5, 24)],
        333: [('RUR', 5, 30000, 1, 23), ('RUR', 5, 30000, 1, 20)],
        444: [('USD', 5, 300, 1, 15)],
    }

    assert expected == actual


def test_default_option():
    regional_delivery = RegionalDeliveryBase(REGION_CONTAINER, CEXCHANGE)
    regional_delivery.raw_buckets = BUCKETS
    regional_delivery.raw_option_groups = DELIVERY_OPTION_GROUPS
    regional_delivery.parse()

    # Set up local delivery.
    local_delivery = LocalDelivery(REGION_CONTAINER, CEXCHANGE)
    local_delivery.load_for_offer(OFFER_STUB)
    regional_delivery.local_delivery = local_delivery

    actual = {}
    for region_id in [1, 2, 3, 5, 6, 7, 8, ROOT_REGION, UNKNOWN_REGION]:
        option = regional_delivery.get_default_option(region_id)
        print('{} - {} - {}'.format(region_id, bool(option), option.opt_type))
        res = (option.delivery_region_id, option.to_dict())
        actual[region_id] = (option.opt_type, res)

    exp_for_1 = {
        'price': {
            'currency': 'RUR',
            'value': 30000,
        },
        'dayTo': 1,
        'dayFrom': 1,
        'order_before': 23,
    }

    ''' For regions 2 and 5 there are two buckets: 111 (RUR) and 444 (USD), but 444 is better
        after conversion: country of 2 is 5 (with 'RUR' currency), 1 USD is 10 RUR, so
        3 USD = 30 RUR < 50 RUR (the best price in bucket 111).
    '''
    exp_for_2_5 = {
        'price': {
            'currency': 'RUR',
            'value': 3000,
        },
        'dayTo': 1,
        'dayFrom': 1,
        'order_before': 15,
    }

    ''' For region 3 there're options in YML. UseYmlDelivery is true, so they are used.
        The cheapest option is 2 USD, converted to user currency it is 20 RUR.
    '''
    exp_for_3 = {
        'price': {
            'currency': 'RUR',
            'value': 2000,
        },
        'dayTo': 10,
        'dayFrom': 1,
        'order_before': 24,
    }

    expected = {
        1: (OptionType.NORMAL, (5, exp_for_1)),
        2: (OptionType.NORMAL, (5, exp_for_2_5)),
        3: (OptionType.NORMAL, (3, exp_for_3)),
        5: (OptionType.NORMAL, (5, exp_for_2_5)),
        6: (OptionType.UNSPECIFIED, (None, {})),
        7: (OptionType.FORBIDDEN, (None, {})),
        8: (OptionType.FORBIDDEN, (None, {})),
        ROOT_REGION: (OptionType.FORBIDDEN, (None, {})),
        UNKNOWN_REGION: (OptionType.FORBIDDEN, (None, {})),
    }

    assert expected == actual
