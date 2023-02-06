# -*- coding: utf-8 -*-

import pytest

from common.models.geo import Country
from common.tester.factories import create_settlement
from geosearch.models import NameSearchIndex
from travel.rasp.admin.scripts.build_namesearchindex import run


@pytest.mark.dbuser
def test_run():
    create_settlement(country=Country.RUSSIA_ID, title=u'AAA', title_ru=u'ААА')
    run(truncate_table=False)

    assert NameSearchIndex.objects.exists()
