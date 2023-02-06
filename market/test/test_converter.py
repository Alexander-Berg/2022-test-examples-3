#!/usr/bin/env python
# coding: utf-8

from market.library.blue_delivery_modifier.python.tariff_converter.tariff_converter import BlueDeliveryTariffConverter

import sys
import pytest


def test_converter():
    '''
    Проверяем успешное создание тарифов
    '''
    converter = BlueDeliveryTariffConverter()

    tariff1 = [{
        "large_size": 0,
        "price_rur_to": 700.0,
        "user_price_rur": 99.0
    }, {
        "large_size": 0,
        "price_rur_to": 2999.0,
        "user_price_rur": 49.0
    }, {
        "large_size": 0,
        "user_price_rur": 0.0
    }, {
        # В этом дефолтном тарифе large_size не указан. Значит ограничения по весу нет
        "user_price_rur": 550.0
    }]

    tariff2 = [{
        "large_size": 0,
        "price_rur_to": 700.0,
        "user_price_rur": 299.0
    }, {
        "large_size": 0,
        "price_rur_to": 4999.0,
        "user_price_rur": 149.0
    }, {
        "large_size": 1,        # Указан КГТ. Это тоже допустимо для дефолтного тарифа
        "user_price_rur": 700.0
    }]

    tariff_default = [{
        "user_price_rur": 800.0
    }]

    ds_tariff1 = [{
        "price_rur_to": 500.0,
        "user_price_rur": 2999.0
    }, {
        "user_price_rur": 149.0
    }]

    ds_tariff2 = [{
        "user_price_rur": 150.0
    }]

    ds_modifier = [{
        "service_ids" : [1, 2, 3],
        "tariffs": ds_tariff1
    }, {
        "service_ids" : [4, 5, 6],
        "tariffs": ds_tariff2
    }]

    converter.add_modifier(tariffs=tariff1, regions=[213], tier="tier_1")
    converter.add_modifier(tariffs=tariff2, regions=[2, 12, 123], tier="tier_2", delivery_service_modifiers=ds_modifier)
    converter.add_modifier(tariffs=tariff_default, regions=[], tier="tier_3")

    sample = {
        'modifiers': [{
            'regions': [213],
            'tariffs': tariff1,
            'tier': 'tier_1',
        }, {
            'regions': [2, 12, 123],
            'tariffs': tariff2,
            "delivery_service_modifiers": ds_modifier,
            'tier': 'tier_2',
        }],
        'default_modifier': {
            'tariffs': tariff_default,
            'tier': 'tier_3',
        }
    }

    result = converter.get_result()

    assert result == sample


def test_missing_user_price():
    tariff1 = [{
        "large_size": 0,
        "price_rur_to": 700.0,
        "invalid_user_price_rur": 99.0
    }, {
        "user_price_rur": 550.0
    }]

    converter = BlueDeliveryTariffConverter()

    with pytest.raises(Exception) as err_info:
        converter.add_modifier(tariff1, [], tier="tier_1")

    assert err_info.value.args[0] == 'user_price_rur or seller_payment_rur required'


def test_missing_default_tariff_large_size():
    tariff1 = [{
        "large_size": 0,
        "price_rur_to": 700.0,
        "user_price_rur": 99.0
    }, {
        "large_size": 0.0,
        "user_price_rur": 550.0
    }]

    converter = BlueDeliveryTariffConverter()

    with pytest.raises(Exception) as err_info:
        converter.add_modifier(tariff1, [], tier="tier_1")

    assert err_info.value.args[0] == 'default tariff required'


def test_missing_default_tariff_price():
    tariff1 = [{
        "large_size": 0,
        "price_rur_to": 700.0,
        "user_price_rur": 99.0
    }, {
        "price_rur_to": 800.0,
        "user_price_rur": 550.0
    }]

    converter = BlueDeliveryTariffConverter()

    with pytest.raises(Exception) as err_info:
        converter.add_modifier(tariff1, [], tier="tier_3")

    assert err_info.value.args[0] == 'default tariff required'


def test_wrong_order_in_tariff():
    # Цена не меняется, КГТ уменьшается
    tariff1 = [{
        "large_size": 1,
        "price_rur_to": 700.0,
        "user_price_rur": 99.0
    }, {
        "large_size": 0,
        "price_rur_to": 700.0,
        "user_price_rur": 99.0
    }]

    # Вес не меняется, цена уменьшается
    tariff2 = [{
        "large_size": 0,
        "price_rur_to": 700.0,
        "user_price_rur": 99.0
    }, {
        "large_size": 0,
        "price_rur_to": 500.0,
        "user_price_rur": 99.0
    }]

    # Ограничения по весу сперва пропадают, а потом появляются снова
    tariff3 = [{
        "large_size": .0,
        "user_price_rur": 99.0
    }, {
        "user_price_rur": 99.0
    }, {
        "large_size": 0,
        "user_price_rur": 99.0
    }]

    # Ограничения по весу сперва пропадают, а потом появляются снова
    # Неизменная цена при этом не важна
    tariff4 = [{
        "large_size": 0,
        "price_rur_to": 500.0,
        "user_price_rur": 99.0
    }, {
        "price_rur_to": 500.0,
        "user_price_rur": 99.0
    }, {
        "large_size": 0,
        "price_rur_to": 500.0,
        "user_price_rur": 99.0
    }]

    converter = BlueDeliveryTariffConverter()

    for tariff in [tariff1, tariff2, tariff3, tariff4]:
        with pytest.raises(Exception):
            converter.add_modifier(tariff, [], tier="tier_3")


def test_duplicate_regions():
    tariff = [{
        "user_price_rur": 99.0
    }]

    converter = BlueDeliveryTariffConverter()

    converter.add_modifier(tariff, [1, 2, 3, 4, 5], tier="tier_1")

    with pytest.raises(Exception) as err_info:
        converter.add_modifier(tariff, [1, 2, 3, 6, 7], tier="tier_2")

    assert err_info.value.args[0] == 'Duplicate regions set([1, 2, 3])' if sys.version_info.major == 2 else 'Duplicate regions {1, 2, 3}'


def test_duplicate_delivery_services():
    tariff = [{
        "user_price_rur": 99.0
    }]

    ds_tariff1 = [{
        "price_rur_to": 500.0,
        "user_price_rur": 2999.0
    }, {
        "user_price_rur": 149.0
    }]

    ds_tariff2 = [{
        "user_price_rur": 150.0
    }]

    ds_modifier = [{
        "service_ids" : [1, 2, 3],
        "tariffs": ds_tariff1
    }, {
        "service_ids" : [3, 5, 5, 6],
        "tariffs": ds_tariff2
    }]

    converter = BlueDeliveryTariffConverter()

    with pytest.raises(Exception) as err_info:
        converter.add_modifier(tariff, [1, 2, 3, 6, 7], tier="tier_2", delivery_service_modifiers=ds_modifier)

    assert err_info.value.args[0] == 'Duplicate delivery services [3, 5] in modifier'


def test_empty_delivery_services():
    tariff = [{
        "user_price_rur": 99.0
    }]

    ds_tariff = [{
        "user_price_rur": 150.0
    }]

    ds_modifier = [{
        "service_ids" : [],
        "tariffs": ds_tariff
    }]

    converter = BlueDeliveryTariffConverter()

    with pytest.raises(Exception) as err_info:
        converter.add_modifier(tariff, [1, 2, 3, 6, 7], tier="tier_2", delivery_service_modifiers=ds_modifier)

    assert err_info.value.args[0] == "service_ids list shouldn't be empty"


def test_missing_default_modifier():
    tariff = [{
        "user_price_rur": 99.0
    }]

    converter = BlueDeliveryTariffConverter()

    converter.add_modifier(tariff, [1], tier="tier_1")
    converter.add_modifier(tariff, [2], tier="tier_2")

    with pytest.raises(Exception) as err_info:
        converter.get_result()

    assert err_info.value.args[0] == 'Modifier without regions required'


def test_multiple_modifier():
    tariff = [{
        "user_price_rur": 99.0
    }]

    converter = BlueDeliveryTariffConverter()

    converter.add_modifier(tariff, [], tier="tier_1")

    with pytest.raises(Exception) as err_info:
        converter.add_modifier(tariff, [], tier="tier_3")

    assert err_info.value.args[0] == 'Multiple default modifier'


def test_exp():
    converter = BlueDeliveryTariffConverter()
    tariff_default = [{
        "user_price_rur": 800.0
    }]
    converter.add_modifier(tariffs=tariff_default, regions=[], tier="tier_3")

    exp_converter = BlueDeliveryTariffConverter()
    exp_tariff_213 = [{
        "user_price_rur": 700.0
    }]

    exp_tariff_default = [{
        "user_price_rur": 600.0
    }]
    exp_converter.add_modifier(tariffs=exp_tariff_213, regions=[213], tier="tier_1")
    exp_converter.add_modifier(tariffs=exp_tariff_default, regions=[], tier="tier_3")

    converter.add_experiment("exp1", exp_converter)

    sample = {
        'experiments': [{
            'exp_name': 'exp1',
            'modifiers': [{
                'regions': [213],
                'tariffs': exp_tariff_213,
                'tier': 'tier_1',
            }],
            'default_modifier': {
                'tariffs': exp_tariff_default,
                'tier': 'tier_3',
            }
        }],
        'modifiers': [],
        'default_modifier': {
            'tariffs': tariff_default,
            'tier': 'tier_3',
        }
    }

    result = converter.get_result()

    assert result == sample


def test_converter_by_type():
    '''
    Проверяем сохранение тарифа с разными ценами для разных типов доставки
    '''
    converter = BlueDeliveryTariffConverter()

    tariff1 = [{
        "large_size": 0,
        "price_rur_to": 700.0,
        "user_price_rur": 99.0,
        "user_price_courier_rur": 77.0,
    }, {
        "large_size": 0,
        "price_rur_to": 2999.0,
        "user_price_rur": 49.0,
        "user_price_pickup_rur": 66.0,
    }, {
        "large_size": 0,
        "user_price_rur": 0.0,
        "user_price_post_rur": 55.0,
    }, {
        # В этом дефолтном тарифе large_size не указан. Значит ограничения по весу нет
        "user_price_rur": 550.0
    }]

    tariff_default = [{
        "user_price_rur": 800.0
    }]

    converter.add_modifier(tariffs=tariff1, regions=[213], tier="tier_1")
    converter.add_modifier(tariffs=tariff_default, regions=[], tier="tier_3")

    sample = {
        'modifiers': [{
            'regions': [213],
            'tariffs': tariff1,
            'tier': 'tier_1',
        }],
        'default_modifier': {
            'tariffs': tariff_default,
            'tier': 'tier_3',
        }
    }

    result = converter.get_result()

    assert result == sample


def test_outlet_flags_threshold():
    '''
    Проверяем сохранение порога бесплатной доставки для маркетных оутлетов, маркетных постаматов и партнерских аутлетов
    '''
    for flag_name in ['market_outlet_threshold', 'market_post_term_threshold', 'market_partner_outlet_threshold']:
        converter = BlueDeliveryTariffConverter()

        tariff1 = [{
            # В этом дефолтном тарифе large_size не указан. Значит ограничения по весу нет
            "user_price_rur": 550.0
        }]

        tariff_default = [{
            "user_price_rur": 800.0
        }]

        converter.add_modifier(tariffs=tariff1, regions=[213], tier="tier_1", **{flag_name: 699})
        converter.add_modifier(tariffs=tariff1, regions=[2], tier="tier_2")
        converter.add_modifier(tariffs=tariff_default, regions=[], tier="tier_3", **{flag_name: 700})

        sample = {
            'modifiers': [{
                'regions': [213],
                'tariffs': tariff1,
                flag_name + '_rur': 699,
                'tier': 'tier_1',
            }, {
                'regions': [2],
                'tariffs': tariff1,
                'tier': 'tier_2',
            }],
            'default_modifier': {
                'tariffs': tariff_default,
                flag_name + '_rur': 700,
                'tier': 'tier_3',
            }
        }

        result = converter.get_result()

        assert result == sample


def test_ya_plus_threshold():
    '''
    Проверяем сохранение порога бесплатной доставки для плюсовиков
    '''
    converter = BlueDeliveryTariffConverter()

    tariff1 = [{
        # В этом дефолтном тарифе large_size не указан. Значит ограничения по весу нет
        "user_price_rur": 550.0
    }]

    tariff_default = [{
        "user_price_rur": 800.0
    }]

    converter.add_modifier(tariffs=tariff1, regions=[213], ya_plus_threshold=123, tier="tier_1")
    converter.add_modifier(tariffs=tariff1, regions=[2], tier="tier_2")
    converter.add_modifier(tariffs=tariff_default, regions=[], ya_plus_threshold=321, tier="tier_3")

    sample = {
        'modifiers': [{
            'regions': [213],
            'tariffs': tariff1,
            'ya_plus_threshold_rur': 123,
            'tier': 'tier_1',
        }, {
            'regions': [2],
            'tariffs': tariff1,
            'tier': 'tier_2',
        }],
        'default_modifier': {
            'tariffs': tariff_default,
            'ya_plus_threshold_rur': 321,
            'tier': 'tier_3',
        }
    }

    result = converter.get_result()

    assert result == sample


def test_beru_bonus_threshold():
    '''
    Проверяем сохранение порога бесплатной доставки для ББ бесплатной доставки
    '''
    converter = BlueDeliveryTariffConverter()

    tariff1 = [{
        # В этом дефолтном тарифе large_size не указан. Значит ограничения по весу нет
        "user_price_rur": 550.0
    }]

    tariff_default = [{
        "user_price_rur": 800.0
    }]

    converter.add_modifier(tariffs=tariff1, regions=[213], beru_bonus_threshold=127, tier="tier_1")
    converter.add_modifier(tariffs=tariff1, regions=[2], tier="tier_2")
    converter.add_modifier(tariffs=tariff_default, regions=[], beru_bonus_threshold=322, tier="tier_3")

    sample = {
        'modifiers': [{
            'regions': [213],
            'tariffs': tariff1,
            'beru_bonus_threshold_rur': 127,
            'tier': 'tier_1'
        }, {
            'regions': [2],
            'tariffs': tariff1,
            'tier': 'tier_2'
        }],
        'default_modifier': {
            'tariffs': tariff_default,
            'beru_bonus_threshold_rur': 322,
            'tier': 'tier_3',
        }
    }

    result = converter.get_result()

    assert result == sample


def test_tier():
    '''
    Проверяем ТИР
    '''
    converter = BlueDeliveryTariffConverter()

    tariff1 = [{
        # В этом дефолтном тарифе large_size не указан. Значит ограничения по весу нет
        "user_price_rur": 550.0
    }]

    tariff_default = [{
        "user_price_rur": 800.0
    }]

    converter.add_modifier(tariffs=tariff1, regions=[213], ya_plus_threshold=123, tier='tier_1')
    converter.add_modifier(tariffs=tariff1, regions=[2], tier='tier_2')
    converter.add_modifier(tariffs=tariff_default, regions=[], ya_plus_threshold=321, tier='tier_3')

    sample = {
        'modifiers': [{
            'tier': 'tier_1',
            'regions': [213],
            'tariffs': tariff1,
            'ya_plus_threshold_rur': 123
        }, {
            'tier': 'tier_2',
            'regions': [2],
            'tariffs': tariff1,
        }],
        'default_modifier': {
            'tier': 'tier_3',
            'tariffs': tariff_default,
            'ya_plus_threshold_rur': 321
        }
    }

    result = converter.get_result()

    assert result == sample


def test_large_size_weight():
    '''
    Проверяем выставление предельного веса для КГТ в модификаторе
    '''
    converter = BlueDeliveryTariffConverter()

    tariff1 = [{
        # В этом дефолтном тарифе large_size не указан. Значит ограничения по весу нет
        "user_price_rur": 550.0
    }]

    tariff_default = [{
        "user_price_rur": 800.0
    }]

    converter.add_modifier(tariffs=tariff1, regions=[213], tier='tier_1')
    converter.add_modifier(tariffs=tariff_default, regions=[], tier='tier_3', large_size_weight=23.7)

    sample = {
        'modifiers': [{
            'tier': 'tier_1',
            'regions': [213],
            'tariffs': tariff1,
        }],
        'default_modifier': {
            'tier': 'tier_3',
            'tariffs': tariff_default,
            'large_size_weight_kg': 23.7,
        }
    }

    result = converter.get_result()

    assert result == sample


def test_special_tariffs():
    '''
    Проверяется корректная работа специальных тарифов - 'fast', 'return', 'dsbs_seller', 'avia'
    '''

    for special_type in ('fast', 'return', 'dsbs_seller', 'avia'):
        special_converter = BlueDeliveryTariffConverter()
        converter = BlueDeliveryTariffConverter()

        price_str = "user_price_rur" if special_type != 'dsbs_seller' else 'seller_payment_rur'

        special_tariff = [{
            price_str: 150.0
        }]

        special_default_tariff = [{
            price_str: 350.0
        }]

        special_converter.add_modifier(tariffs=special_tariff, regions=[213], tier='tier_1')
        special_converter.add_modifier(tariffs=special_default_tariff, regions=[], tier='tier_1')

        default_tariff = [{
            price_str: 880.0
        }]
        converter.add_modifier(tariffs=default_tariff, regions=[], tier='tier_1')
        if special_type == 'fast':
            converter.add_fast(special_converter)
        elif special_type == 'return':
            converter.add_return(special_converter)
        elif special_type == 'dsbs_seller':
            converter.add_dsbs_seller(special_converter)
        elif special_type == 'avia':
            converter.add_avia(special_converter)
        else:
            assert False

        sample = {
            special_type: {
                'modifiers': [{
                    'tier': 'tier_1',
                    'regions': [213],
                    'tariffs': special_tariff,
                }],
                'default_modifier': {
                    'tier': 'tier_1',
                    'tariffs': special_default_tariff,
                }
            },
            'default_modifier': {
                'tier': 'tier_1',
                'tariffs': default_tariff,
            },
            'modifiers': []
        }

        result = converter.get_result()

        assert result == sample


def test_express():
    '''
    Проверяем установку тарифа express доставки
    '''

    special_converter = BlueDeliveryTariffConverter()
    converter = BlueDeliveryTariffConverter()

    special_tariff = [{
        "user_price_rur": 150.0
    }]

    special_default_tariff = [{
        "user_price_rur": 350.0
    }]

    special_converter.add_modifier(tariffs=special_tariff, regions=[213], tier='tier_1')
    special_converter.add_modifier(tariffs=special_default_tariff, regions=[], tier='tier_1')

    default_tariff = [{
        "user_price_rur": 880.0
    }]
    converter.add_modifier(tariffs=default_tariff, regions=[], tier='tier_1')

    converter.add_express('default', special_converter)

    sample = {
        'express': [{
            'exp_name': 'default',
            'modifiers': [{
                'tier': 'tier_1',
                'regions': [213],
                'tariffs': special_tariff,
            }],
            'default_modifier': {
                'tier': 'tier_1',
                'tariffs': special_default_tariff,
            }
        }],
        'default_modifier': {
            'tier': 'tier_1',
            'tariffs': default_tariff,
        },
        'modifiers': []
    }

    result = converter.get_result()

    assert result == sample
