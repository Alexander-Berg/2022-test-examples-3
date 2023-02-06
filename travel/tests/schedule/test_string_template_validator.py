# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.core.exceptions import ValidationError

from travel.rasp.library.python.common23.models.core.schedule.base_supplier import StringTemplateValidator


@pytest.fixture(scope='module')
def validator():
    return StringTemplateValidator(fields=('foo',))


def test_valid(validator):
    validator('plain string')
    validator('string with {foo}')


def test_invalid(validator):
    with pytest.raises(ValidationError):
        validator('{')

    with pytest.raises(ValidationError):
        validator('unmatched { in string')

    with pytest.raises(ValidationError):
        validator('unmatched } in string')

    with pytest.raises(ValidationError):
        validator('string with {bar}')

    with pytest.raises(ValidationError):
        validator('string with {foo} and {bar}')

    with pytest.raises(ValidationError):
        validator('string with {foo} and {}')
