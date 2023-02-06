from travel.avia.country_restrictions.lib.types.metric_type import VISA_REQUIRED
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text
from travel.avia.country_restrictions.parsers.assessors_visa.parse_visa import parser


def helper(row, expected):
    actual = parser(context={}, row=row).get(VISA_REQUIRED.name, None)
    assert actual == expected


def test_simple():
    helper(
        {
            'visa': [
                'yes',
                'e_visa',
                'visa_on_arrival',
            ],
            'currency': '$',
            'visa_cost': 51,
            'WHO_vaccination_required': 'no',
        },
        expected=VISA_REQUIRED.generate_metric(
            value=True,
            additions=[
                new_rich_text('Стоимость визы: 51$'),
                new_rich_text('Возможно оформление электронной визы'),
                new_rich_text('Возможно оформление визы по прибытию'),
            ],
        ),
    )


def test_no_visa():
    helper(
        {
            'visa': [
                'yes',
                'e_visa',
                'visa_on_arrival',
            ],
            'currency': '$',
            'visa_free_stay': 50,
        },
        expected=VISA_REQUIRED.generate_metric(
            value=True,
            additions=[
                new_rich_text('Возможно оформление электронной визы'),
                new_rich_text('Возможно оформление визы по прибытию'),
            ],
            exclusions=[
                new_rich_text('Безвизовый период -  50 дней'),
            ]
        ),
    )
