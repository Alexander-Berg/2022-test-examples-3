# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from travel.avia.library.python.avia_data.models import AviaRecipe
from travel.avia.library.python.common.models.currency import Currency

from travel.avia.backend.tests.main.api_test import TestApiHandler
from travel.avia.backend.repository import recipes_repository


class TestRecipeHandler(TestApiHandler):
    def test_empty_params(self):
        payload = {
            'name': 'recipe',
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'params are not valid',
            'description': {
                'id': ['Missing data for required field.'],
                'from_id': ['Missing data for required field.'],
            }
        })

    def test_correct(self):
        r1 = AviaRecipe.objects.create(id=1, title_ru='рецепт1', enabled_ru=True)

        payload = {
            'name': 'recipe',
            'params': {
                'id': r1.id,
                'fromId': 213,
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': r1.id,
            'title': r1.title_ru
        })

    def test_popular_recipe(self):
        payload = {
            'name': 'recipe',
            'params': {
                'id': 0,
                'fromId': 213,
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect({
            'id': 0,
            'title': 'Популярные направления'
        })


class TestRecipesHandler(TestApiHandler):
    def setUp(self):
        super(TestRecipesHandler, self).setUp()

        self.currency = Currency.objects.create(
            name='Рубли', code='RUR', iso_code='RUB',
            template='t', template_whole='t', template_cents='t',
            template_tr='t', template_whole_tr='t', template_cents_tr='t'
        )

    def test_empty_params(self):
        payload = {
            'name': 'recipes',
        }

        data = self.api_data(payload, status_code=400)

        assert data == self.wrap_error_expect(payload, {
            'status': 'error',
            'reason': 'params are not valid',
            'description': {'from_id': ['Missing data for required field.']}
        })

    def test_no_recipes(self):
        recipes_repository.pre_cache()
        payload = {
            'name': 'recipes',
            'params': {
                'fromId': 213
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect([{
            u'airlineRecipeOffers': None,
            u'title': u'Популярные направления',
            u'metaDescription': u'',
            u'metaTitle': u'',
            u'h1': u'',
            u'recipeOffers': None,
            u'metaTitleKey': None,
            u'id': 0
        }])

    def test_simple(self):
        r1 = AviaRecipe.objects.create(id=1, title_ru='рецепт1', enabled_ru=True)
        r2 = AviaRecipe.objects.create(id=2, title_ru='рецепт2', enabled_ru=True)
        recipes_repository.pre_cache()

        payload = {
            'name': 'recipes',
            'params': {
                'fromId': 213
            }
        }

        data = self.api_data(payload)

        assert data == self.wrap_expect([
            {
                u'airlineRecipeOffers': None,
                u'title': u'Популярные направления',
                u'metaDescription': u'',
                u'metaTitle': u'',
                u'h1': u'',
                u'recipeOffers': None,
                u'metaTitleKey': None,
                u'id': 0
            },
            {
                u'airlineRecipeOffers': None,
                u'title': r1.title_ru,
                u'metaDescription': u'',
                u'metaTitle': u'',
                u'h1': u'',
                u'recipeOffers': None,
                u'metaTitleKey': '',
                u'id': r1.id
            },
            {
                u'airlineRecipeOffers': None,
                u'title': r2.title_ru,
                u'metaDescription': u'',
                u'metaTitle': u'',
                u'h1': u'',
                u'recipeOffers': None,
                u'metaTitleKey': '',
                u'id': r2.id
            }
        ])

    # проверка на даты
    # проверка на фильтрацию

    def test_offers(self):
        from .test_recipe_offers import prepare_simple_test

        r1 = AviaRecipe.objects.create(id=1, title_ru='рецепт1', enabled_ru=True)
        r2 = AviaRecipe.objects.create(id=2, title_ru='рецепт2', enabled_ru=True)
        recipes_repository.pre_cache()

        expected_offers = prepare_simple_test(self.currency)

        payload = {
            'name': 'recipes',
            'params': {
                'fromId': expected_offers[0]['fromCity']['id'],
                'adultSeats': 1,
                'klass': 'economy',
            },
            'fields': ['id', 'title', {
                'name': 'recipeOffers',
                'params': {
                    'recipeId': 0
                }
            }]
        }

        data = self.api_data(payload)

        for o in data['data'][0][0]['recipeOffers']:
            del o['directPrice']
            del o['transfersPrice']

        assert data == self.wrap_expect([{
            'id': 0,
            'title': 'Популярные направления',
            'recipeOffers': expected_offers,
            u'airlineRecipeOffers': None,
            u'metaDescription': u'',
            u'metaTitle': u'',
            u'h1': u'',
            u'metaTitleKey': None,
        }, {
            'id': r1.id,
            'title': r1.title_ru,
            'recipeOffers': None,
            u'airlineRecipeOffers': None,
            u'metaDescription': u'',
            u'metaTitle': u'',
            u'h1': u'',
            u'metaTitleKey': '',
        }, {
            'id': r2.id,
            'title': r2.title_ru,
            'recipeOffers': None,
            u'airlineRecipeOffers': None,
            u'metaDescription': u'',
            u'metaTitle': u'',
            u'h1': u'',
            u'metaTitleKey': '',
        }])
