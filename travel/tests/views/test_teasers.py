# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function
__metaclass__ = type

import pytest

from common.models.factories import create_teaser_page, create_teaser
from common.models.geo import ExternalDirection, ExternalDirectionMarker
from common.tester.factories import create_settlement, create_station, create_rthread_segment
from travel.rasp.library.python.common23.tester.helpers.assertions import assert_model_set
from common.tests.utils import has_route_search
from common.views.teasers import TeaserSetRaspBase, TeaserSetRasp, fetch_rasp_teasers


@pytest.mark.mongouser
@pytest.mark.dbuser
class TestGetTeasers:
    def test_get_teasers_by_type(self):
        """
        Тест группировки тизеров по типам, без фильтрации по максимальной важности
        """
        page = create_teaser_page(code='index')
        create_teaser(id=1, mode='normal', importance=0, is_active_rasp=True, pages=[page])
        create_teaser(id=2, mode='normal', importance=3, is_active_rasp=True, pages=[page])
        create_teaser(id=3, mode='special', importance=1, is_active_rasp=True, pages=[page])
        create_teaser(id=4, mode='special', importance=2, is_active_rasp=True, pages=[page])

        teasers_dict = fetch_rasp_teasers('index', None, False, u'ru', u'ru')

        assert_model_set([1, 2], teasers_dict['normal'])
        assert_model_set([3, 4], teasers_dict['special'])
        assert len(teasers_dict['banner']) == 0
        assert len(teasers_dict['ahtung']) == 0

    def test_get_teasers_by_type_with_filter_by_max_importance(self):
        """
        Тест группировки тизеров по типам, с фильтрацией по максимальной важности.
        """
        page = create_teaser_page(code='index')
        create_teaser(id=1, mode='normal', importance=0, is_active_rasp=True, pages=[page])
        create_teaser(id=2, mode='normal', importance=3, is_active_rasp=True, pages=[page])
        create_teaser(id=3, mode='special', importance=1, is_active_rasp=True, pages=[page])
        create_teaser(id=4, mode='special', importance=2, is_active_rasp=True, pages=[page])

        teasers_dict = fetch_rasp_teasers('index', None, True, u'ru', u'ru')

        assert_model_set([2], teasers_dict['normal'])
        assert_model_set([4], teasers_dict['special'])
        assert len(teasers_dict['banner']) == 0
        assert len(teasers_dict['ahtung']) == 0

    def test_national_version_and_language(self):
        """
        Тест фильтра по национальной версии и языку при выборке тизеров из БД.
        """
        page_code = 'index'
        teaser_view = TeaserSetRasp(None, page_code, None, u'ru', u'ru')

        page = create_teaser_page(code=page_code)

        create_teaser(id=1, national_version='ru', lang='tt', is_active_rasp=True, pages=[page])
        create_teaser(id=2, national_version='ru', lang='ru', is_active_rasp=True, pages=[page])
        create_teaser(id=3, national_version='ua', lang='ru', is_active_rasp=True, pages=[page])
        create_teaser(id=4, national_version='ua', lang='uk', is_active_rasp=True, pages=[page])

        teasers = teaser_view.get_teasers(page_code)
        assert_model_set([2], teasers)

    def test_get_teasers_by_page(self):
        """
        Тест фильтра по страницам при выборке тизеров из БД.
        """
        page_code_all = 'all'
        page_code1 = 'page1'
        page_code2 = 'page2'
        page_code3 = 'page3'

        teaser_view = TeaserSetRasp(None, [page_code1, page_code2], None, u'ru', u'ru')

        all_page = create_teaser_page(code=page_code_all)
        page1 = create_teaser_page(code=page_code1)
        page2 = create_teaser_page(code=page_code2)
        page3 = create_teaser_page(code=page_code3)

        create_teaser(id=1, is_active_rasp=True, pages=[all_page])
        create_teaser(id=2, is_active_rasp=True, pages=[all_page, page1])
        create_teaser(id=3, is_active_rasp=True, pages=[page2, page3])
        create_teaser(id=4, is_active_rasp=True, pages=[all_page, page3])
        create_teaser(id=5, is_active_rasp=True, pages=[page3])

        result1 = teaser_view.get_teasers(page_code1)
        result2 = teaser_view.get_teasers(page_code2)
        result3 = teaser_view.get_teasers(page_code3)

        assert_model_set([1, 2, 4], result1)
        assert_model_set([1, 2, 3, 4], result2)
        assert_model_set([], result3)

    def test_settlement_teasers(self):
        page_code = 'info_settlement'

        msk = create_settlement(id=1)
        ekb = create_settlement(id=2)
        minsk = create_settlement(id=3)
        kiev = create_settlement(id=4)

        create_teaser(id=1, is_active_rasp=True, settlements=[ekb, kiev])
        create_teaser(id=2, is_active_rasp=True, settlements=[minsk])
        create_teaser(id=3, is_active_rasp=True, settlements=[minsk, kiev])
        create_teaser(id=5, is_active_rasp=True)
        create_teaser(id=6, is_active_rasp=True, settlements=[msk, ekb])

        teaser_view = TeaserSetRasp(None, [page_code], ekb, u'ru', u'ru')
        result = teaser_view.get_teasers('info_settlement')
        assert_model_set([1, 6], result)

        teaser_view = TeaserSetRasp(None, [page_code], kiev, u'ru', u'ru')
        result = teaser_view.get_teasers('info_settlement')
        assert_model_set([1, 3], result)

    @has_route_search
    def test_search_suburban_teasers(self):
        settlement_1 = create_settlement()
        station_from_1, station_to_1, station_to_2 = create_station(), create_station(), create_station()
        ext_dir_1 = ExternalDirection.objects.create(full_title='t_d_1', title='t_1', code=1)
        ext_dir_2 = ExternalDirection.objects.create(full_title='t_d_1', title='t_1', code=2)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir_1, station=station_to_1, order=0)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir_1, station=station_from_1, order=1)
        segment_1 = create_rthread_segment(station_from=station_from_1, station_to=station_to_2)
        segment_2 = create_rthread_segment()

        page_code = 'search_suburban'

        create_teaser(id=1, is_active_export=True, settlements=[settlement_1])
        create_teaser(id=2, is_active_export=True, stations=[station_to_1])
        create_teaser(id=3, is_active_export=True, external_directions=[ext_dir_1])
        create_teaser(id=4, is_active_export=True, external_directions=[ext_dir_2])
        create_teaser(id=5, is_active_export=True)

        data = {
            'points': [station_from_1, station_to_1],
            'routes': [segment_1]
        }
        teaser_view = TeaserSetRaspBase('export', page_code, data)
        result = teaser_view.get_teasers(page_code)
        assert_model_set([2, 3], result)

        data = {
            'points': [station_from_1, settlement_1],
            'routes': [segment_1]
        }
        teaser_view = TeaserSetRaspBase('export', page_code, data)
        result = teaser_view.get_teasers(page_code)
        assert_model_set([1], result)

        ExternalDirectionMarker.objects.create(external_direction=ext_dir_2, station=station_to_2, order=0)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir_2, station=station_from_1, order=1)
        data = {
            'points': [station_from_1, settlement_1],
            'routes': [segment_1]
        }
        teaser_view = TeaserSetRaspBase('export', page_code, data)
        result = teaser_view.get_teasers(page_code)
        assert_model_set([1, 4], result)

        data = {
            'points': [station_from_1, station_to_2],
            'routes': [segment_2]
        }
        teaser_view = TeaserSetRaspBase('export', page_code, data)
        result = teaser_view.get_teasers(page_code)
        assert_model_set([4], result)
