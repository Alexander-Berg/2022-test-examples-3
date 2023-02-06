# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import pytest

from common.apps.suburban_events.models import ThreadStationState, LVGD01_TR2PROC_query
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date.environment import now
from travel.rasp.tasks.monitoring.suburban_events import (
    has_recent_valid_queries, has_recent_station_states, CheckError, TSS_UPDATE_DELAY
)


@pytest.mark.mongouser
def test_has_recent_tss():
    # если объеков нет - должны ломаться
    with pytest.raises(CheckError):
        has_recent_station_states()

    for i in range(3):
        ThreadStationState.objects.create()

    # есть недавно созданные объекты - не падаем
    has_recent_station_states()

    # всё еще считаем недавно созданными - не падаем
    with replace_now(now() + TSS_UPDATE_DELAY + timedelta(seconds=-1)):
        has_recent_station_states()

    # объекты созданы давно - падаем
    with replace_now(now() + TSS_UPDATE_DELAY + timedelta(seconds=1)):
        with pytest.raises(CheckError):
            has_recent_station_states()


@pytest.mark.mongouser
def test_has_recent_valid_queries():
    def create_query(**kwargs):
        default_kwargs = {
            'queried_at': now(),
            'query_from': now(),
            'query_to': now(),
        }

        default_kwargs.update(kwargs)

        LVGD01_TR2PROC_query.objects.create(**default_kwargs)

    # нет запросов вообще
    with pytest.raises(CheckError):
        has_recent_valid_queries()

    # хороший, но слишком старый запрос
    create_query(queried_at=now() - timedelta(minutes=60), new_rows_count=10)
    with pytest.raises(CheckError):
        has_recent_valid_queries()

    # запрос без new_rows_count
    create_query()
    with pytest.raises(CheckError):
        has_recent_valid_queries()

    # запрос с new_rows_count == 0
    create_query(new_rows_count=0)
    with pytest.raises(CheckError):
        has_recent_valid_queries()

    # запрос с ошибкой
    create_query(exception='aaaa', new_rows_count=10)
    with pytest.raises(CheckError):
        has_recent_valid_queries()

    # хороший новый запрос
    create_query(new_rows_count=10)
