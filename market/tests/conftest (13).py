import pytest

import yatest

from suggests_storage import SuggestsStorage


@pytest.fixture(scope='session')
def gendir():
    return yatest.common.source_path('market/guru-models-dumper/suggests_storage/tests/tiny_generation')


@pytest.fixture(scope='session')
def suggests_storage(gendir):
    return SuggestsStorage(gendir)
