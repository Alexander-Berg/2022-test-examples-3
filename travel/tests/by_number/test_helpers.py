# -*- coding: utf-8 -*-

import mock

from travel.avia.library.python.common.models.transport import TransportType

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_route, create_deluxe_train
from travel.avia.library.python.tester.mocks import set_setting

from travel.avia.library.python.route_search.by_number.models import RouteNumberIndex
from travel.avia.library.python.route_search.by_number.helpers import (
    get_t_type_ids, get_t_type_id, build_t_type_ids_list, get_search_result,
    _find_numeric_routes, _find_special_train_routes,
    _find_routes_by_wide_search, _get_special_train_query_exact,
    _get_special_train_query_startwith, _strange_trim_zeroes,
    old_sub_find_threads, number_variants
)


class TestHelpers(TestCase):
    def test_get_t_type_ids(self):
        t_type_ids = [TransportType.BUS_ID, TransportType.PLANE_ID]
        t_type_codes = [t_type.code for t_type in TransportType.objects.filter(id__in=t_type_ids)]

        result = get_t_type_ids(t_type_codes)
        assert len(t_type_ids) == len(result)
        assert set(t_type_ids) == set(result)

        assert [] == get_t_type_ids([])

    def test_get_t_type_id(self):
        t_code = TransportType.objects.get(id=TransportType.BUS_ID).code
        assert TransportType.BUS_ID == get_t_type_id(t_code)
        assert get_t_type_id('not exist') is None

    def test_build_t_type_ids_list(self):
        t_type_ids = [TransportType.BUS_ID, TransportType.PLANE_ID]
        assert t_type_ids == build_t_type_ids_list(t_type_ids)
        assert [t_type.id for t_type in TransportType.objects.all()] == build_t_type_ids_list(None)

    def test_get_search_result(self):
        result = get_search_result(None)
        assert result.threads == [] and result.routes == []

        with mock.patch('travel.avia.library.python.route_search.by_number.helpers._find_numeric_routes') as m_num_r, \
                mock.patch('travel.avia.library.python.route_search.by_number.helpers._find_special_train_routes') as m_sp_r, \
                mock.patch('travel.avia.library.python.route_search.by_number.helpers._find_routes_by_wide_search') as m_wide_r:
            query = u'123'
            t_type_ids = [TransportType.TRAIN_ID]
            get_search_result(query, t_type_ids)
            m_num_r.assert_called_once_with(query, t_type_ids)

            query = u'abc'
            get_search_result(query, t_type_ids)
            m_sp_r.assert_called_once_with(query)

            t_type_ids = [TransportType]
            get_search_result(query, t_type_ids, search_for_special_train=True)
            m_wide_r.assert_called_once_with(query, t_type_ids)

    def test_find_numeric_routes(self):
        water_route_12 = create_route(t_type=TransportType.WATER_ID, __={'threads': [{'number': u'12'}]})  # noqa
        plane_route_space12 = create_route(t_type=TransportType.PLANE_ID, __={'threads': [{'number': u' 12'}]})
        bus_route_12 = create_route(t_type=TransportType.BUS_ID, __={'threads': [{'number': u'12'}]})
        train_route_123 = create_route(t_type=TransportType.TRAIN_ID, __={'threads': [{'number': u'123'}]})  # noqa
        suburban_route_123 = create_route(t_type=TransportType.SUBURBAN_ID, __={'threads': [{'number': u'123'}]})
        helicopter_route_123 = create_route(t_type=TransportType.HELICOPTER_ID, __={'threads': [{'number': u'123'}]})

        # тестируем, что находятся только точно номер 12 и только запрашиваемых типов транспорта
        routes_12 = _find_numeric_routes(u'12', [TransportType.BUS_ID, TransportType.TRAIN_ID, TransportType.PLANE_ID])
        assert len(routes_12) == 2
        assert set(routes_12) == {plane_route_space12, bus_route_12}

        # тестируем, что находится только точно номер 123, поезда ищутся с 4-символьным номером
        routes_123 = _find_numeric_routes(u'123', [TransportType.BUS_ID, TransportType.TRAIN_ID,
                                                   TransportType.SUBURBAN_ID, TransportType.HELICOPTER_ID])
        assert len(routes_123) == 2
        assert set(routes_123) == {suburban_route_123, helicopter_route_123}

        # тестируем, что поезда с правильным номером находятся
        train_route_456sh = create_route(t_type=TransportType.TRAIN_ID, __={'threads': [{'number': u'456Ш'}]})
        routes_456 = _find_numeric_routes(u'456', [TransportType.TRAIN_ID])
        assert len(routes_456) == 1
        assert routes_456[0] == train_route_456sh

    def test_get_special_train_query_exact(self):
        query = u'123'

        with set_setting('MODEL_LANGUAGES', ['ru', 'uk']):
            result_query = _get_special_train_query_exact(query)
            assert result_query.children == [('title', u'123'), ('title_ru', u'123'), ('title_uk', u'123')]

    def test__get_special_train_query_startwith(self):
        query = u'123'

        with set_setting('MODEL_LANGUAGES', ['ru', 'uk']):
            result_query = _get_special_train_query_startwith(query)
            assert result_query.children == [('title__startswith', u'123'), ('title_ru__startswith', u'123'),
                                             ('title_uk__startswith', u'123')]

    def test_find_special_train_routes(self):
        create_deluxe_train(title=u'de', numbers=u'11/12/15')
        query = u'de'

        routes = [create_route(t_type=TransportType.TRAIN_ID, __={'threads': [{'number': u'11'}]}),
                  create_route(t_type=TransportType.TRAIN_ID, __={'threads': [{'number': u'15'}]}),
                  create_route(t_type=TransportType.TRAIN_ID, __={'threads': [{'number': u'101'}]}),
                  create_route(t_type=TransportType.TRAIN_ID, __={'threads': [{'number': u'102'}]})]

        result_routes = _find_special_train_routes(query)
        assert set(result_routes) == set(routes[:2])

        assert _find_special_train_routes('not exist') == []

        query = u'delu'
        create_deluxe_train(title=u'deluxe', numbers=u'101/102')

        result_routes = _find_special_train_routes(query)
        assert set(result_routes) == set(routes[2:])

    def test_find_routes_by_wide_search(self):
        t_type_ids = [TransportType.TRAIN_ID, TransportType.SUBURBAN_ID, TransportType.BUS_ID]

        routes = [create_route(t_type=TransportType.TRAIN_ID, __={'threads': [{'number': u'num'}]}),
                  create_route(t_type=TransportType.SUBURBAN_ID, __={'threads': [{'number': u'second_num1'}]})]

        RouteNumberIndex.rebuild_index()

        result_routes = _find_routes_by_wide_search(u'num', t_type_ids)
        assert len(result_routes) == 1
        assert result_routes[0] == routes[0]

        result_routes = _find_routes_by_wide_search(u'нум', t_type_ids)
        assert len(result_routes) == 1
        assert result_routes[0] == routes[0]

        result_routes = _find_routes_by_wide_search(u'second_num000001', t_type_ids)
        assert len(result_routes) == 1
        assert result_routes[0] == routes[1]

        result_routes = _find_routes_by_wide_search(u'*&@second_num000001', t_type_ids)
        assert len(result_routes) == 1
        assert result_routes[0] == routes[1]

    def test_strange_trim_zeroes(self):
        assert _strange_trim_zeroes(u'000') == u'0'
        assert _strange_trim_zeroes(u'001') == u'1'
        assert _strange_trim_zeroes(u'abc000') == u'abc0'
        assert _strange_trim_zeroes(u'abc0001') == u'abc1'

    def test_old_sub_find_threads(self):
        query = u' 123 '
        with mock.patch('travel.avia.library.python.route_search.by_number.helpers.get_search_result') as m_search_result:
            old_sub_find_threads(query)
            m_search_result.assert_called_once_with(query.strip(), search_for_special_train=False)

    def test_number_variants(self):
        number = u' н-у-м '
        variants = number_variants(number)

        assert variants == {u' н-у-м ', u'n-u-m', u' n-u-m ', u'нум', u'н-у-м', u'-n-u-m-', u'num', u'-н-у-м-'}
