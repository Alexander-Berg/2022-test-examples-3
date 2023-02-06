import pytest
import six

import yamarec1.config.parser

from yamarec1.config.exceptions import ConfigParseError


def test_parser_accepts_empty_config():
    result = yamarec1.config.parser.parse("")
    assert "config" in result
    assert len(result.config) == 0


def test_parser_accepts_expressions_as_values():
    result = yamarec1.config.parser.parse("x = 0")
    assert len(result.config) == 1
    assert result.config[0].key == "x"
    assert result.config[0].expression == "0"


def test_parser_accepts_empty_expressions_as_values():
    result = yamarec1.config.parser.parse("x =")
    assert len(result.config) == 1
    assert result.config[0].key == "x"
    assert "expression" in result.config[0]
    assert result.config[0].expression == ""


def test_parser_accepts_subconfigs_as_values():
    result = yamarec1.config.parser.parse("x:\n y = 0")
    assert len(result.config) == 1
    assert result.config[0].key == "x"
    assert len(result.config[0].section.config) == 1
    assert result.config[0].section.config[0].key == "y"
    assert result.config[0].section.config[0].expression == "0"


def test_parser_accepts_empty_subconfigs_as_values():
    result = yamarec1.config.parser.parse("x:\ny = 1")
    assert len(result.config) == 2
    assert result.config[0].key == "x"
    assert "section" in result.config[0]
    assert "meta" not in result.config[0].section
    assert "config" not in result.config[0].section


def test_parser_requires_config_entries_to_be_aligned():
    result = yamarec1.config.parser.parse("x = 0\ny = 1")
    assert len(result.config) == 2
    assert result.config[0].key == "x"
    assert result.config[0].expression == "0"
    assert result.config[1].key == "y"
    assert result.config[1].expression == "1"
    with pytest.raises(ConfigParseError):
        result = yamarec1.config.parser.parse(" x = 0")
    with pytest.raises(ConfigParseError):
        result = yamarec1.config.parser.parse("x:\n  y = 1\n z = 2")
    with pytest.raises(ConfigParseError):
        result = yamarec1.config.parser.parse(" # comment")


def test_parser_supports_multiline_expressions():
    result = yamarec1.config.parser.parse("x = [\n 1,\n 2,\n 3]")
    assert len(result.config) == 1
    assert result.config[0].key == "x"
    assert result.config[0].expression == "[\n 1,\n 2,\n 3]"
    result = yamarec1.config.parser.parse("x =\n {\n  1,\n  2,\n }")
    assert len(result.config) == 1
    assert result.config[0].key == "x"
    assert result.config[0].expression == "{\n  1,\n  2,\n }"
    with pytest.raises(ConfigParseError):
        yamarec1.config.parser.parse("x = {\n}")


def test_parser_handles_subconfig_meta_correctly():
    result = yamarec1.config.parser.parse("x: this is meta\n y = 1")
    assert len(result.config) == 1
    assert result.config[0].key == "x"
    assert result.config[0].section.meta == "this is meta"
    assert len(result.config[0].section.config) == 1
    with pytest.raises(ConfigParseError):
        yamarec1.config.parser.parse("x: should not be\n  multiline\n y = 1")


def test_parser_ignores_comments():
    result = yamarec1.config.parser.parse("# comment")
    assert len(result.config) == 0
    result = yamarec1.config.parser.parse("# first\nx = 1\n# second\ny:\n z = 3\n# third")
    assert len(result.config) == 2
    result = yamarec1.config.parser.parse("x = '''\n # not if part of expression\n '''")
    assert len(result.config) == 1
    assert "#" in result.config[0].expression


def test_parser_ignores_whitespaces():
    result = yamarec1.config.parser.parse(" \t\n# comment\nx = 1\n\ny:\n\n\t\t  z\t= \t3\n\t ")
    assert len(result.config) == 2
    assert result.config[0].key == "x"
    assert result.config[0].expression == "1"
    assert result.config[1].key == "y"
    assert "meta" not in result.config[1]
    assert len(result.config[1].section.config) == 1
    assert result.config[1].section.config[0].key == "z"
    assert result.config[1].section.config[0].expression == "3"


def test_parser_fails_on_invalid_keys():
    with pytest.raises(ConfigParseError):
        yamarec1.config.parser.parse("invalid key = expression")
    with pytest.raises(ConfigParseError):
        yamarec1.config.parser.parse("0_a = 1_b")
    with pytest.raises(ConfigParseError):
        yamarec1.config.parser.parse("invalid key:\n config = 1")


def test_parser_supports_unicode():
    result = yamarec1.config.parser.parse(six.u("x = \u043f\u0440\u0438\u0432\u0435\u0442"))
    assert len(result.config) == 1
    assert result.config[0].key == "x"
    assert result.config[0].expression == six.u("\u043f\u0440\u0438\u0432\u0435\u0442")
