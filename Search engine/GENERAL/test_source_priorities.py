from search.wizard.entitysearch.py.source_priorities import get_source_priority


def test_ru():
    assert get_source_priority("ruw", "ru") < get_source_priority("enw", "ru")


def test_en():
    assert get_source_priority("enw", "en") < get_source_priority("ruw", "en")


def test_custom():
    assert get_source_priority("cst", "ru") < get_source_priority("ruw", "ru")
