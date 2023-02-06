# coding: utf-8


import os
import json
import pytest
import yatest.common
from market.sre.tools.cachemanager.capacity.lib.validator import validate


@pytest.fixture(scope='module')
def fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path('market/sre/tools/cachemanager/capacity/tests/files')


def test_validate_with_mcrouters(fixtures_dir):
    with open(os.path.join(fixtures_dir, 'metrics.json')) as mp:
        metrics = json.load(mp)
    validate(metrics)


def test_validate_no_mcrouters(fixtures_dir):
    with open(os.path.join(fixtures_dir, 'mertics_no_mcrouter.json')) as mp:
        metrics = json.load(mp)
    validate(metrics)
