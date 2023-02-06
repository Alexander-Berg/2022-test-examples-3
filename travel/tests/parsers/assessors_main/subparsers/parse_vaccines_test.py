from typing import Optional

from travel.avia.country_restrictions.lib.types.metric_type import APPROVED_VACCINES, IS_SPUTNIK_APPROVED, \
    VACCINE_REQUIRED
from travel.avia.country_restrictions.parsers.assessors_main.subparsers.parse_vaccines import parser


def generate_row(sputnik_allowed: Optional[bool], sputnik_quarantine: Optional[bool]):
    vaccines = {
        'Moderna': True,
        'Pfizer': True,
        'Другое': True,
    }

    if sputnik_allowed is not None:
        vaccines['Спутник V'] = sputnik_allowed

    quarantine_vaccines = {
        'Moderna': True,
        'Pfizer': True,
        'Другое': True,
    }

    if sputnik_quarantine is not None:
        quarantine_vaccines['Спутник V'] = sputnik_quarantine

    return {
        'vaccines': {
            'quarantine': quarantine_vaccines,
            'requirements': vaccines,
        },
        'comments': {
            'data_match': 'Сертификат о вакцинации должен быть на бумажном носителе',
            'transfer_conditions': 'Для транзита тест не нужен, если срок пребывания в стране  меньше 24 часов',
            'vaccines': {'requirements': 'Sinopharm'},
        },
    }


def test_sputnik_approved_and_sputnik_quarantine():
    row = generate_row(True, True)
    actual = parser(context={}, row=row)

    vaccines_list = ['Moderna', 'Pfizer', 'Спутник V']

    approved_vaccines = APPROVED_VACCINES.generate_metric(value=vaccines_list)
    APPROVED_VACCINES.set_quarantine_vaccines_text(approved_vaccines, vaccines_list)

    sputnik = IS_SPUTNIK_APPROVED.generate_metric(True)
    IS_SPUTNIK_APPROVED.set_required_quarantine(sputnik)

    expected = {
        APPROVED_VACCINES.name: approved_vaccines,
        IS_SPUTNIK_APPROVED.name: sputnik,
        VACCINE_REQUIRED.name: VACCINE_REQUIRED.generate_metric(True),
    }

    assert actual == expected


def test_sputnik_approved_and_no_sputnik_quarantine_info():
    vaccines_list = ['Moderna', 'Pfizer', 'Спутник V']
    quarantine_list = ['Moderna', 'Pfizer']

    approved_vaccines = APPROVED_VACCINES.generate_metric(value=vaccines_list)
    APPROVED_VACCINES.set_quarantine_vaccines_text(approved_vaccines, quarantine_list)

    sputnik = IS_SPUTNIK_APPROVED.generate_metric(True)

    expected = {
        APPROVED_VACCINES.name: approved_vaccines,
        IS_SPUTNIK_APPROVED.name: sputnik,
        VACCINE_REQUIRED.name: VACCINE_REQUIRED.generate_metric(True),
    }

    for sputnik_quarantine_value in [False, None]:
        row = generate_row(True, sputnik_quarantine_value)
        actual = parser(context={}, row=row)
        assert actual == expected


def test_sputnik_not_approved():
    vaccines_list = ['Moderna', 'Pfizer']

    approved_vaccines = APPROVED_VACCINES.generate_metric(value=vaccines_list)
    APPROVED_VACCINES.set_quarantine_vaccines_text(approved_vaccines, vaccines_list)

    sputnik = IS_SPUTNIK_APPROVED.generate_metric(False)

    expected = {
        APPROVED_VACCINES.name: approved_vaccines,
        IS_SPUTNIK_APPROVED.name: sputnik,
        VACCINE_REQUIRED.name: VACCINE_REQUIRED.generate_metric(True),
    }

    for sputnik_quarantine_value in [True, False, None]:
        row = generate_row(False, sputnik_quarantine_value)
        actual = parser(context={}, row=row)
        assert actual == expected
