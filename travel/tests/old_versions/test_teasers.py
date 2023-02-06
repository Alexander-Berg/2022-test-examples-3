# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function
__metaclass__ = type

import pytest

from common.models.factories import create_teaser, create_station
from travel.rasp.export.export.teasers import teasers_to_xml, TeaserSetExport


@pytest.mark.dbuser
@pytest.mark.mongouser
class TestTeasers:
    def test_teasers_to_xml(self):
        station = create_station()
        _teasers = [create_teaser(mode='normal', importance=0, title='normal title', content='ok',
                                  mobile_content='mobile ok', is_active_export=True, stations=[station]),
                    create_teaser(mode='ahtung', importance=5, title='ahtung title', content='ahtung',
                                  mobile_content='mobile ahtung', is_active_export=True, stations=[station])]

        teaser_set = TeaserSetExport('info_station', station)
        teaser_xml = teasers_to_xml(teaser_set)

        teasers = teaser_xml.getchildren()
        assert len(teasers) == 2
        assert teasers[0].attrib['title'] == _teasers[1].title
        assert teasers[1].attrib['title'] == _teasers[0].title

        for i, teaser in enumerate(reversed(teasers)):
            for attr in ['url', 'selected', 'image_url', 'title']:
                assert teaser.attrib.get(attr) is not None

            content, m_content = teaser.getchildren()
            assert content.tag == 'content'
            assert content.text == _teasers[i].content
            assert m_content.tag == 'mobile_content'
            assert m_content.text == _teasers[i].mobile_content

        teaser_xml = teasers_to_xml(None)
        assert teaser_xml.tag == 'teasers'
        teasers = teaser_xml.getchildren()
        assert len(teasers) == 0

    def test_empty_mobile_content(self):
        station = create_station()
        create_teaser(content='cont_1', mobile_content='', is_active_export=True, stations=[station]),
        create_teaser(content='cont_2', mobile_content='mobile_cont_2', is_active_export=True, stations=[station])
        create_teaser(content='cont_3', is_active_export=True, stations=[station])

        teaser_set = TeaserSetExport('info_station', station)
        teaser_xml = teasers_to_xml(teaser_set)

        response_teasers = teaser_xml.getchildren()
        assert len(response_teasers) == 1
        content, m_content = response_teasers[0].getchildren()
        assert m_content.text == 'mobile_cont_2'

    def test_lang_national_version(self):
        station = create_station()

        create_teaser(content='cont_1', mobile_content='mobile_cont_1',
                      is_active_export=True, lang='ru', national_version='ru', stations=[station])
        create_teaser(content='cont_2', mobile_content='mobile_cont_2',
                      is_active_export=True, lang='uk', national_version='ua', stations=[station])
        create_teaser(content='cont_3', mobile_content='mobile_cont_3',
                      is_active_export=True, lang='ru', national_version='ua', stations=[station])
        create_teaser(content='cont_4', mobile_content='mobile_cont_4',
                      is_active_export=True, lang='uk', national_version='ru', stations=[station])

        def get_teasers_list(national_version, lang):
            return TeaserSetExport(
                'info_station', station,
                national_version=national_version,
                lang=lang
            ).get_teasers_by_type_with_selected()['normal']

        teasers_list = get_teasers_list('ru', 'ru')
        assert len(teasers_list) == 1
        assert teasers_list[0].lang == 'ru' and teasers_list[0].national_version == 'ru'

        teasers_list = get_teasers_list('ua', 'uk')
        assert len(teasers_list) == 1
        assert teasers_list[0].lang == 'uk' and teasers_list[0].national_version == 'ua'

        teasers_list = get_teasers_list('ua', 'ru')
        assert len(teasers_list) == 1
        assert teasers_list[0].lang == 'ru' and teasers_list[0].national_version == 'ua'

        teasers_list = get_teasers_list('ru', 'uk')
        assert len(teasers_list) == 1
        assert teasers_list[0].lang == 'uk' and teasers_list[0].national_version == 'ru'
