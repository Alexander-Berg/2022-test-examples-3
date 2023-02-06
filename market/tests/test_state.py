# coding: utf-8

from market.idx.admin.system_offers.lib.state import State


def test_state():
    state = State()
    assert state.opened

    state.close_balancer()
    assert not state.opened
    assert state.ping() == 'closed'

    state.open_balancer()
    assert state.opened
    assert state.ping() == '0;ok'
