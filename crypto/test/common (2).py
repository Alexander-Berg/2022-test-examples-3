from yt.yson import get_bytes

EPS = 0.0001
TEST_SEGMENT_COUNT = 300
TEST_COUNTS_TOTAL = 2972
FEATURES_MAPPING = {
    '216_648': 0, '547_1058': 1, '216_616': 2, '601_261': 3, '601_260': 4, '601_263': 5, '546_1302': 6, 'gender_0': 7,
    'gender_1': 8, 'gender_2': 9, 'age_0': 10, 'age_1': 11, 'age_2': 12, 'age_3': 13, 'age_4': 14, 'age_5': 15,
    'age_6': 16, 'income_0': 17, 'income_1': 18, 'income_2': 19, 'income_3': 20, 'income_4': 21, 'income_5': 22,
    'city_213': 23, 'city_2': 24, 'city_other': 25,
}
FEATURES_MAPPING = {get_bytes(key): value for key, value in FEATURES_MAPPING.items()}


def split_csv_pairs_generator(serialized):
    return (pair.split('_', 1) for pair in serialized.split(','))


def assert_affinities_equal(left, right):
    assert sorted(left) == sorted(right)


def assert_float_features_equal(left, right):
    assert len(left) == len(right)
    for li, ri in zip(left, right):
        assert abs(float(li) - float(ri)) <= EPS
