from travel.avia.country_restrictions.lib.types.metric_type import MOBILE_APP_REQUIRED
from travel.avia.country_restrictions.parsers.assessors_main.subparsers.parse_more_mobile_app_info import parser
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text


def test_case_with_links():
    mobile_app_data = (
        'Lib Travel\n'
        'https://play.google.com/store/apps/details?id=com.tuma.libtravel\n'
        'https://apps.apple.com/us/apps/lib-travel/id1537552090'
    )
    row = {
        'mobile_app': mobile_app_data,
    }

    actual = parser(context={}, row=row).get(MOBILE_APP_REQUIRED.name, None)
    expected = MOBILE_APP_REQUIRED.generate_metric(value=True)
    MOBILE_APP_REQUIRED.set_mobile_app_details_data(
        metric=expected,
        app_name=None,
        links=[
            'https://play.google.com/store/apps/details?id=com.tuma.libtravel',
            'https://apps.apple.com/us/apps/lib-travel/id1537552090',
        ],
    )
    assert actual == expected


def test_case_with_no_links():
    mobile_app_data = 'Lib Travel\n' 'asdf\n' 'ksdlfklsa'
    row = {
        'mobile_app': mobile_app_data,
    }

    actual = parser(context={}, row=row).get(MOBILE_APP_REQUIRED.name, None)
    expected = MOBILE_APP_REQUIRED.generate_metric(value=True)
    assert actual == expected
    assert actual.text == new_rich_text('Необходимо установить мобильное приложение')
