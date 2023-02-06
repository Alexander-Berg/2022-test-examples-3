# coding: utf-8

from datetime import date, datetime
from django.conf import settings

import mock

from common.data_api.ticket_daemon.factories import create_segment, create_variant
from common.data_api.ticket_daemon.jsend import InvalidResponse, Fail, Error
from common.models.transport import TransportType
from common.utils.date import MSK_TZ, KIEV_TZ

from common.tester.factories import create_settlement, create_station, create_partner
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.tariffs.retrieving import (
    DaemonResult, fill_groups_from_segments, fill_groups_from_supplement,
    get_bus_results, get_plane_results, Query, make_tickets_query
)

from travel.rasp.api_public.tests.old_versions.factories import create_request


class TestTariffs(TestCase):
    ticket_side = [mock.sentinel.ticket_query_1, mock.sentinel.ticket_query_2]
    request = create_request()

    @mock.patch('common.data_api.ticket_daemon.query.Query', return_value=ticket_side)
    def test_make_tickets_query(self, m_query):
        segments = [mock.sentinel.seg_1, mock.sentinel.seg_2]
        point_from, point_to = create_station(), create_station()
        user_settlement = create_settlement()
        departure_date = date(2001, 2, 5)
        national_version = 'ru'

        query = Query(segments=segments, point_from=point_from, point_to=point_to,
                      date=departure_date, t_type=TransportType.objects.get(code='plane'))

        result_queries = make_tickets_query(query, user_settlement, national_version)

        assert result_queries == self.ticket_side
        assert m_query.call_args_list[0] == [(), {'user_settlement': user_settlement,
                                                  'point_from': point_from,
                                                  'point_to': point_to,
                                                  'date_forward': departure_date,
                                                  'date_backward': None,
                                                  'passengers': {'adults': 1},
                                                  'klass': 'economy',
                                                  'national_version': national_version,
                                                  't_code': 'plane'}]

    @mock.patch('common.data_api.ticket_daemon.query.Query')
    def test_daemon_make_results(self, m_query):
        point_from, point_to = create_station(), create_station()
        user_settlement = create_settlement()
        departure_date = date(2001, 2, 5)
        _statuses = {'ozone': 'done'}
        variants = {'ozone': [create_variant(segments=[create_segment()]),
                              create_variant(segments=[create_segment(), create_segment()])]}
        query = Query(point_from=point_from, point_to=point_to,
                      date=departure_date, t_type=TransportType.objects.get(code='plane'))
        m_query.return_value.query_all = mock.Mock()
        m_query.return_value.collect_variants = mock.Mock(return_value=(variants, _statuses))
        DaemonResult.make_results(self.request, query, user_settlement, initiate_query=True)
        m_query.return_value.query_all.assert_called_once_with(ignore_cache=settings.IGNORE_DAEMON_CACHE)

        DaemonResult.make_results(self.request, query, user_settlement, initiate_query=False)
        assert m_query.return_value.query_all.call_count == 1

        for error in (InvalidResponse, Fail, Error):
            def raise_ex(*args, **kwargs):
                raise error
            m_query.return_value.query_all = mock.Mock(side_effect=raise_ex)
            assert DaemonResult.make_results(self.request, query, user_settlement, initiate_query=True) == []

            m_query.return_value.query_all = mock.Mock()
            m_query.return_value.collect_variants = mock.Mock(side_effect=raise_ex)
            assert DaemonResult.make_results(self.request, query, user_settlement, initiate_query=False) == []

    @mock.patch('common.data_api.ticket_daemon.query.Query')
    def test_daemon_make_result(self, m_query):
        point_from, point_to = create_station(), create_station()
        departure_date = date(2001, 2, 5)
        partner_code = 'ozone'
        default_datetime = datetime(2001, 2, 1)
        t_type_sub = TransportType.objects.get(id=TransportType.SUBURBAN_ID)
        query = Query(point_from=point_from, point_to=point_to,
                      date=departure_date, t_type=TransportType.objects.get(code='plane'))
        seg_1 = create_segment(departure=default_datetime, arrival=default_datetime, number='num 1', t_type=t_type_sub)
        seg_2 = create_segment(departure=default_datetime, arrival=default_datetime, number='num 2', t_type=t_type_sub)
        seg_3 = create_segment(departure=default_datetime, arrival=default_datetime, number='num 3', t_type=t_type_sub)
        variants = {partner_code: [create_variant(segments=[seg_1]),
                                   create_variant(segments=[create_segment(), seg_2]),
                                   create_variant(segments=[seg_3, seg_2])]}

        with mock.patch.object(DaemonResult, 'make_seats_and_tariffs',
                               side_effect=[(mock.sentinel.seats_1, mock.sentinel.tariffs_1),
                                            (mock.sentinel.seats_3, mock.sentinel.tariffs_3)]):
            result = DaemonResult.make_result(partner_code, query, variants.values()[0])
            assert result.supplier == partner_code
            assert result.query == query
            assert result.segments == {'num-1-0205': seg_1,
                                       'num-3-0205': seg_3}
            assert result.data['num-1'].seats == mock.sentinel.seats_1
            assert result.data['num-1'].tariffs == mock.sentinel.tariffs_1
            assert result.data['num-3'].seats == mock.sentinel.seats_3
            assert result.data['num-3'].tariffs == mock.sentinel.tariffs_3

    def test_fill_groups_from_segments(self):
        seg_1 = create_segment(departure=MSK_TZ.localize(datetime(2001, 1, 1, 12)),
                               t_type=TransportType.objects.get(id=TransportType.BUS_ID),
                               station_from=create_station(), number='num 1')
        seg_2 = create_segment(departure=MSK_TZ.localize(datetime(2002, 5, 6, 13)),
                               t_type=TransportType.objects.get(id=TransportType.TRAIN_ID),
                               station_from=create_station(), number='num 1')
        seg_3 = create_segment(departure=KIEV_TZ.localize(datetime(2005, 4, 7, 23, 30)),
                               t_type=TransportType.objects.get(id=TransportType.PLANE_ID),
                               station_from=create_station(), number='num 1')

        plane_group, bus_group = {}, {}
        fill_groups_from_segments([seg_1, seg_2, seg_3], plane_group, bus_group)
        assert bus_group == {(date(2001, 1, 1), date(2001, 1, 1)): [seg_1]}
        assert plane_group == {(date(2005, 4, 8), date(2005, 4, 7)): [seg_3]}

    @replace_now(datetime(2001, 2, 4))
    def test_get_bus_results(self):
        seg_1 = create_segment(departure=MSK_TZ.localize(datetime(2001, 2, 5, 12)),
                               t_type=TransportType.objects.get(id=TransportType.BUS_ID),
                               station_from=create_station(), number='num 1')
        seg_2 = create_segment(departure=MSK_TZ.localize(datetime(2002, 5, 6, 13)),
                               t_type=TransportType.objects.get(id=TransportType.BUS_ID),
                               station_from=create_station(), number='num 2')
        bus_group = {(date(2001, 2, 5), date(2001, 2, 5)): [seg_1, seg_2]}

        point_from, point_to = create_settlement(), create_settlement()
        user_settlement = create_settlement()

        with mock.patch.object(DaemonResult, 'add_tariff_and_seat',
                               side_effect=[[mock.sentinel.seg_1], [mock.sentinel.seg_2]]) as m_add_t_and_s:
            result = get_bus_results(self.request, bus_group, point_from, point_to, user_settlement)
            assert result == [mock.sentinel.seg_1]
            assert m_add_t_and_s.call_args_list[0][0][0] == self.request
            assert m_add_t_and_s.call_args_list[0][0][2] == user_settlement
            query = m_add_t_and_s.call_args_list[0][0][1]
            assert query.segments == [seg_1, seg_2]
            assert query.point_from == point_from and query.point_to == point_to
            assert query.date == date(2001, 2, 5)
            assert query.t_type == TransportType.objects.get(code='bus')

    def test_get_plane_results(self):
        seg_1 = create_segment(departure=MSK_TZ.localize(datetime(2001, 2, 5, 12)),
                               t_type=TransportType.objects.get(id=TransportType.PLANE_ID),
                               station_from=create_station(), number='num 1')
        seg_2 = create_segment(departure=MSK_TZ.localize(datetime(2002, 5, 6, 13)),
                               t_type=TransportType.objects.get(id=TransportType.PLANE_ID),
                               station_from=create_station(), number='num 2')
        plane_group = {(date(2001, 2, 5), date(2001, 2, 5)): [seg_1, seg_2]}

        point_from, point_to = create_settlement(), create_settlement()
        user_settlement = create_settlement()

        with mock.patch.object(DaemonResult, 'add_tariff_and_seat',
                               return_value=[mock.sentinel.seg_1, mock.sentinel.seg_2]):
            with replace_now(datetime(2001, 2, 4)):
                result = get_plane_results(self.request, plane_group, point_from, point_to, user_settlement)
                assert result == []

                create_partner(enabled_in_rasp_ru=True, code='_code')
                result = get_plane_results(self.request, plane_group, point_from, point_to, user_settlement)
                assert result == [mock.sentinel.seg_1, mock.sentinel.seg_2]

                plane_group = {(date(2001, 2, 3), date(2001, 2, 3)): [seg_1, seg_2]}
                result = get_plane_results(self.request, plane_group, point_from, point_to, user_settlement)
                assert result == []

    def test_fill_groups_from_supplement(self):
        tcodes = ['bus', 'train', 'plane']
        point_from, point_to = create_settlement(), create_settlement()
        plane_group, bus_group = {}, {}
        early_border = MSK_TZ.localize(datetime(2005, 4, 10))
        late_border = MSK_TZ.localize(datetime(2005, 4, 12, 23, 30))
        fill_groups_from_supplement(tcodes, point_from, point_to, early_border, late_border, plane_group, bus_group)

        assert bus_group == {(date(2005, 4, 10), date(2005, 4, 10)): [],
                             (date(2005, 4, 10), date(2005, 4, 11)): [],
                             (date(2005, 4, 10), date(2005, 4, 12)): []}

        assert plane_group == {(date(2005, 4, 10), date(2005, 4, 10)): [],
                               (date(2005, 4, 10), date(2005, 4, 11)): [],
                               (date(2005, 4, 10), date(2005, 4, 12)): []}
