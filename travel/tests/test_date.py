import datetime
from freezegun import freeze_time

from travel.avia.library.python.translations.date import MONTHS_GENITIVE_CASE, to_ru_str


@freeze_time('2000-04-12')
def test_current_year():
    date = datetime.date(year=2000, month=4, day=12)
    assert to_ru_str(date, MONTHS_GENITIVE_CASE, False, True) == '12 апреля 2000 года'
    assert to_ru_str(date, MONTHS_GENITIVE_CASE, True, True) == '12 апреля'


@freeze_time('2001-04-12')
def test_not_current_year():
    date = datetime.date(year=2000, month=4, day=12)
    assert to_ru_str(date, MONTHS_GENITIVE_CASE, False, False) == '12 апреля 2000'
    assert to_ru_str(date, MONTHS_GENITIVE_CASE, True, True) == '12 апреля 2000 года'
    assert to_ru_str(date, MONTHS_GENITIVE_CASE, False, True) == '12 апреля 2000 года'
