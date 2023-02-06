# -*- coding: utf-8 -*-

import pytest
from lxml import etree

from common.models.geo import Settlement, CityMajority
from travel.rasp.admin.importinfo.afdbchanges.actions.www_settlement import SIMPLE_SETTLEMENT_FIELDS_AND_EXAMPLES
from travel.rasp.admin.importinfo.afdbchanges.utils import run_actions
from tester.factories import create_settlement, create_country, create_region, create_district


def make_change_tree(change_type, **kwargs):
    kwargs = {k: unicode(v) for k, v in kwargs.items()}
    kwargs['type'] = change_type
    kwargs['model'] = 'www.settlement'
    root = etree.Element('dbchanges')
    etree.SubElement(root, 'dbchange', attrib=kwargs)

    return root


@pytest.mark.dbuser
def test_change_settlement_title_ok():
    create_settlement(id=10000, title=u'Старое название')
    run_actions(make_change_tree('title', object_id=10000, value=u'Новое название'))
    assert Settlement.objects.get(pk=10000).title == u'Новое название'


@pytest.mark.dbuser
def test_change_settlement_title_not_found_settlement():
    create_settlement(id=10001, title=u'Старое название')
    run_actions(make_change_tree('title', object_id=10000, value=u'Новое название'))
    assert Settlement.objects.get(pk=10001).title == u'Старое название'


@pytest.mark.dbuser
@pytest.mark.parametrize('field_name', [x[0] for x in SIMPLE_SETTLEMENT_FIELDS_AND_EXAMPLES])
def test_change_settlement_simple_field_names(field_name):
    create_settlement(id=10000, **{field_name: u'Старое значение'})
    run_actions(make_change_tree(field_name, object_id=10000, value=u'Новое значение'))
    assert getattr(Settlement.objects.get(pk=10000), field_name) == u'Новое значение'


@pytest.mark.dbuser
def test_change_settlement_title_ru_ok():
    create_settlement(id=10000, title=u'Старое название',
                      title_ru=u'Старое название',
                      title_ru_preposition_v_vo_na=u'xx',
                      title_ru_genitive=u'xxx',
                      title_ru_accusative=u'xxx',
                      title_ru_locative=u'xxx')
    run_actions(make_change_tree('title_ru', object_id=10000, value=u'Новое название'))

    settlement = Settlement.objects.get(pk=10000)
    assert settlement.title == u'Старое название'
    assert settlement.title_ru == u'Новое название'
    assert settlement.title_ru_preposition_v_vo_na == u''
    assert settlement.title_ru_genitive == u''
    assert settlement.title_ru_accusative == u''
    assert settlement.title_ru_locative == u''


@pytest.mark.dbuser
def test_change_settlement_title_ru_title_not_changed():
    create_settlement(id=10000, title=u'Старое название',
                      title_ru=u'Название',
                      title_ru_preposition_v_vo_na=u'xx',
                      title_ru_genitive=u'xxx',
                      title_ru_accusative=u'xxx',
                      title_ru_locative=u'xxx')
    run_actions(make_change_tree('title_ru', object_id=10000, value=u'Название'))

    settlement = Settlement.objects.get(pk=10000)
    assert settlement.title == u'Старое название'
    assert settlement.title_ru == u'Название'
    assert settlement.title_ru_preposition_v_vo_na == u'xx'
    assert settlement.title_ru_genitive == u'xxx'
    assert settlement.title_ru_accusative == u'xxx'
    assert settlement.title_ru_locative == u'xxx'


@pytest.mark.dbuser
def test_change_settlement_title_ru_not_found_settlement():
    create_settlement(id=10001, title_ru=u'Старое название')
    run_actions(make_change_tree('title_ru', object_id=10000, value=u'Новое название'))
    assert Settlement.objects.get(pk=10001).title_ru == u'Старое название'


@pytest.mark.dbuser
def test_change_settlement_majority():
    create_settlement(id=10000, majority=CityMajority.POPULATION_MILLION_ID)
    run_actions(make_change_tree('majority', object_id=10000, value=CityMajority.CAPITAL_ID))

    assert Settlement.objects.get(pk=10000).majority.id == CityMajority.CAPITAL_ID


@pytest.mark.dbuser
def test_change_settlement_majority_not_found():
    create_settlement(id=10000, majority=CityMajority.POPULATION_MILLION_ID)
    run_actions(make_change_tree('majority', object_id=10000, value='asdf'))
    run_actions(make_change_tree('majority', object_id=10000, value=''))

    assert Settlement.objects.get(pk=10000).majority.id == CityMajority.POPULATION_MILLION_ID


@pytest.mark.dbuser
def test_change_settlement_country():
    create_country(id=10000)
    create_country(id=20000)
    create_settlement(id=10000, country=10000)
    run_actions(make_change_tree('country', object_id=10000, value=20000))

    assert Settlement.objects.get(pk=10000).country.id == 20000


@pytest.mark.dbuser
def test_change_settlement_country_not_found():
    create_country(id=10000)
    create_settlement(id=10000, country=10000)
    run_actions(make_change_tree('country', object_id=10000, value='20000'))
    run_actions(make_change_tree('country', object_id=10000, value='asdf'))
    run_actions(make_change_tree('country', object_id=10000, value=''))

    assert Settlement.objects.get(pk=10000).country.id == 10000


@pytest.mark.dbuser
def test_change_settlement_region():
    create_region(id=10000)
    create_region(id=20000)
    create_settlement(id=10000, region=10000)
    run_actions(make_change_tree('region', object_id=10000, value=20000))

    assert Settlement.objects.get(pk=10000).region.id == 20000

    run_actions(make_change_tree('region', object_id=10000, value=u''))
    assert Settlement.objects.get(pk=10000).region is None


@pytest.mark.dbuser
def test_change_settlement_region_not_found():
    create_region(id=10000)
    create_settlement(id=10000, region=10000)
    run_actions(make_change_tree('region', object_id=10000, value='20000'))
    run_actions(make_change_tree('region', object_id=10000, value='asdf'))

    assert Settlement.objects.get(pk=10000).region.id == 10000


@pytest.mark.dbuser
def test_change_settlement_district():
    create_district(id=10000)
    create_district(id=20000)
    create_settlement(id=10000, district=10000)
    run_actions(make_change_tree('district', object_id=10000, value=20000))

    assert Settlement.objects.get(pk=10000).district.id == 20000

    run_actions(make_change_tree('district', object_id=10000, value=u''))
    assert Settlement.objects.get(pk=10000).district is None


@pytest.mark.dbuser
def test_change_settlement_district_not_found():
    create_district(id=10000)
    create_settlement(id=10000, district=10000)
    run_actions(make_change_tree('district', object_id=10000, value='20000'))
    run_actions(make_change_tree('district', object_id=10000, value='asdf'))

    assert Settlement.objects.get(pk=10000).district.id == 10000


@pytest.mark.dbuser
def test_change_settlement_timezone():
    create_settlement(id=10000, time_zone=u'Asia/Yekaterinburg')
    run_actions(make_change_tree('timezone', object_id=10000, value=u'Europe/Moscow'))

    assert Settlement.objects.get(pk=10000).time_zone == u'Europe/Moscow'


@pytest.mark.dbuser
def test_change_settlement_timezone_not_found():
    create_settlement(id=10000, time_zone=u'Asia/Yekaterinburg')

    run_actions(make_change_tree('timezone', object_id=10000, value=u''))
    assert Settlement.objects.get(pk=10000).time_zone == u'Asia/Yekaterinburg'

    run_actions(make_change_tree('timezone', object_id=10000, value=u'222'))
    assert Settlement.objects.get(pk=10000).time_zone == u'Asia/Yekaterinburg'


@pytest.mark.dbuser
def test_change_settlement_geolocation():
    create_settlement(id=10000, latitude=50, longitude=60)
    run_actions(make_change_tree('geolocation', object_id=10000, lat="50.20", lng="60.30"))

    settlement = Settlement.objects.get(pk=10000)
    assert settlement.latitude == 50.20
    assert settlement.longitude == 60.30


@pytest.mark.dbuser
def test_change_settlement_bad_geolocation():
    create_settlement(id=10000, latitude=50, longitude=60)
    run_actions(make_change_tree('geolocation', object_id=10000, lat="50.20a", lng="60.30"))
    run_actions(make_change_tree('geolocation', object_id=10000, lat="50.20", lng="sss"))
    run_actions(make_change_tree('geolocation', object_id=10000, lat="50.20a", lng=""))
    run_actions(make_change_tree('geolocation', object_id=10000, lat="50.20", lng="700"))

    settlement = Settlement.objects.get(pk=10000)
    assert settlement.latitude == 50
    assert settlement.longitude == 60
