import pytest

from travel.rasp.smoke_tests.smoke_tests import conf


@pytest.mark.parametrize('check', conf.tests_data)
def test(check):
    check()
