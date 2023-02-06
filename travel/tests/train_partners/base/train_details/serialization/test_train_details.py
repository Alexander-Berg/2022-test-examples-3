# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import factory
from hamcrest import assert_that, contains, has_entries

from travel.rasp.train_api.train_partners.base.train_details.serialization import TrainDetailsSchema
from travel.rasp.train_api.train_purchase.core.directories import TariffCategories, TariffTypes
from travel.rasp.train_api.train_purchase.core.enums import RoutePolicy


class TrainDetailsFactory(factory.StubFactory):
    pass


class CoachDetailsFactory(factory.StubFactory):
    pass


def test_train_details():
    train_details = TrainDetailsFactory(
        is_cppk=False,
        tariff_categories=TariffCategories().find_categories(
            RoutePolicy.INTERNATIONAL.value, is_suburban=False, is_cppk=False),
        coaches=[
            CoachDetailsFactory(
                tariff_types=TariffTypes().find_types_by_provider_codes(RoutePolicy.INTERNAL.value, ['Junior']),
                is_special_sale_mode=True,
            ),
        ],
    )
    result = TrainDetailsSchema().dump(train_details)

    assert_that(
        result.data,
        has_entries({
            'isCppk': False,
            'tariffCategories': contains(
                has_entries({
                    'code': 'baby',
                    'minAge': 0,
                    'minAgeIncludesBirthday': False,
                    'maxAge': 4,
                    'maxAgeIncludesBirthday': True,
                    'needDocument': False,
                    'withoutPlace': True,
                }),
                has_entries({
                    'code': 'child',
                    'minAge': 4,
                    'minAgeIncludesBirthday': False,
                    'maxAge': 12,
                    'maxAgeIncludesBirthday': True,
                    'needDocument': False,
                    'withoutPlace': False,
                }),
                has_entries({
                    'code': 'full',
                    'minAge': 12,
                    'minAgeIncludesBirthday': False,
                    'maxAge': 150,
                    'maxAgeIncludesBirthday': False,
                    'needDocument': False,
                    'withoutPlace': False,
                }),
            ),
            'coaches': contains(
                has_entries({
                    'isSpecialSaleMode': True,
                    'tariffTypes': contains(
                        has_entries({
                            'code': 'junior',
                            'providerCode': 'Junior',
                            'discount': 0.3,
                            'validators': '{"age":{"min":10,"minIncludesBirthday":false,"max":21,'
                                          '"maxIncludesBirthday":true}}',
                        }),
                    )
                }),
            ),
        }),
    )
