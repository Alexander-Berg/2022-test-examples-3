from datetime import date, timedelta

from pytest import fixture

from travel.hotels.tools.affiliate_data_builder.lib.overridable_config import OverridableConfig


CURRENT_DATE = date(2021, 11, 15)


@fixture
def overridable_config() -> OverridableConfig:
    allowed_values = [
        {'key_1': 'value_1_1', 'key_2': 'value_2_1'},
        {'key_1': 'value_1_1', 'key_2': 'value_2_2'},
        {'key_1': 'value_1_2', 'key_2': 'value_2_1'},
        {'key_1': 'value_1_2', 'key_2': 'value_2_2'},
    ]
    return OverridableConfig(
        key_fields=('key_1', 'key_2',),
        allowed_values=allowed_values,
    )


def test_no_overrides(overridable_config: OverridableConfig):
    values = [
        {'key_1': 'key_1_1', 'key_2': 'key_2_1', 'value': 'value_1'},
        {'key_1': 'key_1_2', 'key_2': 'key_2_2', 'value': 'value_2'},
    ]
    expected = values
    assert expected == overridable_config.apply(values, CURRENT_DATE)


def test_override_unconditional(overridable_config: OverridableConfig):
    values = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_1'},
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2'},
    ]
    expected = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2'},
    ]
    assert expected == overridable_config.apply(values, CURRENT_DATE)


def test_override_by_start_at(overridable_config: OverridableConfig):
    values = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_1'},
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2', 'StartAt': str(CURRENT_DATE + timedelta(days=1))},
    ]
    expected = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_1'},
    ]
    assert expected == overridable_config.apply(values, CURRENT_DATE)

    values = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_1'},
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2', 'StartAt': str(CURRENT_DATE)},
    ]
    expected = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2', 'StartAt': str(CURRENT_DATE)},
    ]
    assert expected == overridable_config.apply(values, CURRENT_DATE)


def test_override_by_end_at(overridable_config: OverridableConfig):
    values = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_1'},
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2', 'EndAt': str(CURRENT_DATE - timedelta(days=1))},
    ]
    expected = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_1'},
    ]
    assert expected == overridable_config.apply(values, CURRENT_DATE)

    values = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_1'},
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2', 'EndAt': str(CURRENT_DATE)},
    ]
    expected = [
        {'key_1': 'key_1', 'key_2': 'key_2', 'value': 'value_2', 'EndAt': str(CURRENT_DATE)},
    ]
    assert expected == overridable_config.apply(values, CURRENT_DATE)
