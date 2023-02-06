# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function
__metaclass__ = type

import mock
import pytest

from common.models.factories import create_teaser
from common.tester.factories import create_station

from travel.rasp.export.export.v3.core.teasers import prepare_teasers, TeaserSetExport


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestTeasers:
    def test_teasers_to_xml(self):
        _teasers = [create_teaser(id=1, mode='normal', importance=0, title='normal title',
                                  content='ok', mobile_content='mobile ok'),
                    create_teaser(id=2, mode='ahtung', importance=5, title='ahtung title',
                                  content='ahtung', mobile_content='mobile ahtung')]

        def m_get_teasers(*args):
            return _teasers

        with mock.patch.object(TeaserSetExport, 'get_teasers', m_get_teasers):
            teaser_set = TeaserSetExport('search_suburban', {})
            teasers = prepare_teasers(teaser_set)

            assert len(teasers) == 2
            assert teasers[0]['id'] == _teasers[1].uid64
            assert teasers[0]['title'] == _teasers[1].title

            assert teasers[1]['id'] == _teasers[0].uid64
            assert teasers[1]['title'] == _teasers[0].title

            for i, teaser in enumerate(reversed(teasers)):
                assert teaser['content'] == _teasers[i].content
                assert teaser['mobile_content'] == _teasers[i].mobile_content

        teasers = prepare_teasers(None)
        assert len(teasers) == 0

    def test_empty_mobile_content(self):
        station = create_station()
        create_teaser(content='cont_1', mobile_content='', is_active_export=True, stations=[station]),
        create_teaser(content='cont_2', mobile_content='mobile_cont_2', is_active_export=True, stations=[station])
        create_teaser(content='cont_3', is_active_export=True, stations=[station])

        teaser_set = TeaserSetExport('info_station', station)
        response_teasers = prepare_teasers(teaser_set)

        assert len(response_teasers) == 1
        assert response_teasers[0]['mobile_content'] == 'mobile_cont_2'

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
