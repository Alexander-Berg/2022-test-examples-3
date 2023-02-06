# -*- coding: utf-8 -*-
from collections import defaultdict

import pytest

from travel.avia.library.python.common.models.schedule import Company

from travel.avia.avia_api.avia.lib.variants_reference import FictionalCompany


@pytest.mark.dbuser
def test_magic_company_id():
    iatas = set(filter(None, Company.objects.values_list('iata', flat=True)))

    # Verbose
    iatas_of_ids = defaultdict(set)
    for iata in iatas:
        id_ = FictionalCompany.magic_company_id(iata)
        iatas_of_ids[id_].add(iata)

    for id_, id_iatas in iatas_of_ids.items():
        assert len(id_iatas) == 1, \
            'Iatas produce same id: %s %r' % (id_, id_iatas)

    # In short
    assert len(iatas) == len(set(map(FictionalCompany.magic_company_id, iatas)))
