# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.morda_backend.morda_backend.search.pm_variants.service import get_full_path


@pytest.mark.parametrize('path, result', [
    ([], None),
    ([101, 102], [101, 102]),
    ([1, 101, 2, 3, 102, 4], [101, 2, 3, 102]),
    ([1, 101, 2, 101, 3, 102, 4], [101, 3, 102]),
    ([1, 101, 2, 102, 3, 101, 4], [101, 2, 102]),
    ([1, 102, 2, 101, 3], None)
])
def test_get_full_path(path, result):
    assert get_full_path(path, 101, 102) == result
