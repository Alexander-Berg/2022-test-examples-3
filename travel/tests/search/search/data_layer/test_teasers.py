# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.apps.info_center.models import Info
from common.models.factories import create_info, create_teaser_page
from common.tester.factories import create_settlement, create_rthread_segment

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.teasers import get_teasers


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_get_teasers_mobile():
    page = create_teaser_page(code='search')
    create_info(info_type='ahtung', text_short='mobile_content', services=[Info.Service.WEB], title='mobile',
                pages=[page])
    create_info(info_type='ahtung', text_short=None, services=[Info.Service.WEB], title='not_mobile', pages=[page])
    create_info(text_short='mobile_content', services=[Info.Service.WEB], title='not_mobile', pages=[page])

    context = mock.Mock(point_from=create_settlement(),
                        point_to=create_settlement(),
                        when=None,
                        nearest=False,
                        transport_type='train',
                        timezones=[],
                        national_version='ru')
    segments = [create_rthread_segment(), create_rthread_segment()]

    context.is_mobile = True
    teasers = get_teasers(context, segments)
    assert teasers['ahtung'].mobile_content == 'mobile_content'
    assert teasers['ahtung'].title == 'mobile'

    context.is_mobile = False
    teasers = get_teasers(context, segments)
    assert len(teasers) == 2
