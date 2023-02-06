from travel.avia.country_restrictions.lib.types.metric_type import QUARANTINE_REQUIRED
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text
from travel.avia.country_restrictions.lib.types.metric import Metric
from travel.avia.country_restrictions.parsers.assessors_main.subparsers.parse_quarantine import parser


def test_case_1():
    row = {
        'has_quarantine': 'yes',
        'quarantine': {
            'Для вакцинированных': True,
            'Для переболевших': True,
            'Общий карантин для всех': True,
            'При наличии ПЦР': True,
        },
        'quarantine_days': {
            'Для вакцинированных': 0,
            'Для переболевших': 5,
            'Общий карантин для всех': 5,
            'При наличии ПЦР': 1,
        },
    }

    actual = parser(context={}, row=row).get(QUARANTINE_REQUIRED.name, None)
    expected = QUARANTINE_REQUIRED.generate_metric(value=5)
    QUARANTINE_REQUIRED.set_for_vaccinated_exclusion(expected, False)
    QUARANTINE_REQUIRED.set_for_having_pcr_exclusion(expected, 1)
    assert actual == Metric(
        value=5,
        text=new_rich_text('Карантин 5 дней'),
        exclusions=[
            new_rich_text('Для вакцинированных: нет карантина'),
            new_rich_text('При наличии ПЦР: карантин 1 день'),
        ],
        additions=[],
    )
