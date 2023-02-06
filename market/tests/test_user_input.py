import pytest

from market.front.tools.service_updater.lib.user_input import user_input, confirm, NonInteractiveInput


def test_user_input(cli_read_fixture, capture_output_fixture):
    cli_read_fixture('test data')
    result = user_input('input')
    assert result == 'test data'
    assert capture_output_fixture() == ['input:', '\a']


def test_user_input_non_interactive(non_interactive_input_fixture):
    with pytest.raises(NonInteractiveInput):
        user_input('input')


def test_confirm(cli_read_fixture, capture_output_fixture):
    cli_read_fixture('no')
    assert confirm('no') is False
    assert capture_output_fixture() == ['\a\a', 'no [yes/no]:', '\a']

    cli_read_fixture('yes')
    assert confirm('yes') is True
    assert capture_output_fixture() == ['\a\a', 'yes [yes/no]:', '\a']


def test_confirm_non_interactive(cli_read_fixture, capture_output_fixture, non_interactive_input_fixture):
    assert confirm('default') is False
    assert capture_output_fixture() == ['']

    assert confirm('false', False) is False
    assert capture_output_fixture() == ['']

    assert confirm('true', True) is True
    assert capture_output_fixture() == ['']
