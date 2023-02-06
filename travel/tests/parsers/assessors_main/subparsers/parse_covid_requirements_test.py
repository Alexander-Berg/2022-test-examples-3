from travel.avia.country_restrictions.lib.types.metric_type import PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED, \
    PcrExpirationPeriodInHoursOrRequiredExtensions
from travel.avia.country_restrictions.parsers.assessors_main.subparsers.parse_covid_requirements import parser
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text


def generate_row_with_children(from_age: int):
    return {
        'requirements': {
            'Вакцинация': True,
            'Обязательное страхование от коронавируса': True,
            'ПЦР': True,
        },
        'pcr_expiration': {'ПЦР перед вылетом из РФ': '72 часов'},
        'children': {'pcr_needed': 'yes', 'starting_from_age': from_age},
    }


def test_case_1():
    row = generate_row_with_children(12)

    actual = parser(context={}, row=row).get(PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.name, None)
    expected = PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.generate_metric(value=72)
    PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.set_children_pcr_free_before_age_text(expected, 12)

    assert actual == expected
    assert actual.text == new_rich_text('Требуется ПЦР, сделанный не более 72 часов назад')


def test_case_2():
    row = {
        'requirements': {
            'Вакцинация': True,
            'Обязательное страхование от коронавируса': True,
            'ПЦР': True,
        },
        'pcr_expiration': {'ПЦР перед вылетом из РФ': '72 часов'},
        'antigen_upon_arrival': True,
        'children': {'pcr_needed': 'no'},
    }

    actual = parser(context={}, row=row).get(PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.name, None)
    expected = PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.generate_metric(value=72)
    expected.exclusions.append(PcrExpirationPeriodInHoursOrRequiredExtensions.CHILDREN_FREE.value.text)
    expected.additions.append(PcrExpirationPeriodInHoursOrRequiredExtensions.ANTIGEN_TEST.value.text)

    assert actual == expected
    assert actual.text == new_rich_text('Требуется ПЦР, сделанный не более 72 часов назад')


def test_case_3():
    row = {
        'requirements': {
            'Вакцинация': True,
            'Обязательное страхование от коронавируса': True,
            'ПЦР': True,
        },
        'pcr_expiration': {'ПЦР перед вылетом из РФ': '72 часов'},
        'antigen_upon_arrival': False,
        'children': {'pcr_needed': 'no'},
    }

    actual = parser(context={}, row=row).get(PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.name, None)
    expected = PCR_EXPIRATION_PERIOD_IN_HOURS_OR_REQUIRED.generate_metric(value=72)
    expected.exclusions.append(PcrExpirationPeriodInHoursOrRequiredExtensions.CHILDREN_FREE.value.text)

    assert actual == expected
    assert actual.text == new_rich_text('Требуется ПЦР, сделанный не более 72 часов назад')
