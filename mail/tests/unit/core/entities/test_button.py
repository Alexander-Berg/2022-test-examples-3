import urllib.parse as urlparse
from datetime import date

from mail.ciao.ciao.conf import settings
from mail.ciao.ciao.core.entities.button import get_calendar_day_button


def test_calendar_day_button():
    day = date(2020, 3, 27)
    button = get_calendar_day_button(day)
    parsed_calendar_uri = urlparse.urlparse(settings.CALENDAR_UI_DAY_URL)
    parsed_uri = urlparse.urlparse(button.uri)
    assert all((
        button.title == 'Открыть календарь',
        parsed_uri.scheme == 'https',
        parsed_uri.netloc == parsed_calendar_uri.netloc,
        parsed_uri.path == '/day',
        dict(urlparse.parse_qsl(parsed_uri.query)) == {
            'touch': '1',
            'show_date': day.isoformat(),
        },
    ))
