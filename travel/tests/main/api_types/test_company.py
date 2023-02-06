# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.tester.factories import create_company

from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestAirlineHandler(TestApiHandler):
    def setUp(self):
        super(TestAirlineHandler, self).setUp()

        self.company = create_company(slug='some_slug')

    def test_get_by_slug_but_slug_is_empty(self):
        payload = {
            'name': 'airline',
            'params': {
                'slug': ''
            },
            'fields': ['id', 'slug']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)

    def test_get_by_id_but_id_is_empty(self):
        payload = {
            'name': 'airline',
            'params': {
                'id': 0
            },
            'fields': ['id', 'slug']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)

    def test_get_without_params(self):
        payload = {
            'name': 'airline',
            'fields': ['id', 'slug']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)

    def test_get_by_unknown_id(self):
        payload = {
            'name': 'airline',
            'fields': ['id', 'slug'],
            'params': {
                'id': self.company.id + 1000
            },
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)

    def test_get_by_unknown_slug(self):
        payload = {
            'name': 'airline',
            'fields': ['id', 'slug'],
            'params': {
                'slug': self.company.slug + '_unknown'
            },
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)

    def test_get_by_slug(self):
        payload = {
            'name': 'airline',
            'params': {
                'slug': self.company.slug
            },
            'fields': ['id', 'slug']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': self.company.id,
            'slug': self.company.slug
        })

    def test_get_by_id(self):
        payload = {
            'name': 'airline',
            'params': {
                'id': self.company.id
            },
            'fields': ['id', 'slug']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': self.company.id,
            'slug': self.company.slug
        })

    def test_id_prefer_than_slug(self):
        payload = {
            'name': 'airline',
            'params': {
                'id': self.company.id,
                'slug': self.company.slug + '-unknown'
            },
            'fields': ['id', 'slug']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': self.company.id,
            'slug': self.company.slug
        })

        payload = {
            'name': 'airline',
            'params': {
                'id': self.company.id + 1000,
                'slug': self.company.slug
            },
            'fields': ['id', 'slug']
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect(None)
