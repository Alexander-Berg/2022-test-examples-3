from travel.avia.country_restrictions.lib.types.metric_type import FORMS_TO_FILL
from travel.avia.country_restrictions.parsers.assessors_main.subparsers.parse_forms_to_fill import parser


def test_singular():
    row = {
        'forms': [
            "https://mauritiusnow.com/wp-content/uploads/2021/09/Public-Health-Passenger-Locator-Form.pdf",
            "",
            None,
        ],
    }

    actual = parser(context={}, row=row).get(FORMS_TO_FILL.name, None)
    expected = FORMS_TO_FILL.generate_metric(
        value=[
            "https://mauritiusnow.com/wp-content/uploads/2021/09/Public-Health-Passenger-Locator-Form.pdf",
        ],
    )
    assert actual == expected


def test_plural():
    row = {
        'forms': [
            "https://mauritiusnow.com/wp-content/uploads/2021/09/Public-Health-Passenger-Locator-Form.pdf",
            "",
            "https://health.govmu.org/Pages/Main%20Page/Openingofborders.aspx",
            None,
        ],
    }

    actual = parser(context={}, row=row).get(FORMS_TO_FILL.name, None)
    expected = FORMS_TO_FILL.generate_metric(
        value=[
            "https://mauritiusnow.com/wp-content/uploads/2021/09/Public-Health-Passenger-Locator-Form.pdf",
            "https://health.govmu.org/Pages/Main%20Page/Openingofborders.aspx",
        ],
    )
    assert actual == expected


def test_empty():
    row = {
        'forms': [
            "",
            None,
        ],
    }

    assert parser(context={}, row=row).get(FORMS_TO_FILL.name, None) is None
