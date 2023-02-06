# -*- coding: utf-8 -*-
import sys
from functools import partial
from json_value import TJsonValue, JSON_UNDEFINED, \
    JSON_NULL, JSON_BOOLEAN, JSON_INTEGER, JSON_DOUBLE, \
    JSON_STRING, JSON_MAP, JSON_ARRAY, JSON_UINTEGER, \
    make_json_value, read_json_value_from_string


def test_constructors():
    a = TJsonValue()  # default constructor creates UNDEFINED json value
    b = TJsonValue(True)
    c = TJsonValue(123)
    d = TJsonValue(3.14159265358979323846)
    e = TJsonValue("123")

    assert a.get_type() == JSON_UNDEFINED
    assert b.get_type() == JSON_BOOLEAN
    assert c.get_type() == JSON_INTEGER
    assert d.get_type() == JSON_DOUBLE
    assert e.get_type() == JSON_STRING


def test_types():
    a = TJsonValue()

    assert a.get_type() == JSON_UNDEFINED

    a.as_boolean = False
    assert a.get_type() == JSON_BOOLEAN

    a.as_string = "123"
    assert a.get_type() == JSON_STRING

    a.as_integer = -123
    assert a.get_type() == JSON_INTEGER

    a.as_uinteger = 123
    assert a.get_type() == JSON_UINTEGER

    a.as_list = [1, 2, 3]
    assert a.get_type() == JSON_ARRAY

    a.as_dict = {"a": 1}
    assert a.get_type() == JSON_MAP

    a.as_double = 3.14159265358979323846
    assert a.get_type() == JSON_DOUBLE

    a.as_object = None
    assert a.get_type() == JSON_NULL


def test_identity_and_equality():
    b = TJsonValue(True)
    e = TJsonValue(b)  # e - is deep copy of b
    f = b  # f - f and b reference same json object

    assert f == b
    assert id(f) == id(b)
    assert e == b
    assert id(e) != id(b)


def test_conversion():
    b = TJsonValue()

    assert not b.is_defined
    assert b.get_type() == JSON_UNDEFINED

    b.as_boolean = False

    assert b.get_type() == JSON_BOOLEAN

    b.set_type(JSON_STRING)
    assert b.get_type() == JSON_STRING

    b.as_integer = 125
    assert b.is_integer

    b.as_list = [1.1, 2, True, "3", None]

    assert b.get_type() == JSON_ARRAY
    assert b[0].get_type() == JSON_DOUBLE
    assert b[1].get_type() == JSON_INTEGER
    assert b[2].get_type() == JSON_BOOLEAN
    assert b[3].get_type() == JSON_STRING
    assert b[4].get_type() == JSON_NULL

    assert b.as_list == [1.1, 2, True, "3", None]

    a = make_json_value({"a": 1})
    try:
        b = a.as_list
        assert(False)
    except TypeError:
        pass  # TypeError as expected


def test_contains():
    b = TJsonValue()
    c = TJsonValue()
    b.as_object = {"qwe": [1, True, 3.14, "3.14"]}

    assert b.as_object == {'qwe': [1, 1, 3.14, '3.14']}
    assert 3.14 in b["qwe"]
    assert 3.1415 not in b["qwe"]
    assert "3.14" in b["qwe"]
    assert True in b["qwe"]

    b.as_list = [0, 1]

    assert True in b
    assert None not in b

    b.as_list = [{"qwe": 1}, {"xyz": 2}]
    c.as_dict = {"xyz": 2}

    assert c in b

    c.as_dict = {"qwe": 1}

    assert c in b

    c.as_dict = {"qwe": True}

    assert c not in b

    c.as_dict = {"qwe": False}

    assert c not in b
    assert not c.is_array
    assert b.is_array

    # strings can be searched among dict keys
    b.as_dict = {"qwe": 1, "xyz": 2}
    assert "xyz" in b

    c.as_string = "qwe"
    assert c in b

    c.as_integer = 123
    try:
        c in b  # c should be string
        assert(False)
    except TypeError:
        pass  # TypeError as expected


def test_dict_iface():
    a = TJsonValue()
    a.as_dict = {
        "a": 1,
        "b": 2
    }
    keys = a.keys()
    keys_j = a.keys_json_value()

    assert keys == ["a", "b"]
    assert keys_j == make_json_value(["a", "b"])

    values = a.values()
    values_j = a.values_json_value()

    assert values == [1, 2]
    assert values_j == make_json_value([1, 2])


def test_list_iface():
    a = TJsonValue()
    a.as_list = ["a", "b", 1, 2, "b"]
    a.append("c")

    assert a.as_list == ["a", "b", 1, 2, "b", "c"]

    a.append_json_value(make_json_value({"a": 1}))

    assert a == make_json_value(["a", "b", 1, 2, "b", "c", {"a": 1}])
    assert a.index("c") == 5
    assert a.index("b") == 1
    assert a.index_json_value(make_json_value("c")) == 5
    assert a.index_json_value(make_json_value("b")) == 1
    assert a.count("b") == 2
    assert a.count("e") == 0
    assert a.count({"a": 1}) == 1
    assert a.count({"a": 2}) == 0
    assert a.count_json_value(make_json_value("b")) == 2
    assert a.count_json_value(make_json_value("e")) == 0
    assert a.count_json_value(make_json_value({"a": 1})) == 1
    assert a.count_json_value(make_json_value({"a": 2})) == 0

    a.as_list = [1, 2]
    a += [3]

    assert a == make_json_value([1, 2, 3])

    a += make_json_value([4, 5])

    assert a == make_json_value([1, 2, 3, 4, 5])


def test_delete():
    a = TJsonValue()
    d = TJsonValue()
    a.as_list = [1, 2, 3]
    i = a[1]
    i.as_string = "123"

    assert a.as_object == [1, "123", 3]

    d.as_dict = {"q": 1}
    e = d["q"]
    e.as_object = a.as_object

    assert d.as_object == {"q": [1, "123", 3]}

    del e[1]

    assert a.as_object == [1, "123", 3]
    assert d.as_object == {"q": [1, 3]}


def test_get_set_value():
    a = make_json_value({"a": 1})
    b = make_json_value([1, 2])

    a.set_value_json_value(b)

    assert a.as_list == [1, 2]

    a = make_json_value({
        "a": {
            "b": {
                "c": [1, 2, 3]
            }
        }
    })

    b = a.get_value_by_path_json_value("a.b.c")

    assert str(b) == "[1,2,3]"

    b[1] = 42

    assert a.as_dict == {"a": {"b": {"c": [1, 42, 3]}}}

    try:
        b = a.get_value_by_path_json_value("a.b.d")
        assert(False)
    except ValueError:
        pass  # ValueError as expected

    a.set_value_by_path_json_value("a.b.c", make_json_value("123"))

    assert str(b) == "\"123\""

    try:
        b = a.get_value_by_path_json_value("a.b.c", delim="..")
        assert(False)
    except ValueError:
        pass  # ValueError as expected

    a.set_value_by_path("a.b.c", 42)

    assert b.as_integer == 42

    a["a"].set_value([1, 2, 3])

    assert str(a) == "{\"a\":[1,2,3]}"


def test_to_from_string():
    a = make_json_value([{"a": 1}, {"b": 2}, {"c": 3}])

    assert str(a) == "[{\"a\":1},{\"b\":2},{\"c\":3}]"

    b = read_json_value_from_string(str(a))

    assert b == a

    try:
        a = read_json_value_from_string("[{\"a\":1},{\"b\":2} {\"c\":3}]")
        assert(False)
    except ValueError:
        pass  # ValueError as expected


if sys.version_info[0] == 3:
    def long(a):
        return a


def test_scan():
    a = make_json_value({
        "a": {
            "e": {
                "f": [1, 2, 3]
            }
        }
    })

    b = make_json_value({
        "b": {
            "1": "1",
            "3": ["1", 2, 3]
        },
    })

    c = make_json_value({
        "c": [
            {
                "d": ["a", "b", [1, 2, 3]]
            },
            [1, 2, 3]
        ]
    })

    def scanner(result, path, value):
        result.append((path, value.as_object))
        return True

    a_result = []
    a.scan(partial(scanner, a_result))

    b_result = []
    b.scan(partial(scanner, b_result))

    c_result = []
    c.scan(partial(scanner, c_result))

    assert a_result == [
        ('', {'a': {'e': {'f': [long(1), long(2), long(3)]}}}),
        ('a', {'e': {'f': [long(1), long(2), long(3)]}}),
        ('a.e', {'f': [long(1), long(2), long(3)]}),
        ('a.e.f', [long(1), long(2), long(3)]),
        ('a.e.f[0]', long(1)),
        ('a.e.f[1]', long(2)),
        ('a.e.f[2]', long(3))
    ]

    assert b_result == [
        ('', {'b': {'1': '1', '3': ['1', long(2), long(3)]}}),
        ('b', {'1': '1', '3': ['1', long(2), long(3)]}),
        ('b.1', '1'),
        ('b.3', ['1', long(2), long(3)]),
        ('b.3[0]', '1'),
        ('b.3[1]', long(2)),
        ('b.3[2]', long(3))
    ]

    assert c_result == [
        ('', {'c': [{'d': ['a', 'b', [long(1), long(2), long(3)]]},
         [long(1), long(2), long(3)]]}),
        ('c', [{'d': ['a', 'b', [long(1), long(2), long(3)]]},
         [long(1), long(2), long(3)]]),
        ('c[0]',
          {'d': ['a', 'b', [long(1), long(2), long(3)]]}),
        ('c[0].d',
          ['a', 'b', [long(1), long(2), long(3)]]),
        ('c[0].d[0]', 'a'),
        ('c[0].d[1]', 'b'),
        ('c[0].d[2]', [long(1), long(2), long(3)]),
        ('c[0].d[2][0]', long(1)),
        ('c[0].d[2][1]', long(2)),
        ('c[0].d[2][2]', long(3)),
        ('c[1]', [long(1), long(2), long(3)]),
        ('c[1][0]', long(1)),
        ('c[1][1]', long(2)),
        ('c[1][2]', long(3))]

    def scanner_bad(path, value):
        return 123

    try:
        a.scan(scanner_bad)
        assert(False)
    except TypeError:
        pass  # TypeError as expected
