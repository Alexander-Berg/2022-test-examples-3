# coding: utf-8

from parse import with_pattern
from tests_common.pytest_bdd import BehaveParser
from tests_common.step_types_defs import extra_parsers as base_extra_parsers


@with_pattern(r'first|second')
def parse_shard_id(text):
    if text == 'first':
        return 1
    return 2


@with_pattern(r'(not )?here')
def parse_is_here(text):
    return not text.startswith('not')


def extra_parsers():
    parsers = base_extra_parsers()
    parsers.update(dict(
        ShardID=parse_shard_id,
        IsHere=parse_is_here,
    ))
    return parsers


BehaveParser.extra_types.update(extra_parsers())
