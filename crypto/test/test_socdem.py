import pytest

from crypta.profile.utils import socdem


@pytest.mark.parametrize('date, ref_value', [
    ('1996-04-30', 1996),
    ('1745-09-12', 1745),
    ('2001-5-6', 2001),
])
def test_get_year_from_birth_date(date, ref_value):
    assert ref_value == socdem.get_year_from_birth_date(date)


@pytest.mark.parametrize('date, current_year, ref_value', [
    ('1930-12-31', 2021, False),
    ('1931-01-1', 2021, True),
    ('2016-12-31', 2021, True),
    ('2017-01-01', 2021, False),
    ('2096-4-30', 2196, False),
    ('2116-02-29', 2196, True),
    ('1703-1-1', 1721, True),
    ('1939-9-1', 1721, False),
])
def test_is_valid_birth_date(date, current_year, ref_value):
    assert ref_value is socdem.is_valid_birth_date(date, current_year)


@pytest.mark.parametrize('age, ref_value', [
    (-1, '0_17'),  # TODO(ermolmak): original function should be updated
    (0, '0_17'),
    (5, '0_17'),
    (17, '0_17'),
    (18, '18_24'),
    (24, '18_24'),
    (25, '25_34'),
    (34, '25_34'),
    (35, '35_44'),
    (44, '35_44'),
    (45, '45_54'),
    (54, '45_54'),
    (55, '55_99'),
    (90, '55_99'),
    (99, '55_99'),
    (100, '55_99'),  # TODO(ermolmak): original function should be updated
])
def test_get_age_segment_from_age(age, ref_value):
    assert ref_value == socdem.get_age_segment_from_age(age)


@pytest.mark.parametrize('sources_by_segments, ref_value', [
    (
        {'25_34': [{'source': 'passport', 'weight': 3}]},
        {'25_34': 3},
    ),
    (
        {
            'm': [{'source': 'watch-log_vk', 'weight': 1}, {'source': 'passport', 'weight': 3}],
            'f': [{'source': 'watch-log_vk', 'weight': 1}],
        },
        {'m': 3},
    ),
    (
        {
            'B1': [{'source': 'rosbank', 'weight': 2}],
            'B2': [
                {'source': 'job_search_for_voting', 'weight': 1},
                {'source': 'job_search_for_voting', 'weight': 1},
                {'source': 'job_search_for_voting', 'weight': 1},
            ],
        },
        {'B1': 2, 'B2': 1},
    ),
    (
        {
            '35_44': [{'source': 'mobile_install_log', 'weight': 0.5547847360339225}],
            '55_99': [
                {'source': 'partner_equifax', 'weight': 1},
                {'source': 'partner_homecredit', 'weight': 1},
                {'source': 'germandb_cryptaup265', 'weight': 3},
            ],
        },
        {'35_44': 0.5547847360339225, '55_99': 5},
    ),
    (
        {
            '35_44': [
                {'source': 'socialdb_ok', 'weight': 2},
                {'source': 'peoplesearch_ok', 'weight': 2},
            ],
            '45_54': [
                {'source': 'socialdb_ok', 'weight': 2},
                {'source': 'peoplesearch_ok', 'weight': 2},
                {'source': 'peoplesearch_ok', 'weight': 2},
            ],
        },
        None,
    ),
    (
        {
            '25_34': [
                {'source': 'mobile_install_log', 'weight': 0.4904650438344575},
                {'source': 'mobile_install_log', 'weight': 0.9734043393240235},
            ],
            '35_44': [
                {'source': 'socialdb_ok', 'weight': 2},
                {'source': 'peoplesearch_ok', 'weight': 2},
            ],
            '45_54': [
                {'source': 'socialdb_ok', 'weight': 2},
                {'source': 'peoplesearch_ok', 'weight': 2},
                {'source': 'peoplesearch_ok', 'weight': 2},
            ],
        },
        {'25_34': 0.9734043393240235},
    ),
])
def test_calculate_segment_scores(sources_by_segments, ref_value):
    actual_value = socdem.calculate_segment_scores(sources_by_segments)
    assert (ref_value is None) == (actual_value is None) and \
           ref_value == actual_value
