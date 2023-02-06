# coding: utf-8

import os
from contextlib import contextmanager

import pytest
from django.db.models import Q

from common.models.schedule import RThreadType, RThread
from common.models.transport import TransportType
from precalc import PrecalcState, SQLiteSnapshot, precalc_search
from precalc.parts.search.sqlite_utils import recreate_sqlite_search_table
from tester.factories import create_thread, create_station, create_settlement


@contextmanager
def calc_search_and_return_connection(partial):
    snapshot = SQLiteSnapshot('schedule', new=True, hack=True)
    prepare_db(snapshot)
    precalc_state = init_precalc_state(partial)

    precalc_search(snapshot.connect, precalc_state, number_of_workers=0)
    snapshot.commit()

    conn = snapshot.connect()
    try:
        yield conn
    finally:
        conn.close()


def prepare_db(snapshot):
    db_path = os.path.dirname(snapshot.db_path)
    if not os.path.exists(db_path):
        os.makedirs(db_path)

    conn = snapshot.connect()
    recreate_sqlite_search_table(conn)
    conn.commit()
    conn.close()


def init_precalc_state(partial):
    precalc_state = PrecalcState(partial=partial)

    threads = RThread.objects.filter(
        Q(type=None) | Q(type__in=[
            RThreadType.BASIC_ID,
            RThreadType.CHANGE_ID,
            RThreadType.THROUGH_TRAIN_ID
        ]),
        route__hidden=False,
    )

    if partial:
        in_precalc = set()
        in_base = set(threads.values_list('id', flat=True))

        precalc_state.threads_deleted = in_precalc - in_base
        precalc_state.threads_added = in_base - in_precalc
        precalc_state.threads_changed = set(
            threads.filter(changed=True).values_list('id', flat=True)
        )

    return precalc_state


@pytest.mark.dbuser
@pytest.mark.parametrize('partial', (True, False))
def test_on_thread(precalc_db_mock, partial):
    create_thread(changed=True, __={'calculate_noderoute': True})

    with calc_search_and_return_connection(partial) as connection:
        result = list(connection.execute('SELECT COUNT(*) FROM search'))
        assert result[0][0] > 0


@pytest.mark.dbuser
@pytest.mark.parametrize('partial', (True, False))
def test_remove_through_train(precalc_db_mock, partial):
    station_a = create_station()
    station_b = create_station()
    create_train_thread = create_thread.mutate(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=(
            (None, 0, station_a),
            (9, None, station_b)
        ),
        changed=True,
        __={'calculate_noderoute': True}
    )

    main_thread = create_train_thread(type=RThreadType.BASIC_ID)
    create_train_thread(type=RThreadType.THROUGH_TRAIN_ID)

    with calc_search_and_return_connection(partial) as connection:
        result = list(connection.execute('SELECT `thread_id` FROM search'))
        thread_ids = {row[0] for row in result}
        assert thread_ids == {main_thread.id}


@pytest.mark.dbuser
@pytest.mark.parametrize('partial', (True, False))
def test_through_train_in_result(precalc_db_mock, partial):
    create_thread(t_type=TransportType.TRAIN_ID, type=RThreadType.THROUGH_TRAIN_ID,
                  changed=True, __={'calculate_noderoute': True})

    with calc_search_and_return_connection(partial) as connection:
        result = list(connection.execute('SELECT COUNT(*) FROM search'))
        assert result[0][0] > 0


@pytest.mark.dbuser
@pytest.mark.parametrize('partial', (True, False))
def test_remove_duplicates(precalc_db_mock, partial):
    settlement_a = create_settlement()
    settlement_b = create_settlement()
    station_a = create_station(settlement=settlement_a)
    station_a2 = create_station(settlement=settlement_a)
    station_b = create_station(settlement=settlement_b)
    create_thread(
        schedule_v1=(
            (None, 0, station_a),
            (4, 5, station_a2),
            (9, None, station_b)
        ),
        changed=True,
        __={'calculate_noderoute': True}
    )

    with calc_search_and_return_connection(partial) as connection:
        result = list(connection.execute('SELECT `key` FROM search'))
        keys = {row[0] for row in result}

        expected_keys = {
            u'{}-{}'.format(f.point_key, t.point_key)
            for f, t in (
                (station_a2, settlement_b),
                (settlement_a, settlement_b),
                (settlement_a, station_b),
                (settlement_a, station_a2),
                (station_a, settlement_a),
                (station_a, settlement_b),

                (station_a, station_a2),
                (station_a, station_b),
                (station_a2, station_b),
            )
        }
        assert keys == expected_keys
