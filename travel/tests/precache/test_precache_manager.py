# -*- coding: utf-8 -*-

import pytest
from django.test.utils import CaptureQueriesContext
from django.db import connection
from django.db.models import Q

from travel.avia.library.python.common.models.geo import StationMajority, Station, Settlement
from travel.avia.library.python.common.models.schedule import Company
from travel.avia.library.python.tester.factories import create_station, create_company, create_settlement


@pytest.mark.dbuser
def test_using_precache():
    assert not StationMajority.objects.precached

    with StationMajority.objects.using_precache():
        assert StationMajority.objects.precached
        StationMajority.objects.get(pk=1)

    assert not StationMajority.objects.precached


@pytest.mark.dbuser
def test_pk_precache():
    bulk_pks = [StationMajority.MAIN_IN_CITY_ID, StationMajority.IN_TABLO, -20]

    StationMajority.objects.get(pk=1)  # Иначе при первом запросе будет дополнительный SELECT VERSION()

    with CaptureQueriesContext(connection) as captured_queries:
        StationMajority.objects.get(pk=1)
        assert len(captured_queries) == 1

    with StationMajority.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            StationMajority.objects.get(pk=1)
            StationMajority.objects.get(id=1)
            StationMajority.objects.filter(pk=1).get()
            StationMajority.objects.filter(id=1).get()
            StationMajority.objects.in_bulk_cached()
            list(StationMajority.objects.all().order_by())

            assert len(list(StationMajority.objects.filter(pk__in=bulk_pks))) == 2
            assert len(StationMajority.objects.in_bulk_list(bulk_pks)) == 2

            assert len(captured_queries) == 0


@pytest.mark.dbuser
def test_pk_foreign_key_precache():
    station_pk = create_station(majority=StationMajority.MAIN_IN_CITY_ID).pk
    station = Station.objects.get(pk=station_pk)
    with CaptureQueriesContext(connection) as captured_queries:
        assert station.majority.id == StationMajority.MAIN_IN_CITY_ID
        assert len(captured_queries) == 1

    station = Station.objects.get(pk=station_pk)
    with StationMajority.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            assert station.majority.id == StationMajority.MAIN_IN_CITY_ID
            assert len(captured_queries) == 0

    station = Station.objects.get(pk=station_pk)
    with CaptureQueriesContext(connection) as captured_queries:
        assert station.majority.id == StationMajority.MAIN_IN_CITY_ID
        assert len(captured_queries) == 1


@pytest.mark.dbuser
def test_code_precache():
    main_in_city = 'main_in_city'
    bulk_codes = [main_in_city, 'in_tablo', 'asdfasdfasdf']

    with CaptureQueriesContext(connection) as captured_queries:
        StationMajority.objects.get(code=main_in_city)
        assert len(captured_queries) == 1

    with StationMajority.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            StationMajority.objects.get(code=main_in_city)
            StationMajority.objects.filter(code=main_in_city).get()
            assert len(list(StationMajority.objects.filter(code__in=bulk_codes))) == 2

            assert len(captured_queries) == 0


@pytest.mark.dbuser
def test_iexact_keys():
    create_company(title=u'AAAA', sirena_id=u'AAA')
    with CaptureQueriesContext(connection) as captured_queries:
        Company.objects.get(sirena_id=u'AAA')
        assert len(captured_queries) == 1

    with Company.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            assert Company.objects.get(sirena_id=u'AAA')
            assert Company.objects.get(sirena_id=u'aAa')
            assert len(captured_queries) == 0


@pytest.mark.dbuser
def test_q_keys():
    create_company(title=u'AAAA', sirena_id=u'AAA')
    assert Company.objects.get(Q(sirena_id=u'AAA') | Q(title=u'BBB'))

    with Company.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            assert Company.objects.get(Q(sirena_id=u'AAA') | Q(title=u'BBB'))
            assert len(captured_queries) == 1


@pytest.mark.dbuser
def test_select_related_keys():
    c1 = create_company(title=u'AAAA', sirena_id=u'AAA')
    Company.objects.get(pk=c1.id)  # Иначе при первом запросе будет дополнительный SELECT VERSION()

    with Company.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            assert Company.objects.get(sirena_id__iexact=u'AAA')
            assert Company.objects.select_related('country').get(sirena_id__iexact=u'AAA')
            assert len(captured_queries) == 1


@pytest.mark.dbuser
def test_cache_missing():
    c1 = create_company(title=u'AAAA', sirena_id=u'AAA')
    Company.objects.get(pk=c1.id)  # Иначе при первом запросе будет дополнительный SELECT VERSION()

    with Company.objects.using_precache():
        c2 = create_company(title=u'BBBB', sirena_id=u'BBB')
        with CaptureQueriesContext(connection) as captured_queries:
            assert Company.objects.get(pk=c1.id) == c1
            assert Company.objects.get(pk=c2.id) == c2
            assert len(captured_queries) == 1


@pytest.mark.dbuser
def test_in_generator_with_query():
    s = create_settlement()
    ids = Settlement.objects.filter(id=s.id).values_list(
        'id', flat=True
    )
    with Settlement.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            assert len(Settlement.objects.filter(id__in=ids)) == 1
            assert len(captured_queries) == 1


@pytest.mark.dbuser
def test_in_generator_with_values():
    s = create_settlement()
    ids = list(Settlement.objects.filter(id=s.id).values_list(
        'id', flat=True
    ))
    with Settlement.objects.using_precache():
        with CaptureQueriesContext(connection) as captured_queries:
            assert len(Settlement.objects.filter(id__in=ids)) == 1
            assert len(captured_queries) == 0
