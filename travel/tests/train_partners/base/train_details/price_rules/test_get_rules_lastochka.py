# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

from common.apps.train.models import PlacePriceRules
from common.models.geo import Country
from common.tester.factories import create_station
from common.tester.testcase import TestCase
from travel.rasp.train_api.train_partners.base.test_utils import CoachDetailsStub
from travel.rasp.train_api.train_partners.base.train_details import TrainDetailsQuery
from travel.rasp.train_api.train_partners.base.train_details.parsers import PlaceDetails
from travel.rasp.train_api.train_partners.base.train_details.price_rules import get_rules

TRAIN_NUMBER = '123Ф'
OWNER_FPK = 'ФПК'
COACH_TYPE = 'sitting'
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

        # Ласточка СПб - Петрозавдоск, поезда №№ 803Ч, 804Ч. Правило для тарифа 100%.
        PlacePriceRules.objects.create(rule_name='Ласточка 803, 804.', coach_type=COACH_TYPE,
                                       place_name='Обычные места',
                                       price_percent=100,
                                       train_number=r'^80[34]Ч$',
                                       _coach_numbers='1',
                                       place_numbers='17, 18, 22, 23',
                                       can_apply_for_child=True)

        # Ласточка СПб - Петрозавдоск, поезда №№ 803Ч, 804Ч. Правило для тарифа 85%.
        PlacePriceRules.objects.create(rule_name='Ласточка 803, 804. Первый вагон', coach_type=COACH_TYPE,
                                       place_name='Места со скидкой',
                                       price_percent=85,
                                       train_number=r'^80[34]Ч$',
                                       _coach_numbers='1',
                                       place_numbers='19, 20, 21',
                                       can_apply_for_child=True)

        # Ласточка СПб - Петрозавдоск, поезда №№ 805Ч, 806Ч. Правило для тарифа 100%.
        PlacePriceRules.objects.create(rule_name='Ласточка 805, 806. Первый вагон', coach_type=COACH_TYPE,
                                       place_name='Обычные места',
                                       price_percent=100,
                                       train_number=r'^80[56]Ч$',
                                       _coach_numbers='1',
                                       place_numbers='17, 18, 22, 23',
                                       can_apply_for_child=True)

        # Ласточка СПб - Петрозавдоск, поезда №№ 805Ч, 806Ч. Правило для тарифа 70%.
        PlacePriceRules.objects.create(rule_name='Ласточка 805, 806.', coach_type=COACH_TYPE,
                                       place_name='Места со скидкой',
                                       price_percent=70,
                                       train_number=r'^80[56]Ч$',
                                       _coach_numbers='1',
                                       place_numbers='19, 20, 21',
                                       can_apply_for_child=True)

        # Ласточка Москва - Смоленск, поезда №№ 751М, 752М. Правило для тарифа 100%.
        PlacePriceRules.objects.create(rule_name='Ласточка 751, 752. Крайние вагоны', coach_type=COACH_TYPE,
                                       place_name='Обычные места',
                                       price_percent=100,
                                       train_number=r'^75[12]М$',
                                       _coach_numbers='1, 5',
                                       place_numbers='18, 19, 20, 21, 22',
                                       can_apply_for_child=True)

        # Ласточка Москва - Смоленск, поезда №№ 751М, 752М. Правило для тарифа 60%.
        PlacePriceRules.objects.create(rule_name='Ласточка 751, 752. Крайние вагоны', coach_type=COACH_TYPE,
                                       place_name='Откидные места',
                                       price_percent=60,
                                       train_number=r'^75[12]М$',
                                       _coach_numbers='1, 5',
                                       place_numbers='17, 23',
                                       can_apply_for_child=True)

        # Ласточка Москва - Смоленск, поезда №№ 751М, 752М. Правило для тарифа 100%.
        PlacePriceRules.objects.create(rule_name='Ласточка 751, 752. Средние вагоны', coach_type=COACH_TYPE,
                                       place_name='Обычные места',
                                       price_percent=100,
                                       train_number=r'^75[12]М$',
                                       _coach_numbers='2, 3, 4',
                                       place_numbers='18, 19, 20',
                                       can_apply_for_child=True)

        # Ласточка Москва - Смоленск, поезда №№ 751М, 752М. Правило для тарифа 60%.
        PlacePriceRules.objects.create(rule_name='Ласточка 751, 752. Средние вагоны', coach_type=COACH_TYPE,
                                       place_name='Откидные места',
                                       price_percent=60,
                                       train_number=r'^75[12]М$',
                                       _coach_numbers='2, 3, 4',
                                       place_numbers='17, 21, 22, 23',
                                       can_apply_for_child=True)

    def test_petrazavodsk_805_coach_2(self):
        rules = get_rules(train_number='805Ч',
                          coach=CoachDetailsStub(coach_type=COACH_TYPE, number=2, owner=OWNER_FPK,
                                                 places=[PlaceDetails(18), PlaceDetails(19)]),
                          query=TrainDetailsQuery(partner=None, when=WHEN,
                                                  station_from=self.station_from, station_to=self.station_to,
                                                  number='805Ч'))

        assert len(rules) == 0

    def test_petrazavodsk_805_coach_1(self):
        rules = get_rules(train_number='805Ч',
                          coach=CoachDetailsStub(coach_type=COACH_TYPE, number=1, owner=OWNER_FPK,
                                                 places=[PlaceDetails(18), PlaceDetails(19)]),
                          query=TrainDetailsQuery(partner=None, when=WHEN,
                                                  station_from=self.station_from, station_to=self.station_to,
                                                  number='805Ч'))

        assert len(rules) == 2

        assert rules[0].price_percent == 100
        assert rules[0].place_numbers_set == {18}
        assert rules[0].can_apply_for_child

        assert rules[1].price_percent == 70
        assert rules[1].place_numbers_set == {19, 20, 21}
        assert rules[1].can_apply_for_child

    def test_smolensk_coach_1(self):
        rules = get_rules(train_number='752М',
                          coach=CoachDetailsStub(coach_type=COACH_TYPE, number=1, owner=OWNER_FPK,
                                                 places=[PlaceDetails(17), PlaceDetails(18), PlaceDetails(19)]),
                          query=TrainDetailsQuery(partner=None, when=WHEN,
                                                  station_from=self.station_from, station_to=self.station_to,
                                                  number='752М'))

        assert len(rules) == 2

        assert rules[0].price_percent == 100
        assert rules[0].place_numbers_set == {18, 19}
        assert rules[0].can_apply_for_child

        assert rules[1].price_percent == 60
        assert rules[1].place_numbers_set == {17, 23}
        assert rules[1].can_apply_for_child

    def test_smolensk_coach_4(self):
        rules = get_rules(train_number='752М',
                          coach=CoachDetailsStub(coach_type=COACH_TYPE, number=4, owner=OWNER_FPK,
                                                 places=[PlaceDetails(17), PlaceDetails(18), PlaceDetails(19)]),
                          query=TrainDetailsQuery(partner=None, when=WHEN,
                                                  station_from=self.station_from, station_to=self.station_to,
                                                  number='752М'))

        assert len(rules) == 2

        assert rules[0].price_percent == 100
        assert rules[0].place_numbers_set == {18, 19}
        assert rules[0].can_apply_for_child

        assert rules[1].price_percent == 60
        assert rules[1].place_numbers_set == {17, 21, 22, 23}
        assert rules[1].can_apply_for_child
