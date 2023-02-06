#!/usr/bin/env python
# -*- coding: utf-8 -*-

import collections
import pytest

from hamcrest import (
    assert_that,
    equal_to,
)

from yt.wrapper import ypath_join

from market.tools.jump_table_dumper.yatf.dumper_env.test_env import JumpTableDumper
from market.tools.jump_table_dumper.yatf.local_dumper_env.test_env import JumpTableLocalDumper
from market.tools.jump_table_dumper.yatf.resources.jump_table_reduced import (
    JumpTableReduced,
    JumpTableReducedLocalJson,
)
from market.proto.msku.jump_table_filters_pb2 import (
    JumpTableModelEntry,
    Picker,
)
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)


def get_key_calculator(showon):
    SHOW_ON_SNIPPET_MASK = 1

    def calculate_big_value_key_jump_table(primary, _):
        return calculate_value_key(primary)

    def calculate_big_value_key_snippet(primary, additional_values):
        values = [primary, ] + additional_values
        keys = list()
        for val in values:
            keys.append(calculate_value_key(val))
        keys.sort()
        return ";".join(keys)

    if showon and SHOW_ON_SNIPPET_MASK:
        return calculate_big_value_key_snippet
    return calculate_big_value_key_jump_table


def calculate_value_key(value):
    for field in ['option_id', 'bool_value', 'numeric_value', 'hypothesis_value', 'value']:
        if field in value:
            return str(value[field])
    for field in ['OptionId', 'BoolValue', 'NumericValue', 'HypothesisValue', 'Value']:
        if field in value:
            return str(value[field]['Value'])
    raise Exception('Empty value: {}'.format(value))


def enum_value(option_id, is_original_value=False):
    return {
        'option_id': option_id,
        'value': option_id,     # Legacy
        'is_original_value': is_original_value
    }


def bool_value(value, is_original_value=False):
    return {
        'bool_value': value,
        'value': value,     # Legacy
        'is_original_value': is_original_value
    }


def numeric_value(value, is_original_value=False):
    return {
        'numeric_value': value,
        'is_original_value': is_original_value
    }


def hypothesis_value(value):
    return {
        'hypothesis_value': value
    }


def create_model_entry(model_data):
    model_entry = JumpTableModelEntry()
    model_entry.model_id = model_data['model_id']
    for param in model_data['param_entries']:
        param_entry = model_entry.param_entries.add()
        param_entry.param_id = param['param_id']
        param_entry.show_on = param['show_on']
        for msku in param['msku_entries']:
            msku_entry = param_entry.msku_entries.add()
            msku_entry.msku = msku['msku']
            if 'msku_exp' in msku:
                msku_entry.msku_exp = msku['msku_exp']
            for value in msku['values']:
                value_entry = msku_entry.values.add()
                for field, v in value.iteritems():
                    setattr(value_entry, field, v)
        pickers = param.get('pickers')
        if pickers:
            param_entry.pickers.extend(map(lambda x: Picker(**x), pickers))

    return model_entry.SerializeToString()


@pytest.fixture(scope='module')
def jump_reduced_table_data():
    return [
        {
            'model_id': 322,
            'param_entries': [
                {
                    'param_id': 3221,
                    'show_on': 2,       # SHOW_ON_JUMP_TABLE
                    'value_type': 'value',
                    'msku_entries': [
                        {
                            'msku': 1,
                            'values': [
                                enum_value(111),
                                enum_value(112),
                                enum_value(113, True),      # Основное значение будет в ключе
                            ],
                            'msku_exp': 0,
                        }, {
                            'msku': 2,
                            'values': [
                                enum_value(112),            # Не отмечены никакие праймари значения, берем первое
                                enum_value(113),
                            ],
                            'msku_exp': 0,
                        }, {
                            'msku': 3,
                            'values': [
                                enum_value(111),
                                # У этого МСКУ пропущено значение 112, но у МСКУ 1, имеющее тоже праймари значение 113, есть это значение
                                # 112 будет в результате в AdditionalValues
                                enum_value(113, True),
                            ],
                            'msku_exp': 0,
                        },
                    ],
                    'pickers': [
                        {
                            'option_id': 3,
                            'image': {
                                'url': 'some_url3.ru',
                                'namespace': 'mpic',
                                'group_id': '499',
                                'image_name': 'ERTYUI',
                                'name': 'ERTYUI_FAKE',
                            },
                        }, {
                            'option_id': 1,
                            'image': {
                                'url': 'some_url1.ru',
                                'namespace': 'mpic',
                                'group_id': '497',
                                'image_name': 'QWERTY',
                                'name': 'QWERTY_FAKE',
                            }
                        }, {
                            'option_id': 4,
                            'image': {
                                'url': 'some_url4.ru',
                                'namespace': 'mpic',
                                'group_id': '500',
                                'image_name': 'RTYUIO',
                                'name': 'RTYUIO_FAKE',
                            },
                        }, {
                            'option_id': 2,
                            'image': {
                                'url': 'some_url2.ru',
                                'namespace': 'mpic',
                                'group_id': '498',
                                'image_name': 'WERTYU',
                                'name': 'WERTYU_FAKE',
                            },
                        },
                    ]
                },
            ],
        }, {
            'model_id': 323,
            'param_entries': [
                {
                    'param_id': 3221,
                    'show_on': 2,       # SHOW_ON_JUMP_TABLE
                    'msku_entries': [
                        {
                            'msku': 1,
                            'values': [
                                numeric_value(32.2),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 3,
                            'values': [
                                numeric_value(32.1, True),
                                numeric_value(32.2),
                                numeric_value(32.3, True),  # В МБО было отмечено два значения, как основные. Берем первое из них
                            ],
                            'msku_exp': 0,
                        },
                    ],
                }, {
                    'param_id': 3222,
                    'show_on': 2,   # SHOW_ON_JUMP_TABLE
                    'msku_entries': [
                        {
                            'msku': 2,
                            'values': [
                                enum_value(111, True),
                                hypothesis_value('some_value'),
                                hypothesis_value('some_other_value'),
                            ],
                            'msku_exp': 0,
                        }, {
                            'msku': 3,
                            'values': [
                                hypothesis_value('some_value'),
                                enum_value(112, False),
                                hypothesis_value('some_other_value'),
                            ],
                            'msku_exp': 0,
                        },
                    ],
                },
            ],
        },
        {
            # для сниппетных фильтров значения параметра собираются по уникальному набору значений,
            # поэтому msku 5 и 6 по сниппетной логике должны быть объединены в одно значение (111, 112),
            # а 7 -е вынесено в отдельный блок т.к. его набор значений уникален (111, 113)
            # по старой же логике все три значения должны быть объединены в одно так как у них будет выбрано общее
            # primary значение (111)
            'model_id': 324,
            'param_entries': [
                {
                    'param_id': 3241,
                    'show_on': 1,  # SHOW_ON_SNIPPET
                    'msku_entries': [
                        {
                            'msku': 5,
                            'values': [
                                enum_value(111, True),
                                enum_value(112, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 6,
                            'values': [
                                enum_value(111, True),
                                enum_value(112, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 7,
                            'values': [
                                enum_value(111, True),
                                enum_value(113, True),
                            ],
                            'msku_exp': 0,
                        },
                    ],
                }, {
                    'param_id': 3242,
                    'show_on': 1,
                    'msku_entries': [
                        {
                            'msku': 5,
                            'values': [
                                enum_value(211, True),
                            ],
                            'msku_exp': 0,
                        }, {
                            'msku': 6,
                            'values': [
                                enum_value(212, True),
                            ],
                            'msku_exp': 0,
                        }, {
                            'msku': 7,
                            'values': [
                                enum_value(213, True),
                            ],
                            'msku_exp': 0,
                        },
                    ],
                },
            ],
        },
        {
            'model_id': 325,
            'param_entries': [
                {
                    'param_id': 3251,
                    'show_on': 1,  # SHOW_ON_SNIPPET
                    'msku_entries': [
                        {
                            'msku': 8,
                            'values': [
                                enum_value(111, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 9,
                            'values': [
                                enum_value(111, True),
                                enum_value(112, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 10,
                            'values': [
                                enum_value(112, True),
                            ],
                            'msku_exp': 0,
                        },
                    ],
                },
            ],
        },
        {
            'model_id': 326,
            'param_entries': [
                {
                    'param_id': 3252,
                    'show_on': 2,  # SHOW_ON_JUMP_TABLE
                    'msku_entries': [
                        {
                            'msku': 11,
                            'values': [
                                enum_value(113, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 12,
                            'values': [
                                enum_value(113, True),
                                enum_value(114, True),
                            ],
                            'msku_exp': 13,
                        },
                        {
                            'msku': 14,
                            'values': [
                                enum_value(114, True),
                            ],
                            'msku_exp': 0,
                        },
                    ],
                },
            ],
        },
        {
            'model_id': 327,
            'param_entries': [
                {
                    'param_id': 3253,
                    'show_on': 1,  # SHOW_ON_SNIPPET
                    'msku_entries': [
                        {
                            'msku': 15,
                            'values': [
                                enum_value(115, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 16,
                            'values': [
                                enum_value(115, True),
                                enum_value(116, True),
                            ],
                            'msku_exp': 17,
                        },
                        {
                            'msku': 18,
                            'values': [
                                enum_value(116, True),
                            ],
                            'msku_exp': 19,
                        },
                    ],
                },
            ],
        },
        {
            'model_id': 328,
            'param_entries': [
                {
                    'param_id': 3254,
                    'show_on': 1,  # SHOW_ON_SNIPPET
                    'msku_entries': [
                        {
                            'msku': 30,
                            'values': [
                                enum_value(115, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 31,
                            'values': [
                                enum_value(115, True),
                                enum_value(116, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 31,
                            'values': [
                                enum_value(115, True),
                                enum_value(116, True),
                            ],
                            'msku_exp': 32,
                        },
                        {
                            'msku': 33,
                            'values': [
                                enum_value(116, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 33,
                            'values': [
                                enum_value(116, True),
                            ],
                            'msku_exp': 34,
                        },
                    ],
                },
            ],
        },
        {
            'model_id': 329,
            'param_entries': [
                {
                    'param_id': 3255,
                    'show_on': 2,  # SHOW_ON_JUMP_TABLE
                    'msku_entries': [
                        {
                            'msku': 40,
                            'values': [
                                enum_value(113, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 41,
                            'values': [
                                enum_value(113, True),
                                enum_value(114, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 41,
                            'values': [
                                enum_value(113, True),
                                enum_value(114, True),
                            ],
                            'msku_exp': 42,
                        },
                        {
                            'msku': 43,
                            'values': [
                                enum_value(114, True),
                            ],
                            'msku_exp': 0,
                        },
                        {
                            'msku': 43,
                            'values': [
                                enum_value(114, True),
                            ],
                            'msku_exp': 44,
                        },
                    ],
                },
            ],
        },
    ]


@pytest.fixture(scope='module')
def expected_jump_table_values():
    return {
        322: {
            "params": {
                3221: [
                    {
                        "primary": enum_value(113),
                        "mskus": [1, 3],
                        "values": [enum_value(111), enum_value(112), ],
                    },
                    {
                        "primary": enum_value(112),
                        "mskus": [2, ],
                        "values": [enum_value(113), ],
                    },
                ]
            }
        },
        323: {
            "params": {
                3221: [
                    {
                        "primary": numeric_value(32.2),
                        "mskus": [1, ],
                        "values": [],
                    },
                    {
                        "primary": numeric_value(32.1),
                        "mskus": [3, ],
                        "values": [numeric_value(32.2), numeric_value(32.3), ],
                    }
                ],
                3222: [
                    {
                        "primary": enum_value(111),
                        "mskus": [2, ],
                        "values": [hypothesis_value("some_value"), hypothesis_value("some_other_value"), ],
                    },
                    {
                        "primary": hypothesis_value("some_value"),
                        "mskus": [3, ],
                        "values": [enum_value(112), hypothesis_value("some_other_value"), ],
                    }
                ]
            }
        },
        324: {
            "params": {
                3241: [
                    {
                        "primary": enum_value(111),
                        "mskus": [5, 6, ],
                        "values": [enum_value(112), ],
                    },
                    {
                        "primary": enum_value(113),
                        "mskus": [7, ],
                        "values": [enum_value(111), ],
                    }
                ],
                3242: [
                    {
                        "primary": enum_value(211),
                        "mskus": [5, ],
                        "values": [],
                    },
                    {
                        "primary": enum_value(212),
                        "mskus": [6, ],
                        "values": [],
                    },
                    {
                        "primary": enum_value(213),
                        "mskus": [7, ],
                        "values": [],
                    }
                ]
            }
        },
        325: {
            "params": {
                3251: [
                    {
                        "primary": enum_value(111),
                        "mskus": [8, ],
                        "values": [],
                    },
                    {
                        "primary": enum_value(112),
                        "mskus": [9, ],
                        "values": [enum_value(111), ],
                    },
                    {
                        "primary": enum_value(112),
                        "mskus": [10, ],
                        "values": [],
                    }
                ],
            }
        },
        326: {
            "params": {
                3252: [
                    {
                        "primary": enum_value(113),
                        "mskus": [11, 13],
                        "values": [enum_value(114)],
                    },
                    {
                        "primary": enum_value(114),
                        "mskus": [14],
                        "values": [],
                    },
                ],
            },
        },
        327: {
            "params": {
                3253: [
                    {
                        "primary": enum_value(115),
                        "mskus": [15],
                        "values": [],
                    },
                    {
                        "primary": enum_value(116),
                        "mskus": [16, 17],
                        "values": [enum_value(115)],
                    },
                    {
                        "primary": enum_value(116),
                        "mskus": [18, 19],
                        "values": [],
                    },
                ],
            }
        },
        328: {
            "params": {
                3254: [
                    {
                        "primary": enum_value(115),
                        "mskus": [30],
                        "values": [],
                    },
                    {
                        "primary": enum_value(116),
                        "mskus": [31, 32],
                        "values": [enum_value(115)],
                    },
                    {
                        "primary": enum_value(116),
                        "mskus": [33, 34],
                        "values": [],
                    },
                ],
            }
        },
        329: {
            "params": {
                3255: [
                    {
                        "primary": enum_value(113),
                        "mskus": [40, 41, 42],
                        "values": [enum_value(114)],
                    },
                    {
                        "primary": enum_value(114),
                        "mskus": [43, 44],
                        "values": [],
                    },
                ],
            },
        },
    }

expected_value_convert_list = {
    'option_id': 'OptionId',
    'bool_value': 'BoolValue',
    'numeric_value': 'NumericValue',
    'hypothesis_value': 'HypothesisValue',
}

expected_picker_convert_list = {
    'url': 'Url',
    'namespace': 'Namespace',
    'group_id': 'GroupId',
    'image_name': 'ImageName',
}


def convert_value_to_expected(value):
    result = {}
    for field, v in value.iteritems():
        if field in expected_value_convert_list:
            result[expected_value_convert_list[field]] = {'Value': v}
    return result


def convert_image_picker(picker):
    pickerData = picker['image']
    del pickerData['name']
    return {
        'OptionId': picker['option_id'],
        'PickerData': {
            expected_picker_convert_list[k]: v
            for k, v in pickerData.iteritems()
        },
    }


@pytest.yield_fixture(scope='module')
def expected(jump_reduced_table_data, expected_jump_table_values):
    def convert_param(param, model_id):
        expected_param_values = expected_jump_table_values[model_id]['params'][param['param_id']]
        values = collections.defaultdict(lambda: dict(Value=dict(), Mskus=set(), AdditionalValues={}))
        calculate_big_value_key = get_key_calculator(param['show_on'])

        for expected_value in expected_param_values:
            expected_primary_value_id = calculate_big_value_key(expected_value['primary'], expected_value['values'])
            values[expected_primary_value_id]['Mskus'] = set(expected_value['mskus'])
            values[expected_primary_value_id]['Value'] = convert_value_to_expected(expected_value['primary'])
            values[expected_primary_value_id]['AdditionalValues'] = {
                calculate_value_key(val): convert_value_to_expected(val) for val in expected_value['values']
            }

        sorted_pickers = sorted(
            param.get('pickers', []),
            key=lambda x: x['option_id']
        )
        result_pickers = [
            convert_image_picker(picker)
            for picker in sorted_pickers
        ]

        return {
            'ParamId': param['param_id'],
            'ShowOn': param['show_on'],
            'ValueNodes': values,
            'Pickers': result_pickers,
        }

    return {
        model['model_id']: {
            'ModelId': model['model_id'],
            'Params': {
                param['param_id']: convert_param(param, model['model_id']) for param in model['param_entries']
            }
        } for model in jump_reduced_table_data
    }


@pytest.yield_fixture(scope='module')
def workflow(yt_server, jump_reduced_table_data):
    resources = {
        'input_reduced_jump_table': JumpTableReduced(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'jump_table_reduced'),
            data=[{
                'model_id': record['model_id'],
                'model_jump_table': create_model_entry(record)
            } for record in jump_reduced_table_data]
        ),
        'use_64bit': False
    }

    with JumpTableDumper(**resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def workflow_64bit(yt_server, jump_reduced_table_data):
    resources = {
        'input_reduced_jump_table': JumpTableReduced(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'jump_table_reduced'),
            data=[{
                'model_id': record['model_id'],
                'model_jump_table': create_model_entry(record)
            } for record in jump_reduced_table_data]
        ),
        'use_64bit': True
    }

    with JumpTableDumper(**resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def local_workflow(jump_reduced_table_data):
    resources = {
        'input_json_file': JumpTableReducedLocalJson(
            data=jump_reduced_table_data
        ),
        'use_64bit': False
    }

    with JumpTableLocalDumper(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def local_workflow_64bit(jump_reduced_table_data):
    resources = {
        'input_json_file': JumpTableReducedLocalJson(
            data=jump_reduced_table_data
        ),
        'use_64bit': True
    }

    with JumpTableLocalDumper(**resources) as env:
        env.execute()
        env.verify()
        yield env


def check_size(wf, sample):
    assert_that(
        len(wf.jump_table_fb),
        equal_to(len(sample)),
        'Number of models in flatbuffer and in protobuf is different'
    )


def test_size(workflow, jump_reduced_table_data):
    check_size(workflow, jump_reduced_table_data)


def test_size_64bit(workflow_64bit, jump_reduced_table_data):
    check_size(workflow_64bit, jump_reduced_table_data)


def test_local_size(local_workflow, jump_reduced_table_data):
    check_size(local_workflow, jump_reduced_table_data)


def test_local_size_64bit(local_workflow_64bit, jump_reduced_table_data):
    check_size(local_workflow_64bit, jump_reduced_table_data)


def check_value(actual_value, expected_value, model_id, param_id):
    assert_that(
        actual_value,
        equal_to(expected_value),
        'Value not matches for model_id: {} and param_id: {}'.format(
            model_id, param_id
        )
    )


def check_values_part(actual_value_node, expected_value_node, model_id, param_id):
    check_value(
        actual_value=actual_value_node['Value'],
        expected_value=expected_value_node['Value'],
        model_id=model_id,
        param_id=param_id
    )
    actual_mskus = actual_value_node['Mskus']
    expected_mskus = expected_value_node['Mskus']
    assert_that(
        set(actual_mskus),
        equal_to(set(expected_mskus)),
        'Mskus not matches for model_id: {} and param_id: {}'.format(
            model_id, param_id
        )
    )

    for actual_additional in actual_value_node['AdditionalValues']:
        value_id = calculate_value_key(actual_additional)
        expected_additional = expected_value_node['AdditionalValues'][value_id]
        check_value(
            actual_value=actual_additional,
            expected_value=expected_additional,
            model_id=model_id,
            param_id=param_id
        )


def check_param_part(actual_param, expected_param, model_id):
    assert_that(
        actual_param['ShowOn'],
        equal_to(expected_param['ShowOn']),
        'ShowOn not matches for model_id: {}'.format(model_id)
    )
    value_entries = actual_param['ValueNodes']
    expected_value_entries = expected_param['ValueNodes']
    assert_that(
        len(value_entries),
        equal_to(len(expected_value_entries)),
        'Different amount of values for model_id: {} and param_id: {}'.format(
            model_id, actual_param['ParamId'],
        )
    )
    calculate_big_value_key = get_key_calculator(expected_param['ShowOn'])
    actual_values_order = []
    for value_node in value_entries:
        value_key = calculate_big_value_key(value_node['Value'], value_node['AdditionalValues'])
        assert_that(value_key in expected_value_entries, equal_to(True),
                    'There are no such value id {} key {} while processing model_id: {} and param_id: {}'.format(
                        value_node['Value'],
                        value_key,
                        model_id,
                        actual_param['ParamId']))
        expected_value_node = expected_value_entries[value_key]
        check_values_part(
            actual_value_node=value_node,
            expected_value_node=expected_value_node,
            model_id=model_id,
            param_id=actual_param['ParamId'],
        )
        actual_values_order.append(value_node['Value'].values())
    sorted_values = sorted(actual_values_order)
    assert_that(
        actual_values_order,
        equal_to(sorted_values),
        'Order is incorrect'
    )
    picker_entries = actual_param.get('Pickers', [])
    expected_picker_entries = expected_param['Pickers']
    assert_that(
        picker_entries,
        equal_to(expected_picker_entries),
        'Different pickers for model_id: {} and param_id: {}'.format(
            model_id, actual_param['ParamId']
        )
    )


def check_content(wf, expected):
    assert_that(
        len(wf.jump_table_fb),
        equal_to(len(expected)),
        'Same amount of models'
    )
    for model_entry in wf.jump_table_fb:
        content = model_entry['ModelContent']
        actual_model_id = content['ModelId']
        expected_model_entry = expected[actual_model_id]

        param_entries = content['Params']
        expected_param_entries = expected_model_entry['Params']
        assert_that(
            len(param_entries),
            equal_to(len(expected_param_entries)),
            'Different amount of params for model_id: {}'.format(actual_model_id)
        )

        for param in param_entries:
            actual_param_id = param['ParamId']
            expected_param = expected_param_entries[actual_param_id]
            check_param_part(
                actual_param=param,
                expected_param=expected_param,
                model_id=actual_model_id,
            )


def test_yt_content(workflow, expected):
    check_content(workflow, expected)


def test_yt_content_64bit(workflow_64bit, expected):
    check_content(workflow_64bit, expected)


def test_local_content(local_workflow, expected):
    check_content(local_workflow, expected)


def test_local_content_64bit(local_workflow_64bit, expected):
    check_content(local_workflow_64bit, expected)
