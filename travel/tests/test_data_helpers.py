# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from travel.hotels.feeders.lib.common.data_helpers import merge_photo_duplicates


class TestDataHelpers(object):

    def test_merge_photo_duplicates_preserve_order(self):
        photos = [
            {'link': '1'},
            {'link': '2'},
            {'link': '3'},
        ]
        photos = merge_photo_duplicates(photos)

        assert photos == [
            {'link': '1'},
            {'link': '2'},
            {'link': '3'},
        ]

    def test_merge_photo_duplicates_merge_photo_with_tag(self):
        photos = [
            {'link': '1'},
            {'link': '2'},
            {'link': '2', 'custom_tags': ['tag']},
        ]
        photos = merge_photo_duplicates(photos)

        assert photos == [
            {'link': '1'},
            {'link': '2', 'custom_tags': ['tag']},
        ]

    def test_merge_photo_duplicates_merge_photo_with_multiple_tags(self):
        photos = [
            {'link': '1'},
            {'link': '1', 'custom_tags': ['tag-3']},
            {'link': '1', 'custom_tags': ['tag-1']},
            {'link': '1', 'custom_tags': ['tag-2']},
            {'link': '2'},
            {'link': '4', 'custom_tags': ['tag-4']},
        ]
        photos = merge_photo_duplicates(photos)

        assert photos == [
            {'link': '1', 'custom_tags': ['tag-1', 'tag-2', 'tag-3']},
            {'link': '2'},
            {'link': '4', 'custom_tags': ['tag-4']},
        ]
