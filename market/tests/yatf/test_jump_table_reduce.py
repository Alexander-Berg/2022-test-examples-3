#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest

from hamcrest import (
    assert_that,
    contains_inanyorder,
    equal_to,
)

from yt.wrapper import ypath_join

from market.tools.jump_table_dumper.yatf.reducer_env.test_env import JumpTableReducer
from market.tools.jump_table_dumper.yatf.resources.jump_table import JumpTable
from market.proto.msku.jump_table_filters_pb2 import (
    JumpTableFilter,
    Picker,
)

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from msku_uploader.yatf.utils import make_mbo_picker_image_pb


def create_jump_table_filter(show_on, values, image_pickers_data=None):
    if image_pickers_data is None:
        image_pickers_data = []
    jump_filter = JumpTableFilter()
    jump_filter.show_on = show_on
    for field, value in values:
        value_proto = jump_filter.values.add()
        setattr(value_proto, field, value)
    jump_filter.pickers.extend(image_pickers_data)
    return jump_filter.SerializeToString()


@pytest.fixture(scope='module')
def jump_table_data():
    return [
        {
            'model_id': 322,
            'param_id': 3221,
            'msku': 1,
            'jump_filter': create_jump_table_filter(
                show_on=1,
                values=[
                    ('value', 111),
                    ('value', 112),
                    ('value', 113),
                ],
                image_pickers_data=[
                    Picker(
                        option_id=1,
                        image=make_mbo_picker_image_pb(
                            url='some_url1.ru',
                            namespace='mpic',
                            group_id='497',
                            image_name='QWERTY',
                            name='QWERTY_FAKE',
                        ),
                    ),
                    Picker(
                        option_id=2,
                        image=make_mbo_picker_image_pb(
                            url='some_url2.ru',
                            namespace='mpic',
                            group_id='498',
                            image_name='WERTYU',
                            name='WERTYU_FAKE',
                        ),
                    ),
                ]
            ),
        }, {
            'model_id': 322,
            'param_id': 3221,
            'msku': 2,
            'jump_filter': create_jump_table_filter(
                show_on=1,
                values=[
                    ('value', 112),
                    ('value', 113),
                ],
                image_pickers_data=[
                    Picker(
                        option_id=1,
                        image=make_mbo_picker_image_pb(
                            url='some_url1.ru',
                            namespace='mpic',
                            group_id='497',
                            image_name='QWERTY',
                            name='QWERTY_FAKE',
                        ),
                    ),
                    Picker(
                        option_id=3,
                        image=make_mbo_picker_image_pb(
                            url='some_url3.ru',
                            namespace='mpic',
                            group_id='499',
                            image_name='ERTYUI',
                            name='ERTYUI_FAKE',
                        ),
                    ),
                ]
            ),
        }, {
            'model_id': 322,
            'param_id': 3221,
            'msku': 3,
            'jump_filter': create_jump_table_filter(
                show_on=1,
                values=[
                    ('value', 113),
                    ('value', 111),
                ],
                image_pickers_data=[
                    Picker(
                        option_id=2,
                        image=make_mbo_picker_image_pb(
                            url='some_url2.ru',
                            namespace='mpic',
                            group_id='498',
                            image_name='WERTYU',
                            name='WERTYU_FAKE',
                        ),
                    ),
                    Picker(
                        option_id=4,
                        image=make_mbo_picker_image_pb(
                            url='some_url4.ru',
                            namespace='mpic',
                            group_id='500',
                            image_name='RTYUIO',
                            name='RTYUIO_FAKE',
                        ),
                    ),
                ]
            ),
        }, {
            'model_id': 323,
            'param_id': 3221,
            'msku': 1,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 32.2),
                ]
            ),
        }, {
            'model_id': 323,
            'param_id': 3221,
            'msku': 3,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 32.1),
                    ('numeric_value', 32.2),
                    ('numeric_value', 32.3),
                ]
            ),
        }, {
            'model_id': 323,
            'param_id': 3222,
            'msku': 2,
            'jump_filter': create_jump_table_filter(
                show_on=3,
                values=[
                    ('hypothesis_value', 'some_value'),
                    ('hypothesis_value', 'some_other_value'),
                ]
            ),
        }, {
            'model_id': 324,
            'param_id': 3221,
            'msku': 4,
            'msku_exp': 0,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 36.6),
                ]
            ),
        }, {
            'model_id': 324,
            'param_id': 3221,
            'msku': 4,
            'msku_exp': 5,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 39.),
                ]
            ),
        }, {
            'model_id': 324,
            'param_id': 3221,
            'msku': 6,
            'msku_exp': 0,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 54.),
                ]
            ),
        }, {
            'model_id': 325,
            'param_id': 3221,
            'msku': 6,
            'msku_exp': 7,
            'jump_filter': create_jump_table_filter(
                show_on=2,
                values=[
                    ('numeric_value', 42.),
                ]
            ),
        },
    ]


@pytest.fixture(scope='module')
def expected_reduced_data():
    return {
        322: {  # model_id
            'params': [
                {
                    'param_id': 3221,
                    'show_on': 1,
                    'mskus': [
                        {
                            'msku': 1,
                            'values': {111, 112, 113},
                            'extractor': lambda x: x.value
                        }, {
                            'msku': 2,
                            'values': {112, 113},
                            'extractor': lambda x: x.value
                        }, {
                            'msku': 3,
                            'values': {111, 113},
                            'extractor': lambda x: x.value
                        },
                    ],
                    'pickers': [
                        Picker(
                            option_id=1,
                            image=make_mbo_picker_image_pb(
                                url='some_url1.ru',
                                namespace='mpic',
                                group_id='497',
                                image_name='QWERTY',
                                name='QWERTY_FAKE',
                            ),
                        ),
                        Picker(
                            option_id=2,
                            image=make_mbo_picker_image_pb(
                                url='some_url2.ru',
                                namespace='mpic',
                                group_id='498',
                                image_name='WERTYU',
                                name='WERTYU_FAKE',
                            ),
                        ),
                        Picker(
                            option_id=3,
                            image=make_mbo_picker_image_pb(
                                url='some_url3.ru',
                                namespace='mpic',
                                group_id='499',
                                image_name='ERTYUI',
                                name='ERTYUI_FAKE'
                            ),
                        ),
                        Picker(
                            option_id=4,
                            image=make_mbo_picker_image_pb(
                                url='some_url4.ru',
                                namespace='mpic',
                                group_id='500',
                                image_name='RTYUIO',
                                name='RTYUIO_FAKE',
                            ),
                        ),
                    ]
                },
            ]
        },
        323: {  # model_id
            'params': [
                {
                    'param_id': 3221,
                    'show_on': 2,
                    'mskus': [
                        {
                            'msku': 1,
                            'values': {32.2},
                            'extractor': lambda x: x.numeric_value
                        },
                        {
                            'msku': 3,
                            'values': {32.1, 32.2, 32.3},
                            'extractor': lambda x: x.numeric_value
                        },
                    ]
                }, {
                    'param_id': 3222,
                    'show_on': 3,
                    'mskus': [
                        {
                            'msku': 2,
                            'values': {'some_value', 'some_other_value'},
                            'extractor': lambda x: x.hypothesis_value
                        },
                    ]
                },
            ]
        },
        324: {  # model_id
            'params': [
                {
                    'param_id': 3221,
                    'show_on': 2,
                    'mskus': [
                        {
                            'msku': 4,
                            'msku_exp': 0,
                            'values': {36.6},
                            'extractor': lambda x: x.numeric_value
                        },
                        {
                            'msku': 4,
                            'msku_exp': 5,
                            'values': {39.},
                            'extractor': lambda x: x.numeric_value
                        },
                        {
                            'msku': 6,
                            'msku_exp': 0,
                            'values': {54.},
                            'extractor': lambda x: x.numeric_value
                        },
                    ],
                },
            ]
        },
        325: {  # model_id
            'params': [
                {
                    'param_id': 3221,
                    'show_on': 2,
                    'mskus': [
                        {
                            'msku': 6,
                            'msku_exp': 7,
                            'values': {42.},
                            'extractor': lambda x: x.numeric_value
                        },
                    ],
                },
            ]
        },
    }


@pytest.yield_fixture(scope='module')
def workflow(yt_server, jump_table_data):
    resources = {
        'input_jump_table': JumpTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'jump_table'),
            data=jump_table_data
        )
    }

    with JumpTableReducer(**resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


def test_table_size(workflow, expected_reduced_data):
    assert_that(
        len(workflow.reduced_table),
        equal_to(len(expected_reduced_data)),
        'There are unexpected rows count in result table'
    )


def check_msku_part(actual_msku, expected_msku, model_id, param_id):
    assert_that(
        actual_msku.msku,
        equal_to(expected_msku['msku']),
        'Field msku is not matching for model_id: {} and param_id: {}'.format(
            model_id, param_id
        )
    )
    assert_that(
        actual_msku.msku_exp,
        equal_to(expected_msku.get('msku_exp', 0)),
        'Field msku_exp is not matching for model_id: {} and param_id: {}'.format(
            model_id, param_id
        )
    )
    extractor = expected_msku['extractor']
    actual_values = set([
        extractor(value)
        for value in actual_msku.values
    ])
    assert_that(
        actual_values,
        equal_to(expected_msku['values']),
        'Param values is not matching for model_id: {} and param_id: {}'.format(
            model_id, param_id
        )
    )


def check_param_part(actual_param, expected_param, model_id):
    assert_that(
        actual_param.param_id,
        equal_to(expected_param['param_id']),
        'ParamIds is not matching for model_id: {}'.format(model_id)
    )
    assert_that(
        actual_param.show_on,
        equal_to(expected_param['show_on']),
        'ShowOn is not matching for model_id: {}'.format(model_id)
    )
    msku_entries = actual_param.msku_entries
    expected_msku_entries = expected_param['mskus']
    assert_that(
        len(msku_entries),
        equal_to(len(expected_msku_entries)),
        'Different amount of msku for model_id: {} and param_id: {}'.format(
            model_id, actual_param.param_id
        )
    )
    for msku, expected_msku in zip(msku_entries, expected_msku_entries):
        check_msku_part(
            actual_msku=msku,
            expected_msku=expected_msku,
            model_id=model_id,
            param_id=actual_param.param_id,
        )


def test_content(workflow, expected_reduced_data):
    for row in workflow.reduced_table:
        actual_model_id = row['model_id']
        model_jump_table = row['model_jump_table']
        actual_model_id_in_jump_table = model_jump_table.model_id
        assert_that(
            actual_model_id,
            equal_to(actual_model_id_in_jump_table),
            "ModelIds in table aren't equal: model_id {}, model_id_in_jump_table {}".format(
                actual_model_id, actual_model_id_in_jump_table
            )
        )
        expected_data = expected_reduced_data[actual_model_id]
        param_entries = model_jump_table.param_entries
        expected_param_entries = expected_data['params']
        assert_that(
            len(param_entries),
            equal_to(len(expected_param_entries)),
            'Different amount of params for model_id: {}'.format(actual_model_id)
        )
        for param, expected_param in zip(param_entries, expected_param_entries):
            check_param_part(
                actual_param=param,
                expected_param=expected_param,
                model_id=actual_model_id,
            )


def test_pickers(workflow, expected_reduced_data):
    for row in workflow.reduced_table:
        actual_model_id = row['model_id']
        model_jump_table = row['model_jump_table']
        expected_data = expected_reduced_data[actual_model_id]
        param_entries = model_jump_table.param_entries
        expected_param_entries = expected_data['params']
        for param, expected_param in zip(param_entries, expected_param_entries):
            actual_pickers = param.pickers
            expected_pickers = expected_param.get('pickers')
            if expected_pickers is None:
                assert_that(
                    len(actual_pickers),
                    equal_to(0),
                    'Unexpected pickers for model_id: {} and param_id: {}'.format(
                        actual_model_id, param.param_id
                    )
                )
            else:
                assert_that(
                    len(actual_pickers),
                    equal_to(len(expected_pickers)),
                    'Number of pickers for model_id: {} and param_id: {}'.format(
                        actual_model_id, param.param_id
                    )
                )
                assert_that(
                    actual_pickers,
                    contains_inanyorder(*expected_pickers),
                    'Images of pickers are different for model_id: {} and param_id: {}'.format(
                        actual_model_id, param.param_id
                    )
                )
