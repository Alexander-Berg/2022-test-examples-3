# -*- coding: utf-8 -*-
import ujson as json

from mock import patch

from travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation import (
    get_cont_details, create_next_cont, ContDetails
)


def test_create_next_cont_base():
    cd = ContDetails('qid', 1, {'partner1'}, {'partner1': 0}, 1)
    with patch('travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation.default_cache') as default_cache:
        next_cd = create_next_cont(cd, ['partner2'], {'partner1': 0, 'partner2': 0}, ['partner3'], 1)
        assert next_cd.cont == 2
        assert next_cd.variants_count == 2
        assert next_cd.skip_partner_codes == {'partner1', 'partner2'}
        assert next_cd.revisions == {'partner1': 0, 'partner2': 0}
        default_cache.set.assert_called()


def test_create_next_cont_query_finished():
    cd = ContDetails('qid', 1, {'partner1'}, {'partner1': 0}, 0)
    with patch('travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation.default_cache') as default_cache:
        next_cd = create_next_cont(cd, ['partner2'], {'partner2': 0}, [], 1)
        default_cache.set.assert_not_called()
        assert next_cd.cont is None


def test_create_next_cont_no_updates():
    revisions = {'partner1': 0}
    cd = ContDetails('qid', 1, {'partner1'}, revisions , 0)
    with patch('travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation.default_cache') as default_cache:
        next_cd = create_next_cont(cd, [], revisions, ['partner_in_progress'], 0)
        default_cache.set.assert_not_called()
        assert next_cd.cont == 1


def test_create_next_cont_same_partner_updates():
    before_update_revisions = {'partner1': 0}
    after_update_revisions = {'partner1': 1}
    cd = ContDetails('qid', 1, set(), before_update_revisions , 1)
    with patch('travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation.default_cache') as default_cache:
        next_cd = create_next_cont(cd, [], after_update_revisions, ['partner1', 'partner_in_progress'], 1)
        default_cache.set.assert_called()
        assert next_cd.cont == 2
        assert next_cd.revisions == after_update_revisions


def test_get_cont():
    cont = {'qid': 'qid', 'cont': 1, 'skip_partner_codes': {'partner1'}, 'revisions': {'partner1': 0}, 'variants_count': 1}
    with patch('travel.avia.ticket_daemon_api.jsonrpc.lib.result.continuation.default_cache.get', return_value=json.dumps(cont)) as default_cache:
        cd = get_cont_details('qid', 1)
        default_cache.assert_called()
        assert isinstance(cd, ContDetails)
        assert cd.cont == 1
        assert cd.qid == 'qid'
        assert cd.revisions == {'partner1': 0}
        assert cd.skip_partner_codes == {'partner1'}
        assert cd.variants_count == 1


def test_get_initial_cont():
    cd = get_cont_details('qid', 0)
    assert isinstance(cd, ContDetails)
    assert cd.cont == 0
    assert cd.qid == 'qid'
    assert cd.revisions == {}
    assert cd.skip_partner_codes == set()
    assert cd.variants_count == 0
