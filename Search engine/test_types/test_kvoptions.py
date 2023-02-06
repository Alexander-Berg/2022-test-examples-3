# -*- coding: utf-8 -*-
from rtcc.dataprovider.searchconfigs.types.kvoptions import KVOption


def test_repr_line_empty():
    kvoption = KVOption("")
    assert kvoption.repr_string() == ''


def test_repr_line_empty_name():
    kvoption = KVOption("", value="1")
    assert kvoption.repr_string() == '(1)'


def test_repr_line_empty_value():
    kvoption = KVOption("name")
    assert kvoption.repr_string() == 'name'


def test_repr_line_simple():
    kvoption = KVOption("name", "value")
    assert kvoption.repr_string() == 'name(value)'


def test_repr_pretty_empty():
    kvoption = KVOption("")
    assert kvoption.repr_pretty() == ''


def test_repr_pretty_empty_name():
    kvoption = KVOption("", value="1")
    assert kvoption.repr_pretty() == ' 1'


def test_repr_pretty_empty_value():
    kvoption = KVOption("name")
    assert kvoption.repr_pretty() == 'name'


def test_repr_pretty_simple():
    kvoption = KVOption("name", "value")
    assert kvoption.repr_pretty() == 'name value'


def test_parse_line_empty():
    data = ['']
    option = KVOption.read_from_lines(data, "LINE")
    assert option[0] == []


def test_parse_oldstyle_empty():
    data = ['']
    option = KVOption.read_from_lines(data, "OLDSTYLE")
    assert option[0].name == ""
    assert option[0].value == ""


def test_parse_pretty_empty():
    data = ['']
    option = KVOption.read_from_lines(data, "PRETTY")
    assert option[0].name == ""
    assert option[0].value == ""


def test_parse_line_simple():
    data = ['name()']
    option = KVOption.read_from_lines(data, "LINE")
    assert option[0][0].name == "name"
    assert option[0][0].value == ''


def test_parse_oldstyle_simple():
    data = ['name value']
    option = KVOption.read_from_lines(data, "OLDSTYLE")
    assert option[0].name == "name"
    assert option[0].value == "value"


def test_parse_pretty_simple():
    data = ['name']
    option = KVOption.read_from_lines(data, "PRETTY")
    assert option[0].name == "name"
    assert option[0].value == ""
