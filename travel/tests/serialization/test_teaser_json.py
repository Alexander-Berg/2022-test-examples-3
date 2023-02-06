# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, has_entries

from common.models.factories import create_teaser
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.serialization.teasers import teaser_json


class TestTeaserJson(TestCase):
    def test_teaser_without_image(self):
        teaser = create_teaser(id=25, importance=10, title='teaser title', content='teaser content', url='teaser url')
        assert_that(teaser_json(teaser), has_entries({
            'id': 25,
            'importance': 10,
            'title': 'teaser title',
            'content': 'teaser content',
            'url': 'teaser url',
            'image_url': None,
            'imageUrl': None
        }))
