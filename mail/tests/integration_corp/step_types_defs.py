# coding: utf-8

from enum import Enum
from parse import with_pattern
from parse_type import TypeBuilder

from tests_common.pytest_bdd import BehaveParser
from tests_common.step_types_defs import extra_parsers as base_extra_parsers

RULE_TYPES = [
    'clean',
    'archive',
]

WORDS_COUNTS = {
    'one': 1,
    'no': 0
}


class Shard(Enum):
    first = 1
    second = 2


@with_pattern(r'(is|are) (one|no|"\d+") messages?')
def parse_is_message_count(text):
    count_str = text.split()[1]
    if count_str in WORDS_COUNTS:
        return WORDS_COUNTS[count_str]
    return int(count_str.strip('"'))


@with_pattern(r'(not )?here')
def parse_is_here(text):
    return not text.startswith('not')


@with_pattern(r'(%s)' % '|'.join(RULE_TYPES))
def parse_rule_type(text):
    return text


def extra_parsers():
    parsers = base_extra_parsers()
    parsers.update(dict(
        Shard=TypeBuilder.make_enum(Shard),
        RuleType=parse_rule_type,
        IsMessageCount=parse_is_message_count,
        IsHere=parse_is_here,
    ))
    return parsers

BehaveParser.extra_types.update(extra_parsers())
