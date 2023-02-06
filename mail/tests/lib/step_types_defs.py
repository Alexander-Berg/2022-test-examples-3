from parse import with_pattern
from parse_type import TypeBuilder

from tests_common.pytest_bdd import BehaveParser


@with_pattern(r"main|second")
def parse_main(text):
    return text == 'main'


@with_pattern(r"true|false")
def parse_true(text):
    return text == 'true'


@with_pattern(r'(not )?empty')
def parse_empty(text):
    return text == 'empty'


@with_pattern(r'(un)?initialized')
def parse_initialized(text):
    return text == 'initialized'


@with_pattern(r'no|some')
def parse_no_or_some(text):
    return text == 'some'


@with_pattern(r'has( not)?')
def parse_has_or_not(text):
    return 'not' not in text


@with_pattern(r'\$\w+')
def parse_var(text):
    return text


@with_pattern(r'shared\s*')
def parse_shared(_):
    return True


STID_TYPES = ["shared", "welcome"]
RULE_TYPES = ['clean', 'archive']


def extra_parsers():
    return dict(
        Empty=parse_empty,
        Initialized=parse_initialized,
        NoOrSome=parse_no_or_some,
        HasOrNot=parse_has_or_not,
        Var=parse_var,
        StidType=TypeBuilder.make_choice(STID_TYPES),
        OneOrMore=TypeBuilder.with_many(lambda x: x),
        RuleType=TypeBuilder.make_choice(RULE_TYPES),
        IsShared=parse_shared,
        IsTrue=parse_true,
        IsMain=parse_main,
    )


BehaveParser.extra_types.update(extra_parsers())
