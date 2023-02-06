from pytest import fixture

from travel.hotels.tools.affiliate_data_builder.lib.overridable_config import KeyExpander, OverridableConfig


@fixture
def key_expander() -> KeyExpander:
    allowed_values = [
        {'key_1': 'value_1_1', 'key_2': 'value_2_1'},
        {'key_1': 'value_1_1', 'key_2': 'value_2_2'},
        {'key_1': 'value_1_2', 'key_2': 'value_2_1'},
        {'key_1': 'value_1_2', 'key_2': 'value_2_2'},
    ]
    return KeyExpander(('key_1', 'key_2'), allowed_values)


def sort_by_key(items):
    return sorted(items, key=lambda x: OverridableConfig.get_item_key(('key_1', 'key_2'), x))


def test_no_substitution(key_expander: KeyExpander):

    value = {'key_1': 'value_1', 'key_2': 'value_2'}
    expected = [value]

    assert expected == key_expander.get_expanded(value)


def test_substitution_key_1(key_expander: KeyExpander):

    value = {'key_1': '*', 'key_2': 'value_2'}
    expected = [
        {'key_1': 'value_1_1', 'key_2': 'value_2'},
        {'key_1': 'value_1_2', 'key_2': 'value_2'},
    ]

    assert sort_by_key(expected) == sort_by_key(key_expander.get_expanded(value))


def test_substitution_key_2(key_expander: KeyExpander):

    value = {'key_1': 'value_1', 'key_2': '*'}
    expected = [
        {'key_1': 'value_1', 'key_2': 'value_2_1'},
        {'key_1': 'value_1', 'key_2': 'value_2_2'},
    ]

    assert sort_by_key(expected) == sort_by_key(key_expander.get_expanded(value))


def test_substitution_all_keys(key_expander: KeyExpander):

    value = {'key_1': '*', 'key_2': '*'}
    expected = [
        {'key_1': 'value_1_1', 'key_2': 'value_2_1'},
        {'key_1': 'value_1_1', 'key_2': 'value_2_2'},
        {'key_1': 'value_1_2', 'key_2': 'value_2_1'},
        {'key_1': 'value_1_2', 'key_2': 'value_2_2'},
    ]

    assert sort_by_key(expected) == sort_by_key(key_expander.get_expanded(value))
