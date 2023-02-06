# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from copy import deepcopy

import mock

from common.models.geo import Station, Settlement, StationType
from common.models.transport import TransportType

import travel.rasp.suggests_tasks.suggests.generate.generate as generate
from travel.rasp.suggests_tasks.suggests.generate.caches import SettlementCodePrecache, StationCodePrecache, SynonymsPrecache
from travel.rasp.suggests_tasks.suggests.generate.generate import (
    precache, ttype_id_to_code, prepare_titles_data, prepare_objects_data, prepare_station_prefixes,
    generate_objs_data, convert_stat_routes_ids, generate_all
)
from travel.rasp.suggests_tasks.suggests.generate.shared_objects import set_objs, get_obj
from travel.rasp.suggests_tasks.suggests.generate.titles import StationWrapper
from travel.rasp.suggests_tasks.suggests.objects_utils import ObjIdConverter
from travel.rasp.suggests_tasks.suggests.storage import Storage

from common.tester.testcase import TestCase


class TestGenerate(TestCase):
    def test_ttype_id_to_code(self):
        for tr_id in [TransportType.BUS_ID, TransportType.TRAIN_ID, TransportType.PLANE_ID,
                      TransportType.SUBURBAN_ID, TransportType.WATER_ID]:
            assert ttype_id_to_code(tr_id) == TransportType.objects.get(id=tr_id).code

        assert ttype_id_to_code(TransportType.SEA_ID) == TransportType.objects.get(id=TransportType.WATER_ID).code
        assert ttype_id_to_code(TransportType.RIVER_ID) == TransportType.objects.get(id=TransportType.WATER_ID).code

    def test_precache(self):
        with mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.precache_django_models') as m_django_precache, \
                mock.patch.object(StationCodePrecache, u'precache', autospec=True) as m_st_precache, \
                mock.patch.object(SynonymsPrecache, u'precache', autospec=True) as m_syn_precache, \
                mock.patch.object(SettlementCodePrecache, u'precache', autospec=True) as m_sett_precache:
            station_codes = StationCodePrecache([])
            settlement_codes = SettlementCodePrecache([])
            synonym_codes = SynonymsPrecache([])

            set_objs(**{'station_codes': station_codes,
                        'settlement_codes': settlement_codes,
                        'synonyms': synonym_codes})

            precache()
            m_django_precache.assert_called_once_with()
            m_st_precache.assert_called_with(station_codes)
            m_sett_precache.assert_called_with(settlement_codes)
            m_syn_precache.assert_called_with(synonym_codes)

    def test_prepare_titles_data(self):
        db_id_to_local = {'station': {1: 11, 2: 22}, 'settlement': {3: 33}}
        ids_converter = ObjIdConverter(db_id_to_local_id=db_id_to_local)
        set_objs(id_converter=ids_converter)

        titles_data = {
            ('station', 1): [{'title': 'title_1', 'id': 1}],
            ('settlement', 3): [{'title': 'title_2', 'id': 2}],
        }

        objs_data = {
            11: {'obj_type': 'station', 't_types': [TransportType.PLANE_ID]},
            22: {'obj_type': 'station', 't_types': [TransportType.TRAIN_ID]},
            33: {'obj_type': 'settlement', 't_types': [TransportType.BUS_ID]},
        }
        result = prepare_titles_data(titles_data, objs_data)

        assert result == {'ru': {
            u'bus': {'title_2': {(33, True)}},
            u'all': {'title_1': {(11, True)}, 'title_2': {(33, True)}},
            u'plane': {'title_1': {(11, True)}}
        }}

    def test_prepare_objects_data(self):
        db_id_to_local = {'station': {1: 11}, 'settlement': {3: 33}}
        ids_converter = ObjIdConverter(db_id_to_local_id=db_id_to_local)
        set_objs(id_converter=ids_converter)

        titles_data = {
            ('station', 1): [{
                'title': 'title_1', 'obj_type': 'station', 't_types': [TransportType.PLANE_ID], 'slug': 'title-1'
            }],
            ('settlement', 3): [{
                'title': 'title_2', 'obj_type': 'settlement', 't_types': [TransportType.BUS_ID], 'slug': 'title-2'
            }]
        }

        objs_data = {
            11: {
                'local_id': 11, 'title': 'title_1', 'obj_type': 'station', 't_types': [TransportType.PLANE_ID],
                'search_titles': ['title_1'], 'slug': 'title-1'
            },
            33: {
                'local_id': 33, 'title': 'title_2', 'obj_type': 'settlement', 't_types': [TransportType.BUS_ID],
                'search_titles': ['title_2'], 'slug': 'title-2'
            },
        }

        result = prepare_objects_data(titles_data)
        assert result == objs_data

    def test_prepare_station_prefixes(self):
        st_type_transport_type = {
            TransportType.TRAIN_ID: [StationType.TRAIN_STATION_ID, StationType.PLATFORM_ID],
            TransportType.PLANE_ID: [StationType.AIRPORT_ID]
        }

        with mock.patch.object(StationWrapper, 'STATION_TYPE_BY_TRANSPORT_TYPE') as m_st_types, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.TITLE_LANGS', ['ru', 'en']):

            m_st_types.__get__ = mock.Mock(return_value=st_type_transport_type)
            prefixes = prepare_station_prefixes()
            assert prefixes == {
                'ru': {
                    u'аэропорт': (TransportType.PLANE_ID,),
                    u'вокзал': (TransportType.TRAIN_ID,),
                    u'платформа': (TransportType.TRAIN_ID,)
                },
                'en': {
                    u'airport': (TransportType.PLANE_ID,),
                    u'train station': (TransportType.TRAIN_ID,),
                    u'platform': (TransportType.TRAIN_ID,)
                }
            }

    def test_generate_objs_data(self):
        model_ids = [(Station, [1, 2]), (Settlement, [3])]
        pool_size = 1
        with mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.generate_titles_data', autospec=True) as m_gen_titles, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.prepare_objects_data', autospec=True) as m_pr_obj, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.prepare_titles_data', autospec=True) as m_pr_titles, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.prepare_station_prefixes', autospec=True) as m_pr_prefixes:

            result = generate_objs_data(model_ids, pool_size)
            m_gen_titles.assert_called_with(model_ids, pool_size)
            m_pr_obj.assert_called_with(m_gen_titles.return_value)
            m_pr_titles.assert_called_with(m_gen_titles.return_value, m_pr_obj.return_value)
            m_pr_prefixes.assert_called_with()

            assert result == {
                'objects_data': m_pr_obj.return_value,
                'titles': m_pr_titles.return_value,
                'station_prefixes': m_pr_prefixes.return_value,
                'db_info': generate.get_db_info(),
            }

    def test_convert_stat_routes_ids(self):
        db_id_to_local = {'station': {1: 11, 2: 22}, 'settlement': {3: 33}}
        ids_converter = ObjIdConverter(db_id_to_local_id=db_id_to_local)
        set_objs(id_converter=ids_converter)

        stat_routes = {
            u'water': {(u's', 1): {(u's', 2): {110267: 2}}},
            u'train': {(u'c', 3): {(u's', 2): {213: 5, 959: 3}}}
        }

        routes = convert_stat_routes_ids(stat_routes)
        assert routes == {
            u'water': {11: {22: {110267: 2}}},
            u'train': {33: {22: {213: 5, 959: 3}}}
        }

    def test_generate_all(self):
        model_ids = [(Station, [1, 2]), (Settlement, [3])]

        with mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.get_pool_size', autospec=True, return_value=10) as m_pool_size, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.get_ttypes', autospec=True, return_value=(mock.Mock(), mock.Mock())) as m_get_ttypes, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.generate_objs_data', autospec=True) as m_gen_objs, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.generate.precache', autospec=True) as m_precache, \
             mock.patch('travel.rasp.suggests_tasks.suggests.generate.shared_objects.clear_objs', autospec=True) as m_clear_objs, \
             mock.patch.object(Storage, 'save_ttypes', autospec=True) as m_save_tytpes, \
             mock.patch.object(Storage, 'load_ttypes', autospec=True), \
             mock.patch.object(Storage, 'load_objs_data', autospec=True) as m_load_objs, \
             mock.patch.object(Storage, 'save_objs_data', autospec=True) as m_save_objs, \
             mock.patch.object(Storage, 'load_stat', autospec=True) as m_load_stat, \
             mock.patch.object(Storage, 'save_id_converter', autospec=True) as m_save_conv, \
             mock.patch.object(Storage, 'load_stat_converted', autospec=True) as m_load_stat_converted, \
             mock.patch.object(Storage, 'save_stat_converted', autospec=True) as m_save_stat_converted:

            storage = Storage('dir')
            generate_all(
                storage, model_ids,
                skip_stat=True,
                skip_objs_data=True,
                skip_precache=True)

            m_pool_size.assert_called_once_with()
            m_clear_objs.assert_called_once_with()
            assert isinstance(get_obj('id_converter'), ObjIdConverter)
            m_get_ttypes.assert_called_once_with(10)
            m_load_objs.assert_called_once_with(storage)
            m_save_tytpes.assert_called_once_with(storage, {'settlements': m_get_ttypes.return_value[1],
                                                            'stations': m_get_ttypes.return_value[0]})
            m_save_conv.assert_called_once_with(storage, get_obj('id_converter'))

            m_gen_objs.return_value = {'some_data': 123}
            m_load_stat_converted.return_value = {'by_obj': 123, 'routes': 42}

            # Т.к. мы проверяем 2 подряд идущих вызова save_objs_data, вызываемых с одним инстансом data,
            # но в моменты вызовов имеющим разный контент, проверка mock.call_args_list не сработает.
            # Поэтому сохраняем аргументы сами с копированием.
            save_objs_call_args_list = []

            def save_objs(*args, **kwargs):
                args_copy = deepcopy(args)
                kwargs_copy = deepcopy(kwargs)
                save_objs_call_args_list.append((args_copy, kwargs_copy))
                return mock.Mock()

            m_save_objs.side_effect = save_objs

            # сохраняли 2 раза - сначала при основной генерации, потом после генерации empty cache
            first_save_expected = {
                'some_data': 123,
                'stat_routes': 42
            }

            generate_all(storage, model_ids)

            m_load_stat.assert_called_once_with(storage)
            m_save_stat_converted.assert_called_once_with(storage, m_load_stat.return_value)
            m_gen_objs.assert_called_once_with(model_ids, pool_size=10)

            assert save_objs_call_args_list == [
                ((storage, first_save_expected), {})
            ]

            m_precache.assert_called_once_with()
