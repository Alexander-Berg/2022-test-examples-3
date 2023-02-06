# coding: utf-8
from hamcrest import equal_to, described_as, not_
from parse import with_pattern
from parse_type import TypeBuilder
from tests_common.pytest_bdd import BehaveParser


@with_pattern(r'"\w+"')
def parse_quoted_word(text):
    return text.strip('"')


@with_pattern(r'has( not)? metadata')
def parse_has_as_not_none(text):
    if text == 'has metadata':
        return described_as('user has row mail.users', not_(equal_to(None)))
    return described_as('user has not row in mail.users', equal_to(None))


# in some cases name can be empty
@with_pattern(r'[\w-]*')
def parse_name(text):
    return text


def extra_parsers():
    return dict(
        QuotedWords=TypeBuilder.with_one_or_more(parse_quoted_word, listsep="and"),
        HasMetadataMatcher=parse_has_as_not_none,
        Name=parse_name,
        NameList=TypeBuilder.with_many(parse_name, listsep=","),
    )


BehaveParser.extra_types.update(extra_parsers())
