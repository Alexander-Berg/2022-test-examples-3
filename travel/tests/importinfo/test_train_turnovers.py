# coding: utf8

from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from datetime import date

import mock
from hamcrest import assert_that, has_properties
from xml.etree import ElementTree

from common.models.geo import CodeSystem
from common.models.schedule import TrainTurnover
from common.tester.factories import create_station_code, create_train_schedule_plan, create_station
from common.utils.date import RunMask
from common.utils.calendar_matcher import YCalendar

from travel.rasp.admin.importinfo.turnovers import load_turnovers_from_file, parse_and_save_turnover


create_station = create_station.mutate(t_type="suburban")


load_xml = """
<?xml version="1.0" encoding="Windows-1251"?>
<turnovers>
  <train_turnover graph_code="plan_code" station_esr="111" number_before="number_1" number_after="number_2"
   template_code="-123" start_date="2017-09-05" end_date="2017-09-28"/>

  <train_turnover graph_code="plan_code" station_esr="222" number_before="number_3" number_after="number_4"
  template_code="1234" start_date="2017-09-01" end_date="2017-09-17"/>
</turnovers>
""".strip().encode('cp1251')


changes_xml = """
<?xml version="1.0" encoding="Windows-1251"?>
<turnovers>
  <train_turnover graph_code="plan_code" station_esr="111" number_before="number_1" number_after="number_7"
  template_code="-123" start_date="2017-09-05" end_date="2017-09-28" mode='change'/>

  <train_turnover graph_code="plan_code" station_esr="222" number_before="number_3" number_after="number_4"
   template_code="1234" start_date="2017-09-01" end_date="2017-09-17" mode='delete'/>
</turnovers>
""".strip().encode('cp1251')


load_2_xml = """
<?xml version="1.0" encoding="Windows-1251"?>
<turnovers>
  <train_turnover graph_code="plan_code" station_esr="222" number_before="number_11" number_after="number_12"
   template_code="1234567" start_date="2017-10-3" end_date="2017-10-8"/>
</turnovers>
""".strip().encode('cp1251')


@pytest.mark.dbuser
def test_load_turnovers_from_file():
    station_1, station_2 = create_station(), create_station()

    esr_code_system = CodeSystem.objects.get(code='esr')
    create_station_code(station=station_1, system=esr_code_system, code='111')
    create_station_code(station=station_2, system=esr_code_system, code='222')
    plan = create_train_schedule_plan(code='plan_code')

    # Создаем два оборота.
    load_turnovers_from_file(load_xml, 'test_load.xml')
    assert TrainTurnover.objects.count() == 2
    turnover_station_1 = TrainTurnover.objects.get(station=station_1)
    turnover_station_2 = TrainTurnover.objects.get(station=station_2)

    assert_that(turnover_station_1,
                has_properties({
                    'station': station_1,
                    'graph': plan,
                    'number_before': 'number_1',
                    'number_after': 'number_2',
                    'template_code': '-123',
                    'start_date': date(2017, 9, 5),
                    'end_date': date(2017, 9, 28)
                }))

    assert_that(turnover_station_2,
                has_properties({
                    'station': station_2,
                    'graph': plan,
                    'number_before': 'number_3',
                    'number_after': 'number_4',
                    'template_code': '1234',
                    'start_date': date(2017, 9, 1),
                    'end_date': date(2017, 9, 17)
                }))

    # Изменяем созданные обороты.
    load_turnovers_from_file(changes_xml, 'test_changes.xml')
    # Один оборот должен был удалиться.
    # Во втором должен поменяться номер поезда после оборота.
    assert TrainTurnover.objects.count() == 1
    turnover_station_1 = TrainTurnover.objects.get(station=station_1)

    assert_that(turnover_station_1,
                has_properties({
                    'number_after': "number_7"
                }))

    # Загружаем новый файл оборота.
    # Должен добавиться еще один оборот на станции esr=222.
    load_turnovers_from_file(load_2_xml, 'test_load_2.xml')
    assert TrainTurnover.objects.count() == 2
    turnover_station_1 = TrainTurnover.objects.get(station=station_2)

    assert_that(turnover_station_1,
                has_properties({
                    'station': station_2,
                    'graph': plan,
                    'number_before': 'number_11',
                    'number_after': 'number_12',
                    'template_code': '1234567',
                    'start_date': date(2017, 10, 3),
                    'end_date': date(2017, 10, 8)
                }))


@pytest.mark.dbuser
def test_parse_and_save_turnover():
    station_1 = create_station()
    esr_code_system = CodeSystem.objects.get(code='esr')
    create_station_code(station=station_1, system=esr_code_system, code='111')
    plan = create_train_schedule_plan(code='plan_code')
    attrib = {
        'station_esr': '111',
        'graph_code': 'plan_code',
        'number_before': 'number_1',
        'number_after': 'number_2',
        'template_code': '1234',
        'start_date': '2017-09-10',
        'end_date': '2017-09-15'
    }
    xml_elem = ElementTree.Element('train_turnover', attrib=attrib)

    # Создаем оборот.
    parse_and_save_turnover(xml_elem)
    assert TrainTurnover.objects.count() == 1
    turnover_station_1 = TrainTurnover.objects.get(station=station_1)

    assert_that(turnover_station_1,
                has_properties({
                    'station': station_1,
                    'graph': plan,
                    'number_before': 'number_1',
                    'number_after': 'number_2',
                    'template_code': '1234',
                    'start_date': date(2017, 9, 10),
                    'end_date': date(2017, 9, 15)
                }))
    mask = RunMask(turnover_station_1.year_days, date(2017, 9, 5))
    assert mask.dates() == [
        date(2017, 9, 11),
        date(2017, 9, 12),
        date(2017, 9, 13),
        date(2017, 9, 14)
    ]

    # Меняем оборот.
    attrib.update({
        'number_after': 'number_12',
        'end_date': '2017-10-10',
        'mode': 'change'
    })
    xml_elem = ElementTree.Element('train_turnover', attrib=attrib)
    parse_and_save_turnover(xml_elem)
    assert TrainTurnover.objects.count() == 1
    turnover_station_1 = TrainTurnover.objects.get(station=station_1)

    assert_that(turnover_station_1,
                has_properties({
                    'number_after': 'number_12',
                    'end_date': date(2017, 10, 10)
                }))

    # Удаляем оборот.
    attrib.update({'mode': 'delete'})
    xml_elem = ElementTree.Element('train_turnover', attrib=attrib)
    parse_and_save_turnover(xml_elem)
    assert TrainTurnover.objects.count() == 0

    # Добавляем через изменение
    attrib.update({
        'mode': 'change',
        'template_code': 'H'
    })
    xml_elem = ElementTree.Element('train_turnover', attrib=attrib)
    with mock.patch.object(YCalendar, 'get_weekends') as m_get_weekends:
        m_get_weekends.return_value = [date(2017, 9, 10)]
        parse_and_save_turnover(xml_elem)
        assert TrainTurnover.objects.count() == 1
        turnover_station_1 = TrainTurnover.objects.get(station=station_1)
        mask = RunMask(turnover_station_1.year_days, date(2017, 9, 5))
        assert mask.dates() == [date(2017, 9, 10)]

        # Пытаемся создать уже существующий оборот.
        # Оборот должен обновиться.
        attrib.pop('mode')
        attrib.update({'number_after': 'number_121'})
        xml_elem = ElementTree.Element('train_turnover', attrib=attrib)
        parse_and_save_turnover(xml_elem)
        turnover_station_1 = TrainTurnover.objects.get(station=station_1)
        assert_that(turnover_station_1,
                    has_properties({
                        'number_after': 'number_121',
                    }))
