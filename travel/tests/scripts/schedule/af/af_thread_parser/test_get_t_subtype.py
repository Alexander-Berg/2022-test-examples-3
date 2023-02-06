# coding: utf-8

from __future__ import unicode_literals

from xml.etree import ElementTree

import pytest

from common.models.transport import TransportType, TransportSubtype
from travel.rasp.admin.scripts.schedule.af_processors.utils import parse_thread
from tester.factories import create_station, create_transport_subtype
from tester.transaction_context import transaction_fixture


STRIZH_ID = 33333


@pytest.fixture(autouse=True)
@transaction_fixture
def db_fixtures(request):
    create_station(__={'codes': {'esr': '100001'}}, t_type=TransportType.TRAIN_ID,
                   title='Откуда', title_ru='Откуда', title_uk='УкОткуда')
    create_station(__={'codes': {'esr': '100002'}}, t_type=TransportType.TRAIN_ID,
                   title='Куда', title_ru='Куда', title_uk='УкКуда')
    create_transport_subtype(id=STRIZH_ID, t_type=TransportType.SUBURBAN_ID, code='strizh')


@pytest.mark.dbuser
@pytest.mark.parametrize('t_type_code, t_subtype_code, result_t_subtype_id', [
    ('train', '', TransportSubtype.TRAIN_ID),
    ('plane', '', TransportSubtype.PLANE_ID),
    ('bus', '', TransportSubtype.BUS_ID),
    ('suburban', '', TransportSubtype.SUBURBAN_ID),
    ('suburban', 'suburban', TransportSubtype.SUBURBAN_ID),
    ('suburban', 'strizh', STRIZH_ID),
    ('suburban', 'strizh2', None),
])
def test_filling_default_subtype(t_type_code, t_subtype_code, result_t_subtype_id):
    thread_el = ElementTree.fromstring("""
    <thread t_type="{t_type_code}" number="3333" startstationparent="000001" express_subtype="{t_subtype_code}"
            dates="2015-06-25" changemode="insert">
      <stations>
        <station esrcode="100001" stname="1"
                 arrival_time="" stop_time=""
                 departure_time="12:00" minutes_from_start="0" in_station_schedule="0" is_searchable_to="0" />
        <station esrcode="100002" stname="2"
                 arrival_time="12:40" stop_time="10"
                 departure_time="12:50" minutes_from_start="40" in_station_schedule="1" />
      </stations>
    </thread>""".format(t_type_code=t_type_code, t_subtype_code=t_subtype_code))

    thread = parse_thread(thread_el)
    assert thread.t_subtype_id == result_t_subtype_id


