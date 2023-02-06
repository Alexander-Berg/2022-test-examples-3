# -*- coding: utf-8 -*-

from datetime import datetime

import pytest
import mock
from faker import Factory
from werkzeug.exceptions import BadRequest

from travel.avia.library.python.common.models.service import Service
from travel.avia.library.python.common.models.team import Team

from travel.avia.ticket_daemon_api.jsonrpc.query import Query
from travel.avia.ticket_daemon_api.jsonrpc.lib.enabled_partner_provider import enabled_partner_provider
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.models_utils.geo import get_point_tuple_by_key
from travel.avia.library.python.tester.factories import get_model_factory, create_settlement, create_airport


def _generate_test_query(national_version='ru', lang='ru', service='test_service'):
    create_settlement(id=213)
    create_settlement(id=2)
    return Query(
        point_from=get_point_tuple_by_key('c213'),
        point_to=get_point_tuple_by_key('c2'),
        passengers={
            'adults': 1,
            'children': 0,
            'infants': 0,
        },
        date_forward=datetime.now(),
        national_version=national_version,
        lang=lang,
        service=service,
    )


@pytest.mark.dbuser
def test_Query_should_set_language_to_ru_in_case_of_kz_national_version():
    with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.replace_kk_with_ru_in_kz', return_value=True):
        query = _generate_test_query(national_version='kz', lang='kk')

        assert query.lang == 'ru'


@pytest.mark.dbuser
@pytest.mark.parametrize('flag_is_enabled,expected_language', [(True, 'ru'), (False, 'kk')])
def test_Query_should_change_language_to_run_in_kz_only_if_flag_is_enabled(flag_is_enabled, expected_language):
    with mock.patch('travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.replace_kk_with_ru_in_kz', return_value=flag_is_enabled):
        query = _generate_test_query(national_version='kz', lang='kk')

        assert query.lang == expected_language


@pytest.fixture()
def service():
    faker = Factory.create()
    reset_all_caches()
    team = get_model_factory(Team)(code=faker.pystr(max_chars=10))
    return get_model_factory(Service)(code=faker.pystr(max_chars=10), team=team)


@pytest.mark.dbuser
def test_query__get_enabled_partner_codes__good_query(service):
    expected = object()
    query = _generate_test_query(national_version='ru', service=service.code)
    with mock.patch.object(enabled_partner_provider, 'get_codes', return_value=expected):
        assert query.get_enabled_partner_codes() == expected


@pytest.mark.dbuser
def test_query__get_enabled_partner_codes__bad_query(service):
    query = _generate_test_query(national_version='by', service=service.code)
    with pytest.raises(BadRequest):
        query.get_enabled_partner_codes()


@pytest.mark.dbuser
@mock.patch(
    'travel.avia.ticket_daemon_api.jsonrpc.lib.feature_flags.replace_search_to_station_with_search_to_city', return_value=True,
)
def test_init_multiple_routes(*mocks):
    reset_all_caches()
    from_city = create_settlement(title='CITY-FROM')
    to_city = create_settlement(title='CITY-TO')

    from_airport = create_airport(title='AIRPORT-FROM', iata='IATA-FROM', settlement=from_city)

    key = '{from_key}_{to_key}_2017-01-01_None_economy_1_0_0_ru'.format(
        from_key=from_airport.point_key,
        to_key=to_city.point_key,
    )

    query = Query.from_key(key, service='ticket', lang='ru', t_code='plane')
    assert len(query.queries) == 1
    assert query.queries[0].base_query is not None
    assert query.id != query.queries[0].id
