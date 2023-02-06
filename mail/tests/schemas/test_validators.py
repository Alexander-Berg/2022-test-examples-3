import marshmallow as mm
import pytest

from sendr_utils.schemas.validators import NonEmptyString

from hamcrest import assert_that, equal_to, has_item


class TestNonEmptyStringValidator:
    validator = NonEmptyString()

    def test_validates_empty_string(self):
        with pytest.raises(mm.exceptions.ValidationError) as exc_info:
            self.validator('')

        assert_that(exc_info.value.messages, has_item(equal_to('String should not be empty.')))

    def test_validates_none_value(self):
        with pytest.raises(mm.exceptions.ValidationError) as exc_info:
            self.validator(None)

        assert_that(exc_info.value.messages, has_item(equal_to('Field may not be null.')))

    def test_validates_string_of_spaces(self):
        with pytest.raises(mm.exceptions.ValidationError) as exc_info:
            self.validator('    ')

        assert_that(exc_info.value.messages, has_item(equal_to('String should not be empty.')))

    def test_validates_value_type(self):
        with pytest.raises(mm.exceptions.ValidationError) as exc_info:
            self.validator(object())

        assert_that(exc_info.value.messages, has_item(equal_to('Invalid type.')))
