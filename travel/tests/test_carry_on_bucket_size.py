# -*- coding: utf-8 -*-
from travel.avia.ticket_daemon_api.jsonrpc.carry_on_size_bucket import get_carry_on_bucket_size


def test_carry_on_bucket_size():
    assert get_carry_on_bucket_size(0) == 'unknown'
    assert get_carry_on_bucket_size(49) == 'small'
    assert get_carry_on_bucket_size(55) == 'regular'
