import pytest

from cli_test_utils import check_cli_line


def test_check_cli_line():
    with pytest.raises(AssertionError):
        check_cli_line("qg local 1 --konfiguration konfiguration.json")
