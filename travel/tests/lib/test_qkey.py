import pytest

from dataclasses import asdict, is_dataclass
from datetime import date, datetime
from itertools import product

from travel.avia.subscriptions.app.lib.qkey import (
    qkey_from_params, validate_qkey, structure_from_qkey,
    QkeyValidationError, QkeyStructure
)


def test_qkey_validate(qkey_factory):
    qkey = qkey_factory()
    actual = validate_qkey(None, qkey)
    assert actual == qkey


def test_qkey_validate_from(qid_factory):
    qid, qkey = qid_factory(return_qkey=True)
    actual = validate_qkey(qid, None)
    assert actual == qkey


def test_qkey_validate_invalid_qkey():
    with pytest.raises(QkeyValidationError):
        validate_qkey(None, 'some.bad_qkey')

    with pytest.raises(QkeyValidationError):
        validate_qkey(None, 'some$bad_qkey')


def test_validate_qkey_both_none_passed():
    with pytest.raises(QkeyValidationError):
        validate_qkey(None, None)


def test_structure_from_qkey(qkey_factory):
    qkey = qkey_factory()
    structure = structure_from_qkey(qkey)

    assert is_dataclass(structure)
    assert isinstance(structure, QkeyStructure)
    assert qkey_from_params(**asdict(structure)) == qkey


def test_structure_from_qkey_bad_passengers_count(qkey_factory):
    qkey = qkey_factory(adults='0', infants='0', children='0')
    with pytest.raises(ValueError):
        structure_from_qkey(qkey)


dates_variants = [
    None, '2020-12-12', date(2020, 12, 12),
    datetime(2020, 12, 12, 12, 12, 12)
]
dates_combinations = list(
    filter(
        lambda x: x[0] is not None,
        product(dates_variants, repeat=2)
    )
)


@pytest.mark.parametrize(
    'date_forward,date_backward',
    dates_combinations
)
def test_qkey_from_params(date_forward, date_backward):
    actual = qkey_from_params(
        'c1', 'c2', date_forward, date_backward,
        'economy', 1, 2, 3, 'ru'
    )
    if date_backward is None:
        expected = 'c1_c2_2020-12-12_None_economy_1_2_3_ru'
    else:
        expected = 'c1_c2_2020-12-12_2020-12-12_economy_1_2_3_ru'

    assert actual == expected
