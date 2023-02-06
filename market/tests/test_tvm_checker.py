# coding: utf-8


import pytest
import yatest.common


from pathlib import Path
from market.sre.tools.tvm_checker.lib.checker import get_from_file


@pytest.fixture(scope='module')
def fixture_secrets_dir():
    return yatest.common.source_path('market/sre/tools/tvm_checker/tests/data')


def test_get_from_file(fixture_secrets_dir):

    secret = get_from_file(Path(fixture_secrets_dir, 'test_secret_file'))
    assert secret == 'SomeVerySecretInformation'
