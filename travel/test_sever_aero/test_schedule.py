import pkgutil
import pytest

from travel.avia.flight_status_fetcher.sources import sever_aero


@pytest.fixture()
def schedule():
    _schedule = sever_aero.Schedule(pkgutil.get_data('tests', 'resources/subjects.xml').decode('utf-8'))
    return _schedule


def test_subjects_by_code(schedule: sever_aero.Schedule):
    assert schedule.subjects_by_code['000000319'] == sever_aero.Subject('000000319', 'Айхал', 'Ayhal')


def test_subjects_by_name_ru(schedule: sever_aero.Schedule):
    assert schedule.subjects_by_name_ru['Хандыга'] == sever_aero.Subject('000000217', 'Хандыга', 'Khandyga')
    assert schedule.subjects_by_name_ru['Чокурдах'] == sever_aero.Subject('000000226', 'Чокурдах', 'Chokurdakh')
