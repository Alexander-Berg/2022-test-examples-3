# -*- coding: utf-8 -*-
from travel.avia.ticket_daemon.ticket_daemon.api.result import Status, Result


QUERY_KEY = 'c213_c54_2017-06-21_None_economy_1_0_0_ru'


def test_make_result_key():
    assert (
        Result.make_result_key(QUERY_KEY, 'dohop') ==
        'c213_c54_2017-06-21_None_economy_1_0_0_ru/any/dohop'
    )


def test_make_status_key():
    assert (
        Status.make_result_key(QUERY_KEY, 'dohop') ==
        '/yandex/ticket-daemon/c213_c54_2017-06-21_None_economy_1_0_0_ru_any_dohop_status'
    )
