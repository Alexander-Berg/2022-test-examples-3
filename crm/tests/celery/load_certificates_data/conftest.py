import pytest


@pytest.fixture
def data_row():
    return {
        "agency_id": 1234,
        "general_curr": 6.5,
        "general_min": 5,
        "rsya_curr": 1,
        "rsya_min": 2,
        "search_curr": 3,
        "search_min": 2,
        "some_kpi_rate": 1.5,
        "agency_case_value": 1.5,
        "agency_case_threshold": 2,
        "agency_case_score": 0,
        "media_general_value": "",
        "media_general_threshold": "",
        "media_general_score": 5,
        "media_revenue_bonus_value": 1000,
        "media_revenue_bonus_threshold": 500,
        "media_revenue_bonus_score": 0,
        "agency_media_sert_value": 1000,
        "agency_media_sert_threshold": 3.4,
        "agency_media_sert_score": 0,
        "pdz_value": 0,
        "pdz_threshold": "отсутствует",
        "pdz_score": 0,
        "agency_active_clients_value": 1,
        "agency_active_clients_threshold": 1,
        "agency_metrika_sert_spec_value": 0,
        "agency_metrika_sert_spec_threshold": 0,
        "agency_direct_sert_spec_value": 18.7,
        "agency_direct_sert_spec_threshold": 14.88,
        "meta_data": [
            [
                ["label", ["some_kpi_rate"]],
                ["value_type", ["Поиск"]],
                ["max_score", ["3"]],
            ]
        ],
    }


@pytest.fixture
def data_rows(data_row):
    first_row = data_row.copy()
    second_row = data_row.copy()

    second_row["agency_id"] = 4321

    return [first_row, second_row]


@pytest.fixture
def agency_certificates():
    return [
        {
            "agency_id": 1234,
            "external_id": "some_id",
            "project": "direct",
            "confirmed_date": "2021-05-31 13:35:23+03:00",
            "due_date": "2022-05-31 13:35:23+03:00",
            "auto_prolongation": False,
        },
        {
            "agency_id": 4321,
            "external_id": "another_id",
            "project": "direct",
            "confirmed_date": "2021-05-31 13:35:23+03:00",
            "due_date": "2022-05-31 13:35:23+03:00",
            "auto_prolongation": True,
        },
    ]
