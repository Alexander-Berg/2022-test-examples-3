from datetime import datetime, timezone
from decimal import Decimal

import pytest
from marshmallow.validate import Length, Regexp, ValidationError, Validator

from sendr_utils.schemas.fields import AmountField, BytesField, CurrencyField, EpochTimestamp
from sendr_utils.schemas.validators import DecimalExponentValidator

from hamcrest import (
    assert_that, contains, contains_inanyorder, contains_string, equal_to, has_entries, has_properties, instance_of,
    none
)


class TestCurrencyField:
    def test_init(self):
        field = CurrencyField()

        assert_that(
            field,
            has_properties(
                metadata=has_entries(
                    description=equal_to('ISO 4217 alpha code. E.g. RUB, USD, XTS'),
                ),
                validators=contains(instance_of(Regexp)),
            ),
        )

    def test_multiple_validators(self):
        field = CurrencyField(validate=Length(min=0))

        assert_that(
            field,
            has_properties(
                validators=contains(
                    instance_of(Length),
                    instance_of(Regexp),
                )
            ),
        )

    @pytest.mark.parametrize('currency', ['RUB', 'USD', 'XTS', 'FOO', 'BAR', None])
    def test_validation_passes(self, currency):
        field = CurrencyField(allow_none=True)

        deserialized = field.deserialize(currency)
        assert_that(deserialized, equal_to(currency))

    @pytest.mark.parametrize('currency', ['RU', 'rub', 'Rub', 'ABCD', 'U$D', '123', '', None])
    def test_validation_fails(self, currency):
        field = CurrencyField()

        with pytest.raises(ValidationError):
            field.deserialize(currency)


class DummyValidator(Validator):
    def __call__(self, *args, **kwargs):
        pass


class TestAmountField:
    @pytest.mark.parametrize(
        'format_enforced,initial_validator,expected_validators',
        [
            (True, [], [DecimalExponentValidator]),
            (True, DummyValidator(), [DummyValidator, DecimalExponentValidator]),
            (False, [], []),
            (False, DummyValidator(), [DummyValidator]),
        ],
    )
    def test_init(self, format_enforced, initial_validator, expected_validators):
        field = AmountField(validate=initial_validator, format_enforced=format_enforced)
        assert_that(
            field,
            has_properties(
                metadata=has_entries(
                    description=contains_string('Не должно содержать больше двух знаков ' 'после запятой'),
                ),
                validators=contains_inanyorder(*(instance_of(ev) for ev in expected_validators)),
            ),
        )

    @pytest.mark.parametrize('amount', ['0', '0.', '.0', '0.0', '0.00', '1.23', '4', None])
    def test_validation_passes(self, amount):
        field = AmountField(allow_none=True, format_enforced=True)

        deserialized = field.deserialize(amount)
        if amount is None:
            assert_that(deserialized, none())
        else:
            assert_that(deserialized, instance_of(Decimal))

    @pytest.mark.parametrize('amount', ['0.000', '1.234', '', None])
    def test_validation_fails(self, amount):
        field = AmountField(format_enforced=True)

        with pytest.raises(ValidationError):
            field.deserialize(amount)


UTC_NOW = datetime.fromtimestamp(1615570734.551668, tz=timezone.utc)


class TestEpochTimestamp:
    @pytest.mark.parametrize(
        'field_kwargs,input_value,expected_serialized_value',
        [
            ({}, None, None),
            ({}, UTC_NOW, int(UTC_NOW.timestamp())),
            ({'as_string': True}, UTC_NOW, str(int(UTC_NOW.timestamp()))),
            ({'in_milliseconds': True}, UTC_NOW, int(UTC_NOW.timestamp() * 1000)),
            ({'as_string': True, 'in_milliseconds': True}, UTC_NOW, str(int(UTC_NOW.timestamp() * 1000))),
        ],
    )
    def test_serialize(self, field_kwargs, input_value, expected_serialized_value):
        field = EpochTimestamp(**field_kwargs)
        assert_that(field._serialize(input_value, None, None), equal_to(expected_serialized_value))

    @pytest.mark.parametrize(
        'field_kwargs,input_value,expected_deserialized_value',
        [
            ({}, None, None),
            ({}, 0, datetime(1970, 1, 1, 0, 0, 0, tzinfo=timezone.utc)),
            ({}, 1, datetime(1970, 1, 1, 0, 0, 1, tzinfo=timezone.utc)),
            ({'in_milliseconds': True}, 1, datetime(1970, 1, 1, 0, 0, 0, 1000, tzinfo=timezone.utc)),
            ({}, -1, datetime(1969, 12, 31, 23, 59, 59, tzinfo=timezone.utc)),
            ({}, str(UTC_NOW.timestamp()), UTC_NOW),
            ({}, int(UTC_NOW.timestamp()), UTC_NOW.replace(microsecond=0)),
            ({}, UTC_NOW.timestamp(), UTC_NOW),
            ({}, "1e9", datetime(2001, 9, 9, 1, 46, 40, tzinfo=timezone.utc)),
        ],
    )
    def test_deserialize(self, field_kwargs, input_value, expected_deserialized_value):
        field = EpochTimestamp(**field_kwargs)
        assert_that(field._deserialize(input_value, None, None), equal_to(expected_deserialized_value))

    @pytest.mark.parametrize('in_milliseconds', [True, False])
    @pytest.mark.parametrize('as_string', [True, False])
    def test_invariance(self, in_milliseconds, as_string):
        field = EpochTimestamp(in_milliseconds=in_milliseconds, as_string=as_string)
        serialized = field._serialize(UTC_NOW, None, None)
        deserialized = field._deserialize(serialized, None, None)

        rounded_mcs = UTC_NOW.microsecond // 1000 * 1000 if in_milliseconds else 0
        expected_deserialized = UTC_NOW.replace(microsecond=rounded_mcs)
        assert_that(deserialized, equal_to(expected_deserialized))

    @pytest.mark.parametrize('malformed', ['', 'not-a-number', '1,2', '1.2.3', '1ee2'])
    def test_deserialize_validation_error(self, malformed):
        field = EpochTimestamp()
        with pytest.raises(ValidationError, match='Not a valid number'):
            field._deserialize(malformed, None, None)


class TestBytesField:
    def test_serialize(self):
        field = BytesField()
        assert_that(field._serialize(bytes.fromhex('deadbeef'), None, None), equal_to('3q2+7w=='))

    def test_deserialize(self):
        field = BytesField()
        assert_that(field._deserialize('3q2+7w==', None, None), equal_to(bytes.fromhex('deadbeef')))

    @pytest.mark.parametrize('malformed', ['a', '123'])
    def test_deserialize_validation_error(self, malformed):
        field = BytesField()
        with pytest.raises(ValidationError, match='Not a valid base64 string.'):
            field._deserialize(malformed, None, None)
