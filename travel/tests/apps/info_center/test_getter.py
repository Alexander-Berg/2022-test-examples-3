# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import mock
import pytest
from mongoengine import Q

from common.apps.info_center.getter import InfoGetter, get_info
from common.apps.info_center.models import Info
from common.models.factories import create_teaser, create_info, create_teaser_page
from travel.rasp.library.python.common23.tester.factories import create_external_direction, create_external_direction_marker
from common.models.teasers import Teaser
from common.tester.factories import create_station, create_settlement, create_rthread_segment
from common.tester.utils.datetime import replace_now
from common.tests.utils import has_route_search
from common.views.teasers import TeaserSetRaspBase

TEST_NOW = datetime(2016, 3, 9, 13, 10, 30)
pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


def assert_info_get(expected, page, **kwargs):
    getter = InfoGetter()

    services = kwargs.pop('services', [Info.Service.WEB])
    kwargs.setdefault('national_versions', ['ru'])

    infos = getter.get(services, page, **kwargs)
    assert len(infos) == len(expected)
    assert set(info.id for info in infos) == set(info.id for info in expected)


class TestInfoGetter(object):
    def test_get_base_params(self):
        now = datetime(2020, 10, 10)

        page = create_teaser_page(code='somepage')

        with replace_now(now):
            info1 = create_info(
                services=[Info.Service.WEB],
                pages=[page],
                national_versions=['ru', 'uk']
            )
            info2 = create_info(
                services=[Info.Service.MOBILE_APPS],
                pages=[page]
            )
            info3 = create_info(
                services=[Info.Service.WEB, Info.Service.MOBILE_APPS],
                pages=[page]
            )
            info4 = create_info(
                services=[Info.Service.WEB, Info.Service.MOBILE_APPS],
                pages=[page],
                lang='tr'
            )
            create_info(dt_from=now + timedelta(days=1), dt_to=now + timedelta(days=1))  # не подходит по времени

            assert_info_get([info1, info3], 'somepage', lang='ru')
            assert_info_get([info1], 'somepage', national_versions=['uk'], lang='ru')
            assert_info_get([info4], 'somepage', lang='tr')
            assert_info_get([info2, info3], 'somepage', services=[Info.Service.MOBILE_APPS], national_versions=['ru'])
            assert_info_get([], 'somepage', services=[Info.Service.MOBILE_APPS], lang='uk')

    def test_additional_query(self):
        page = create_teaser_page(code='somepage')
        info1 = create_info(pages=[page])
        info2 = create_info(pages=[page], title='123')

        assert_info_get([info1, info2], 'somepage')
        assert_info_get([info2], 'somepage', additional_query=Q(title='123'))

    def test_page_all(self):
        create_info(pages=[{'code': 'all123'}])
        assert_info_get([], 'somepage')

        # code 'all' is a special case
        info = create_info(pages=[{'code': 'all'}])
        assert_info_get([info], 'somepage')

    def test_page_info_settlement(self):
        settlement1 = create_settlement()
        settlement2 = create_settlement()
        info1 = create_info(settlements=[settlement1])
        info2 = create_info(settlements=[settlement2])
        create_info(settlements=[create_settlement()])

        assert_info_get([info1], 'info_settlement', data=settlement1)
        assert_info_get([info2], 'info_settlement', data=settlement2)

    def test_page_info_station(self):
        station = create_station()
        direction = create_external_direction()
        create_external_direction_marker(external_direction=direction, station=station)
        info1 = create_info(stations=[station])
        info2 = create_info(external_directions=[direction])

        station2 = create_station()
        direction2 = create_external_direction()
        create_external_direction_marker(external_direction=direction2, station=station2)
        info3 = create_info(stations=[station2])
        info4 = create_info(external_directions=[direction2])

        assert_info_get([info1, info2], 'info_station', data=station)
        assert_info_get([info3, info4], 'info_station', data=station2)

    @has_route_search
    def test_page_search(self):
        sett1, sett2 = create_settlement(), create_settlement()
        station_from, station_to = create_station(), create_station()
        direction1 = create_external_direction()
        create_external_direction_marker(external_direction=direction1, station=station_from)
        create_external_direction_marker(external_direction=direction1, station=station_to)

        direction2 = create_external_direction()
        station_from2 = create_station()
        create_external_direction_marker(external_direction=direction2, station=station_from2)

        info_st_from = create_info(stations=[station_from])
        info_st_from2 = create_info(stations=[station_from2])
        info_st_to = create_info(stations=[station_to])
        info_dir1 = create_info(external_directions=[direction1])
        info_setts = create_info(settlements=[sett1, sett2])
        create_info(external_directions=[direction2])

        # находим инфо для станций и их общего направления
        assert_info_get(
            [info_st_to, info_st_from, info_dir1],
            'search', data={'points': [station_from, station_to], 'routes': []}
        )

        # находим для станций, но не находим по направлению - т.к. оно разное у станций
        assert_info_get([info_st_to, info_st_from2], 'search',
                        data={'points': [station_from2, station_to], 'routes': []})

        # находим для городов, не находим для направлений (т.к. направления работают только для пары станций)
        assert_info_get([info_st_from, info_setts], 'search', data={'points': [station_from, sett2], 'routes': []})
        assert_info_get([info_st_from, info_setts], 'search', data={'points': [sett1, station_from], 'routes': []})
        assert_info_get([info_setts], 'search', data={'points': [sett1, sett2], 'routes': []})

        # поиск для городов, но в результате "поиска" мы получаем сегменты с парами станций,
        # по которым находятся так же инфо для направления
        segm1 = create_rthread_segment(station_from=station_from, station_to=station_to)  # direction1 - найдется
        segm2 = create_rthread_segment(station_from=station_from2, station_to=station_to)  # direction2 - не найдется
        assert_info_get(
            [info_st_from, info_setts, info_dir1],
            'search', data={'points': [station_from, sett2], 'routes': [segm1, segm2]}
        )

    def test_page_direction(self):
        direction = create_external_direction()
        create_external_direction_marker(external_direction=direction, station=create_station())
        info1 = create_info(external_directions=[direction])

        direction2 = create_external_direction()
        create_external_direction_marker(external_direction=direction2, station=create_station())
        info2 = create_info(external_directions=[direction2])

        assert_info_get([info1], 'info_station', data=direction)
        assert_info_get([info2], 'info_station', data=direction2)


def test_old_get_teaser_tablo_page():
    station = create_station()
    ts = TeaserSetRaspBase('rasp', 'tablo', data=(station, []))
    create_teaser(is_active_rasp=True, stations=[station], title='33', content='333')
    teasers = ts.get_teasers('tablo')
    assert len(teasers) == 1
    assert isinstance(teasers[0], Teaser)
    assert teasers[0].title == '33'
    assert teasers[0].content == '333'


def test_get_info():
    with mock.patch.object(InfoGetter, 'get') as m_getter:
        m_getter.return_value = mock.sentinel.res

        assert get_info(1, 2, 3, a=4, b=5) == mock.sentinel.res
        m_getter.assert_called_once_with(1, 2, 3, a=4, b=5)

    with mock.patch.object(InfoGetter, 'get') as m_getter:
        m_getter.side_effect = ValueError

        assert get_info(1, 2, 3, a=4, b=5) == []
        m_getter.assert_called_once_with(1, 2, 3, a=4, b=5)
