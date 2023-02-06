# -*- coding: utf-8 -*-

import mock
import os.path
import pytest
import shutil

from requests import Response

from travel.avia.library.python.avia_data.models import AmadeusMerchant
from travel.avia.library.python.common.models.partner import Partner, DohopVendor
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import BadPartnerResponse
from travel.avia.ticket_daemon.ticket_daemon.lib.tracker import QueryTracker, ExchangeMessage


class ExchangesAssertion:
    LIMIT = 10

    def __init__(self, tracker, dirpath):
        self.tracker = tracker
        self.dirpath = dirpath

    def __enter__(self):
        for i in range(self.LIMIT):
            exchange = self.tracker.add_exchange('exchange%d' % i)

            for j in range(self.LIMIT):
                exchange.events.append(
                    ExchangeMessage('code%d%d' % (i, j), str(j), compress=False)
                )

    def __exit__(self, exc_type, exc_val, exc_tb):
        for i in range(self.LIMIT):
            exchange_key = '%iexchange%d' % (i + 1, i)

            for j in range(self.LIMIT):
                event_key = 'code%d%d' % (i, j)
                title = '%s_%s' % (exchange_key, event_key)
                file_path = os.path.join(self.dirpath, title) + '.txt'

                assert os.path.exists(file_path)

                with open(file_path) as f:
                    assert str(j) == f.read()


def test_check_response_should_raise_BadPartnerResponse_if_got_not_ok_response():
    q_mock = mock.Mock()
    q_mock.trackers = {}

    query_tracker = QueryTracker('code', q_mock)

    response = Response()
    response.status_code = 500

    with pytest.raises(BadPartnerResponse):
        query_tracker.check_response(response)


@pytest.mark.parametrize('partner, code', [
    (Partner(code='partner'), 'partner'),
    (AmadeusMerchant(code='amadeus'), 'amadeus'),
    (AmadeusMerchant(code='amadeus_12'), 'amadeus'),
    (DohopVendor(code='dohop'), 'dohop'),
    (DohopVendor(code='dohop_ab'), 'dohop'),
])
@mock.patch.object(QueryTracker, 'done')
def test_defining_right_partner_code_in_init(_, partner, code):
    q_mock = mock.MagicMock()
    q_mock.importer.partners = [partner]

    class QueryContext(object):
        tracker = None

    def fake_query(tracker, _):
        QueryContext.tracker = tracker

    fake_query = QueryTracker.init_query(fake_query)
    list(fake_query(q_mock))

    tracker = QueryContext.tracker
    assert isinstance(tracker, QueryTracker)
    assert tracker.partner_code == code


def test_tracker_done():
    q_mock = mock.Mock()
    q_mock.trackers = {}

    query_tracker = QueryTracker('code', q_mock)
    query_tracker.parsing_exception = 'some_exception'
    dirpath = os.path.join(
        QueryTracker.TRACKS_PATH,
        'bad/%s/%s' % (
            query_tracker.partner_code,
            query_tracker.parsing_exception,
        )
    )

    with ExchangesAssertion(query_tracker, dirpath):
        query_tracker.done()

        assert os.path.exists(dirpath)


def teardown():
    dirpath = os.path.join(
        QueryTracker.TRACKS_PATH,
        'bad/code/'
    )

    shutil.rmtree(dirpath, ignore_errors=True)
