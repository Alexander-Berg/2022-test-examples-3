from travel.avia.library.python.translations.number_with_measurement_unit import to_ru_str


def test_func():
    values = {
        1: '1 день',
        2: '2 дня',
        3: '3 дня',
        4: '4 дня',
        5: '5 дней',
        6: '6 дней',
        7: '7 дней',
        8: '8 дней',
        9: '9 дней',
        10: '10 дней',
        11: '11 дней',
        19: '19 дней',
        20: '20 дней',
        21: '21 день',
        30: '30 дней',
        31: '31 день',
        32: '32 дня',
        111: '111 дней',
        1000: '1000 дней',
        1001: '1001 день',
        1111: '1111 дней',
    }

    for k, v in values.items():
        res = to_ru_str(k, 'день', 'дня', 'дней')
        assert res == v, f"failed: {values[v]} - {res}"
