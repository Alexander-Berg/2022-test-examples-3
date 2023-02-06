# coding: utf-8
import pytest

from pyparsing import ParseException
from tools.filters_lang import parse_filter, FilterCondition, FilterAction


class Condition(FilterCondition):
    def __init__(self, field_type, field, oper, param, negative=False, link='or'):
        FilterCondition.__init__(
            self,
            field_type=field_type,
            field=field,
            oper=oper,
            pattern=param,
            negative=negative,
            link=link
        )


class Action(FilterAction):
    def __init__(self, oper, param, verified=False):
        FilterAction.__init__(
            self,
            oper=oper,
            param=param,
            verified=verified
        )


@pytest.mark.parametrize('invalid_str', [
    '',
    'IF',
    'IF body matches bar',
    'IF body matches bar THEN',
    'if body contains foo then forward to bar'
])
def test_invalid(invalid_str):
    with pytest.raises(ParseException):
        parse_filter(invalid_str)


def test_simple():
    res = parse_filter(
        '''IF body contains alcohol
           THEN move to safe@home''')
    assert res.conditions == [Condition('body', '', 'contains', 'alcohol')]
    assert res.actions == [Action('move', 'safe@home')]


def test_2_conditions():
    res = parse_filter(
        '''  IF body contains alcohol
            AND header.legs matches week
           THEN forward to taxi@yandex'''
    )
    assert res.conditions == [
        Condition('body', '', 'contains', 'alcohol', link='and'),
        Condition('header', 'legs', 'matches', 'week', link='and')]
    assert res.actions == [Action('forward', 'taxi@yandex')]


def test_2_or_conditions():
    res = parse_filter(
        '''  IF header.head contains Headache
             OR header.time matches end-of-work-day
           THEN move to home'''
    )
    assert res.conditions == [
        Condition('header', 'head', 'contains', 'Headache'),
        Condition('header', 'time', 'matches', 'end-of-work-day')]
    assert res.actions == [Action('move', 'home')]


def test_review():
    res = parse_filter(
        '''IF header.you contains review
        THEN movel as "+2"'''
    )
    assert res.conditions == [Condition('header', 'you', 'contains', 'review')]
    assert res.actions == [Action('movel', '+2')]


def test_2_actions():
    res = parse_filter(
        '''  IF header.you matches reading
           THEN move to coffee-point
            AND movel as raf'''
    )
    assert res.conditions == [Condition('header', 'you', 'matches', 'reading')]
    assert res.actions == [
        Action('move', 'coffee-point'),
        Action('movel', 'raf')]


def test_muti_word_action_params():
    res = parse_filter(
        '''  IF header.title matches "Thank God I'm Pretty"
           THEN movel as "Emilie Autumn"'''
    )
    assert res.conditions == [Condition('header', 'title', 'matches', "Thank God I'm Pretty")]
    assert res.actions == [Action('movel', 'Emilie Autumn')]


def test_not_condition():
    res = parse_filter(
        '''  IF NOT body matches strong
           THEN move to gym
        '''
    )
    assert res.conditions == [Condition(
        'body', '', 'matches', 'strong', negative=True)]
    assert res.actions == [Action('move', 'gym')]


def test_verified_action():
    res = parse_filter(
        '''  IF header.from contains foo
           THEN forward to VERIFIED foo@bar'''
    )
    assert res.conditions == [Condition('header', 'from', 'contains', 'foo')]
    assert res.actions == [Action('forward', 'foo@bar', verified=True)]


def test_not_verified_action():
    res = parse_filter(
        '''  IF header.subject contains money
           THEN forward to NOT VERIFIED wife@'''
    )
    assert res.conditions == [Condition('header', 'subject', 'contains', 'money')]
    assert res.actions == [Action('forward', 'wife@', verified=False)]


def test_all_in_one():
    res = parse_filter(
        '''  IF header.you matches want-beer
            AND header.your-friend matches want-beer
            AND NOT header.head contains headache
           THEN move to VERIFIED bar
            AND movel as beer'''
    )
    assert res.conditions == [
        Condition('header', 'you', 'matches', 'want-beer', link='and'),
        Condition('header', 'your-friend', 'matches', 'want-beer', link='and'),
        Condition('header', 'head', 'contains', 'headache', link='and', negative=True),
    ]
    assert res.actions == [
        Action('move', 'bar', verified=True),
        Action('movel', 'beer'),
    ]


IWATCH_CASE = '''
    IF from contains apple
   AND body contains iWatch
  THEN movel as system:priority_high
'''

WIFE_CASE = 'IF from contains school THEN forward to wife@'


@pytest.mark.parametrize(
    ('filter_str', 'conditions', 'actions'),
    [
        (
            IWATCH_CASE,
            [
                Condition('header', 'from', 'contains', 'apple', link='and'),
                Condition('body', '', 'contains', 'iWatch', link='and')
            ],
            [Action('movel', 'system:priority_high')]
        ),
        (
            WIFE_CASE,
            [Condition('header', 'from', 'contains', 'school')],
            [Action('forward', 'wife@')],
        )
    ]
)
def test_some_broken(filter_str, conditions, actions):
    res = parse_filter(filter_str)
    assert res.conditions == conditions
    assert res.actions == actions
