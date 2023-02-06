# -*- coding: utf-8 -*-

import mock
import pytest

from travel.avia.library.python.common.models.transport import TransportType

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester import transaction_context
from travel.avia.library.python.tester import factories

from travel.avia.library.python.route_search.by_number import find_threads_by_query, get_cleaned_plane_number
from travel.avia.library.python.route_search.by_number.models import RouteNumberIndex


@pytest.fixture(scope='module', autouse=True)
def load_routes(request):
    atomic = transaction_context.enter_atomic()

    factories.create_route(__={
        'threads': [
            {'number': 'AA 133', 'title': 'plane_aa_133', 't_type': 'plane'},
        ]
    })

    factories.create_route(__={
        'threads': [
            {'number': u'133Б', 'title': 'train_133_b', 't_type': 'train'},
        ]
    })

    factories.create_route(__={
        'threads': [
            {'number': u'222Ч', 'title': 'train_222_ch', 't_type': 'train'},
        ]
    })

    factories.create_route(__={
        'threads': [
            {'number': '133', 'title': 'bus_133', 't_type': 'bus'},
        ]
    })

    factories.create_route(__={
        'threads': [
            {'number': '1333', 'title': 'suburban_1333', 't_type': 'suburban'},
        ]
    })

    factories.create_deluxe_train(title=u'Сапсан', title_ru=u'Тигр', title_uk=u'Сало', numbers=u'222Ч/333Б')

    RouteNumberIndex.rebuild_index()

    def fin():
        transaction_context.rollback_atomic(atomic)

    request.addfinalizer(fin)


class TestRouteNumberIndex(TestCase):
    def check_search(self, search_params, thread_count, titles=tuple()):
        if isinstance(titles, basestring):
            titles = (titles,)

        if isinstance(search_params, basestring):
            search_params = (search_params,)

        threads = find_threads_by_query(*search_params).threads
        found_titles = [t.title for t in threads]
        assert len(found_titles) == thread_count

        if titles:
            found_titles = [t.title for t in threads]
            assert set(found_titles) == set(titles)

    def test_numeric_search(self):
        self.check_search(u'133', 4, ['plane_aa_133', 'train_133_b', 'bus_133', 'suburban_1333'])
        self.check_search(u'0133', 2, ['plane_aa_133', 'train_133_b'])
        self.check_search((u'133', [TransportType.PLANE_ID]), 1, 'plane_aa_133')
        self.check_search(u'13', 0)

    def test_wide_search(self):
        self.check_search(u'АА 1', 1, 'plane_aa_133')  # русские АА
        self.check_search(u'АА1', 1, 'plane_aa_133')  # русские АА
        self.check_search(u'AA 13', 1, 'plane_aa_133')  # английские AA
        self.check_search(u'AA13', 1, 'plane_aa_133')  # английские AA
        self.check_search(u'33Б', 0)
        self.check_search(u'133Б', 1, 'train_133_b')

    def test_search_special_train(self):
        self.check_search(u'Сап', 0)
        self.check_search(u'Сапс', 1, 'train_222_ch')
        self.check_search(u'Сапсан', 1, 'train_222_ch')
        self.check_search(u'Тигр', 1, 'train_222_ch')
        self.check_search(u'Сало', 1, 'train_222_ch')


class TestFindThreads(TestCase):
    def test_find_threads_by_query(self):
        query = u' query '
        t_type_ids = [TransportType.BUS_ID, TransportType.TRAIN_ID]
        with mock.patch('travel.avia.library.python.route_search.by_number.get_search_result') as m_search_r, \
                mock.patch('travel.avia.library.python.route_search.by_number.build_t_type_ids_list', side_effect=lambda x: x) as m_b_ids, \
                mock.patch('travel.avia.library.python.route_search.by_number.get_cleaned_plane_number') as m_clean_n:
            m_search_r.return_value = mock_result = mock.Mock()
            result = find_threads_by_query(query, t_type_ids)

            assert result is mock_result
            m_b_ids.assert_called_once_with(t_type_ids)
            m_search_r.assert_called_once_with(query.strip(), t_type_ids)
            assert not m_clean_n.called

            mock_result.routes = None
            result = find_threads_by_query(query, t_type_ids)

            assert m_b_ids.call_count == 2
            assert m_search_r.call_count == 3
            assert m_clean_n.called

    def test_get_cleaned_plane_number(self):
        assert get_cleaned_plane_number(u'ABC       12') == u'ABC 12'
        assert get_cleaned_plane_number(u'ABC*%*12') == u'ABC 12'
        assert get_cleaned_plane_number(u'ABC   ') is None
        assert get_cleaned_plane_number(u'12') is None
        assert get_cleaned_plane_number(u'   ABC 12') is None
