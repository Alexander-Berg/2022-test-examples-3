# coding: utf8

import mock
import pytest

from common.apps.facility.factories import create_suburban_facility
from common.models.transport import TransportType, TransportSubtype
from common.models.schedule import RThread

from common.tester.factories import create_transport_subtype, create_transport_subtype_color
from common.tester.utils.replace_setting import replace_setting

from travel.rasp.export.export.v3.core.helpers import get_transport_type, get_days_and_except_texts, set_key,  get_facilities_list
from travel.rasp.export.tests.v3.factories import create_thread


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestFillTransportType(object):
    def test_valid(self):
        thread = create_thread(express_type='not_express')
        assert get_transport_type(thread) == {}

        thread = create_thread(express_type='aeroexpress')
        transport_data = get_transport_type(thread)
        assert transport_data == {'express_type': 'aeroexpress'}

        thread = create_thread(express_type='express')
        transport_data = get_transport_type(thread)
        assert transport_data == {'express_type': 'express'}

        color = create_transport_subtype_color(code=0, color='#ABCDEF')
        thread = create_thread(
            t_subtype=create_transport_subtype(t_type_id=TransportType.PLANE_ID, code='bla', title_ru=u'Бла', color=color)
        )
        transport_data = get_transport_type(thread)
        assert transport_data == {'subtype': {'code': 'bla',
                                              'title': u'Бла',
                                              'color': '#ABCDEF'}}

        # при невыставленном цвете отдаем пустой цвет
        thread.t_subtype.color = None
        transport_data = get_transport_type(thread)
        assert transport_data['subtype']['color'] is None

        # если подтип - дефолтный для suburban, то не отдаем его
        thread.t_subtype = TransportSubtype.objects.get(id=TransportSubtype.SUBURBAN_ID)
        transport_data = get_transport_type(thread)
        assert transport_data == {}


class TestFillDaysAndExceptTexts(object):
    def test_valid(self):
        m_days_text = mock.Mock(side_effect=[
            {
                'days_text': 'stay awhile',
                'except_days_text': 'and listen',
            },
            {
                'days_text': 'stay awhile2',
            }
        ])

        thread = create_thread()
        today, shift, next_plan = 'a', 'b', 'c'

        with mock.patch.object(RThread, 'L_days_text_dict', m_days_text):
            days_text, except_text = get_days_and_except_texts(today, thread, shift, next_plan)
            assert len(m_days_text.call_args_list) == 1
            assert m_days_text.call_args_list[0][1] == {
                'shift': shift,
                'thread_start_date': today,
                'next_plan': next_plan,
                'show_days': True,
            }
            assert days_text == 'stay awhile'
            assert except_text == 'and listen'

            days_text, except_text = get_days_and_except_texts(today, thread, shift, next_plan)
            assert days_text == 'stay awhile2'
            assert except_text is None


class TestSetKey(object):
    def test_set_key(self):
        def get_test_data():
            return {'k1': 'v1'}

        data = get_test_data()
        set_key(data, 'k2', 'v2')
        assert data == {'k1': 'v1',
                        'k2': 'v2'}

        data = get_test_data()
        set_key(data, 'k2', None)
        assert data == {'k1': 'v1'}

        data = get_test_data()
        set_key(data, 'k2', None, False)
        assert data == {'k1': 'v1',
                        'k2': None}

        data = get_test_data()
        set_key(data, 'k2', '', False)
        assert data == {'k1': 'v1',
                        'k2': ''}

        data = get_test_data()
        set_key(data, 'k2', '', True)
        assert data == {'k1': 'v1'}

        # не пропускаем 0, т.к. не считаем его "пустым" значением
        data = get_test_data()
        set_key(data, 'k2', 0)
        assert data == {'k1': 'v1', 'k2': 0}


class TestAddFacilitiesData(object):

    @replace_setting('MEDIA_URL', 'https://static/')
    def test_valid(self):
        lezhanki_facility = create_suburban_facility(title_ru=u'Лежанки', code='lezhanki')
        spalniki_facility = create_suburban_facility(title_ru=u'Спальники', code='spalniki')
        spalniki_facility.icon.name = u'some/file/path.svg'
        spalniki_facility.save()

        facilities = get_facilities_list([lezhanki_facility, spalniki_facility])

        assert len(facilities) == 2
        assert facilities[0] == {'title': u'Лежанки',
                                 'code': u'lezhanki',
                                 'icon': None}
        assert facilities[1] == {'title': u'Спальники',
                                 'icon': u'https://static/some/file/path.svg',
                                 'code': u'spalniki'}
