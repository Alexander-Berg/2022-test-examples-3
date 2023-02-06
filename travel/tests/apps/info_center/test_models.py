from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date

import pytest

from common.apps.info_center.models import Info, ObjectLink
from common.models.factories import create_teaser_page, create_info, create_external_direction
from common.models.teasers import Teaser
from common.tester.factories import create_station

pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


class TestInfo(object):
    def test_creation(self):
        info = Info(title='42', text='542')
        info.save()

        infos = list(Info.objects.filter())
        assert len(infos) == 1
        info = infos[0]
        assert info.title == '42'
        assert info.text == '542'
        assert info.id == 1
        assert info.info_url == 'info1'
        assert info.uuid is not None

    def test_to_old_teaser(self):
        info = Info(
            id=100500,
            title='42', text='t42', text_short='ts42', url='myurl', lang='uk', national_versions=['ua', 'ru'],
            info_type=Info.Type.AHTUNG, importance=43, services=[Info.Service.MOBILE_APPS, Info.Service.WEB],
            dt_from=datetime(2017, 11, 30), dt_to=datetime(2017, 12, 31)
        )
        info.save()

        info = Info.objects.get(id=100500)
        teaser = info.to_old_teaser()
        assert isinstance(teaser, Teaser)
        expected = dict(
            id=100500,
            title='42', content='t42', mobile_content='ts42', url='myurl', lang='uk', national_version='ua',
            mode=Info.Type.AHTUNG, importance=43, is_active_rasp=True, is_active_export=True,
            date_start=date(2017, 11, 30), date_finish=date(2017, 12, 31)
        )

        for field, value in expected.items():
            assert getattr(teaser, field) == value

    def test_get_object_by_link(self):
        page1, page2 = create_teaser_page(), create_teaser_page()
        station = create_station()

        info = create_info(stations=[station], pages=[page1, page2])

        link = info.linked_objects[0]
        assert Info.get_object_by_link(link[0]) == station
        assert Info.get_object_by_link(link[1]) == page1
        assert Info.get_object_by_link(link[2]) == page2

    def test_get_link_by_obj_id(self):
        page = create_teaser_page(code='code42')
        link = Info.get_link_by_obj_id('page', page.id)
        assert isinstance(link, ObjectLink)
        assert link.obj_type == 'page'
        assert link.obj_key == 'code42'

        direction = create_external_direction()
        link = Info.get_link_by_obj_id('externaldirection', direction.id)
        assert isinstance(link, ObjectLink)
        assert link.obj_type == 'externaldirection'
        assert link.obj_key == direction.id

    def test_get_linked_obj_title(self):
        page = create_teaser_page(title='MyPage')
        assert Info.get_linked_obj_title('page', page) == 'MyPage'

        station = create_station(title_ru='MyStation')
        assert Info.get_linked_obj_title('station', station) == 'MyStation'


class TestObjectLink(object):
    def test_eq(self):
        ol1 = ObjectLink(obj_type='settlement', obj_key=2)
        ol2 = ObjectLink(obj_type='settlement', obj_key=2)
        ol3 = ObjectLink(obj_type='settlement', obj_key=3)
        ol4 = ObjectLink(obj_type='page', obj_key=2)

        assert ol1 == ol2
        assert ol1 == ol1
        assert ol1 != ol3
        assert ol1 != ol4
