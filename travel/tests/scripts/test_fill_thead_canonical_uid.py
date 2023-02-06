# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models.schedule import RThread
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_company
from travel.rasp.admin.scripts.fill_threads_canonical_uid import fill_threads_canonical_uid


@pytest.mark.dbuser
def test_fill_threads_canonical_uid():
    black_company = create_company(id=59181)
    company = create_company(id=111)
    route = {'route_uid': 'sub_route_uid'}

    create_thread(id=666, uid='_uid')
    create_thread(id=667, canonical_uid='_canonical_uid')
    create_thread(id=668, t_type=TransportType.SUBURBAN_ID, company=company, route=route)
    create_thread(id=669, uid='sub_uid_1', t_type=TransportType.SUBURBAN_ID, company=black_company, route=route)
    create_thread(id=670, t_type=TransportType.SUBURBAN_ID, route=route)
    create_thread(id=671, canonical_uid='', uid='_u')
    fill_threads_canonical_uid()

    assert RThread.objects.get(id=666).canonical_uid == 'T__uid'
    assert RThread.objects.get(id=667).canonical_uid == '_canonical_uid'
    assert RThread.objects.get(id=668).canonical_uid == 'R_sub_route_uid'
    assert RThread.objects.get(id=669).canonical_uid == 'T_sub_uid_1'
    assert RThread.objects.get(id=670).canonical_uid == 'R_sub_route_uid'
    assert RThread.objects.get(id=671).canonical_uid == 'T__u'
