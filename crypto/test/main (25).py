import pytest

import helpers


@pytest.mark.parametrize("test_name,drop_input", [
    ("MetaInvalid__BindingsInvalid", True),
    ("MetaInvalid__BindingsMissing", True),
    ("MetaInvalid__BindingsValid", True),
    ("MetaDuplicate__BindingsValid", True),
    ("MetaValid__BindingsInvalid", True),
    ("MetaValid__BindingsDuplicate", True),
    ("MetaValid__BindingsMissing", True),
    ("MetaValid__BindingsValid", True),
    ("ValidInvalidMix", True),
    ("ValidInvalidMix", False)
])
def test_zero_rc(yt_stuff, test_name, drop_input):
    return helpers.execute_binary(yt_stuff, test_name, drop_input, must_be_execution_error=False)


@pytest.mark.parametrize("test_name", [
    ("MetaValid__BindingsValid"),
    ("MetaNoTariff__BindingsValid"),
])
def test_owner_login(yt_stuff, test_name):
    return helpers.execute_binary(
        yt_stuff,
        test_name,
        drop_input=True,
        owner_login="owner",
        must_be_execution_error=False,
    )


@pytest.mark.parametrize("drop_input", [
    True,
    False
])
@pytest.mark.parametrize("test_name", [
    "InputWithEmptyTimestampDir",
    "InputWithNonTimestampDir"
])
def test_nonzero_rc(yt_stuff, test_name, drop_input):
    return helpers.execute_binary(yt_stuff, test_name, drop_input, must_be_execution_error=True)
