from collections import OrderedDict

from market.tools.hroom.lib.wiki_parser.cell_parsers import parse_day, parse_question, parse_answers, parse_username


def test_parse_day():
    assert parse_day("1 день") == 1


def test_parse_question():
    assert parse_question("   test") == "test"


def test_parse_answers():
    parsed = OrderedDict([(1, "aaa"), (2, "bbb"), (3, "ccc")])
    assert parsed == parse_answers("1. aaa | 2. bbb | 3. ccc", "|")


def test_parse_answers_range():
    parsed = OrderedDict([(1, "aaa"), (2, "aaa"), (3, "ccc")])
    assert parsed == parse_answers("1-2. aaa  | 3. ccc", "|")


def test_parse_answers_separate():
    parsed = OrderedDict([(1, "aaa"), (3, "aaa"), (2, "ccc")])
    assert parsed == parse_answers("1,3. aaa | 2. ccc", "|")


def test_parse_username():
    assert parse_username('123<иМя>qw') == "123${username}qw"
