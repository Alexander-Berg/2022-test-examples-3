# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import hamcrest

from common.models.currency import Price
from common.tester.factories import create_deluxe_train
from common.tester.testcase import TestCase
from travel.rasp.wizards.train_wizard_api.direction.filters.brand_filter import BrandFilter
from travel.rasp.wizards.train_wizard_api.direction.segments import TrainSegment, TrainVariant
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import Place


class TestBrandFilter(TestCase):
    def setUp(self):
        self._some_delux_train = create_deluxe_train(title_ru='some_delux_train')
        self._another_delux_train = create_deluxe_train(title_ru='another_delux_train', high_speed=True)

    def test_load__all_cases(self):
        valid_params = [
            [],
            ['1'],
            ['2', '3'],
            ['сапсан', '3'],
        ]

        for params in valid_params:
            BrandFilter.load(params)

    def _make_variant(self, delux_train_model, price=None):
        return TrainVariant(
            segment=TrainSegment(
                train_number=None,
                train_title=None,
                train_brand=delux_train_model,
                thread_type=None,
                duration=None,
                departure_station=None,
                departure_local_dt=None,
                arrival_station=None,
                arrival_local_dt=None,
                electronic_ticket=False,
                places=None,
                facilities_ids=None,
                updated_at=None,
                display_number=None,
                has_dynamic_pricing=None,
                two_storey=None,
                is_suburban=None,
                coach_owners=None,
                first_country_code=None,
                last_country_code=None,
                broken_classes=None,
                provider=None,
                raw_train_name=None,
                t_subtype_id=None,
            ),
            places_group=Place(
                coach_type='compartment',
                count=0,
                max_seats_in_the_same_car=0,
                price=Price(price, 'RUB'),
                price_details=None,
                service_class='2Л',
            ) if price is not None else None
        )

    def test_bind__all_cases(self):
        for train in [self._some_delux_train, self._another_delux_train, None]:
            f = BrandFilter.load([])
            f.bind([
                self._make_variant(train)
            ])
            assert f.is_bound

    def test_list_selectors__all_cases(self):
        def check(params, expected_values, actual_variants):
            f = BrandFilter.load(params)
            f.bind(actual_variants)
            assert f.list_selectors() == expected_values

        variants = [
            self._make_variant(None),
            self._make_variant(self._some_delux_train),
            self._make_variant(self._another_delux_train)
        ]

        check([], [True] * len(variants), variants)
        check([999], [True] * len(variants), variants)
        check(['xxx'], [True] * len(variants), variants)
        check([self._some_delux_train.id], [False, True, False], variants)
        check([self._another_delux_train.id], [False, False, True], variants)
        check([self._some_delux_train.id, 'another_delux_train'], [False, True, True], variants)

    def test_dump(self):
        variants = [
            self._make_variant(None),
            self._make_variant(self._some_delux_train),
            self._make_variant(self._another_delux_train, 1500),
            self._make_variant(self._another_delux_train),
            self._make_variant(self._another_delux_train, 1000),
            self._make_variant(self._some_delux_train),
        ]
        f = BrandFilter.load(['another_delux_train'])
        f.bind(variants)
        f.update_availability([True] * len(variants))

        assert f.dump() == [
            {
                'available': True,
                'minimum_price': None,
                'selected': False,
                'title': 'some_delux_train',
                'is_high_speed': False,
                'value': self._some_delux_train.id
            },
            {
                'available': True,
                'minimum_price': {'currency': 'RUB', 'value': 1000},
                'selected': True,
                'is_high_speed': True,
                'title': 'another_delux_train',
                'value': self._another_delux_train.id
            },
        ]

        variants = []
        f = BrandFilter.load(['another_delux_train'])
        f.bind(variants)
        f.update_availability([])

        assert f.dump() == []

    def test_get_search_params(self):
        variants = [
            self._make_variant(self._some_delux_train),
            self._make_variant(self._another_delux_train),
        ]
        f = BrandFilter.load([])
        f.bind(variants)
        assert f.get_search_params() == ()

        f = BrandFilter.load(['some_delux_train', 'another_delux_train'])
        f.bind(variants)
        expected = [('highSpeedTrain', v.segment.train_brand.id) for v in variants]

        hamcrest.assert_that(
            f.get_search_params(),
            hamcrest.contains_inanyorder(
                *expected
            )
        )

    def test_get_brand_title(self):
        sapsan_delux_train = create_deluxe_train(pk=50111, title_ru=u'Сапсан')

        variants = [self._make_variant(sapsan_delux_train)]
        f = BrandFilter.load([u'Сапсан-Тест'])
        f.bind(variants)
        f.update_availability([True] * len(variants))

        assert f.get_brand_title() == (True, u'Сапсаны')

        variants = []
        f = BrandFilter.load(['another_delux_train'])
        f.bind(variants)
        f.update_availability([])

        assert f.get_brand_title() == (False, None)

        variants = [
            self._make_variant(self._some_delux_train),
            self._make_variant(self._another_delux_train),
        ]
        f = BrandFilter.load(['some_delux_train', 'another_delux_train'])
        f.bind(variants)
        f.update_availability([])

        assert f.get_brand_title() == (False, None)
