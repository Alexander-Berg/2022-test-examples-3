# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import pytest

from common.tester.factories import create_settlement
from travel.rasp.morda_backend.morda_backend.tariffs.daemon.serialization import DaemonQuerySchema, TariffsPollQuerySchema


@pytest.mark.dbuser
def test_make_query():
    daemon_query = DaemonQuerySchema().make_query({})
    assert daemon_query.client_settlement is None

    client_settlement = create_settlement()
    daemon_query = DaemonQuerySchema().make_query({'client_settlement': client_settlement.id})
    assert daemon_query.client_settlement == client_settlement


@pytest.mark.parametrize('qid,skip_partners', [
    ('111', ['Org1', 'Org2']),
    ('111', []),
    ('111', None),
])
def test_tariffs_poll_query_schema(qid, skip_partners):
    request = {'qid': qid}
    if skip_partners is not None:
        request['skip_partners'] = json.dumps(skip_partners)
    query, errors = TariffsPollQuerySchema().load(request)
    assert not errors
    assert query['qid'] == qid
    assert query['skip_partners'] == skip_partners
