# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import hamcrest
import pytest

from common.tester.factories import create_settlement
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.proxy_api.lib.general.formats import format_general_response
from travel.rasp.wizards.proxy_api.lib.tests_utils import make_general_query
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.serialization.intent_type import IntentType


pytestmark = pytest.mark.dbuser


@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_format_general_response():
    settlement = create_settlement(type_choices='bus,bycicle,train')
    expected_desktop_url = 'https://rasp.yandex.ru/city/{}?from=wraspgeneral'.format(settlement.id)
    expected_mobile_url = 'https://t.rasp.yandex.ru/city/{}?from=wraspgeneral'.format(settlement.id)

    assert format_general_response(make_general_query(settlement)) == {
        'content': {
            'departure_settlement': {
                'key': settlement.point_key,
                'title': 'НазваниеГорода'
            },
            'transports': [
                {
                    'code': 'train',
                    'selected': False,
                    'title': 'Поезд',
                },
                {
                    'code': 'bus',
                    'selected': False,
                    'title': 'Автобус',
                }
            ]
        },
        'path_items': [
            {
                'text': 'rasp.yandex.ru',
                'touch_url': 'https://t.rasp.yandex.ru/?from=wraspgeneral',
                'url': 'https://rasp.yandex.ru/?from=wraspgeneral'
            },
            {
                'text': 'НазваниеГорода',
                'touch_url': expected_mobile_url,
                'url': expected_desktop_url
            }
        ],
        'title': {
            '__hl': 'Расписание и дешёвые билеты из НазваниеГорода'
        },
        'touch_url': expected_mobile_url,
        'type': 'direction_query',
        'url': expected_desktop_url
    }


def test_format_general_response_departure_date():
    settlement = create_settlement()

    hamcrest.assert_that(
        format_general_response(make_general_query(settlement, departure_date=None)),
        hamcrest.has_entry('content', hamcrest.not_(hamcrest.has_key('date')))  # noqa: W601 (has_key of hamcrest)
    )

    hamcrest.assert_that(
        format_general_response(make_general_query(settlement, departure_date=date(2000, 1, 1))),
        hamcrest.has_entry('content', hamcrest.has_entry('date', '2000-01-01'))
    )


@pytest.mark.parametrize('transport_code, expected', (
    (None, False),
    ('bus', False),
    ('train', True)
))
def test_format_general_response_transports(transport_code, expected):
    settlement = create_settlement(type_choices='train')

    hamcrest.assert_that(
        format_general_response(make_general_query(settlement, transport_code=transport_code)),
        hamcrest.has_entry('content', hamcrest.has_entry('transports', hamcrest.contains(
            hamcrest.has_entries({'code': 'train', 'selected': expected})
        )))
    )


@pytest.mark.parametrize('transport_code, experiment_flags, intent, expected', (
    (
        'train',
        frozenset([ExperimentFlag.TRAIN_LINK_ALL]),
        None,
        {'text': 'trains', 'url': 'https://trains', 'touch_url': 'https://trains'},
    ),
    (
        'train',
        frozenset([ExperimentFlag.TRAIN_LINK_BUY_INTENT]),
        IntentType.BUY,
        {'text': 'trains', 'url': 'https://trains', 'touch_url': 'https://trains'},
    ),
))
def test_format_general_response_redirected_to_train(transport_code, experiment_flags, intent, expected):
    settlement = create_settlement(type_choices='train')

    response = format_general_response(make_general_query(
        settlement,
        transport_code=transport_code,
        experiment_flags=experiment_flags,
        intent=intent,
    ))
    assert len(response['path_items']) == 1
    for key in expected:
        assert response['path_items'][0][key].startswith(expected[key])
    assert response['url'].startswith(expected['url'])
    assert '/city/' not in response['url']
    assert response['touch_url'].startswith(expected['touch_url'])
    assert '/city/' not in response['touch_url']


@pytest.mark.parametrize('transport_code, experiment_flags, intent, expected', (
    (
        'bus',
        frozenset([ExperimentFlag.TRAIN_LINK_ALL]),
        None,
        {'text': 'rasp', 'url': 'https://rasp', 'touch_url': 'https://t.rasp'},
    ),
    (
        'bus',
        frozenset([ExperimentFlag.TRAIN_LINK_BUY_INTENT]),
        IntentType.BUY,
        {'text': 'rasp', 'url': 'https://rasp', 'touch_url': 'https://t.rasp'},
    ),
    (
        'bus',
        frozenset([ExperimentFlag.TRAIN_LINK_BUY_INTENT]),
        None,
        {'text': 'rasp', 'url': 'https://rasp', 'touch_url': 'https://t.rasp'},
    ),
    (
        'train',
        frozenset([ExperimentFlag.TRAIN_LINK_BUY_INTENT]),
        None,
        {'text': 'rasp', 'url': 'https://rasp', 'touch_url': 'https://t.rasp'},
    ),
))
def test_format_general_response_not_redirected_to_train(transport_code, experiment_flags, intent, expected):
    settlement = create_settlement(type_choices='train')

    response = format_general_response(make_general_query(
        settlement,
        transport_code=transport_code,
        experiment_flags=experiment_flags,
        intent=intent,
    ))
    assert len(response['path_items']) == 2
    for key in expected:
        assert response['path_items'][0][key].startswith(expected[key])
    assert response['url'].startswith(expected['url'])
    assert '/city/' in response['url']
    assert response['touch_url'].startswith(expected['touch_url'])
    assert '/city/' in response['touch_url']
