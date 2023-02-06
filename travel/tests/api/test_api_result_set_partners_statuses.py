# coding: utf-8
import pytest
from mock import patch

from travel.avia.library.python.tester.factories import create_partner

from travel.avia.ticket_daemon.ticket_daemon.api.result import set_partners_statuses
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches


@pytest.mark.dbuser
def test_set_partners_statuses():
    reset_all_caches()
    TEST_PARTNER_CODES = (u'one', u'two', u'trois')
    partners = map(lambda code: create_partner(code=code), TEST_PARTNER_CODES)
    q = create_query()
    expected_set_keys = {
        '/yandex/ticket-daemon/{}_any_{}_status'.format(q.qkey, partner_code)
        for partner_code in TEST_PARTNER_CODES
    }
    cache_box = {}

    def set_many(mapping, *args, **kwargs):
        cache_box.update(mapping)

    with patch('travel.avia.ticket_daemon.ticket_daemon.api.result.cache_backends.shared_cache.set_many', side_effect=set_many):
        set_partners_statuses(q, partners, 'ok', 1024)

    assert set(cache_box.keys()) == expected_set_keys
