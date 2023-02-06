# -*- coding: utf-8 -*-
from rtcc.dataprovider.searchconfigs.types.collectionlist import Collection


def test_repr_oldstyle_empty_options_params():
    collection = Collection("name", options=[], params=[])
    assert collection.repr_oldstyle() == '<Collection  id="{}">\n'.format("name") + \
                                         '</Collection>'


def test_repr_oldstyle_empty_params():
    collection = Collection("name", options=["first"], params=[])
    assert collection.repr_oldstyle() == '<Collection first id="{}">\n'.format("name") + \
                                         '</Collection>'


def test_repr_oldstyle_empty_options():
    collection = Collection("name", options=[], params=["first"])
    assert collection.repr_oldstyle() == '<Collection  id="{}">\n'.format("name") + \
                                         'first\n' + \
                                         '</Collection>'


def test_repr_oldstyle_empty():
    collection = Collection("", options=[], params=[])
    assert collection.repr_oldstyle() == '<Collection  id="{}">\n'.format("") + \
                                         '</Collection>'


def test_repr_oldstyle_simple():
    collection = Collection("name", options=["option"], params=["params"])
    assert collection.repr_oldstyle() == '<Collection option id="{}">\n'.format("name") + \
                                         'params\n' + \
                                         '</Collection>'


def test_repr_pretty_empty_options_params():
    collection = Collection("name", options=[], params=[])
    assert collection.repr_pretty() == 'name\n' + \
                                       '    options\n' + \
                                       '    params'


def test_repr_pretty_empty_params():
    collection = Collection("name", options=["first"], params=[])
    assert collection.repr_pretty() == 'name\n' + \
                                       '    options\n' + \
                                       '        first\n' + \
                                       '    params'


def test_repr_pretty_empty_options():
    collection = Collection("name", options=[], params=["first"])
    assert collection.repr_pretty() == 'name\n' + \
                                       '    options\n' + \
                                       '    params\n' + \
                                       '        first'


def test_repr_pretty_empty():
    collection = Collection("", options=[], params=[])
    assert collection.repr_pretty() == '\n' + \
                                       '    options\n' + \
                                       '    params'


def test_repr_pretty_simple():
    collection = Collection("name", options=["option"], params=["params"])
    assert collection.repr_pretty() == 'name\n' + \
                                       '    options\n' + \
                                       '        option\n' + \
                                       '    params\n' + \
                                       '        params'


def test_repr_pretty_simple_with_indent():
    # TODO: check indent property
    collection = Collection("name", options=["option"], params=["params"])
    assert collection.repr_pretty(4) == 'name\n' + \
                                        '        options\n' + \
                                        '            option\n' + \
                                        '        params\n' + \
                                        '            params'


def test_repr_line_empty_options_params():
    collection = Collection("name", options=[], params=[])
    assert collection.repr_string() == 'name(;)'


def test_repr_line_empty_params():
    collection = Collection("name", options=["first"], params=[])
    assert collection.repr_string() == 'name(first;)'


def test_repr_line_empty_options():
    collection = Collection("name", options=[], params=["first"])
    assert collection.repr_string() == 'name(;first)'


def test_repr_line_empty():
    collection = Collection("", options=[], params=[])
    assert collection.repr_string() == '(;)'


def test_repr_line_simple():
    collection = Collection("name", options=["option1", "option2"], params=["param1", "param2"])
    assert collection.repr_string() == 'name(option1,option2;param1,param2)'


def test_parse_line_empty():
    # TODO: params and options should be []
    data = ["(;)"]
    collection = Collection.read_from_lines(data, "LINE")
    assert collection[0][0].name == ""
    assert collection[0][0].params == [""]
    assert collection[0][0].options == [""]


def test_parse_line_empty_options():
    # TODO: params and options should be []
    data = ["name(;param1,param2)"]
    collection = Collection.read_from_lines(data, "LINE")
    assert collection[0][0].name == "name"
    assert collection[0][0].params == ["param1", "param2"]
    assert collection[0][0].options == [""]


def test_parse_pretty_empty_options():
    data = ['name\n',
            '    options\n',
            '    params\n',
            '        first', ]
    collection = Collection.read_from_lines(data, "PRETTY")
    assert collection[0].name == "name"
    assert collection[0].params == ["first"]
    assert collection[0].options == []


def test_parse_pretty_empty():
    data = [
        '\n',
        '    options\n',
        '    params\n',
    ]
    collection = Collection.read_from_lines(data, "PRETTY")
    assert collection[0].name == ""
    assert collection[0].params == []
    assert collection[0].options == []


def test_parse_oldstyle_simple():
    data = [
        '<Collection option id="name">\n',
        'params\n',
        '</Collection>'
    ]
    collection = Collection.read_from_lines(data, "OLDSTYLE")
    assert collection[0].name == "name"
    assert collection[0].params == ["params"]
    assert collection[0].options == ["option"]


def test_parse_oldstyle_empty():
    data = [
        '<Collection>\n',
        '</Collection>'
    ]
    collection = Collection.read_from_lines(data, "OLDSTYLE")
    assert collection[0].name == ""
    assert collection[0].params == []
    assert collection[0].options == []
