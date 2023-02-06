# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from travel.avia.backend.tests.main.api_test import TestApiHandler

from travel.avia.library.python.tester.factories import create_static_page


class TestInfoHandler(TestApiHandler):
    def setUp(self):
        super(TestInfoHandler, self).setUp()

        self.slug = 'page'
        self.page = create_static_page(slug=self.slug,
                                       is_published=1,
                                       is_ticket_page=True
                                       )
        self.other_slug = 'other_page'
        self.other_page = create_static_page(slug=self.other_slug,
                                             is_published=2,
                                             parent=self.page,
                                             is_ticket_page=True)

        self.unpublished_slug = 'unknown'
        self.unpublished_page = create_static_page(slug=self.unpublished_slug,

                                                   is_published=0,
                                                   is_ticket_page=True)

        self.too_deep_slug = 'too_deep'
        self.too_deep_page = create_static_page(slug=self.too_deep_slug,
                                                is_published=1,
                                                parent=self.other_page,
                                                is_ticket_page=True)

    def get_static_text(self, slug):
        return self.api_data({
            'name': 'info',
            'params': {
                'slug': slug
            }
        })

    def test_different_page(self):
        response = self.get_static_text(self.slug)

        assert response['status'] == 'success'

        data = response['data'][0]
        assert data['slug'] == self.slug

        response = self.get_static_text(self.other_slug)

        assert response['status'] == 'success'

        data = response['data'][0]
        assert data['slug'] == self.other_slug

    def test_unpublished_page(self):
        response = self.get_static_text(self.unpublished_slug)

        assert response['status'] == 'success'
        assert response['data'][0] is None

    def test_too_deep(self):
        response = self.get_static_text(self.too_deep_slug)

        assert response['status'] == 'success'
        assert response['data'][0] is None
