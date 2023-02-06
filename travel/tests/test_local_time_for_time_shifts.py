# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from django.core.files.base import ContentFile

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from tester.factories import create_supplier, create_station
from tester.utils.datetime import replace_now


pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def _get_xml_by_shift(channel_timezone, group_number, thread_number):
    return """
    	<?xml version='1.0' encoding='utf-8'?>
    	<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="{0}">
    	    <group code="{1}">
    	        <stations>
    	            <station code="1" title="Екатеринбург"/>
    	            <station code="2" title="Уфа"/>
    	            <station code="3" title="Самара"/>
    	            <station code="4" title="Ижевск"/>
    	            <station code="5" title="Казань"/>
    	            <station code="6" title="Москва"/>
    	        </stations>
    	        <threads>
    	            <thread title="R-F" t_type="bus" number="{2}">
    	              <stoppoints>
    	                <stoppoint station_code="1" departure_shift="0"/>
    	                <stoppoint station_code="2" arrival_shift="3300" departure_shift="3600"/>
    	                <stoppoint station_code="3" arrival_shift="10500"/>
    	                <stoppoint station_code="4" departure_shift="14400"/>
    	                <stoppoint station_code="5" arrival_shift="21300" departure_shift="21600"/>
    	                <stoppoint station_code="6" arrival_shift="25200"/>
    	              </stoppoints>
    	              <schedules>
    	                <schedule times="10:00" timezone="{0}" period_start_date="2019-11-01" period_end_date="2019-12-31" days="1234567"/>
    	              </schedules>
    	            </thread>
    	        </threads>
    	    </group>
    	</channel>
    	            """.format(channel_timezone, group_number, thread_number)


def _get_xml_by_time(channel_timezone, group_number, thread_number):
    return """
    	<?xml version='1.0' encoding='utf-8'?>
    	<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="{0}">
    	    <group code="{1}">
    	        <stations>
    	            <station code="1" title="Екатеринбург"/>
    	            <station code="2" title="Уфа"/>
    	            <station code="3" title="Самара"/>
    	            <station code="4" title="Ижевск"/>
    	            <station code="5" title="Казань"/>
    	            <station code="6" title="Москва"/>
    	        </stations>
    	        <threads>
    	            <thread title="R-F" t_type="bus" number="{2}">
    	              <stoppoints>
    	                <stoppoint station_code="1" departure_time="10:00"/>
    	                <stoppoint station_code="2" arrival_time="10:55" departure_time="11:00"/>
    	                <stoppoint station_code="3" arrival_time="12:55"/>
    	                <stoppoint station_code="4" departure_time="14:00"/>
    	                <stoppoint station_code="5" arrival_time="15:55" departure_time="16:00"/>
    	                <stoppoint station_code="6" arrival_time="17:00"/>
    	              </stoppoints>
    	              <schedules>
    	                <schedule times="10:00" timezone="{0}" period_start_date="2019-11-01" period_end_date="2019-12-31" days="1234567"/>
    	              </schedules>
    	            </thread>
    	        </threads>
    	    </group>
    	</channel>
    	            """.format(channel_timezone, group_number, thread_number)


def _make_rtstations(use_shift, channel_timezone, group_number, thread_number):
    supplier = create_supplier()

    ekb = create_station(time_zone='Etc/GMT-5', title='Екатеринбург', settlement=None)
    ufa = create_station(time_zone='Etc/GMT-5', title='Уфа', settlement=None)
    samara = create_station(time_zone='Etc/GMT-4', title='Самара', settlement=None)
    igevsk = create_station(time_zone='Etc/GMT-4', title='Ижевск', settlement=None)
    kazan = create_station(time_zone='Etc/GMT-3', title='Казань', settlement=None)
    moscow = create_station(time_zone='Etc/GMT-3', title='Москва', settlement=None)

    StationMapping.objects.create(
        supplier=supplier, station=ekb, code='{}_vendor_1'.format(group_number), title='Екатеринбург'
    )
    StationMapping.objects.create(
        supplier=supplier, station=ufa, code='{}_vendor_2'.format(group_number), title='Уфа'
    )
    StationMapping.objects.create(
        supplier=supplier, station=samara, code='{}_vendor_3'.format(group_number), title='Самара'
    )
    StationMapping.objects.create(
        supplier=supplier, station=igevsk, code='{}_vendor_4'.format(group_number), title='Ижевск'
    )
    StationMapping.objects.create(
        supplier=supplier, station=kazan, code='{}_vendor_5'.format(group_number), title='Казань'
    )
    StationMapping.objects.create(
        supplier=supplier, station=moscow, code='{}_vendor_6'.format(group_number), title='Москва'
    )

    xml_string = (
        _get_xml_by_shift(channel_timezone, group_number, thread_number)
        if use_shift
        else _get_xml_by_time(channel_timezone, group_number, thread_number)
    )

    package = create_tsi_package(supplier=supplier)
    package.package_file = ContentFile(name=u'cysix.xml', content=xml_string.strip())
    factory = package.get_two_stage_factory()
    importer = factory.get_two_stage_importer()
    importer.reimport_package()

    thread = RThread.objects.get(hidden_number=thread_number)
    return list(thread.path)


def _check_rtstation(rtstation, station_time_zone, rts_time_zone, tz_arrival, tz_departure):
    assert rtstation.station.time_zone == station_time_zone
    assert rtstation.time_zone == rts_time_zone
    assert rtstation.tz_arrival == tz_arrival
    assert rtstation.tz_departure == tz_departure


@replace_now(datetime(2019, 12, 1))
def test_local_time_for_rtstations():
    rtstations = _make_rtstations(True, 'local', '1', 'shift_local')

    _check_rtstation(rtstations[0], 'Etc/GMT-5', 'Etc/GMT-5', None, 0)
    _check_rtstation(rtstations[1], 'Etc/GMT-5', 'Etc/GMT-5', 55, 60)
    _check_rtstation(rtstations[2], 'Etc/GMT-4', 'Etc/GMT-4', 175, 176)
    _check_rtstation(rtstations[3], 'Etc/GMT-4', 'Etc/GMT-4', 239, 240)
    _check_rtstation(rtstations[4], 'Etc/GMT-3', 'Etc/GMT-3', 355, 360)
    _check_rtstation(rtstations[5], 'Etc/GMT-3', 'Etc/GMT-3', 420, None)

    rtstations = _make_rtstations(True, 'start_station', '2', 'shift_start')

    _check_rtstation(rtstations[0], 'Etc/GMT-5', 'Etc/GMT-5', None, 0)
    _check_rtstation(rtstations[1], 'Etc/GMT-5', 'Etc/GMT-5', 55, 60)
    _check_rtstation(rtstations[2], 'Etc/GMT-4', 'Etc/GMT-5', 175, 176)
    _check_rtstation(rtstations[3], 'Etc/GMT-4', 'Etc/GMT-5', 239, 240)
    _check_rtstation(rtstations[4], 'Etc/GMT-3', 'Etc/GMT-5', 355, 360)
    _check_rtstation(rtstations[5], 'Etc/GMT-3', 'Etc/GMT-5', 420, None)

    rtstations = _make_rtstations(False, 'local', '3', 'time_local')

    _check_rtstation(rtstations[0], 'Etc/GMT-5', 'Etc/GMT-5', None, 0)
    _check_rtstation(rtstations[1], 'Etc/GMT-5', 'Etc/GMT-5', 55, 60)
    _check_rtstation(rtstations[2], 'Etc/GMT-4', 'Etc/GMT-4', 175, 176)
    _check_rtstation(rtstations[3], 'Etc/GMT-4', 'Etc/GMT-4', 239, 240)
    _check_rtstation(rtstations[4], 'Etc/GMT-3', 'Etc/GMT-3', 355, 360)
    _check_rtstation(rtstations[5], 'Etc/GMT-3', 'Etc/GMT-3', 420, None)

    rtstations = _make_rtstations(False, 'start_station', '4', 'time_start')

    _check_rtstation(rtstations[0], 'Etc/GMT-5', 'Etc/GMT-5', None, 0)
    _check_rtstation(rtstations[1], 'Etc/GMT-5', 'Etc/GMT-5', 55, 60)
    _check_rtstation(rtstations[2], 'Etc/GMT-4', 'Etc/GMT-5', 175, 176)
    _check_rtstation(rtstations[3], 'Etc/GMT-4', 'Etc/GMT-5', 239, 240)
    _check_rtstation(rtstations[4], 'Etc/GMT-3', 'Etc/GMT-5', 355, 360)
    _check_rtstation(rtstations[5], 'Etc/GMT-3', 'Etc/GMT-5', 420, None)


@replace_now(datetime(2019, 12, 1))
def test_local_time_for_time_shifts():
    supplier = create_supplier()
    package = create_tsi_package(supplier=supplier)
    package.package_file = ContentFile(name=u'cysix.xml', content="""
        <?xml version='1.0' encoding='utf-8'?>
        <channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
            <group code="1">
                <stations>
                    <station code="1" title="Warsaw"/>
                    <station code="2" title="Belostok"/>
                    <station code="3" title="Grodno"/>
                    <station code="4" title="Minsk"/>
                </stations>
                <threads>
                    <thread title="R-F" t_type="bus" number="RF">
                      <stoppoints>
                        <stoppoint station_code="1" departure_shift="0"/>
                        <stoppoint station_code="2" arrival_shift="3300" departure_shift="3600"/>
                        <stoppoint station_code="3" arrival_shift="14100" departure_shift="14400"/>
                        <stoppoint station_code="4" arrival_shift="18000"/>
                      </stoppoints>
                      <schedules>
                        <schedule times="06:00" timezone="local" period_start_date="2019-11-01" period_end_date="2019-12-31" days="1234567"/>
                      </schedules>
                    </thread>
                </threads>
            </group>
        </channel>
                """.strip())

    warsaw = create_station(time_zone='Europe/Warsaw', title='Варшава', settlement=None)
    belostok = create_station(time_zone='Europe/Warsaw', title='Белосток', settlement=None)
    grodno = create_station(time_zone='Europe/Minsk', title='Гродно', settlement=None)
    minsk = create_station(time_zone='Europe/Minsk', title='Минск', settlement=None)

    StationMapping.objects.create(supplier=supplier, station=warsaw, code='1_vendor_1', title='Warsaw')
    StationMapping.objects.create(supplier=supplier, station=belostok, code='1_vendor_2', title='Belostok')
    StationMapping.objects.create(supplier=supplier, station=grodno, code='1_vendor_3', title='Grodno')
    StationMapping.objects.create(supplier=supplier, station=minsk, code='1_vendor_4', title='Minsk')

    factory = package.get_two_stage_factory()
    importer = factory.get_two_stage_importer()
    importer.reimport_package()

    thread = RThread.objects.get(hidden_number='RF')

    rtstations = list(thread.path)

    _check_rtstation(rtstations[0], 'Europe/Warsaw', 'Europe/Warsaw', None, 0)
    _check_rtstation(rtstations[1], 'Europe/Warsaw', 'Europe/Warsaw', 55, 60)
    _check_rtstation(rtstations[2], 'Europe/Minsk', 'Europe/Minsk', 235, 240)
    _check_rtstation(rtstations[3], 'Europe/Minsk', 'Europe/Minsk', 300, None)
