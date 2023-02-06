from bot.aiostaff import staff_reducer

STAFF_DATA_1 = {
    "department_group": {"url": "yandex_search_tech_ont_dep77192", "name": "Группа разработки СУП"},
    "official": {"is_dismissed": False},
    "accounts": [
        {"value": "MikailBag", "type": "telegram", "private": False, "value_lower": "mikailbag", "id": 228780},
        {"value": "_A_1_A_", "type": "vkontakte", "private": True, "value_lower": "_a_1_a_", "id": 231359},
    ],
    "login": "mikailbag",
    "id": 316218
}

STAFF_DATA_2 = {
    "department_group": {"url": "yandex_search_tech_ont_dep77192", "name": "Группа разработки СУП"},
    "work_phone": 28113,
    "official": {"is_dismissed": False},
    "accounts": [
        {"value": "kEk@kek.kek", "type": "personal_email", "private": True, "value_lower": "kek@kek.kek", "id": 66662},
        {"value": "Okay_Google", "type": "telegram", "private": False, "value_lower": "okay_google", "id": 66953}
    ],
    "login": "mickxolotl",
    "id": 39304
}

STAFF_DATA_3 = {
    "department_group": {"url": "yandex_search_tech_ont_dep77192", "name": "Группа разработки СУП"},
    "work_phone": 273,
    "official": {"is_dismissed": False},
    "accounts": [
        {"value": "pub3", "type": "telegram", "private": False, "value_lower": "pub", "id": 1},
        {"value": "priv3", "type": "telegram", "private": True, "value_lower": "priv", "id": 1}
    ],
    "login": "vasya",
    "id": 1
}

STAFF_DATA_4 = {
    "department_group": {"url": "yandex_search_tech_ont_dep77192", "name": "Группа разработки СУП"},
    "work_phone": 273,
    "official": {"is_dismissed": False},
    "accounts": [
        {"value": "priv4", "type": "telegram", "private": True, "value_lower": "priv", "id": 1}
    ],
    "login": "vasya",
    "id": 1
}

STAFF_DATA = [
    STAFF_DATA_1,
    STAFF_DATA_2,
    STAFF_DATA_3,
    STAFF_DATA_4
]


def test_reduce():
    logins, telegrams = staff_reducer(STAFF_DATA)
    assert "mikailbag" in logins
    assert logins['mikailbag']['id'] == 316218

    assert 'mikailbag' in telegrams
    assert 'MikailBag' not in telegrams
    assert telegrams['mikailbag']['id'] == 316218

    assert 'mickxolotl' in logins
    assert 'mickxolotl' not in telegrams

    assert 'okay_google' in telegrams
    assert 'okay_google' not in logins

    assert 'pub3' in telegrams
    assert 'priv3' not in telegrams

    assert 'priv4' in telegrams
