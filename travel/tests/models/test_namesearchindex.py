# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from contextlib2 import ExitStack

from common.models.geo import Settlement
from common.tester.factories import create_settlement
from geosearch.models import NameSearchIndex


@pytest.mark.dbuser
@pytest.mark.parametrize('precache', (False, True))
@pytest.mark.parametrize('text', ('нижний новгород', 'нижний', 'новгород',))
def test_find_by_words(precache, text):
    nn = create_settlement(title='Нижний Новгород')
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    with (NameSearchIndex.using_precache() if precache else ExitStack()):
        assert nn in NameSearchIndex.find('words', Settlement, text).objects
