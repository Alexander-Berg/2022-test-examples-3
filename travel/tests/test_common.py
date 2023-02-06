from travel.avia.flight_status_fetcher.common import canonical_name_ru


def test_canonical_name_ru():
    assert canonical_name_ru('Олекминск') == canonical_name_ru('Олёкминск')
