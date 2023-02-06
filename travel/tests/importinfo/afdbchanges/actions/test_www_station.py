# -*- coding: utf-8 -*-

import pytest
from lxml import etree

from common.models.geo import Station, StationType, StationMajority
from travel.rasp.admin.importinfo.afdbchanges.utils import run_actions
from tester.factories import create_station, create_country, create_region, create_district, create_settlement


def make_change_tree(change_type, **kwargs):
    kwargs = {k: unicode(v) for k, v in kwargs.items()}
    kwargs['type'] = change_type
    kwargs['model'] = 'www.station'
    root = etree.Element('dbchanges')
    etree.SubElement(root, 'dbchange', attrib=kwargs)

    return root


@pytest.mark.dbuser
def test_change_station_title_ok():
    station = create_station(title=u'Старое название')
    run_actions(make_change_tree('title', object_id=station.id, value=u'Новое название'))
    assert Station.objects.get(pk=station.id).title == u'Новое название'


@pytest.mark.dbuser
def test_change_station_title_not_found_station():
    create_station(id=10001, title=u'Старое название')
    run_actions(make_change_tree('title', object_id=10000, value=u'Новое название'))
    assert Station.objects.get(pk=10001).title == u'Старое название'


@pytest.mark.dbuser
@pytest.mark.parametrize('field_name', ['title', 'title_uk', 'title_en'])
def test_change_station_simple_field_names(field_name):
    station = create_station({field_name: u'Старое значение'})
    run_actions(make_change_tree(field_name, object_id=station.id, value=u'Новое значение'))
    assert getattr(Station.objects.get(pk=station.id), field_name) == u'Новое значение'


@pytest.mark.dbuser
def test_change_station_title_ru_ok():
    station = create_station(title=u'Старое название',
                             title_ru=u'Старое название',
                             title_ru_preposition_v_vo_na=u'xx',
                             title_ru_genitive=u'xxx',
                             title_ru_accusative=u'xxx',
                             title_ru_locative=u'xxx')
    run_actions(make_change_tree('title_ru', object_id=station.id, value=u'Новое название'))

    station = Station.objects.get(pk=station.id)
    assert station.title == u'Старое название'
    assert station.title_ru == u'Новое название'
    assert station.title_ru_preposition_v_vo_na == u''
    assert station.title_ru_genitive == u''
    assert station.title_ru_accusative == u''
    assert station.title_ru_locative == u''


@pytest.mark.dbuser
def test_change_station_title_ru_title_not_changed():
    station = create_station(title=u'Старое название',
                             title_ru=u'Название',
                             title_ru_preposition_v_vo_na=u'xx',
                             title_ru_genitive=u'xxx',
                             title_ru_accusative=u'xxx',
                             title_ru_locative=u'xxx')
    run_actions(make_change_tree('title_ru', object_id=station.id, value=u'Название'))

    station = Station.objects.get(pk=station.id)
    assert station.title == u'Старое название'
    assert station.title_ru == u'Название'
    assert station.title_ru_preposition_v_vo_na == u'xx'
    assert station.title_ru_genitive == u'xxx'
    assert station.title_ru_accusative == u'xxx'
    assert station.title_ru_locative == u'xxx'


@pytest.mark.dbuser
def test_change_station_title_ru_not_found_station():
    create_station(id=10001, title_ru=u'Старое название')
    run_actions(make_change_tree('title_ru', object_id=10000, value=u'Новое название'))
    assert Station.objects.get(pk=10001).title_ru == u'Старое название'


@pytest.mark.dbuser
def test_change_station_type():
    station = create_station(station_type=StationType.STATION_ID)
    run_actions(make_change_tree('station_type', object_id=station.id, value=StationType.BUS_STOP_ID))

    assert Station.objects.get(pk=station.id).station_type.id == StationType.BUS_STOP_ID


@pytest.mark.dbuser
def test_change_station_type_not_found():
    station = create_station(station_type=StationType.STATION_ID)
    run_actions(make_change_tree('station_type', object_id=station.id, value='asdf'))
    run_actions(make_change_tree('station_type', object_id=station.id, value=''))

    assert Station.objects.get(pk=station.id).station_type.id == StationType.STATION_ID


@pytest.mark.dbuser
def test_change_station_country():
    country_1 = create_country()
    country_2 = create_country()
    station = create_station(country=country_1)
    run_actions(make_change_tree('country', object_id=station.id, value=country_2.id))

    assert Station.objects.get(pk=station.id).country == country_2


@pytest.mark.dbuser
def test_change_station_country_not_found():
    country = create_country()
    station = create_station(country=country)
    run_actions(make_change_tree('country', object_id=station.id, value='20000'))
    run_actions(make_change_tree('country', object_id=station.id, value='asdf'))

    assert Station.objects.get(pk=station.id).country == country


@pytest.mark.dbuser
def test_change_station_region():
    region_1 = create_region()
    region_2 = create_region()
    station = create_station(region=region_1)
    run_actions(make_change_tree('region', object_id=station.id, value=region_2.id))

    assert Station.objects.get(pk=station.id).region == region_2

    run_actions(make_change_tree('region', object_id=station.id, value=u''))
    assert Station.objects.get(pk=station.id).region is None


@pytest.mark.dbuser
def test_change_station_region_not_found():
    region = create_region()
    station = create_station(region=region)
    run_actions(make_change_tree('region', object_id=station.id, value='20000'))
    run_actions(make_change_tree('region', object_id=station.id, value='asdf'))

    assert Station.objects.get(pk=station.id).region == region


@pytest.mark.dbuser
def test_change_station_district():
    district_1 = create_district()
    district_2 = create_district()
    station = create_station(district=district_1)
    run_actions(make_change_tree('district', object_id=station.id, value=district_2.id))

    assert Station.objects.get(pk=station.id).district == district_2

    run_actions(make_change_tree('district', object_id=station.id, value=u''))
    assert Station.objects.get(pk=station.id).district is None


@pytest.mark.dbuser
def test_change_station_district_not_found():
    district = create_district()
    station = create_station(district=district)
    run_actions(make_change_tree('district', object_id=station.id, value='20000'))
    run_actions(make_change_tree('district', object_id=station.id, value='asdf'))

    assert Station.objects.get(pk=station.id).district == district


@pytest.mark.dbuser
def test_change_station_timezone():
    station = create_station(time_zone=u'Asia/Yekaterinburg')
    run_actions(make_change_tree('timezone', object_id=station.id, value=u'Europe/Moscow'))

    assert Station.objects.get(pk=station.id).time_zone == u'Europe/Moscow'


@pytest.mark.dbuser
def test_change_station_timezone_not_found():
    station = create_station(time_zone=u'Asia/Yekaterinburg')

    run_actions(make_change_tree('timezone', object_id=station.id, value=u''))
    assert Station.objects.get(pk=station.id).time_zone == u'Asia/Yekaterinburg'

    run_actions(make_change_tree('timezone', object_id=station.id, value=u'222'))
    assert Station.objects.get(pk=station.id).time_zone == u'Asia/Yekaterinburg'


@pytest.mark.dbuser
def test_change_station_geolocation():
    station = create_station(latitude=50, longitude=60)
    run_actions(make_change_tree('geolocation', object_id=station.id, lat="50.20", lng="60.30"))

    station = Station.objects.get(pk=station.id)
    assert station.latitude == 50.20
    assert station.longitude == 60.30


@pytest.mark.dbuser
def test_change_station_bad_geolocation():
    station = create_station(latitude=50, longitude=60)
    run_actions(make_change_tree('geolocation', object_id=station.id, lat="50.20a", lng="60.30"))
    run_actions(make_change_tree('geolocation', object_id=station.id, lat="50.20", lng="sss"))
    run_actions(make_change_tree('geolocation', object_id=station.id, lat="50.20a", lng=""))
    run_actions(make_change_tree('geolocation', object_id=station.id, lat="50.20", lng="700"))

    station = Station.objects.get(pk=station.id)
    assert station.latitude == 50
    assert station.longitude == 60


@pytest.mark.dbuser
def test_change_station_settlement():
    settlement_1 = create_settlement()
    settlement_2 = create_settlement()
    station = create_station(settlement=settlement_1)

    run_actions(make_change_tree('settlement', object_id=station.id, value=settlement_2.id))
    assert Station.objects.get(pk=station.id).settlement == settlement_2

    run_actions(make_change_tree('settlement', object_id=station.id, value=u''))
    assert Station.objects.get(pk=station.id).settlement is None


@pytest.mark.dbuser
def test_change_station_majority():
    station = create_station(majority=StationMajority.MAIN_IN_CITY_ID)
    run_actions(make_change_tree('majority', object_id=station.id, value=StationMajority.NOT_IN_TABLO_ID))

    assert Station.objects.get(pk=station.id).majority.id == StationMajority.NOT_IN_TABLO_ID


@pytest.mark.dbuser
def test_change_station_code():
    station = create_station()

    run_actions(make_change_tree('code', object_id=station.id, system='iata', value='AAA'))
    assert station.get_code('iata') == 'AAA'

    run_actions(make_change_tree('code', object_id=station.id, system='iata', value='BBB'))
    assert station.get_code('iata') == 'BBB'


@pytest.mark.dbuser
def test_change_station_code_delete():
    station = create_station(__={'codes': {'iata': 'AAA'}})

    run_actions(make_change_tree('code', object_id=station.id, system='iata', value=''))
    assert station.get_code('iata') is None

    station.set_code('iata', 'AAA')
    run_actions(make_change_tree('code', object_id=station.id, system='iata'))
    assert station.get_code('iata') is None


@pytest.mark.dbuser
def test_change_station_code_bad_system():
    station = create_station(__={'codes': {'iata': 'AAA'}})

    run_actions(make_change_tree('code', object_id=station.id, system='iatasss', value='BBB'))
    run_actions(make_change_tree('code', object_id=station.id, system='', value='BBB'))
    run_actions(make_change_tree('code', object_id=station.id, value='BBB'))
    assert station.get_code('iata') == 'AAA'
