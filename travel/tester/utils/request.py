# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.rasp.library.python.common23.date.environment import delete_time_context


@pytest.fixture()
def clean_context(request):
    def fin():
        delete_time_context()
    request.addfinalizer(fin)
