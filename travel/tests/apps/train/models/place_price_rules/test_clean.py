# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.core.exceptions import ValidationError

from common.apps.train.models import PlacePriceRules
from common.apps.train_order.enums import CoachType


@pytest.mark.dbuser
@pytest.mark.parametrize('new_rule, old_rule, raises', [
    (dict(place_numbers='1, 2, 3'), None, False),
    (dict(id=1, place_numbers='1, 2, 3'), None, False),
    (dict(id=1, place_numbers='1, 2, 3'), dict(place_numbers='4, 5, 6'), False),
    (dict(place_numbers='1, 2, 3'), dict(place_numbers='1, 2, 3'), True),
    (dict(place_numbers='1, 2, 3', owner='ФПК, СПБ'), dict(place_numbers='1,2', owner='СПБ,ФПК'), True),
    (dict(place_numbers='1, 2, 3', owner='ФПК, СПБ'), dict(place_numbers='1,2', owner='СПБ,ФПК,Ф'), False),
    (dict(place_numbers='1, 2, 3', _coach_numbers='01, 02'), dict(place_numbers='1,2', _coach_numbers='02, 01'), True),
    (dict(place_numbers='1, 2, 3', _coach_numbers='01, 02'), dict(place_numbers='1,2', _coach_numbers='01'), False),
    (dict(place_numbers='1, 2, 3', coach_type=CoachType.PLATZKARTE.value),
     dict(place_numbers='1,2', coach_type=CoachType.PLATZKARTE.value), True),
    (dict(place_numbers='1, 2, 3', coach_type=CoachType.PLATZKARTE.value),
     dict(place_numbers='1,2', coach_type=CoachType.COMPARTMENT.value), False),
    (dict(place_numbers='1, 2, 3', service_class='2Д'), dict(place_numbers='1,2', service_class='2Д'), True),
    (dict(place_numbers='1, 2, 3', service_class='2Д'), dict(place_numbers='1,2', service_class='2К'), False),

])
def test_clean(new_rule, old_rule, raises):
    old_rule and PlacePriceRules(**old_rule).save()
    new_rule = PlacePriceRules(**new_rule)
    if new_rule.id:
        new_rule.save()

    if raises:
        with pytest.raises(ValidationError):
            new_rule.clean()
    else:
        new_rule.clean()
