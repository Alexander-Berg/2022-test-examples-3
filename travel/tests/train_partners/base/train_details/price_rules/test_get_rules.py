# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime

from common.apps.train.models import PlacePriceRules
from common.models.geo import Country
from common.tester.factories import create_station
from common.tester.testcase import TestCase
from travel.rasp.train_api.train_partners.base.test_utils import CoachDetailsStub
from travel.rasp.train_api.train_partners.base.train_details import TrainDetailsQuery
from travel.rasp.train_api.train_partners.base.train_details.parsers import PlaceDetails
from travel.rasp.train_api.train_partners.base.train_details.price_rules import get_rules


TRAIN_NUMBER = '123Ф'
COACH_NUMBER = 17
OWNER_FPK = 'ФПК'
OWNER_BELARUS = 'БЧ'
WHEN = datetime(2016, 11, 24, 11, 30)
EXPRESS_FROM = '2024000'
EXPRESS_TO = '2024001'


class TestGetRules(TestCase):
    def setUp(self):
        super(TestGetRules, self).setUp()
        self.station_from = create_station(country_id=Country.RUSSIA_ID,
                                           __={'codes': {'express': EXPRESS_FROM}})

        self.station_to = create_station(country_id=Country.RUSSIA_ID,
                                         __={'codes': {'express': EXPRESS_TO}})

        self.query = TrainDetailsQuery(partner=None, when=WHEN, number=TRAIN_NUMBER,
                                       station_from=self.station_from, station_to=self.station_to)

    def test_platzkarte_rules(self):
        """
        Упрощенный 12-местный плацкарт. Схема такая:
                    ___________________
        верх       | 2  4 |  6  8 | WC |
        низ        | 1  3 |  5  7 |____|
                   |                   |
        верх бок   |  12  |   10  |    |
        низ бок    |  11  |    9  |    |
                    -------------------
        """

        # Прошлогоднее неактуальное правило
        PlacePriceRules.objects.create(rule_name='Плацкарт-2015', coach_type='platzkarte', place_name='Нижние',
                                       price_percent=100,
                                       departure_period_begin=date(2015, 2, 16),
                                       departure_period_end=date(2015, 12, 31),
                                       place_numbers='1, 3, 5, 7, 9, 11',
                                       can_apply_for_child=False)

        # Прошлогоднее неактуальное правило
        PlacePriceRules.objects.create(rule_name='Плацкарт-2015', coach_type='platzkarte', place_name='Верхние',
                                       price_percent=70,
                                       departure_period_begin=date(2015, 2, 16),
                                       departure_period_end=date(2015, 12, 31),
                                       place_numbers='2, 4, 6, 8, 10, 12',
                                       can_apply_for_child=False)

        # Актуальное правило, тариф 100%
        PlacePriceRules.objects.create(rule_name='Плацкарт-2016', coach_type='platzkarte', place_name='Не у туалета',
                                       price_percent=100,
                                       departure_period_begin=date(2016, 3, 1),
                                       departure_period_end=date(2016, 12, 31),
                                       place_numbers='1, 2, 3, 4, 11, 12',
                                       can_apply_for_child=False)

        # Актуальное правило, тариф 85%
        PlacePriceRules.objects.create(rule_name='Плацкарт-2016', coach_type='platzkarte',
                                       place_name='У туалета кроме верхней боковушки',
                                       price_percent=85,
                                       place_numbers='5, 6, 7, 8, 9',
                                       can_apply_for_child=False)

        # Актуальное правило, тариф 70%
        PlacePriceRules.objects.create(rule_name='Плацкарт-2016', coach_type='platzkarte',
                                       place_name='У туалета верхняя боковушка',
                                       price_percent=70,
                                       place_numbers='10',
                                       can_apply_for_child=False)

        # Правило для купейных вагонов, оно не должно попасть в результат
        PlacePriceRules.objects.create(rule_name='Купе', coach_type='compartment', price_percent=100)

        rules = get_rules(
            train_number=TRAIN_NUMBER,
            coach=CoachDetailsStub(
                coach_type='platzkarte',
                number=COACH_NUMBER,
                owner=OWNER_FPK,
                places=[PlaceDetails(1), PlaceDetails(2), PlaceDetails(5), PlaceDetails(10)]
            ),
            query=self.query
        )

        assert len(rules) == 3

        assert rules[0].price_percent == 100
        assert rules[0].place_numbers_set == {1, 2}
        assert not rules[0].can_apply_for_child

        assert rules[1].price_percent == 85
        assert rules[1].place_numbers_set == {5, 6, 7, 8, 9}
        assert not rules[1].can_apply_for_child

        assert rules[2].price_percent == 70
        assert rules[2].place_numbers_set == {10}
        assert not rules[2].can_apply_for_child

    def test_platzkarte_rules_middle_price_only(self):
        """
        В наличии только места средней ценовой катеогории.
        Упрощенный 12-местный плацкарт. Схема такая:
                    ___________________
        верх       | 2  4 |  6  8 | WC |
        низ        | 1  3 |  5  7 |____|
                   |                   |
        верх бок   |  12  |   10  |    |
        низ бок    |  11  |    9  |    |
                    -------------------
        """

        # Прошлогоднее неактуальное правило
        PlacePriceRules.objects.create(rule_name='Плацкарт-2015', coach_type='platzkarte', place_name='Нижние',
                                       price_percent=100,
                                       departure_period_begin=date(2015, 2, 16),
                                       departure_period_end=date(2015, 12, 31),
                                       place_numbers='1, 3, 5, 7, 9, 11',
                                       can_apply_for_child=False)

        # Прошлогоднее неактуальное правило
        PlacePriceRules.objects.create(rule_name='Плацкарт-2015', coach_type='platzkarte', place_name='Верхние',
                                       price_percent=70,
                                       departure_period_begin=date(2015, 2, 16),
                                       departure_period_end=date(2015, 12, 31),
                                       place_numbers='2, 4, 6, 8, 10, 12',
                                       can_apply_for_child=False)

        # Актуальное правило, тариф 100%
        PlacePriceRules.objects.create(rule_name='Плацкарт-2016', coach_type='platzkarte', place_name='Не у туалета',
                                       price_percent=100,
                                       departure_period_begin=date(2016, 3, 1),
                                       departure_period_end=date(2016, 12, 31),
                                       place_numbers='1, 2, 3, 4, 11, 12',
                                       can_apply_for_child=False)

        # Актуальное правило, тариф 85%
        PlacePriceRules.objects.create(rule_name='Плацкарт-2016', coach_type='platzkarte',
                                       place_name='У туалета кроме верхней боковушки',
                                       price_percent=85,
                                       place_numbers='5, 6, 7, 8, 9',
                                       can_apply_for_child=False)

        # Актуальное правило, тариф 70%
        PlacePriceRules.objects.create(rule_name='Плацкарт-2016', coach_type='platzkarte',
                                       place_name='У туалета верхняя боковушка',
                                       price_percent=70,
                                       place_numbers='10',
                                       can_apply_for_child=False)

        # Правило для купейных вагонов, оно не должно попасть в результат
        PlacePriceRules.objects.create(rule_name='Купе', coach_type='compartment', price_percent=100)

        rules = get_rules(train_number=TRAIN_NUMBER,
                          coach=CoachDetailsStub(coach_type='platzkarte', number=COACH_NUMBER, owner=OWNER_FPK,
                                                 places=[PlaceDetails(5), PlaceDetails(6)]),
                          query=self.query)

        assert len(rules) == 1

        assert rules[0].price_percent == 85
        assert rules[0].place_numbers_set == {5, 6, 7, 8, 9}
        assert not rules[0].can_apply_for_child

    def test_compartment_rules(self):
        """
        Упрощенный 8-местный купейный вагон. Схема такая:
                    __________________
        верх       | 2  4 | 6  8 | WC |
        низ        | 1  3 | 5  7 |____|
                   |--  -- --  --     |
                   |                  |
                    ------------------
        """
        PlacePriceRules.objects.create(rule_name='Купе', coach_type='compartment', place_name='Нижние',
                                       place_numbers='1, 3, 5, 7',
                                       price_percent=100)

        PlacePriceRules.objects.create(rule_name='Купе', coach_type='compartment', place_name='Верхние',
                                       place_numbers='2, 4, 6, 8',
                                       price_percent=60)

        rules = get_rules(train_number=TRAIN_NUMBER,
                          coach=CoachDetailsStub(coach_type='compartment', number=COACH_NUMBER, owner=OWNER_FPK,
                                                 places=[PlaceDetails(1), PlaceDetails(3), PlaceDetails(6)]),
                          query=self.query)

        assert len(rules) == 2

        assert rules[0].price_percent == 100
        assert rules[0].place_numbers_set == {1, 3}
        assert rules[0].can_apply_for_child

        assert rules[1].price_percent == 60
        assert rules[1].place_numbers_set == {2, 4, 6, 8}
        assert rules[1].can_apply_for_child

    def test_platzkarte_belarus_rules(self):
        # РЖД. Правило для тарифа 100%. Владелец вагона не указан.
        PlacePriceRules.objects.create(rule_name='РЖД. Плацкарт-2016', coach_type='platzkarte',
                                       place_name='Не у туалета',
                                       price_percent=100,
                                       place_numbers='1, 2, 3, 4, 11, 12',
                                       can_apply_for_child=False)

        # РЖД. Правило для тарифа 85%. Владелец вагона не указан.
        PlacePriceRules.objects.create(rule_name='РЖД. Плацкарт-2016', coach_type='platzkarte',
                                       place_name='У туалета кроме верхней боковушки',
                                       price_percent=85,
                                       place_numbers='5, 6, 7, 8, 9',
                                       can_apply_for_child=False)

        # РЖД. Правило для тарифа 70%. Владелец вагона не указан.
        PlacePriceRules.objects.create(rule_name='РЖД. Плацкарт-2016', coach_type='platzkarte',
                                       place_name='У туалета верхняя боковушка',
                                       price_percent=70,
                                       place_numbers='10',
                                       can_apply_for_child=False)

        # БЧ. Правило для тарифа 100%.
        PlacePriceRules.objects.create(rule_name='БЧ. Плацкарт-2016', coach_type='platzkarte', place_name='Не боковые',
                                       price_percent=100,
                                       place_numbers='1, 2, 3, 4, 5, 6, 7, 8',
                                       owner=OWNER_BELARUS,
                                       can_apply_for_child=True)

        # РЖД. Правило для тарифа 90%.
        PlacePriceRules.objects.create(rule_name='БЧ. Плацкарт-2016', coach_type='platzkarte',
                                       place_name='Боковые',
                                       price_percent=90,
                                       place_numbers='9, 10, 11, 12',
                                       owner=OWNER_BELARUS,
                                       can_apply_for_child=True)

        rules = get_rules(train_number=TRAIN_NUMBER,
                          coach=CoachDetailsStub(coach_type='platzkarte', number=COACH_NUMBER, owner=OWNER_BELARUS,
                                                 places=[PlaceDetails(1), PlaceDetails(2), PlaceDetails(9)]),
                          query=self.query)

        assert len(rules) == 2

        assert rules[0].price_percent == 100
        assert rules[0].place_numbers_set == {1, 2}
        assert rules[0].can_apply_for_child

        assert rules[1].price_percent == 90
        assert rules[1].place_numbers_set == {9, 10, 11, 12}
        assert rules[1].can_apply_for_child
