from base_parsers import JSONSerpParser


def test_simple_path():
    parser = JSONSerpParser()
    parser.parse("""{"foo": {"bar": "baz"}}""")
    assert "baz" == parser._get_element_by_path("foo", "bar")


def test_path_with_list_index():
    parser = JSONSerpParser()
    parser.parse("""{"foo": ["a", "b", "c"]}""")
    assert "c" == parser._get_element_by_path("foo", 2)


def test_path_with_missing_key():
    parser = JSONSerpParser()
    parser.parse("""{"foo": {"bar": "baz"}}""")
    assert None is parser._get_element_by_path("foo", "bar", "missing_key")


def test_path_with_list_index_out_of_bounds():
    parser = JSONSerpParser()
    parser.parse("""{"foo": ["a", "b", "c"]}""")
    assert None is parser._get_element_by_path("foo", 3)


def test_default_value():
    parser = JSONSerpParser()
    parser.parse("""{"foo": ["a", "b", "c"]}""")
    assert "meow" == parser._get_element_by_path("foo", 5, default="meow")
    parser.parse("""{"a": {"b": {"c": "d"}}}""")
    assert "meow" == parser._get_element_by_path("a", "b", "missing_key", "another_missing_key", default="meow")


def test_longer_path():
    parser = JSONSerpParser()
    parser.parse("""{"some_key": {"subtree": {"some_list": [{"another_key": 2}]}}}""")
    assert 2 == parser._get_element_by_path("some_key", "subtree", "some_list", 0, "another_key")


def test_json_argument():
    parser = JSONSerpParser()
    parser.parse("""{"some_key": {"subtree": {"some_list": [{"another_key": 2}]}}}""")
    assert 2 == parser._get_element_by_path("some_list", 0, "another_key", json={"some_list": [{"another_key": 2}]})


def test_empty_json_argument():
    parser = JSONSerpParser()
    parser.parse("""{"a": "b"}""")
    assert None is parser._get_element_by_path("a", json=None)
