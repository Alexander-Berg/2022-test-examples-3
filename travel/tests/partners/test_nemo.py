# -*- coding: utf-8 -*-
import mock
import pytest

from travel.avia.ticket_daemon.tests.partners.helper import get_mocked_response, get_query
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import BadPartnerResponse, PartnerErrorTypes
from travel.avia.ticket_daemon.ticket_daemon.lib.tracker import QueryTracker
from travel.avia.ticket_daemon.ticket_daemon.partners import nemo


def test_nemo_query_error():
    nemo_importer = nemo.NemoImporter(mock.Mock(), mock.Mock(), 'http://search_url', mock.Mock(), mock.Mock())

    with mock.patch.object(nemo.requests, 'post', return_value=get_mocked_response('nemo_errors.xml')) as requests:
        q = get_query()
        q.station_iatas_from = ['MOW']
        q.station_iatas_to = ['SVX']
        with pytest.raises(BadPartnerResponse) as excinfo:
            next(QueryTracker.init_query(nemo_importer.query)(q))
        assert excinfo.value.errors == PartnerErrorTypes.ERROR
        requests.assert_called_once()
