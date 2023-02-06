from rules_assortment import make_all_regions_dict, regions_mapper


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


DATETIME = '2021-01-01T20:01:01'


def test_make_all_regions_dict():
    records = [
        {
            'id': 1,
            'parents': [2, 3, 4]
        },
        {
            'id': 5,
            'parents': [1, 2, 3, 4]
        },
        {
            'id': 6,
            'parents': [3, 4]
        }

    ]
    d = make_all_regions_dict(records)
    assert len(d) == 6
    assert d[1] == set([1, 2, 3, 4, 5])
    assert d[2] == set([1, 2, 3, 4, 5])
    assert d[3] == set([1, 2, 3, 4, 5, 6])
    assert d[4] == set([1, 2, 3, 4, 5, 6])
    assert d[5] == set([1, 2, 3, 4, 5])
    assert d[6] == set([3, 4, 6])


def test_regions_mapper():
    records = [
        {
            'regions': '5 6 7',
            'delivery_days': {2: 2},
            'promo_max_delivery_days': 3,
            'promo_regions': [2],
            'promo_regions_dict': {2: [1, 2, 6]}
        }
    ]
    res = regions_mapper(records)
    assert len(list(res)) == 1

    records = [
        {
            'regions': '5 6 7',
            'delivery_days': {2: 20, 3: 1},
            'promo_max_delivery_days': 3,
            'promo_regions': [2, 3],
            'promo_regions_dict': {2: [1, 2, 6], 3: [1, 3, 7]}
        }
    ]
    res = regions_mapper(records)
    assert len(list(res)) == 1

    # too long delivery
    records = [
        {
            'regions': '5 6 7',
            'delivery_days': {2: 4},
            'promo_max_delivery_days': 3,
            'promo_regions': [2],
            'promo_regions_dict': {2: [1, 2, 6]}
        }
    ]
    res = regions_mapper(records)
    assert len(list(res)) == 0

    # no regions intersection
    records = [
        {
            'regions': '5 6 7',
            'delivery_days': {4: 1},
            'promo_max_delivery_days': 3,
            'promo_regions': [2],
            'promo_regions_dict': {2: [1, 2, 3]}
        }
    ]
    res = regions_mapper(records)
    assert len(list(res)) == 0

    # any delivery speed
    records = [
        {
            'regions': '5 6 7',
            'delivery_days': {2: 20},
            'promo_max_delivery_days': None,
            'promo_regions': [2],
            'promo_regions_dict': {2: [1, 2, 6]}
        }
    ]
    res = regions_mapper(records)
    assert len(list(res)) == 1
